package com.micontrolfinanciero.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Lógica de la pantalla Inicio. La pantalla solo observa [uiState].
 *
 * Saludo y fecha con la zona America/Bogota; el nombre sale del perfil guardado.
 * En la Fase 4 se agregan los totales del mes desde la base de datos.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    profileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState.from(ZonedDateTime.now(DEFAULT_ZONE)))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var userName: String? = null

    init {
        viewModelScope.launch {
            profileRepository.observeProfile().collect { profile ->
                userName = profile.firstName.ifBlank { null }
                refreshClock()
            }
        }
    }

    /** Recalcula saludo y fecha (por ejemplo, al volver a la app después de horas). */
    fun refreshClock() {
        _uiState.value = DashboardUiState.from(ZonedDateTime.now(DEFAULT_ZONE), userName)
    }

    private companion object {
        val DEFAULT_ZONE: ZoneId = ZoneId.of("America/Bogota")
    }
}
