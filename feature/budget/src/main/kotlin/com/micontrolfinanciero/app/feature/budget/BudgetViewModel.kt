package com.micontrolfinanciero.app.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.finance.CategoryRepository
import com.micontrolfinanciero.app.core.domain.finance.FinanceValidator
import com.micontrolfinanciero.app.core.domain.finance.MonthRange
import com.micontrolfinanciero.app.core.domain.finance.MovementRepository
import com.micontrolfinanciero.app.core.domain.finance.BudgetRepository
import com.micontrolfinanciero.app.core.model.Budget
import com.micontrolfinanciero.app.core.model.ExpenseCategory
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.MonthSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetScreenState(
    val loading: Boolean = true,
    val range: MonthRange = MonthRange.current(),
    val summary: MonthSummary? = null,
    /** Rubros inactivos (no cuentan en el mes) para poder reactivarlos. */
    val inactiveBudgets: List<Budget> = emptyList(),
    val categories: List<ExpenseCategory> = emptyList(),
)

/** Diálogo abierto en Presupuesto: crear una categoría o administrar una existente. */
sealed interface CategoryDialog {
    data object None : CategoryDialog
    data object Create : CategoryDialog
    data class Manage(val category: ExpenseCategory) : CategoryDialog
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val movementRepository: MovementRepository,
    budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val range = MutableStateFlow(MonthRange.current())

    val state: StateFlow<BudgetScreenState> = combine(
        range,
        range.flatMapLatest { movementRepository.observeMonthSummary(it) },
        budgetRepository.observeBudgets(),
        categoryRepository.observeAllCategories(),
    ) { r, summary, budgets, categories ->
        BudgetScreenState(
            loading = false,
            range = r,
            summary = summary,
            inactiveBudgets = budgets.filter { !it.active },
            categories = categories,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetScreenState())

    private val _dialog = MutableStateFlow<CategoryDialog>(CategoryDialog.None)
    val dialog: StateFlow<CategoryDialog> = _dialog

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun previousMonth() { range.value = range.value.previous() }
    fun nextMonth() { range.value = range.value.next() }

    fun openCreateCategory() { _dialog.value = CategoryDialog.Create }
    fun openManageCategory(category: ExpenseCategory) { _dialog.value = CategoryDialog.Manage(category) }
    fun closeDialog() { _dialog.value = CategoryDialog.None }
    fun onMessageShown() { _message.value = null }

    /** Error del nombre, comparando con las demás categorías (sin contar la que se renombra). */
    fun nameError(name: String, exceptId: Long? = null): String? =
        FinanceValidator.categoryNameError(name, state.value.categories.filter { it.id != exceptId && !it.archived }.map { it.name })

    fun createCategory(name: String, nature: ExpenseNature) = perform("Categoría creada.") {
        categoryRepository.addCategory(name, nature)
    }

    fun renameCategory(id: Long, name: String) = perform("Nombre actualizado.") {
        categoryRepository.renameCategory(id, name)
    }

    fun setHidden(id: Long, hidden: Boolean) = perform(if (hidden) "Categoría oculta. Sus gastos se conservan." else "Categoría visible de nuevo.") {
        categoryRepository.setCategoryHidden(id, hidden)
    }

    private fun perform(success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _message.value = success; closeDialog() }
                .onFailure { e -> _message.value = e.message ?: "No se pudo guardar." }
        }
    }
}
