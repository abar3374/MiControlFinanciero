package com.micontrolfinanciero.app.feature.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.finance.DEFAULT_ZONE
import com.micontrolfinanciero.app.core.domain.finance.FinanceValidator
import com.micontrolfinanciero.app.core.domain.finance.FormField
import com.micontrolfinanciero.app.core.domain.finance.IncomeInput
import com.micontrolfinanciero.app.core.domain.finance.LocalTimes
import com.micontrolfinanciero.app.core.domain.finance.MovementRepository
import com.micontrolfinanciero.app.core.model.Income
import com.micontrolfinanciero.app.core.model.IncomeDestination
import com.micontrolfinanciero.app.core.model.IncomeType
import com.micontrolfinanciero.app.core.model.MoneyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class IncomeFormState(
    val loading: Boolean = false,
    val incomeId: Long = 0L,
    val amountDigits: String = "",
    val source: String = "",
    val type: IncomeType = IncomeType.FIXED,
    val destination: IncomeDestination = IncomeDestination.ACCOUNT,
    val date: LocalDate = LocalDate.now(DEFAULT_ZONE),
    val time: LocalTime = LocalTime.now(DEFAULT_ZONE).withSecond(0).withNano(0),
    val notes: String = "",
    val errors: Map<FormField, String> = emptyMap(),
    val saving: Boolean = false,
    val confirmDelete: Boolean = false,
    val savedMessage: String? = null,
    val closed: Boolean = false,
    val error: String? = null,
) {
    val isEdit: Boolean get() = incomeId != 0L
}

@HiltViewModel
class IncomeFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val movementRepository: MovementRepository,
) : ViewModel() {

    private val editId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _state = MutableStateFlow(IncomeFormState(incomeId = editId, loading = editId != 0L))
    val state: StateFlow<IncomeFormState> = _state.asStateFlow()

    init {
        if (editId != 0L) {
            viewModelScope.launch {
                val income = movementRepository.getIncome(editId)
                if (income == null) {
                    _state.update { it.copy(loading = false, error = "Ese ingreso ya no existe.", closed = true) }
                } else {
                    val local = LocalTimes.toLocal(income.receivedAt)
                    _state.update {
                        it.copy(
                            loading = false,
                            amountDigits = (income.amount.cents / 100).toString(),
                            source = income.source,
                            type = income.type,
                            destination = income.destination,
                            date = local.toLocalDate(),
                            time = local.toLocalTime(),
                            notes = income.notes,
                        )
                    }
                }
            }
        }
    }

    fun onAmountChange(v: String) = edit { copy(amountDigits = v) }
    fun onSourceChange(v: String) = edit { copy(source = v) }
    fun onTypeChange(v: IncomeType) = edit { copy(type = v) }
    fun onDestinationChange(v: IncomeDestination) = edit { copy(destination = v) }
    fun onDateChange(v: LocalDate) = edit { copy(date = v) }
    fun onTimeChange(v: LocalTime) = edit { copy(time = v) }
    fun onNotesChange(v: String) = edit { copy(notes = v) }

    /** Atajos de origen frecuentes: llenan el campo de un toque. */
    fun onQuickSource(v: String) = edit {
        copy(source = v, type = if (v == "Salario") IncomeType.FIXED else type)
    }

    fun save() {
        val s = _state.value
        val receivedAt = LocalTimes.toMillis(s.date, s.time)
        val result = FinanceValidator.validateIncome(IncomeInput(s.amountDigits, s.source, receivedAt, s.notes))
        val clean = result.value
        if (!result.isValid || clean == null) {
            _state.update { it.copy(errors = result.errors) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                movementRepository.saveIncome(
                    Income(
                        id = s.incomeId,
                        source = clean.source,
                        amount = clean.amount,
                        receivedAt = receivedAt,
                        type = s.type,
                        destination = s.destination,
                        notes = clean.notes,
                    ),
                )
            }.onSuccess {
                val verb = if (s.isEdit) "actualizado" else "guardado"
                _state.update {
                    it.copy(saving = false, savedMessage = "Ingreso $verb: ${clean.source} · ${MoneyFormat.COLOMBIA.format(clean.amount)}")
                }
            }.onFailure {
                _state.update { it.copy(saving = false, error = "No se pudo guardar. Intenta de nuevo.") }
            }
        }
    }

    fun registerAnother() = _state.update { IncomeFormState(destination = it.destination) }

    fun askDelete() = _state.update { it.copy(confirmDelete = true) }
    fun cancelDelete() = _state.update { it.copy(confirmDelete = false) }

    fun delete() {
        val id = _state.value.incomeId
        if (id == 0L) return
        viewModelScope.launch {
            movementRepository.deleteIncome(id)
            _state.update { it.copy(confirmDelete = false, closed = true) }
        }
    }

    fun onErrorShown() = _state.update { it.copy(error = null) }

    private inline fun edit(crossinline change: IncomeFormState.() -> IncomeFormState) =
        _state.update { it.change().copy(errors = emptyMap()) }
}
