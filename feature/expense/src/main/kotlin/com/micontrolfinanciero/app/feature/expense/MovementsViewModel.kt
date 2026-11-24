package com.micontrolfinanciero.app.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.finance.MonthRange
import com.micontrolfinanciero.app.core.domain.finance.MovementFilter
import com.micontrolfinanciero.app.core.domain.finance.MovementRepository
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.MovementItem
import com.micontrolfinanciero.app.core.model.MovementKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class MovementTypeFilter(val label: String) { ALL("Todos"), EXPENSES("Gastos"), INCOMES("Ingresos") }

data class MovementsState(
    val range: MonthRange = MonthRange.current(),
    val query: String = "",
    val type: MovementTypeFilter = MovementTypeFilter.ALL,
    val items: List<MovementItem> = emptyList(),
    val loading: Boolean = true,
) {
    val totalExpenses: Money get() = Money.sum(items.filter { it.kind == MovementKind.EXPENSE }.map { it.amount })
    val totalIncomes: Money get() = Money.sum(items.filter { it.kind == MovementKind.INCOME }.map { it.amount })
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MovementsViewModel @Inject constructor(
    private val repository: MovementRepository,
) : ViewModel() {

    private val range = MutableStateFlow(MonthRange.current())
    private val query = MutableStateFlow("")
    private val type = MutableStateFlow(MovementTypeFilter.ALL)

    private val items = combine(range, query.debounce(250), type) { r, q, t -> MovementFilter(
        range = r,
        query = q,
        includeExpenses = t != MovementTypeFilter.INCOMES,
        includeIncomes = t != MovementTypeFilter.EXPENSES,
    ) }.flatMapLatest { repository.observeMovements(it) }

    val state: StateFlow<MovementsState> = combine(range, query, type, items) { r, q, t, list ->
        MovementsState(range = r, query = q, type = t, items = list, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovementsState())

    fun previousMonth() { range.value = range.value.previous() }
    fun nextMonth() { range.value = range.value.next() }
    fun onQueryChange(value: String) { query.value = value.take(60) }
    fun onTypeChange(value: MovementTypeFilter) { type.value = value }
}
