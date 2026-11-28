package com.micontrolfinanciero.app.core.model

/** Canal de actualizaciones (Fase 9). */
enum class UpdateChannel(val label: String) { STABLE("Estable"), BETA("Pruebas") }

/**
 * Ajustes de la app que el usuario puede cambiar. Se guardan cifrados en la base de datos
 * (tabla app_setting). Los valores por defecto son los del proyecto.
 */
data class AppSettings(
    /** Recordatorios de pagos activados. */
    val remindersEnabled: Boolean = true,
    /** Días antes del vencimiento en que se avisa (7, 3, 1 y el mismo día = 0). */
    val reminderDays: Set<Int> = DEFAULT_REMINDER_DAYS,
    /** Recordatorio personalizado adicional (días antes), o null. */
    val customReminderDays: Int? = null,
    /** Hora del día (0–23) en que se revisan los vencimientos. */
    val reminderHour: Int = 8,
    /** Minutos fuera de la app antes de pedir contraseña o huella (0 = siempre). */
    val autoLockMinutes: Int = 1,
    /** Impide capturas de pantalla y oculta la app en "recientes". */
    val secureScreen: Boolean = true,
    val updateChannel: UpdateChannel = UpdateChannel.STABLE,
) {
    /** Todos los días de aviso, incluido el personalizado, sin repetir. */
    val allReminderDays: Set<Int> get() = reminderDays + listOfNotNull(customReminderDays)

    companion object {
        val DEFAULT_REMINDER_DAYS = setOf(7, 3, 1, 0)
        val AUTO_LOCK_OPTIONS = listOf(0, 1, 5, 15)
    }
}
