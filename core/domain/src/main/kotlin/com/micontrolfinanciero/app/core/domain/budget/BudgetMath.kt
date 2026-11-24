package com.micontrolfinanciero.app.core.domain.budget

import com.micontrolfinanciero.app.core.model.Money

/**
 * Cálculos de presupuesto. Son funciones puras: mismo dato de entrada, mismo resultado,
 * sin tocar base de datos ni pantalla. Por eso se prueban fácil.
 *
 * Conceptos que la app NO mezcla (sección 18 del proyecto):
 *  - Presupuesto restante = presupuesto del periodo - gastos del periodo.
 *  - Balance del mes      = ingresos recibidos - gastos del mes.
 *  - Saldo bancario real  = lo que diga tu banco; la app no lo adivina.
 */
object BudgetMath {

    /** Umbrales en los que se avisará al usuario (Fase 4). */
    val ALERT_THRESHOLDS: List<Int> = listOf(50, 75, 90, 100)

    /** Presupuesto restante. Puede ser negativo si te pasaste. */
    fun remaining(budget: Money, spent: Money): Money = budget - spent

    /** Balance del mes: ingresos menos gastos. */
    fun monthlyBalance(income: Money, expenses: Money): Money = income - expenses

    /**
     * Porcentaje consumido, redondeado hacia abajo (68,9 % -> 68).
     * Con presupuesto en cero devuelve 0 si no hay gasto, y 100 si lo hay.
     */
    fun percentUsed(budget: Money, spent: Money): Int {
        if (budget.cents <= 0L) return if (spent.cents > 0L) 100 else 0
        if (spent.cents <= 0L) return 0
        return spent.cents.toBigInteger()
            .multiply(100.toBigInteger())
            .divide(budget.cents.toBigInteger())
            .toInt()
    }

    /** El umbral más alto alcanzado (50, 75, 90 o 100), o null si aún no llega a 50 %. */
    fun highestThresholdReached(budget: Money, spent: Money): Int? {
        val percent = percentUsed(budget, spent)
        return ALERT_THRESHOLDS.lastOrNull { percent >= it }
    }
}
