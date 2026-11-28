package com.micontrolfinanciero.app.feature.credit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.designsystem.component.BackTopBar
import com.micontrolfinanciero.app.core.designsystem.component.ChipOption
import com.micontrolfinanciero.app.core.designsystem.component.ChoiceChips
import com.micontrolfinanciero.app.core.designsystem.component.ConfirmDeleteDialog
import com.micontrolfinanciero.app.core.designsystem.component.DateTimeRow
import com.micontrolfinanciero.app.core.designsystem.component.FieldLabel
import com.micontrolfinanciero.app.core.designsystem.component.FinanceFormat
import com.micontrolfinanciero.app.core.designsystem.component.FinanceTextField
import com.micontrolfinanciero.app.core.designsystem.component.MoneyField
import com.micontrolfinanciero.app.core.designsystem.component.PrimaryActionButton
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.finance.DEFAULT_ZONE
import com.micontrolfinanciero.app.core.domain.finance.ExpectedIncomeInput
import com.micontrolfinanciero.app.core.domain.finance.FinanceValidator
import com.micontrolfinanciero.app.core.domain.finance.FixedPaymentRepository
import com.micontrolfinanciero.app.core.domain.finance.FormField
import com.micontrolfinanciero.app.core.model.ExpectedIncome
import com.micontrolfinanciero.app.core.model.IncomeDestination
import com.micontrolfinanciero.app.core.model.MonthInterval
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

data class ExpectedIncomeFormState(
    val loading: Boolean = false,
    val id: Long = 0L,
    val source: String = "Salario",
    val amountDigits: String = "",
    val dayText: String = "30",
    val interval: MonthInterval = MonthInterval.MONTHLY,
    val destination: IncomeDestination = IncomeDestination.ACCOUNT,
    val startMonth: YearMonth = YearMonth.now(DEFAULT_ZONE),
    val notes: String = "",
    val errors: Map<FormField, String> = emptyMap(),
    val saving: Boolean = false,
    val confirmDelete: Boolean = false,
    val closed: Boolean = false,
) {
    val isEdit: Boolean get() = id != 0L
}

