package com.micontrolfinanciero.app.core.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Modelos de la Fase 4: categorías, rubros (presupuestos), gastos e ingresos.
 * Las fechas y horas de movimientos se guardan como milisegundos UTC (epoch);
 * se muestran en la zona horaria configurada (America/Bogota).
 */

/** Clasificación que se muestra al registrar un gasto o un rubro (subconjunto de [ExpenseNature]). */
val CLASSIFICATIONS: List<ExpenseNature> = listOf(
    ExpenseNature.NEED,
    ExpenseNature.OBLIGATION,
    ExpenseNature.LEISURE,
    ExpenseNature.CREDIT,
    ExpenseNature.SAVINGS,
    ExpenseNature.OTHER,
)

/** Clasificación sugerida según la naturaleza de la categoría. */
fun ExpenseNature.defaultClassification(): ExpenseNature = when (this) {
    ExpenseNature.FOOD, ExpenseNature.TRANSPORT, ExpenseNature.HOUSING,
    ExpenseNature.HEALTH, ExpenseNature.EDUCATION, ExpenseNature.NEED -> ExpenseNature.NEED
    ExpenseNature.LEISURE, ExpenseNature.ENTERTAINMENT -> ExpenseNature.LEISURE
    ExpenseNature.CREDIT -> ExpenseNature.CREDIT
    ExpenseNature.SAVINGS -> ExpenseNature.SAVINGS
    ExpenseNature.OBLIGATION -> ExpenseNature.OBLIGATION
    ExpenseNature.OTHER -> ExpenseNature.OTHER
}

/** Categoría personalizable (Alimentación, Transporte…). */
data class ExpenseCategory(
    val id: Long,
    val name: String,
    val nature: ExpenseNature,
    val isDefault: Boolean,
    val archived: Boolean,
)

/** Rubro: un presupuesto mensual para una categoría (por ejemplo "Mercado", $800.000 al mes). */
data class Budget(
    val id: Long,
    val name: String,
    val description: String,
    val categoryId: Long,
    /** Valor presupuestado por mes. */
    val amount: Money,
    val frequencyType: ExpenseFrequencyType,
    val nature: ExpenseNature,
    val startDate: LocalDate,
    val active: Boolean,
    val notes: String,
)

/** Un gasto real que ya ocurrió. [concept] ("¿En qué gastaste?") es obligatorio. */
data class Expense(
    val id: Long,
    val concept: String,
    val place: String,
    val amount: Money,
    val occurredAt: Long,
    val categoryId: Long,
    val budgetId: Long?,
    val nature: ExpenseNature,
    val paymentMethod: PaymentMethod,
    val description: String,
)

/** Un ingreso recibido. [source] ("¿De dónde viene?") es obligatorio. */
data class Income(
    val id: Long,
    val source: String,
    val amount: Money,
    val receivedAt: Long,
    val type: IncomeType,
    val destination: IncomeDestination,
    val notes: String,
)

enum class MovementKind { EXPENSE, INCOME }

/** Una fila del historial: gasto o ingreso, ya listo para mostrar. */
data class MovementItem(
    val kind: MovementKind,
    val id: Long,
    /** Concepto del gasto u origen del ingreso. */
    val title: String,
    /** Categoría del gasto, o tipo del ingreso. */
    val subtitle: String,
    val amount: Money,
    val at: Long,
    /** Método de pago (gasto) o dónde se recibió (ingreso), como nombre interno. */
    val detail: String,
)

/** Avance de un rubro en el mes. */
data class BudgetProgress(
    val budget: Budget,
    val categoryName: String,
    val spent: Money,
) {
    val remaining: Money get() = budget.amount - spent
}

/** Total gastado en una categoría durante el mes. */
data class CategoryTotal(
    val categoryId: Long,
    val name: String,
    val total: Money,
)

/** Resumen de un mes: lo que alimenta Inicio y Presupuesto. */
data class MonthSummary(
    val month: YearMonth,
    /** Suma de los rubros activos del mes = presupuesto global. */
    val budgetTotal: Money,
    /** Todo lo gastado en el mes (con o sin rubro). */
    val spent: Money,
    /** Gastado en el mes en algún rubro. */
    val spentInBudgets: Money,
    /** Todo lo recibido en el mes. */
    val income: Money,
    val budgets: List<BudgetProgress>,
    val byCategory: List<CategoryTotal>,
) {
    /** Presupuesto restante (rubros − gastado en rubros). No es el saldo del banco. */
    val budgetRemaining: Money get() = budgetTotal - spentInBudgets

    /** Balance del mes: ingresos − todos los gastos. */
    val balance: Money get() = income - spent
}
