package com.micontrolfinanciero.app.core.domain.finance

import com.micontrolfinanciero.app.core.model.Money

/** Campos de los formularios de gasto, ingreso, rubro y categoría. */
enum class FormField { AMOUNT, CONCEPT, PLACE, CATEGORY, DATE, SOURCE, NAME, DESCRIPTION, DUE_DAY, INSTALLMENTS, PAID_INSTALLMENTS }

/** Resultado de validar un formulario: valor limpio o errores por campo. */
data class FormValidation<T>(
    val value: T?,
    val errors: Map<FormField, String>,
) {
    val isValid: Boolean get() = errors.isEmpty() && value != null
}

/** Datos crudos del formulario de gasto, tal como los escribe el usuario. */
data class ExpenseInput(
    /** Solo dígitos en pesos, por ejemplo "200465". */
    val amountDigits: String,
    val concept: String,
    val place: String,
    val categoryId: Long?,
    val occurredAt: Long,
    val description: String,
)

data class CleanExpense(
    val amount: Money,
    val concept: String,
    val place: String,
    val categoryId: Long,
    val description: String,
)

data class IncomeInput(
    val amountDigits: String,
    val source: String,
    val receivedAt: Long,
    val notes: String,
)

data class CleanIncome(val amount: Money, val source: String, val notes: String)

data class BudgetInput(
    val name: String,
    val amountDigits: String,
    val categoryId: Long?,
    val description: String,
    val notes: String,
)

data class CleanBudget(
    val name: String,
    val amount: Money,
    val categoryId: Long,
    val description: String,
    val notes: String,
)

data class FixedPaymentInput(
    val name: String,
    val amountDigits: String,
    val dueDayText: String,
    val categoryId: Long?,
    val isCredit: Boolean,
    val totalInstallmentsText: String,
    val paidInstallmentsText: String,
    val notes: String,
)

data class CleanFixedPayment(
    val name: String,
    val amount: Money,
    val dueDay: Int,
    val categoryId: Long,
    val totalInstallments: Int?,
    val initialPaidInstallments: Int,
    val notes: String,
)

/**
 * Reglas de los formularios de dinero (secciones 8 y 13 del proyecto):
 *  - todo gasto dice EN QUÉ se gastó (regla acordada con el usuario);
 *  - los valores son pesos enteros positivos con un tope razonable;
 *  - no se aceptan fechas más de un día en el futuro (evita errores de dedo).
 */
object FinanceValidator {

    /** $999.999.999.999: tope para evitar valores absurdos por error. */
    const val MAX_AMOUNT_UNITS = 999_999_999_999L
    const val MAX_DIGITS = 12
    const val CONCEPT_MAX = 80
    const val PLACE_MAX = 80
    const val TEXT_MAX = 300
    const val NAME_MAX = 40
    private const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000

    fun validateExpense(input: ExpenseInput, nowMillis: Long = System.currentTimeMillis()): FormValidation<CleanExpense> {
        val errors = mutableMapOf<FormField, String>()
        val amount = amountOrError(input.amountDigits, errors)

        val concept = clean(input.concept)
        when {
            concept.isEmpty() -> errors[FormField.CONCEPT] =
                "Escribe en qué gastaste. Es obligatorio, también en Otros (ejemplo: Sistecrédito)."
            concept.length < 2 -> errors[FormField.CONCEPT] = "Escribe al menos 2 caracteres."
            concept.length > CONCEPT_MAX -> errors[FormField.CONCEPT] = "Máximo $CONCEPT_MAX caracteres."
        }
        val place = clean(input.place)
        if (place.length > PLACE_MAX) errors[FormField.PLACE] = "Máximo $PLACE_MAX caracteres."
        if (input.categoryId == null) errors[FormField.CATEGORY] = "Elige una categoría."
        dateError(input.occurredAt, nowMillis)?.let { errors[FormField.DATE] = it }
        val description = input.description.trim()
        if (description.length > TEXT_MAX) errors[FormField.DESCRIPTION] = "Máximo $TEXT_MAX caracteres."

        val value = if (errors.isEmpty() && amount != null && input.categoryId != null) {
            CleanExpense(amount, concept, place, input.categoryId, description)
        } else {
            null
        }
        return FormValidation(value, errors)
    }

    fun validateIncome(input: IncomeInput, nowMillis: Long = System.currentTimeMillis()): FormValidation<CleanIncome> {
        val errors = mutableMapOf<FormField, String>()
        val amount = amountOrError(input.amountDigits, errors)
        val source = clean(input.source)
        when {
            source.isEmpty() -> errors[FormField.SOURCE] = "Escribe de dónde viene el ingreso (ejemplo: Salario)."
            source.length < 2 -> errors[FormField.SOURCE] = "Escribe al menos 2 caracteres."
            source.length > CONCEPT_MAX -> errors[FormField.SOURCE] = "Máximo $CONCEPT_MAX caracteres."
        }
        dateError(input.receivedAt, nowMillis)?.let { errors[FormField.DATE] = it }
        val notes = input.notes.trim()
        if (notes.length > TEXT_MAX) errors[FormField.DESCRIPTION] = "Máximo $TEXT_MAX caracteres."
        val value = if (errors.isEmpty() && amount != null) CleanIncome(amount, source, notes) else null
        return FormValidation(value, errors)
    }

