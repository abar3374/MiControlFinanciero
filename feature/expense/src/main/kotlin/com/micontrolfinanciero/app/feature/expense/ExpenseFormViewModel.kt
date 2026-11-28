package com.micontrolfinanciero.app.feature.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.budget.BudgetMath
import com.micontrolfinanciero.app.core.domain.finance.AttachmentRepository
import com.micontrolfinanciero.app.core.domain.finance.BudgetRepository
import com.micontrolfinanciero.app.core.domain.finance.CategoryRepository
import com.micontrolfinanciero.app.core.domain.finance.DEFAULT_ZONE
import com.micontrolfinanciero.app.core.domain.finance.ExpenseInput
import com.micontrolfinanciero.app.core.domain.finance.FinanceValidator
import com.micontrolfinanciero.app.core.domain.finance.FormField
import com.micontrolfinanciero.app.core.domain.finance.LocalTimes
import com.micontrolfinanciero.app.core.domain.finance.MonthRange
import com.micontrolfinanciero.app.core.domain.finance.MovementRepository
import com.micontrolfinanciero.app.core.model.Attachment
import com.micontrolfinanciero.app.core.model.Budget
import com.micontrolfinanciero.app.core.model.Expense
import com.micontrolfinanciero.app.core.model.ExpenseCategory
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.MoneyFormat
import com.micontrolfinanciero.app.core.model.PaymentMethod
import com.micontrolfinanciero.app.core.model.defaultClassification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

data class ExpenseFormState(
    val loading: Boolean = true,
    val expenseId: Long = 0L,
    val amountDigits: String = "",
    val concept: String = "",
    val place: String = "",
    val categories: List<ExpenseCategory> = emptyList(),
    val categoryId: Long? = null,
    /** Rubros activos de la categoría elegida. */
    val budgetsForCategory: List<Budget> = emptyList(),
    val budgetId: Long? = null,
    /** true si el usuario eligió el rubro a mano (no se cambia solo). */
    val budgetChosenByUser: Boolean = false,
    val nature: ExpenseNature = ExpenseNature.NEED,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val date: LocalDate = LocalDate.now(DEFAULT_ZONE),
    val time: LocalTime = LocalTime.now(DEFAULT_ZONE).withSecond(0).withNano(0),
    val description: String = "",
    val errors: Map<FormField, String> = emptyMap(),
    val saving: Boolean = false,
    val confirmDelete: Boolean = false,
    /** No nulo cuando se guardó: muestra la confirmación. */
    val saved: SavedResult? = null,
    /** true cuando el gasto fue eliminado: la pantalla se cierra. */
    val closed: Boolean = false,
    val error: String? = null,
    /** Comprobantes ya guardados (al editar). */
    val attachments: List<Attachment> = emptyList(),
    /** Comprobantes elegidos para un gasto nuevo: se guardan al guardar el gasto. */
    val pendingAttachments: List<PendingAttachment> = emptyList(),
    val attaching: Boolean = false,
) {
    val isEdit: Boolean get() = expenseId != 0L
}

data class PendingAttachment(val uri: String, val name: String, val mimeType: String)

data class SavedResult(val message: String, val alert: String?)

