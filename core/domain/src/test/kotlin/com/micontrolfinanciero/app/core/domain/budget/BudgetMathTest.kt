package com.micontrolfinanciero.app.core.domain.budget

import com.micontrolfinanciero.app.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetMathTest {

    private fun pesos(value: Long) = Money.ofUnits(value)

    @Test
    fun `porcentaje consumido se redondea hacia abajo`() {
        // 2.520.000 de 3.700.000 = 68,1 %
        assertEquals(68, BudgetMath.percentUsed(pesos(3_700_000), pesos(2_520_000)))
    }

    @Test
    fun `presupuesto en cero no divide por cero`() {
        assertEquals(0, BudgetMath.percentUsed(Money.ZERO, Money.ZERO))
        assertEquals(100, BudgetMath.percentUsed(Money.ZERO, pesos(1)))
    }

    @Test
    fun `restante puede quedar negativo`() {
        assertEquals(pesos(-10_000), BudgetMath.remaining(pesos(200_000), pesos(210_000)))
    }

    @Test
    fun `balance del mes es ingresos menos gastos`() {
        assertEquals(pesos(1_680_000), BudgetMath.monthlyBalance(pesos(4_200_000), pesos(2_520_000)))
    }

    @Test
    fun `umbrales de alerta`() {
        val budget = pesos(200_000)
        assertNull(BudgetMath.highestThresholdReached(budget, pesos(99_999)))
        assertEquals(50, BudgetMath.highestThresholdReached(budget, pesos(100_000)))
        assertEquals(75, BudgetMath.highestThresholdReached(budget, pesos(150_000)))
        assertEquals(90, BudgetMath.highestThresholdReached(budget, pesos(190_000)))
        assertEquals(100, BudgetMath.highestThresholdReached(budget, pesos(250_000)))
    }
}
