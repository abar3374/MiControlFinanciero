package com.micontrolfinanciero.app.core.domain.finance

import com.micontrolfinanciero.app.core.model.FixedPayment
import com.micontrolfinanciero.app.core.model.FixedPaymentRecord
import com.micontrolfinanciero.app.core.model.MonthlyPayment
import com.micontrolfinanciero.app.core.model.PaymentStatus
import java.time.LocalDate
import java.time.YearMonth

/**
 * Reglas de los pagos del mes:
 *  1. Cada mes, todo pago fijo activo que ya empezó aparece de nuevo (pendiente o pagado).
 *  2. Un pago está "pagado" en un mes solo si hay un registro de pago de ese mes.
 *     La llegada de la fecha NO lo marca como pagado (sección 7 del proyecto).
 *  3. Un crédito aparece mientras al comenzar el mes todavía deba cuotas. El mes en que se
 *     paga la última cuota sigue apareciendo (como pagado); desde el mes siguiente ya no.
 */
object FixedPaymentSchedule {

    /** Días antes del vencimiento en que se marca "Vence pronto". */
    const val DUE_SOON_DAYS = 5L

    fun dueDate(payment: FixedPayment, month: YearMonth): LocalDate =
        month.atDay(payment.dueDay.coerceIn(1, month.lengthOfMonth()))

    /** Cuotas pagadas antes de [month] (las iniciales más las registradas en la app). */
    fun paidBefore(payment: FixedPayment, month: YearMonth, records: List<FixedPaymentRecord>): Int =
        payment.initialPaidInstallments + records.count { it.fixedPaymentId == payment.id && it.month < month }

    fun isVisible(payment: FixedPayment, month: YearMonth, records: List<FixedPaymentRecord>): Boolean {
        if (!payment.active || month < payment.startMonth) return false
        val total = payment.totalInstallments ?: return true
        return paidBefore(payment, month, records) < total
    }

    /** Estado de cada pago visible en el mes, ordenados: vencidos, próximos, pendientes y pagados. */
    fun forMonth(
        payments: List<FixedPayment>,
        records: List<FixedPaymentRecord>,
        month: YearMonth,
        today: LocalDate,
    ): List<MonthlyPayment> = payments
        .filter { isVisible(it, month, records) }
        .map { statusOf(it, month, records, today) }
        .sortedWith(compareBy<MonthlyPayment> { it.status.ordinalForSort() }.thenBy { it.dueDate }.thenBy { it.payment.name })

    fun statusOf(payment: FixedPayment, month: YearMonth, records: List<FixedPaymentRecord>, today: LocalDate): MonthlyPayment {
        val record = records.firstOrNull { it.fixedPaymentId == payment.id && it.month == month }
        val due = dueDate(payment, month)
        val status = when {
            record != null -> PaymentStatus.PAID
            today.isAfter(due) -> PaymentStatus.OVERDUE
            !today.isBefore(due.minusDays(DUE_SOON_DAYS)) -> PaymentStatus.DUE_SOON
            else -> PaymentStatus.PENDING
        }
        val total = payment.totalInstallments
        val before = paidBefore(payment, month, records)
        return MonthlyPayment(
            payment = payment,
            month = month,
            dueDate = due,
            status = status,
            record = record,
            installmentNumber = total?.let { before + 1 },
            remainingAfter = total?.let { it - before - 1 },
        )
    }

    private fun PaymentStatus.ordinalForSort(): Int = when (this) {
        PaymentStatus.OVERDUE -> 0
        PaymentStatus.DUE_SOON -> 1
        PaymentStatus.PENDING -> 2
        PaymentStatus.PAID -> 3
    }
}
