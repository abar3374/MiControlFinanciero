package com.micontrolfinanciero.app.feature.credit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.finance.BudgetRepository
import com.micontrolfinanciero.app.core.domain.finance.CategoryRepository
import com.micontrolfinanciero.app.core.domain.finance.DEFAULT_ZONE
import com.micontrolfinanciero.app.core.domain.finance.FinanceValidator
import com.micontrolfinanciero.app.core.domain.finance.FixedPaymentInput
import com.micontrolfinanciero.app.core.domain.finance.FixedPaymentRepository
import com.micontrolfinanciero.app.core.domain.finance.FormField
import com.micontrolfinanciero.app.core.model.Budget
import com.micontrolfinanciero.app.core.model.ExpenseCategory
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.FixedPayment
import com.micontrolfinanciero.app.core.model.PaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class FixedPaymentFormState(
    val loading: Boolean = true,
    val paymentId: Long = 0L,
    val name: String = "",
    val amountDigits: String = "",
    val dueDayText: String = "",
    val categories: List<ExpenseCategory> = emptyList(),
    val categoryId: Long? = null,
    val budgetsForCategory: List<Budget> = emptyList(),
    val budgetId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.TRANSFER,
    val isCredit: Boolean = false,
    val totalInstallmentsText: String = "",
    val paidInstallmentsText: String = "",
    val startMonth: YearMonth = YearMonth.now(DEFAULT_ZONE),
    val notes: String = "",
    val errors: Map<FormField, String> = emptyMap(),
    val saving: Boolean = false,
    val confirmDelete: Boolean = false,
    val closed: Boolean = false,
    val error: String? = null,
) {
    val isEdit: Boolean get() = paymentId != 0L
}

@HiltViewModel
class FixedPaymentFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FixedPaymentRepository,
    private val categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository,
) : ViewModel() {

    private val editId: Long = savedStateHandle.get<Long>("id") ?: 0L
    private val _state = MutableStateFlow(FixedPaymentFormState(paymentId = editId))
    val state: StateFlow<FixedPaymentFormState> = _state.asStateFlow()
    private var activeBudgets: List<Budget> = emptyList()

    init {
        viewModelScope.launch {
            if (editId != 0L) {
                val p = repository.getFixedPayment(editId)
                if (p == null) {
                    _state.update { it.copy(closed = true) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        name = p.name,
                        amountDigits = (p.amount.cents / 100).toString(),
                        dueDayText = p.dueDay.toString(),
                        categoryId = p.categoryId,
                        budgetId = p.budgetId,
                        paymentMethod = p.paymentMethod,
                        isCredit = p.isCredit,
                        totalInstallmentsText = p.totalInstallments?.toString().orEmpty(),
                        paidInstallmentsText = p.initialPaidInstallments.toString(),
                        startMonth = p.startMonth,
                        notes = p.notes,
                    )
                }
            }
            combine(categoryRepository.observeCategories(), budgetRepository.observeBudgets()) { c, b -> c to b }
                .collect { (cats, budgets) ->
                    activeBudgets = budgets.filter { it.active }
                    _state.update { s ->
                        s.copy(
                            loading = false,
                            categories = cats,
                            budgetsForCategory = activeBudgets.filter { it.categoryId == s.categoryId },
                        )
                    }
                }
        }
    }

    fun onNameChange(v: String) = edit { copy(name = v) }
    fun onAmountChange(v: String) = edit { copy(amountDigits = v) }
    fun onDueDayChange(v: String) = edit { copy(dueDayText = v.filter { it.isDigit() }.take(2)) }
    fun onMethodChange(v: PaymentMethod) = edit { copy(paymentMethod = v) }
    fun onCreditChange(v: Boolean) = edit { copy(isCredit = v) }
    fun onTotalChange(v: String) = edit { copy(totalInstallmentsText = v.filter { it.isDigit() }.take(3)) }
    fun onPaidChange(v: String) = edit { copy(paidInstallmentsText = v.filter { it.isDigit() }.take(3)) }
    fun onStartDateChange(v: LocalDate) = edit { copy(startMonth = YearMonth.from(v)) }
    fun onNotesChange(v: String) = edit { copy(notes = v) }
    fun onBudgetChange(v: Long?) = edit { copy(budgetId = v) }

    fun onCategoryChange(id: Long) {
        val budgets = activeBudgets.filter { it.categoryId == id }
        edit { copy(categoryId = id, budgetsForCategory = budgets, budgetId = budgets.firstOrNull()?.id) }
    }

    fun createCategory(name: String, nature: ExpenseNature) {
        viewModelScope.launch {
            runCatching { categoryRepository.addCategory(name, nature) }
                .onSuccess { id -> edit { copy(categoryId = id, budgetsForCategory = emptyList(), budgetId = null) } }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo crear la categoría.") } }
        }
    }

    fun save() {
        val s = _state.value
        val result = FinanceValidator.validateFixedPayment(
            FixedPaymentInput(s.name, s.amountDigits, s.dueDayText, s.categoryId, s.isCredit, s.totalInstallmentsText, s.paidInstallmentsText, s.notes),
        )
        val clean = result.value
        if (!result.isValid || clean == null) {
            _state.update { it.copy(errors = result.errors) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                repository.saveFixedPayment(
                    FixedPayment(
                        id = s.paymentId,
                        name = clean.name,
                        categoryId = clean.categoryId,
                        budgetId = s.budgetId?.takeIf { id -> s.budgetsForCategory.any { it.id == id } },
                        amount = clean.amount,
                        dueDay = clean.dueDay,
                        totalInstallments = clean.totalInstallments,
                        initialPaidInstallments = clean.initialPaidInstallments,
                        startMonth = s.startMonth,
                        paymentMethod = s.paymentMethod,
                        notes = clean.notes,
                        active = true,
                    ),
                )
            }.onSuccess {
                _state.update { it.copy(saving = false, closed = true) }
            }.onFailure {
                _state.update { it.copy(saving = false, error = "No se pudo guardar. Intenta de nuevo.") }
            }
        }
    }

    fun askDelete() = _state.update { it.copy(confirmDelete = true) }
    fun cancelDelete() = _state.update { it.copy(confirmDelete = false) }

    fun delete() {
        viewModelScope.launch {
            repository.deactivate(_state.value.paymentId)
            _state.update { it.copy(confirmDelete = false, closed = true) }
        }
    }

    fun onErrorShown() = _state.update { it.copy(error = null) }

    private inline fun edit(crossinline change: FixedPaymentFormState.() -> FixedPaymentFormState) =
        _state.update { it.change().copy(errors = emptyMap()) }
}
