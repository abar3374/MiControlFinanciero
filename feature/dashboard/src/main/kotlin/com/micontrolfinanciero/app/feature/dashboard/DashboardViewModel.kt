package com.micontrolfinanciero.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.finance.FixedPaymentRepository
import com.micontrolfinanciero.app.core.domain.finance.MonthRange
import com.micontrolfinanciero.app.core.domain.finance.MovementRepository
import com.micontrolfinanciero.app.core.domain.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Lógica de la pantalla Inicio. Todo se recalcula solo cuando se registra, edita o
 * elimina un movimiento (Room avisa por Flow).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    profileRepository: UserProfileRepository,
    movementRepository: MovementRepository,
    fixedPaymentRepository: FixedPaymentRepository,
) : ViewModel() {

    /** Cambia al volver a la app: actualiza saludo, fecha y, si cambió el mes, el resumen. */
    private val clock = MutableStateFlow(ZonedDateTime.now(DEFAULT_ZONE))

    val uiState: StateFlow<DashboardUiState> = combine(
        clock,
        profileRepository.observeProfile(),
        clock.flatMapLatest { movementRepository.observeMonthSummary(MonthRange.current(it.toInstant().toEpochMilli())) },
        movementRepository.observeRecent(limit = 5),
        clock.flatMapLatest { now -> fixedPaymentRepository.observeMonth(YearMonth.from(now)) { now.toLocalDate() } },
    ) { now, profile, summary, recent, payments ->
        DashboardUiState.from(now, profile.firstName.ifBlank { null })
            .copy(summary = summary, recent = recent, payments = payments)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardUiState.from(ZonedDateTime.now(DEFAULT_ZONE)),
    )

    fun refreshClock() {
        val now = ZonedDateTime.now(DEFAULT_ZONE)
        val old = clock.value
        // Solo emite si cambió el minuto, para no recalcular sin necesidad.
        if (now.withSecond(0).withNano(0) != old.withSecond(0).withNano(0)) clock.value = now
    }

    private companion object {
        val DEFAULT_ZONE: ZoneId = ZoneId.of("America/Bogota")
    }
}
