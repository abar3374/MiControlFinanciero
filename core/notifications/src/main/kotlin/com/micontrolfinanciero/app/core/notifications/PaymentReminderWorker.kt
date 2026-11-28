package com.micontrolfinanciero.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.micontrolfinanciero.app.core.database.dao.NotificationLogDao
import com.micontrolfinanciero.app.core.database.entity.NotificationLogEntity
import com.micontrolfinanciero.app.core.domain.finance.DEFAULT_ZONE
import com.micontrolfinanciero.app.core.domain.finance.FixedPaymentRepository
import com.micontrolfinanciero.app.core.domain.finance.SettingsRepository
import com.micontrolfinanciero.app.core.domain.reminders.Reminder
import com.micontrolfinanciero.app.core.domain.reminders.ReminderPlanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Revisa una vez al día los pagos pendientes y envía los recordatorios que correspondan.
 * WorkManager guarda la tarea: sigue funcionando aunque se cierre la app o se reinicie el teléfono.
 */
@HiltWorker
class PaymentReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val payments: FixedPaymentRepository,
    private val settingsRepository: SettingsRepository,
    private val logDao: NotificationLogDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.observeSettings().first()
        if (!settings.remindersEnabled || !ReminderNotifications.canNotify(applicationContext)) return Result.success()

        val today = LocalDate.now(DEFAULT_ZONE)
        val month = YearMonth.from(today)
        // Mes actual y siguiente: un pago del 2 de diciembre se avisa desde finales de noviembre.
        val items = payments.observeMonth(month) { today }.first() +
            payments.observeMonth(month.plusMonths(1)) { today }.first()

        val reminders = ReminderPlanner.plan(items, settings, today)
        val now = System.currentTimeMillis()
        reminders.forEach { reminder ->
            val inserted = logDao.insert(
                NotificationLogEntity(kind = KIND_PAYMENT, refId = reminder.paymentId, period = reminder.period, daysBefore = reminder.daysBefore, sentAt = now),
            )
            // insert devuelve -1 si ya existía: ese recordatorio ya se envió antes.
            if (inserted != -1L) ReminderNotifications.show(applicationContext, reminder)
        }
        logDao.deleteOlderThan(now - Duration.ofDays(400).toMillis())
        return Result.success()
    }

    companion object {
        const val KIND_PAYMENT = "payment"
    }
}

/** Programa la revisión diaria a la hora elegida por el usuario. */
object ReminderScheduler {
    private const val DAILY = "finance-payment-reminders"
    private const val NOW = "finance-payment-reminders-now"

    /** Revisión diaria. [replace] = true cuando el usuario cambia la hora. */
    fun scheduleDaily(context: Context, hour: Int, replace: Boolean = false) {
        val now = ZonedDateTime.now(DEFAULT_ZONE)
        var next = now.withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()
        val request = PeriodicWorkRequestBuilder<PaymentReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Revisión inmediata (al abrir la app o al crear/cambiar un pago). Sin duplicados gracias al registro. */
    fun checkNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PaymentReminderWorker>().build(),
        )
    }
}

/** Canal y aspecto de las notificaciones de recordatorio. */
object ReminderNotifications {
    const val CHANNEL_ID = "payment_reminders"

    /** Extra que indica a la app qué pantalla abrir al tocar la notificación. */
    const val EXTRA_OPEN = "finance.open"
    const val OPEN_PAYMENTS = "payments"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Recordatorios de pago", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Avisos antes de que venzan tus pagos fijos y cuotas."
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, reminder: Reminder) {
        if (!canNotify(context)) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra(EXTRA_OPEN, OPEN_PAYMENTS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        } ?: return
        val pending = PendingIntent.getActivity(
            context,
            reminder.notificationId,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // En la pantalla bloqueada no se muestran nombres ni valores (información financiera).
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_finance)
            .setContentTitle("Finance")
            .setContentText("Tienes un pago por revisar")
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_finance)
            .setContentTitle(reminder.title)
            .setContentText(reminder.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()
        // Android 13+: sin permiso de notificaciones no se intenta mostrar nada.
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(reminder.notificationId, notification)
        } catch (e: SecurityException) {
            // El usuario quitó el permiso entre la revisión y el envío: no se hace nada.
        }
    }
}
