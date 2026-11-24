package com.micontrolfinanciero.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    private val cop = MoneyFormat.COLOMBIA

    @Test
    fun `pesos enteros se guardan como centavos`() {
        assertEquals(20_046_500L, Money.ofUnits(200_465).cents)
    }

    @Test
    fun `formato colombiano usa punto de miles y simbolo pesos`() {
        assertEquals("$200.465", cop.format(Money.ofUnits(200_465)))
        assertEquals("$120.000", cop.format(Money.ofUnits(120_000)))
        assertEquals("$3.800.000", cop.format(Money.ofUnits(3_800_000)))
        assertEquals("$0", cop.format(Money.ZERO))
        assertEquals("$999", cop.format(Money.ofUnits(999)))
        assertEquals("$1.000", cop.format(Money.ofUnits(1_000)))
    }

    @Test
    fun `formato con decimales usa coma`() {
        val withDecimals = cop.copy(showDecimals = true)
        assertEquals("$1.234.567,89", withDecimals.format(Money(123_456_789L)))
        assertEquals("$0,05", withDecimals.format(Money(5L)))
    }

    @Test
    fun `valores negativos llevan signo menos`() {
        assertEquals("-$48.000", cop.format(-Money.ofUnits(48_000)))
    }

    @Test
    fun `la suma no pierde centavos`() {
        val total = Money.sum(List(10) { Money(10L) })
        assertEquals(Money(100L), total)
    }

    @Test
    fun `ingresos menos gastos da lo que queda`() {
        val ingresos = Money.ofUnits(4_200_000)
        val gastos = Money.ofUnits(2_520_000)
        assertEquals(Money.ofUnits(1_680_000), ingresos - gastos)
        assertTrue((gastos - ingresos).isNegative)
    }

    @Test(expected = ArithmeticException::class)
    fun `un desbordamiento se detecta en vez de dar un numero falso`() {
        Money(Long.MAX_VALUE) + Money(1L)
    }
}
