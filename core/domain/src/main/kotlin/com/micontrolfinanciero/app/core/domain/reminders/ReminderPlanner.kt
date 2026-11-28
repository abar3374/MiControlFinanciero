package com.micontrolfinanciero.app.core.domain.reminders

import com.micontrolfinanciero.app.core.model.AppSettings
import com.micontrolfinanciero.app.core.model.MonthlyPayment
import com.micontrolfinanciero.app.core.model.MoneyFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Un recordatorio por enviar. [daysBefore] = -1 significa "ya venció". */
data class Reminder(
    val paymentId: Long,
    val period: Int,
    val daysBefore: Int,
    val title: String,
    val text: String,
) {
    /** Identificador estable: el mismo recordatorio siempre usa la misma notificación. */
    val notificationId: Int get() = (paymentId * 1_000_003L + period * 101L + daysBefore).hashCode()
}

/**
 * Decide qué recordatorios enviar hoy (sección 10 del proyecto).
 *
 * Para cada pago pendiente se toma el aviso configurado más cercano que ya se alcanzó
 * (por ejemplo, si faltan 2 días y hay avisos de 7, 3 y 1, corresponde el de 3). Así, si el
 * teléfono estuvo apagado el día exacto, el aviso llega igual al día siguiente, y como cada
 * aviso queda registrado (pago, mes, días antes), nunca se envía dos veces.
 * Si el pago ya venció y sigue pendiente, se envía un único aviso de "vencido".
 */
object ReminderPlanner {

    const val OVERDUE = -1
    private val locale = Locale.forLanguageTag("es-CO")
    private val dayFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", locale)

    fun plan(
        payments: List<MonthlyPayment>,
        settings: AppSettings,
        today: LocalDate,
        money: MoneyFormat = MoneyFormat.COLOMBIA,
    ): List<Reminder> {
        if (!settings.remindersEnabled) return emptyList()
        val offsets = settings.allReminderDays.filter { it >= 0 }.sorted()
        if (offsets.isEmpty()) return emptyList()
        return payments
            .filter { !it.isPaid }
            .distinctBy { it.payment.id to it.month }
            .mapNotNull { item ->
                val daysLeft = ChronoUnit.DAYS.between(today, item.dueDate).toInt()
                val period = item.month.year * 100 + item.month.monthValue
                val amount = money.format(item.payment.amount)
                val name = item.payment.name
                val date = item.dueDate.format(dayFormat)
                when {
                    daysLeft < 0 -> Reminder(
                        paymentId = item.payment.id,
                        period = period,
                        daysBefore = OVERDUE,
                        title = "Pago vencido: $name",
                        text = "$name por $amount venció el $date y sigue pendiente.",
                    )
                    else -> {
                        // El aviso más cercano que ya se alcanzó (offset >= días que faltan).
                        val offset = offsets.firstOrNull { it >= daysLeft } ?: return@mapNotNull null
                        val title = when (daysLeft) {
                            0 -> "Hoy vence $name"
                            1 -> "Mañana vence $name"
                            else -> "En $daysLeft días vence $name"
                        }
                        Reminder(
                            paymentId = item.payment.id,
                            period = period,
                            daysBefore = offset,
                            title = title,
                            text = "Recordatorio: ${title.replaceFirstChar { it.lowercase() }} por $amount ($date).",
                        )
                    }
                }
            }
    }
}
