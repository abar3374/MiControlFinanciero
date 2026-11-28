package com.micontrolfinanciero.app.core.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Pago fijo mensual: arriendo, internet, cuota de un crédito (Sistecrédito)…
 * Cada mes aparece de nuevo como pendiente; al pagarlo se crea un gasto real.
 *
 * Si [totalInstallments] no es null, es un crédito con cuotas: cuando se pagan todas
 * queda saldado y deja de aparecer desde el mes siguiente. El historial se conserva.
 */
data class FixedPayment(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val budgetId: Long?,
    /** Valor esperado de cada pago o cuota. */
    val amount: Money,
    /** Día del mes en que vence (1–31; en meses cortos se toma el último día). */
    val dueDay: Int,
    /** Número total de cuotas; null = se repite sin fin (arriendo, servicios). */
    val totalInstallments: Int?,
    /** Cuotas que ya estaban pagadas antes de registrarlo en la app. */
    val initialPaidInstallments: Int,
    /** Primer mes en que aparece. */
    val startMonth: YearMonth,
    val paymentMethod: PaymentMethod,
    val notes: String,
    val active: Boolean,
    /** Cada cuántos meses se paga: 1 mensual, 2 bimestral, 3 trimestral, 6 semestral, 12 anual. */
    val intervalMonths: Int = 1,
    /** Entidad o establecimiento (solo créditos, opcional). */
    val entity: String = "",
    /** Tasa de interés mensual en centésimas de punto (1,85 % = 185). Solo informativa. */
    val interestRateBp: Int? = null,
    /** Valor inicial del crédito (opcional). */
    val originalAmount: Money? = null,
) {
    val isCredit: Boolean get() = totalInstallments != null
}

/** Frecuencias por meses que admiten los pagos fijos e ingresos esperados. */
enum class MonthInterval(val months: Int, val label: String) {
    MONTHLY(1, "Mensual"),
    BIMONTHLY(2, "Bimestral"),
    QUARTERLY(3, "Trimestral"),
    SEMIANNUAL(6, "Semestral"),
    ANNUAL(12, "Anual");

    companion object {
        fun of(months: Int): MonthInterval = entries.firstOrNull { it.months == months } ?: MONTHLY
    }
}

/** Resumen de un crédito: cuánto se ha pagado y cuánto falta. */
data class CreditSummary(
    val totalInstallments: Int,
    val paidInstallments: Int,
    val totalPaid: Money,
    /** Cuotas que faltan × valor de la cuota (estimado; el banco puede liquidar distinto). */
    val estimatedBalance: Money,
    val percentPaid: Int,
) {
    val remainingInstallments: Int get() = totalInstallments - paidInstallments
    val isSettled: Boolean get() = remainingInstallments <= 0
}

/** Ingreso fijo esperado (salario): se espera cada periodo y el usuario confirma cuando llega. */
data class ExpectedIncome(
    val id: Long,
    val source: String,
    val amount: Money,
    val day: Int,
    val intervalMonths: Int,
    val destination: IncomeDestination,
    val startMonth: YearMonth,
    val notes: String,
    val active: Boolean,
)

data class ExpectedIncomeRecord(
    val expectedIncomeId: Long,
    val month: YearMonth,
    val incomeId: Long,
    val amount: Money,
    val receivedAt: Long,
)

enum class IncomeStatus(val label: String) { RECEIVED("Recibido"), LATE("Atrasado"), EXPECTED("Esperado") }

data class MonthlyIncome(
    val expected: ExpectedIncome,
    val month: YearMonth,
    val date: LocalDate,
    val status: IncomeStatus,
    val record: ExpectedIncomeRecord?,
) {
    val isReceived: Boolean get() = record != null
}

/** Comprobante adjunto a un gasto (foto o PDF guardado en la carpeta privada de la app). */
data class Attachment(
    val id: Long,
    val expenseId: Long,
    val path: String,
    val mimeType: String,
    val displayName: String,
)

/** Un pago ya hecho de un mes: apunta al gasto que se creó. */
data class FixedPaymentRecord(
    val fixedPaymentId: Long,
    val month: YearMonth,
    val expenseId: Long,
    val amount: Money,
    val paidAt: Long,
)

enum class PaymentStatus(val label: String) {
    PAID("Pagado"),
    OVERDUE("Vencido"),
    DUE_SOON("Vence pronto"),
    PENDING("Pendiente"),
}

/** Cómo está un pago fijo en un mes dado. */
data class MonthlyPayment(
    val payment: FixedPayment,
    val month: YearMonth,
    val dueDate: LocalDate,
    val status: PaymentStatus,
    /** Si se pagó este mes: el registro (con el valor real pagado). */
    val record: FixedPaymentRecord?,
    /** Número de cuota de este mes (solo créditos). */
    val installmentNumber: Int?,
    /** Cuotas que faltan después de este mes (solo créditos). */
    val remainingAfter: Int?,
) {
    val isPaid: Boolean get() = record != null

    /** true si con este pago el crédito queda saldado. */
    val settlesCredit: Boolean get() = remainingAfter == 0
}
