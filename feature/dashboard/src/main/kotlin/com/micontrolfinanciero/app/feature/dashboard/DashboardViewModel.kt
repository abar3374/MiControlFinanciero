package com.micontrolfinanciero.app.feature.dashboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Lógica de la pantalla Inicio. La pantalla solo observa [uiState].
 *
 * Fase 1: calcula el saludo y la fecha con la zona horaria America/Bogota.
 * En la Fase 3 la zona y el nombre vendrán de AppSettings y UserProfile, y en la
 * Fase 4 se agregarán los totales del mes desde la base de datos (Flow de Room).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState.from(ZonedDateTime.now(DEFAULT_ZONE)))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /** Recalcula saludo y fecha (por ejemplo, al volver a la app después de horas). */
    fun refreshClock() {
        _uiState.value = DashboardUiState.from(ZonedDateTime.now(DEFAULT_ZONE), _uiState.value.userName)
    }

    private companion object {
        val DEFAULT_ZONE: ZoneId = ZoneId.of("America/Bogota")
    }
}
