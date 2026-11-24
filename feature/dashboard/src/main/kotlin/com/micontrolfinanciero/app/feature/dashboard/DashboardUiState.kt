package com.micontrolfinanciero.app.feature.dashboard

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Todo lo que la pantalla Inicio necesita para dibujarse. Es inmutable:
 * para cambiar algo, el ViewModel crea un estado nuevo.
 *
 * Fase 1: saludo y fecha reales. Las tarjetas de dinero llegan en la Fase 4,
 * cuando existan gastos e ingresos guardados.
 */
data class DashboardUiState(
    val greeting: String,
    val formattedDate: String,
    /** Nombre del perfil. Null mientras no se haya configurado (Fase 3). */
    val userName: String?,
) {
    companion object {
        private val SPANISH = Locale.forLanguageTag("es-CO")
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", SPANISH)

        fun from(now: ZonedDateTime, userName: String? = null): DashboardUiState =
            DashboardUiState(
                greeting = greetingFor(now.hour),
                formattedDate = now.format(DATE_FORMAT).replaceFirstChar { it.titlecase(SPANISH) },
                userName = userName,
            )

        /** 5:00–11:59 días, 12:00–18:59 tardes, el resto noches. */
        fun greetingFor(hour: Int): String = when (hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
}
