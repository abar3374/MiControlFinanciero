package com.micontrolfinanciero.app.core.model

/**
 * Un valor de dinero guardado como número entero de centavos.
 *
 * Por qué: Float y Double redondean mal (0.1 + 0.2 != 0.3). Con centavos enteros
 * las sumas siempre cuadran. Ejemplo: $200.465 se guarda como 20_046_500 centavos.
 *
 * La moneda se maneja aparte (en la configuración); este tipo solo guarda la cantidad.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(Math.addExact(cents, other.cents))

    operator fun minus(other: Money): Money = Money(Math.subtractExact(cents, other.cents))

    operator fun unaryMinus(): Money = Money(Math.negateExact(cents))

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    val isZero: Boolean get() = cents == 0L
    val isNegative: Boolean get() = cents < 0L

    companion object {
        val ZERO = Money(0L)

        /** Crea un valor a partir de pesos enteros: Money.ofUnits(200_465) = $200.465. */
        fun ofUnits(units: Long): Money = Money(Math.multiplyExact(units, 100L))

        /** Suma una lista de valores sin perder precisión. */
        fun sum(values: Iterable<Money>): Money = values.fold(ZERO) { acc, m -> acc + m }
    }
}
