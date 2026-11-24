package com.micontrolfinanciero.app.core.model

/**
 * Clasificaciones fijas que pide el proyecto. Las categorías (Alimentación, Transporte…)
 * NO están aquí: serán personalizables y vivirán en la base de datos (Fase 3).
 *
 * Cada valor tiene un [label] en español para mostrarlo. El nombre interno va en
 * inglés (por ejemplo CASH) y es el que se guarda en la base de datos, para que
 * un cambio de idioma no rompa los datos guardados.
 */

/** ¿El gasto se repite o pasa de vez en cuando? */
enum class ExpenseFrequencyType(val label: String) {
    FIXED("Gasto fijo"),
    OCCASIONAL("Gasto ocasional"),
}

/** Naturaleza del gasto (sección 7 del documento del proyecto). */
enum class ExpenseNature(val label: String) {
    NEED("Necesidad"),
    OBLIGATION("Obligación"),
    LEISURE("Ocio"),
    ENTERTAINMENT("Entretenimiento"),
    FOOD("Alimentación"),
    TRANSPORT("Transporte"),
    HOUSING("Vivienda"),
    HEALTH("Salud"),
    EDUCATION("Educación"),
    CREDIT("Crédito"),
    SAVINGS("Ahorro"),
    OTHER("Otros"),
}

/** Cada cuánto se repite un gasto fijo, un ingreso fijo o una cuota. */
enum class RecurrenceFrequency(val label: String) {
    DAILY("Diario"),
    WEEKLY("Semanal"),
    BIWEEKLY("Quincenal"),
    MONTHLY("Mensual"),
    BIMONTHLY("Bimestral"),
    QUARTERLY("Trimestral"),
    SEMIANNUAL("Semestral"),
    ANNUAL("Anual"),
    CUSTOM("Personalizado"),
}

/** Cómo se pagó un gasto. */
enum class PaymentMethod(val label: String) {
    CASH("Efectivo"),
    TRANSFER("Transferencia"),
    DEBIT_CARD("Tarjeta débito"),
    CREDIT_CARD("Tarjeta crédito"),
    PSE("PSE"),
    OTHER("Otro"),
}

/** Ingreso fijo (salario, se espera cada periodo) o variable (llega cuando llega). */
enum class IncomeType(val label: String) {
    FIXED("Ingreso fijo"),
    VARIABLE("Ingreso variable"),
}

/** Dónde se recibió un ingreso. */
enum class IncomeDestination(val label: String) {
    ACCOUNT("Cuenta"),
    CASH("Efectivo"),
    OTHER("Otro"),
}
