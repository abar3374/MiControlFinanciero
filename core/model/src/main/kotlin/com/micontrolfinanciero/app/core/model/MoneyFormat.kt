package com.micontrolfinanciero.app.core.model

/**
 * Reglas para mostrar dinero en pantalla. Por defecto: Colombia (COP).
 * Ejemplo: 20_046_500 centavos -> "$200.465".
 *
 * Se arma a mano (sin java.text) para que el resultado sea idéntico en todos los teléfonos.
 */
data class MoneyFormat(
    val symbol: String = "$",
    val thousandsSeparator: Char = '.',
    val decimalSeparator: Char = ',',
    /** COP normalmente se muestra sin centavos. */
    val showDecimals: Boolean = false,
) {

    fun format(money: Money): String {
        val negative = money.cents < 0
        // Se trabaja con el valor absoluto en Long para no perder precisión.
        val absCents = money.cents.toBigInteger().abs()
        val units = absCents.divide(HUNDRED)
        val remainder = absCents.mod(HUNDRED).toInt()

        val grouped = groupThousands(units.toString())
        val body = if (showDecimals) {
            grouped + decimalSeparator + remainder.toString().padStart(2, '0')
        } else {
            grouped
        }
        return (if (negative) "-" else "") + symbol + body
    }

    private fun groupThousands(digits: String): String {
        val out = StringBuilder()
        digits.forEachIndexed { index, c ->
            val fromEnd = digits.length - index
            out.append(c)
            if (fromEnd > 1 && (fromEnd - 1) % 3 == 0) out.append(thousandsSeparator)
        }
        return out.toString()
    }

    companion object {
        private val HUNDRED = 100.toBigInteger()

        /** Formato inicial pedido: Colombia, peso colombiano, punto de miles y coma decimal. */
        val COLOMBIA = MoneyFormat()
    }
}
