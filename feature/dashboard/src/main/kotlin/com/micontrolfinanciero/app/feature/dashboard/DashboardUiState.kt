package com.micontrolfinanciero.app.feature.dashboard

import com.micontrolfinanciero.app.core.model.MonthSummary
import com.micontrolfinanciero.app.core.model.MonthlyPayment
import com.micontrolfinanciero.app.core.model.MovementItem
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Todo lo que la pantalla Inicio necesita para dibujarse. Es inmutable:
 * para cambiar algo, el ViewModel crea un estado nuevo.
 *
 * Saludo, fecha y nombre; resumen del mes y últimos movimientos, todo desde la base de datos.
 */
data class DashboardUiState(
    val greeting: String,
    val formattedDate: String,
    /** Nombre del perfil. Null mientras no se haya configurado. */
    val userName: String?,
    /** Resumen del mes actual (null mientras carga). */
    val summary: MonthSummary? = null,
    val recent: List<MovementItem> = emptyList(),
    /** Pagos fijos del mes (pendientes primero). */
    val payments: List<MonthlyPayment> = emptyList(),
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
