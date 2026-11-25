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
) {
    val isCredit: Boolean get() = totalInstallments != null
}

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
