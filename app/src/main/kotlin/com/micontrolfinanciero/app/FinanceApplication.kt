package com.micontrolfinanciero.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.micontrolfinanciero.app.core.notifications.ReminderNotifications
import com.micontrolfinanciero.app.core.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Punto de arranque de la app. @HiltAndroidApp prepara la inyección de dependencias.
 *
 * Aquí se crea el canal de notificaciones y se programa la revisión diaria de pagos
 * (WorkManager la conserva aunque se reinicie el teléfono).
 */
@HiltAndroidApp
class FinanceApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannel(this)
        // KEEP: si ya estaba programada, no la cambia. La hora se ajusta desde Perfil.
        ReminderScheduler.scheduleDaily(this, hour = 8)
        ReminderScheduler.checkNow(this)
    }
}