/** Ingreso fijo esperado: el salario u otro ingreso que llega cada periodo. */
@HiltViewModel
class ExpectedIncomeFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FixedPaymentRepository,
) : ViewModel() {

    private val editId: Long = savedStateHandle.get<Long>("id") ?: 0L
    private val _state = MutableStateFlow(ExpectedIncomeFormState(id = editId, loading = editId != 0L))
    val state: StateFlow<ExpectedIncomeFormState> = _state.asStateFlow()

    init {
        if (editId != 0L) {
            viewModelScope.launch {
                val e = repository.getExpectedIncome(editId)
                if (e == null) {
                    _state.update { it.copy(closed = true) }
                } else {
                    _state.update {
                        it.copy(
                            loading = false,
                            source = e.source,
                            amountDigits = (e.amount.cents / 100).toString(),
                            dayText = e.day.toString(),
                            interval = MonthInterval.of(e.intervalMonths),
                            destination = e.destination,
                            startMonth = e.startMonth,
                            notes = e.notes,
                        )
                    }
                }
            }
        }
    }

    fun onSource(v: String) = edit { copy(source = v) }
    fun onAmount(v: String) = edit { copy(amountDigits = v) }
    fun onDay(v: String) = edit { copy(dayText = v.filter { it.isDigit() }.take(2)) }
    fun onInterval(v: MonthInterval) = edit { copy(interval = v) }
    fun onDestination(v: IncomeDestination) = edit { copy(destination = v) }
    fun onStart(v: LocalDate) = edit { copy(startMonth = YearMonth.from(v)) }
    fun onNotes(v: String) = edit { copy(notes = v) }

    fun save() {
        val s = _state.value
        val result = FinanceValidator.validateExpectedIncome(ExpectedIncomeInput(s.source, s.amountDigits, s.dayText, s.notes))
        val clean = result.value
        if (!result.isValid || clean == null) {
            _state.update { it.copy(errors = result.errors) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.saveExpectedIncome(
                ExpectedIncome(
                    id = s.id,
                    source = clean.source,
                    amount = clean.amount,
                    day = clean.day,
                    intervalMonths = s.interval.months,
                    destination = s.destination,
                    startMonth = s.startMonth,
                    notes = clean.notes,
                    active = true,
                ),
            )
            _state.update { it.copy(saving = false, closed = true) }
        }
    }

    fun askDelete() = _state.update { it.copy(confirmDelete = true) }
    fun cancelDelete() = _state.update { it.copy(confirmDelete = false) }
    fun delete() {
        viewModelScope.launch {
            repository.deactivateExpectedIncome(_state.value.id)
            _state.update { it.copy(confirmDelete = false, closed = true) }
        }
    }

    private inline fun edit(crossinline change: ExpectedIncomeFormState.() -> ExpectedIncomeFormState) =
        _state.update { it.change().copy(errors = emptyMap()) }
}

@Composable
fun ExpectedIncomeFormRoute(onDone: () -> Unit, viewModel: ExpectedIncomeFormViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) { if (state.closed) onDone() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.loading) {
            CircularProgressIndicator(color = FinanceColors.Green, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            BackTopBar(
                title = if (state.isEdit) "Editar ingreso fijo" else "Nuevo ingreso fijo",
                onBack = onDone,
                actionIcon = if (state.isEdit) DesignR.drawable.ic_delete else null,
                actionDescription = "Quitar ingreso fijo",
                onAction = if (state.isEdit) viewModel::askDelete else null,
            )
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FinanceTextField(
                    value = state.source,
                    onValueChange = viewModel::onSource,
                    label = "¿De dónde viene? *",
                    error = state.errors[FormField.SOURCE],
                    supporting = "Ejemplo: Salario, pensión, arriendo que recibes.",
                )
                MoneyField(digits = state.amountDigits, onDigitsChange = viewModel::onAmount, label = "Valor esperado", error = state.errors[FormField.AMOUNT])
                OutlinedTextField(
                    value = state.dayText,
                    onValueChange = viewModel::onDay,
                    label = { Text("Día en que llega (1 a 31) *") },
                    isError = state.errors[FormField.DUE_DAY] != null,
                    supportingText = if (state.errors[FormField.DUE_DAY] != null) {
                        { Text(state.errors[FormField.DUE_DAY].orEmpty()) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                FieldLabel("Frecuencia")
                ChoiceChips(options = MonthInterval.entries.map { ChipOption(it, it.label) }, selected = state.interval, onSelect = viewModel::onInterval)
                FieldLabel("Se recibe en")
                ChoiceChips(options = IncomeDestination.entries.map { ChipOption(it, it.label) }, selected = state.destination, onSelect = viewModel::onDestination)
                FieldLabel("Empieza en el mes de")
                DateTimeRow(
                    date = state.startMonth.atDay(1),
                    time = LocalTime.NOON,
                    dateText = FinanceFormat.month(state.startMonth),
                    timeText = "",
                    onDateChange = viewModel::onStart,
                    onTimeChange = {},
                    error = null,
                    showTime = false,
                )
                FinanceTextField(value = state.notes, onValueChange = viewModel::onNotes, label = "Observaciones (opcional)", singleLine = false, maxLength = 300, imeAction = ImeAction.Default)
                Text(
                    "Cada mes aparecerá como «Esperado». Cuando llegue, lo confirmas y se registra como ingreso; esperado no es lo mismo que recibido.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.MediumGray,
                )
            }
            PrimaryActionButton(
                text = if (state.isEdit) "Guardar cambios" else "Guardar ingreso fijo",
                onClick = viewModel::save,
                busy = state.saving,
                color = FinanceColors.Green,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
    if (state.confirmDelete) {
        ConfirmDeleteDialog(
            title = "¿Quitar este ingreso fijo?",
            message = "Dejará de aparecer cada mes. Los ingresos que ya confirmaste se conservan.",
            onConfirm = viewModel::delete,
            onDismiss = viewModel::cancelDelete,
        )
    }
}
