package com.micontrolfinanciero.app.core.domain.finance

import com.micontrolfinanciero.app.core.model.Budget
import com.micontrolfinanciero.app.core.model.Expense
import com.micontrolfinanciero.app.core.model.ExpenseCategory
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.Income
import com.micontrolfinanciero.app.core.model.MonthSummary
import com.micontrolfinanciero.app.core.model.MovementItem
import com.micontrolfinanciero.app.core.model.Attachment
import com.micontrolfinanciero.app.core.model.ExpectedIncome
import com.micontrolfinanciero.app.core.model.FixedPayment
import com.micontrolfinanciero.app.core.model.IncomeDestination
import com.micontrolfinanciero.app.core.model.MonthlyIncome
import com.micontrolfinanciero.app.core.model.FixedPaymentRecord
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.MonthlyPayment
import com.micontrolfinanciero.app.core.model.PaymentMethod
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/** Categorías personalizables. Las predeterminadas se crean solas la primera vez. */
interface CategoryRepository {
    /** Categorías visibles (no ocultas). */
    fun observeCategories(): Flow<List<ExpenseCategory>>

    /** Todas, incluidas las ocultas. */
    fun observeAllCategories(): Flow<List<ExpenseCategory>>

    /**
     * Crea una categoría y devuelve su id. Si ya existía oculta con ese nombre, la vuelve a
     * mostrar. Lanza [IllegalArgumentException] si ya existe una visible con ese nombre.
     */
    suspend fun addCategory(name: String, nature: ExpenseNature): Long

    suspend fun renameCategory(id: Long, name: String)

    /** Ocultar no borra: los gastos que la usan la conservan. */
    suspend fun setCategoryHidden(id: Long, hidden: Boolean)
}

/** Pagos fijos mensuales y créditos con cuotas. */
interface FixedPaymentRepository {
    fun observeFixedPayments(): Flow<List<FixedPayment>>
    suspend fun getFixedPayment(id: Long): FixedPayment?
    suspend fun saveFixedPayment(payment: FixedPayment): Long

    /** Lo desactiva (deja de aparecer); sus pagos anteriores y gastos se conservan. */
    suspend fun deactivate(id: Long)

    fun observeRecords(): Flow<List<FixedPaymentRecord>>

    /** Estado de los pagos del mes (pendiente, vencido, pagado), recalculado ante cada cambio. */
    fun observeMonth(month: YearMonth, today: () -> LocalDate): Flow<List<MonthlyPayment>>

    /**
     * Registra el pago del mes: crea el gasto (con su concepto, categoría y rubro) y lo enlaza.
     * Devuelve el id del gasto creado.
     */
    suspend fun markPaid(
        paymentId: Long,
        month: YearMonth,
        amount: Money,
        paidAt: Long,
        method: PaymentMethod,
    ): Long

    /** Deshace el pago del mes: anula el gasto creado y el pago vuelve a pendiente. */
    suspend fun undoPaid(paymentId: Long, month: YearMonth)

    // ---------------- Ingresos fijos esperados (salario)

    fun observeExpectedIncomes(): Flow<List<ExpectedIncome>>
    suspend fun getExpectedIncome(id: Long): ExpectedIncome?
    suspend fun saveExpectedIncome(income: ExpectedIncome): Long
    suspend fun deactivateExpectedIncome(id: Long)
    fun observeIncomeMonth(month: YearMonth, today: () -> LocalDate): Flow<List<MonthlyIncome>>

    /** Confirma que llegó el ingreso esperado del mes: crea el ingreso real y lo enlaza. */
    suspend fun markIncomeReceived(expectedId: Long, month: YearMonth, amount: Money, receivedAt: Long, destination: IncomeDestination): Long

    suspend fun undoIncomeReceived(expectedId: Long, month: YearMonth)
}

/** Comprobantes adjuntos a los gastos. */
interface AttachmentRepository {
    fun observeForExpense(expenseId: Long): Flow<List<Attachment>>

    /** Copia el archivo elegido ([sourceUri], content://) a la carpeta privada y lo enlaza al gasto. */
    suspend fun add(expenseId: Long, sourceUri: String): Attachment

    suspend fun delete(attachmentId: Long)
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

/** Ajustes de la app (recordatorios, bloqueo, pantalla segura, canal de actualizaciones). */
interface SettingsRepository {
    fun observeSettings(): Flow<com.micontrolfinanciero.app.core.model.AppSettings>
    suspend fun update(change: (com.micontrolfinanciero.app.core.model.AppSettings) -> com.micontrolfinanciero.app.core.model.AppSettings)
}

/** Datos reales para los reportes (gastos e ingresos vigentes de un rango). */
interface ReportRepository {
    fun observeData(startMillis: Long, endMillis: Long): Flow<Pair<List<com.micontrolfinanciero.app.core.domain.report.ReportExpense>, List<com.micontrolfinanciero.app.core.domain.report.ReportIncome>>>
}
