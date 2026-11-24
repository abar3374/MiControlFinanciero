package com.micontrolfinanciero.app.core.domain.finance

import com.micontrolfinanciero.app.core.model.Budget
import com.micontrolfinanciero.app.core.model.Expense
import com.micontrolfinanciero.app.core.model.ExpenseCategory
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.Income
import com.micontrolfinanciero.app.core.model.MonthSummary
import com.micontrolfinanciero.app.core.model.MovementItem
import kotlinx.coroutines.flow.Flow

/** Categorías personalizables. Las predeterminadas se crean solas la primera vez. */
interface CategoryRepository {
    fun observeCategories(): Flow<List<ExpenseCategory>>
    suspend fun addCategory(name: String, nature: ExpenseNature): Long
}

/** Rubros (presupuestos mensuales por categoría). */
interface BudgetRepository {
    fun observeBudgets(): Flow<List<Budget>>
    suspend fun getBudget(id: Long): Budget?

    /** Crea (id = 0) o actualiza un rubro. Devuelve su id. Deja registro de auditoría. */
    suspend fun saveBudget(budget: Budget): Long

    /** Borra el rubro. Los gastos asociados se conservan, quedan "sin rubro". */
    suspend fun deleteBudget(id: Long)
}

/** Filtro del historial de movimientos. */
data class MovementFilter(
    val range: MonthRange,
    val query: String = "",
    val includeExpenses: Boolean = true,
    val includeIncomes: Boolean = true,
    val limit: Int = 500,
)

/**
 * Gastos e ingresos. Al borrar no se pierde nada: el movimiento se marca como anulado y
 * se deja registro en la auditoría (sección 8: "conservando registros de auditoría").
 */
interface MovementRepository {
    suspend fun getExpense(id: Long): Expense?
    suspend fun saveExpense(expense: Expense): Long
    suspend fun deleteExpense(id: Long)

    suspend fun getIncome(id: Long): Income?
    suspend fun saveIncome(income: Income): Long
    suspend fun deleteIncome(id: Long)

    fun observeMovements(filter: MovementFilter): Flow<List<MovementItem>>
    fun observeRecent(limit: Int): Flow<List<MovementItem>>

    /** Resumen del mes; se vuelve a emitir con cada cambio en gastos, ingresos o rubros. */
    fun observeMonthSummary(range: MonthRange): Flow<MonthSummary>
}