@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val movementRepository: MovementRepository,
    private val attachmentRepository: AttachmentRepository,
) : ViewModel() {

    private val editId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _state = MutableStateFlow(ExpenseFormState(expenseId = editId))
    val state: StateFlow<ExpenseFormState> = _state.asStateFlow()

    private var allBudgets: List<Budget> = emptyList()
    private val money = MoneyFormat.COLOMBIA

    init {
        if (editId != 0L) {
            viewModelScope.launch {
                attachmentRepository.observeForExpense(editId).collect { list -> _state.update { it.copy(attachments = list) } }
            }
        }
        viewModelScope.launch {
            if (editId != 0L) loadExisting(editId)
            combine(categoryRepository.observeCategories(), budgetRepository.observeBudgets()) { cats, budgets ->
                cats to budgets
            }.collect { (cats, budgets) ->
                allBudgets = budgets.filter { it.active }
                _state.update { s ->
                    s.copy(
                        loading = false,
                        categories = cats,
                        budgetsForCategory = allBudgets.filter { it.categoryId == s.categoryId },
                    )
                }
            }
        }
    }

    private suspend fun loadExisting(id: Long) {
        val expense = movementRepository.getExpense(id)
        if (expense == null) {
            _state.update { it.copy(error = "Ese gasto ya no existe.", closed = true) }
            return
        }
        val local = LocalTimes.toLocal(expense.occurredAt)
        _state.update {
            it.copy(
                amountDigits = (expense.amount.cents / 100).toString(),
                concept = expense.concept,
                place = expense.place,
                categoryId = expense.categoryId,
                budgetId = expense.budgetId,
                budgetChosenByUser = true,
                nature = expense.nature,
                paymentMethod = expense.paymentMethod,
                date = local.toLocalDate(),
                time = local.toLocalTime(),
                description = expense.description,
            )
        }
    }

    fun onAmountChange(digits: String) = edit { copy(amountDigits = digits) }
    fun onPlaceChange(value: String) = edit { copy(place = value) }
    fun onDescriptionChange(value: String) = edit { copy(description = value) }
    fun onNatureChange(value: ExpenseNature) = edit { copy(nature = value) }
    fun onPaymentMethodChange(value: PaymentMethod) = edit { copy(paymentMethod = value) }
    fun onDateChange(value: LocalDate) = edit { copy(date = value) }
    fun onTimeChange(value: LocalTime) = edit { copy(time = value) }

    /**
     * Al escribir el concepto, si coincide con el nombre de un rubro y aún no se eligió
     * categoría, se propone ese rubro y su categoría (identificación automática).
     */
    fun onConceptChange(value: String) {
        edit { copy(concept = value) }
        val s = _state.value
        if (s.categoryId != null || s.isEdit) return
        val text = value.trim()
        if (text.length < 3) return
        val match = allBudgets.firstOrNull { b ->
            text.contains(b.name, ignoreCase = true) || b.name.contains(text, ignoreCase = true)
        } ?: return
        selectCategory(match.categoryId, preferredBudget = match.id)
    }

    fun onCategoryChange(categoryId: Long) = selectCategory(categoryId, preferredBudget = null)

    private fun selectCategory(categoryId: Long, preferredBudget: Long?) {
        val category = _state.value.categories.firstOrNull { it.id == categoryId }
        val budgets = allBudgets.filter { it.categoryId == categoryId }
        val autoBudget = preferredBudget ?: budgets.firstOrNull()?.id
        edit {
            copy(
                categoryId = categoryId,
                budgetsForCategory = budgets,
                budgetId = autoBudget,
                budgetChosenByUser = preferredBudget != null,
                nature = category?.nature?.defaultClassification() ?: nature,
            )
        }
    }

    /** Crea una categoría desde el formulario y la deja elegida. */
    fun createCategory(name: String, nature: ExpenseNature) {
        viewModelScope.launch {
            runCatching { categoryRepository.addCategory(name, nature) }
                .onSuccess { id ->
                    edit {
                        copy(
                            categoryId = id,
                            budgetsForCategory = allBudgets.filter { it.categoryId == id },
                            budgetId = allBudgets.firstOrNull { it.categoryId == id }?.id,
                            nature = nature.defaultClassification(),
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "No se pudo crear la categoría.") } }
        }
    }

    fun onBudgetChange(budgetId: Long?) = edit { copy(budgetId = budgetId, budgetChosenByUser = true) }

    fun save() {
        val s = _state.value
        val occurredAt = LocalTimes.toMillis(s.date, s.time)
        val result = FinanceValidator.validateExpense(
            ExpenseInput(s.amountDigits, s.concept, s.place, s.categoryId, occurredAt, s.description),
        )
        val clean = result.value
        if (!result.isValid || clean == null) {
            _state.update { it.copy(errors = result.errors) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                val savedId = movementRepository.saveExpense(
                    Expense(
                        id = s.expenseId,
                        concept = clean.concept,
                        place = clean.place,
                        amount = clean.amount,
                        occurredAt = occurredAt,
                        categoryId = clean.categoryId,
                        budgetId = s.budgetId?.takeIf { id -> s.budgetsForCategory.any { it.id == id } },
                        nature = s.nature,
                        paymentMethod = s.paymentMethod,
                        description = clean.description,
                    ),
                )
                // Comprobantes elegidos antes de guardar (gasto nuevo).
                s.pendingAttachments.forEach { pending ->
                    runCatching { attachmentRepository.add(savedId, pending.uri) }
                }
                budgetAlert(s.budgetId, YearMonth.from(s.date))
            }.onSuccess { alert ->
                val verb = if (s.isEdit) "actualizado" else "guardado"
                _state.update {
                    it.copy(
                        saving = false,
                        saved = SavedResult("${clean.concept} · ${money.format(clean.amount)}", alert).let { r ->
                            r.copy(message = "Gasto $verb: ${r.message}")
                        },
                    )
                }
            }.onFailure {
                _state.update { it.copy(saving = false, error = "No se pudo guardar. Intenta de nuevo.") }
            }
        }
    }

    /** Mensaje de alerta si el rubro llegó a 50, 75, 90 o 100 % en el mes del gasto. */
    private suspend fun budgetAlert(budgetId: Long?, month: YearMonth): String? {
        if (budgetId == null) return null
        val summary = movementRepository.observeMonthSummary(MonthRange.of(month)).first()
        val progress = summary.budgets.firstOrNull { it.budget.id == budgetId } ?: return null
        val percent = BudgetMath.percentUsed(progress.budget.amount, progress.spent)
        val name = progress.budget.name
        return when (BudgetMath.highestThresholdReached(progress.budget.amount, progress.spent)) {
            100 -> if (percent > 100) {
                "Superaste el presupuesto de «$name»: llevas $percent % (${money.format(progress.spent)} de ${money.format(progress.budget.amount)})."
            } else {
                "Llegaste al 100 % del presupuesto de «$name»."
            }
            90 -> "Atención: llevas $percent % del presupuesto de «$name». Quedan ${money.format(progress.remaining)}."
            75 -> "Llevas $percent % del presupuesto de «$name». Quedan ${money.format(progress.remaining)}."
            50 -> "Vas en la mitad del presupuesto de «$name» ($percent %)."
            else -> null
        }
    }

    /** Deja el formulario listo para otro gasto, conservando método de pago y fecha. */
    fun registerAnother() = _state.update {
        ExpenseFormState(
            loading = false,
            categories = it.categories,
            paymentMethod = it.paymentMethod,
            date = it.date,
        )
    }

    /** Comprobante elegido con el selector de archivos. */
    fun onAttachmentPicked(uri: String, name: String, mimeType: String) {
        val s = _state.value
        if (!s.isEdit) {
            _state.update { it.copy(pendingAttachments = it.pendingAttachments + PendingAttachment(uri, name, mimeType)) }
            return
        }
        _state.update { it.copy(attaching = true) }
        viewModelScope.launch {
            runCatching { attachmentRepository.add(s.expenseId, uri) }
                .onSuccess { _state.update { it.copy(attaching = false) } }
                .onFailure { e -> _state.update { it.copy(attaching = false, error = e.message ?: "No se pudo adjuntar.") } }
        }
    }

    fun removePending(item: PendingAttachment) = _state.update { it.copy(pendingAttachments = it.pendingAttachments - item) }

    fun removeAttachment(id: Long) {
        viewModelScope.launch { attachmentRepository.delete(id) }
    }

    fun askDelete() = _state.update { it.copy(confirmDelete = true) }
    fun cancelDelete() = _state.update { it.copy(confirmDelete = false) }

    fun delete() {
        val id = _state.value.expenseId
        if (id == 0L) return
        viewModelScope.launch {
            movementRepository.deleteExpense(id)
            _state.update { it.copy(confirmDelete = false, closed = true) }
        }
    }

    fun onErrorShown() = _state.update { it.copy(error = null) }

    private inline fun edit(crossinline change: ExpenseFormState.() -> ExpenseFormState) =
        _state.update { it.change().copy(errors = emptyMap()) }
}
