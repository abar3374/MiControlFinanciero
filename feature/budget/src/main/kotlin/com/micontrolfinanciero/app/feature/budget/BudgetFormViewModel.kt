package com.micontrolfinanciero.app.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.finance.BudgetInput
import com.micontrolfinanciero.app.core.domain.finance.BudgetRepository
import com.micontrolfinanciero.app.core.domain.finance.CategoryRepository
import com.micontrolfinanciero.app.core.domain.finance.DEFAULT_ZONE
import com.micontrolfinanciero.app.core.domain.finance.FinanceValidator
import com.micontrolfinanciero.app.core.domain.finance.FormField
import com.micontrolfinanciero.app.core.model.Budget
import com.micontrolfinanciero.app.core.model.ExpenseCategory
import com.micontrolfinanciero.app.core.model.ExpenseFrequencyType
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.defaultClassification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BudgetFormState(
    val loading: Boolean = true,
    val budgetId: Long = 0L,
    val name: String = "",
    val amountDigits: String = "",
    val categories: List<ExpenseCategory> = emptyList(),
    val categoryId: Long? = null,
    val frequencyType: ExpenseFrequencyType = ExpenseFrequencyType.OCCASIONAL,
    val nature: ExpenseNature = ExpenseNature.NEED,
    val startDate: LocalDate = LocalDate.now(DEFAULT_ZONE).withDayOfMonth(1),
    val active: Boolean = true,
    val description: String = "",
    val notes: String = "",
    val errors: Map<FormField, String> = emptyMap(),
    val saving: Boolean = false,
    val confirmDelete: Boolean = false,
    val closed: Boolean = false,
    val error: String? = null,
) {
    val isEdit: Boolean get() = budgetId != 0L
}

@HiltViewModel
class BudgetFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val editId: Long = savedStateHandle.get<Long>("id") ?: 0L
    private val _state = MutableStateFlow(BudgetFormState(budgetId = editId))
    val state: StateFlow<BudgetFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (editId != 0L) {
                val b = budgetRepository.getBudget(editId)
                if (b == null) {
                    _state.update { it.copy(closed = true) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        name = b.name,
                        amountDigits = (b.amount.cents / 100).toString(),
                        categoryId = b.categoryId,
                        frequencyType = b.frequencyType,
                        nature = b.nature,
                        startDate = b.startDate,
                        active = b.active,
                        description = b.description,
                        notes = b.notes,
                    )
                }
            }
            categoryRepository.observeCategories().collect { cats ->
                _state.update { it.copy(loading = false, categories = cats) }
            }
        }
    }

    fun onNameChange(v: String) = edit { copy(name = v) }
    fun onAmountChange(v: String) = edit { copy(amountDigits = v) }
    fun onFrequencyChange(v: ExpenseFrequencyType) = edit { copy(frequencyType = v) }
    fun onNatureChange(v: ExpenseNature) = edit { copy(nature = v) }
    fun onStartDateChange(v: LocalDate) = edit { copy(startDate = v) }
    fun onActiveChange(v: Boolean) = edit { copy(active = v) }
    fun onDescriptionChange(v: String) = edit { copy(description = v) }
    fun onNotesChange(v: String) = edit { copy(notes = v) }

    fun onCategoryChange(id: Long) {
        val category = _state.value.categories.firstOrNull { it.id == id }
        edit {
            copy(
                categoryId = id,
                nature = category?.nature?.defaultClassification() ?: nature,
                // Si aún no tiene nombre, se propone el de la categoría.
                name = name.ifBlank { category?.name.orEmpty() },
            )
        }
    }

    fun createCategory(name: String, nature: ExpenseNature) {
        viewModelScope.launch {
            runCatching { categoryRepository.addCategory(name, nature) }
                .onSuccess { id ->
                    edit { copy(categoryId = id, nature = nature.defaultClassification(), name = this.name.ifBlank { name }) }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo crear la categoría.") } }
        }
    }

    fun save() {
        val s = _state.value
        val result = FinanceValidator.validateBudget(BudgetInput(s.name, s.amountDigits, s.categoryId, s.description, s.notes))
        val clean = result.value
        if (!result.isValid || clean == null) {
            _state.update { it.copy(errors = result.errors) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                budgetRepository.saveBudget(
                    Budget(
                        id = s.budgetId,
                        name = clean.name,
                        description = clean.description,
                        categoryId = clean.categoryId,
                        amount = clean.amount,
                        frequencyType = s.frequencyType,
                        nature = s.nature,
                        startDate = s.startDate,
                        active = s.active,
                        notes = clean.notes,
                    ),
                )
            }.onSuccess {
                _state.update { it.copy(saving = false, closed = true) }
            }.onFailure {
                _state.update { it.copy(saving = false, error = "No se pudo guardar el rubro. Intenta de nuevo.") }
            }
        }
    }

    fun askDelete() = _state.update { it.copy(confirmDelete = true) }
    fun cancelDelete() = _state.update { it.copy(confirmDelete = false) }

    fun delete() {
        viewModelScope.launch {
            budgetRepository.deleteBudget(_state.value.budgetId)
            _state.update { it.copy(confirmDelete = false, closed = true) }
        }
    }

    fun onErrorShown() = _state.update { it.copy(error = null) }

    private inline fun edit(crossinline change: BudgetFormState.() -> BudgetFormState) =
        _state.update { it.change().copy(errors = emptyMap()) }
}