    fun validateBudget(input: BudgetInput): FormValidation<CleanBudget> {
        val errors = mutableMapOf<FormField, String>()
        val amount = amountOrError(input.amountDigits, errors)
        val name = clean(input.name)
        when {
            name.length < 2 -> errors[FormField.NAME] = "Escribe el nombre del rubro (ejemplo: Mercado)."
            name.length > NAME_MAX -> errors[FormField.NAME] = "Máximo $NAME_MAX caracteres."
        }
        if (input.categoryId == null) errors[FormField.CATEGORY] = "Elige una categoría."
        val description = input.description.trim()
        val notes = input.notes.trim()
        if (description.length > TEXT_MAX || notes.length > TEXT_MAX) {
            errors[FormField.DESCRIPTION] = "Máximo $TEXT_MAX caracteres."
        }
        val value = if (errors.isEmpty() && amount != null && input.categoryId != null) {
            CleanBudget(name, amount, input.categoryId, description, notes)
        } else {
            null
        }
        return FormValidation(value, errors)
    }

    const val MAX_INSTALLMENTS = 360

    fun validateFixedPayment(input: FixedPaymentInput): FormValidation<CleanFixedPayment> {
        val errors = mutableMapOf<FormField, String>()
        val amount = amountOrError(input.amountDigits, errors)
        val name = clean(input.name)
        when {
            name.length < 2 -> errors[FormField.NAME] = "Escribe qué vas a pagar (ejemplo: Sistecrédito, Arriendo)."
            name.length > CONCEPT_MAX -> errors[FormField.NAME] = "Máximo $CONCEPT_MAX caracteres."
        }
        val dueDay = input.dueDayText.trim().toIntOrNull()
        if (dueDay == null || dueDay !in 1..31) errors[FormField.DUE_DAY] = "Escribe el día de pago, de 1 a 31."
        if (input.categoryId == null) errors[FormField.CATEGORY] = "Elige una categoría."

        var total: Int? = null
        var paid = 0
        if (input.isCredit) {
            total = input.totalInstallmentsText.trim().toIntOrNull()
            if (total == null || total !in 1..MAX_INSTALLMENTS) {
                errors[FormField.INSTALLMENTS] = "Escribe el número total de cuotas (1 a $MAX_INSTALLMENTS)."
            }
            val paidText = input.paidInstallmentsText.trim()
            paid = if (paidText.isEmpty()) 0 else paidText.toIntOrNull() ?: -1
            if (paid < 0 || (total != null && paid >= total)) {
                errors[FormField.PAID_INSTALLMENTS] = "Las cuotas ya pagadas deben ser menos que el total."
            }
        }
        val notes = input.notes.trim()
        if (notes.length > TEXT_MAX) errors[FormField.DESCRIPTION] = "Máximo $TEXT_MAX caracteres."

        val value = if (errors.isEmpty() && amount != null && dueDay != null && input.categoryId != null) {
            CleanFixedPayment(name, amount, dueDay, input.categoryId, total, paid, notes)
        } else {
            null
        }
        return FormValidation(value, errors)
    }

    /** Nombre de una categoría nueva; null si es válido, o el mensaje de error. */
    fun categoryNameError(name: String, existing: Collection<String>): String? {
        val clean = clean(name)
        return when {
            clean.length < 2 -> "Escribe al menos 2 caracteres."
            clean.length > NAME_MAX -> "Máximo $NAME_MAX caracteres."
            existing.any { it.equals(clean, ignoreCase = true) } -> "Ya existe una categoría con ese nombre."
            else -> null
        }
    }

    /** Pesos escritos ("200.465", "$ 200465") a dígitos ("200465"), máximo [MAX_DIGITS]. */
    fun digitsOnly(text: String): String = text.filter { it.isDigit() }.trimStart('0').take(MAX_DIGITS)

    fun clean(text: String): String = text.trim().replace(Regex("\\s+"), " ")

    private fun amountOrError(digits: String, errors: MutableMap<FormField, String>): Money? {
        val units = digitsOnly(digits).toLongOrNull()
        return when {
            units == null || units <= 0L -> {
                errors[FormField.AMOUNT] = "Escribe el valor (mayor que cero)."
                null
            }
            units > MAX_AMOUNT_UNITS -> {
                errors[FormField.AMOUNT] = "El valor es demasiado grande."
                null
            }
            else -> Money.ofUnits(units)
        }
    }

    private fun dateError(epochMillis: Long, nowMillis: Long): String? =
        if (epochMillis > nowMillis + ONE_DAY_MILLIS) "La fecha no puede estar en el futuro." else null
}
