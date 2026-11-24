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
import kotlinx.coroutines.flow.update
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

data class CategoryDialogState(
    val visible: Boolean = false,
    val name: String = "",
    val nature: ExpenseNature = ExpenseNature.OTHER,
    val error: String? = null,
)

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
        categoryRepository.observeCategories(),
    ) { r, summary, budgets, categories ->
        BudgetScreenState(
            loading = false,
            range = r,
            summary = summary,
            inactiveBudgets = budgets.filter { !it.active },
            categories = categories,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetScreenState())

    private val _dialog = MutableStateFlow(CategoryDialogState())
    val dialog: StateFlow<CategoryDialogState> = _dialog

    fun previousMonth() { range.value = range.value.previous() }
    fun nextMonth() { range.value = range.value.next() }

    fun openCategoryDialog() { _dialog.value = CategoryDialogState(visible = true) }
    fun closeCategoryDialog() { _dialog.value = CategoryDialogState() }
    fun onCategoryNameChange(v: String) = _dialog.update { it.copy(name = v.take(FinanceValidator.NAME_MAX), error = null) }
    fun onCategoryNatureChange(v: ExpenseNature) = _dialog.update { it.copy(nature = v) }

    fun saveCategory() {
        val d = _dialog.value
        val error = FinanceValidator.categoryNameError(d.name, state.value.categories.map { it.name })
        if (error != null) {
            _dialog.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            runCatching { categoryRepository.addCategory(d.name, d.nature) }
                .onSuccess { closeCategoryDialog() }
                .onFailure { _dialog.update { it.copy(error = "Ya existe una categoría con ese nombre.") } }
        }
    }
}
