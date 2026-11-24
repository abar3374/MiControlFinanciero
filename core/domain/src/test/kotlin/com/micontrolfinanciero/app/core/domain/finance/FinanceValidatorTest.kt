package com.micontrolfinanciero.app.core.domain.finance

import com.micontrolfinanciero.app.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class FinanceValidatorTest {

    private val now = LocalTimes.toMillis(LocalDate.of(2026, 11, 24), LocalTime.of(15, 0))

    private fun expense(amount: String = "200465", concept: String = "Sistecrédito", category: Long? = 1L, at: Long = now) =
        ExpenseInput(amount, concept, "", category, at, "")

    @Test
    fun `gasto valido con el ejemplo del usuario`() {
        val result = FinanceValidator.validateExpense(expense(amount = "$ 200.465", concept = "  Sistecrédito  "), now)
        assertTrue(result.isValid)
        assertEquals(Money.ofUnits(200_465), result.value!!.amount)
        assertEquals("Sistecrédito", result.value!!.concept)
    }

    @Test
    fun `sin concepto no se guarda aunque sea Otros`() {
        val result = FinanceValidator.validateExpense(expense(concept = "   "), now)
        assertTrue(result.errors.containsKey(FormField.CONCEPT))
        assertNull(result.value)
    }

    @Test
    fun `valor cero vacio o enorme no se acepta`() {
        assertTrue(FinanceValidator.validateExpense(expense(amount = "0"), now).errors.containsKey(FormField.AMOUNT))
        assertTrue(FinanceValidator.validateExpense(expense(amount = ""), now).errors.containsKey(FormField.AMOUNT))
        assertEquals("999999999999", FinanceValidator.digitsOnly("9999999999999999"))
    }

    @Test
    fun `categoria y fecha futura`() {
        val result = FinanceValidator.validateExpense(expense(category = null, at = now + 3L * 86_400_000), now)
        assertTrue(result.errors.containsKey(FormField.CATEGORY))
        assertTrue(result.errors.containsKey(FormField.DATE))
    }

    @Test
    fun `ingreso requiere origen`() {
        assertTrue(FinanceValidator.validateIncome(IncomeInput("3800000", "", now, ""), now).errors.containsKey(FormField.SOURCE))
        val ok = FinanceValidator.validateIncome(IncomeInput("3.800.000", "Salario", now, ""), now)
        assertEquals(Money.ofUnits(3_800_000), ok.value!!.amount)
    }

    @Test
    fun `rubro valido y categoria repetida`() {
        val ok = FinanceValidator.validateBudget(BudgetInput("Mercado", "800000", 1L, "", ""))
        assertTrue(ok.isValid)
        assertEquals("Ya existe una categoría con ese nombre.", FinanceValidator.categoryNameError(" mercado ", listOf("Mercado")))
        assertNull(FinanceValidator.categoryNameError("Mascotas", listOf("Mercado")))
    }

    @Test
    fun `el mes va del primer dia a las 00 al primer dia del siguiente en hora de Colombia`() {
        val range = MonthRange.of(YearMonth.of(2026, 11))
        // 1 nov 2026 00:00 en Bogotá = 05:00 UTC
        assertEquals(1_793_509_200_000L, range.startMillis)
        assertTrue(now in range)
        assertTrue(range.endMillis !in range)
        assertEquals(YearMonth.of(2026, 12), range.next().month)
        assertEquals(YearMonth.of(2026, 10), range.previous().month)
    }
}
