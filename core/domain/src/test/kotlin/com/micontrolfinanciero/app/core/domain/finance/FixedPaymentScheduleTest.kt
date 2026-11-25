package com.micontrolfinanciero.app.core.domain.finance

import com.micontrolfinanciero.app.core.model.FixedPayment
import com.micontrolfinanciero.app.core.model.FixedPaymentRecord
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.PaymentMethod
import com.micontrolfinanciero.app.core.model.PaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FixedPaymentScheduleTest {

    private val nov = YearMonth.of(2026, 11)
    private val dec = YearMonth.of(2026, 12)
    private val jan = YearMonth.of(2027, 1)

    private fun payment(id: Long = 1, total: Int? = null, paid: Int = 0, dueDay: Int = 25, start: YearMonth = nov) = FixedPayment(
        id = id, name = "Sistecrédito", categoryId = 1, budgetId = null, amount = Money.ofUnits(200_465),
        dueDay = dueDay, totalInstallments = total, initialPaidInstallments = paid, startMonth = start,
        paymentMethod = PaymentMethod.PSE, notes = "", active = true,
    )

    private fun record(month: YearMonth, id: Long = 1) =
        FixedPaymentRecord(fixedPaymentId = id, month = month, expenseId = 99, amount = Money.ofUnits(200_465), paidAt = 0)

    @Test
    fun `cada mes vuelve a aparecer pendiente`() {
        val p = payment()
        val novPaid = FixedPaymentSchedule.statusOf(p, nov, listOf(record(nov)), LocalDate.of(2026, 11, 26))
        assertEquals(PaymentStatus.PAID, novPaid.status)
        val decStatus = FixedPaymentSchedule.statusOf(p, dec, listOf(record(nov)), LocalDate.of(2026, 12, 1))
        assertEquals(PaymentStatus.PENDING, decStatus.status)
        assertNull(decStatus.record)
    }

    @Test
    fun `la fecha no marca como pagado sino como vencido`() {
        val s = FixedPaymentSchedule.statusOf(payment(), nov, emptyList(), LocalDate.of(2026, 11, 26))
        assertEquals(PaymentStatus.OVERDUE, s.status)
        val soon = FixedPaymentSchedule.statusOf(payment(), nov, emptyList(), LocalDate.of(2026, 11, 21))
        assertEquals(PaymentStatus.DUE_SOON, soon.status)
    }

    @Test
    fun `credito saldado deja de aparecer desde el mes siguiente`() {
        // 6 cuotas, ya iba en la 4 (3 pagadas antes de la app)
        val p = payment(total = 6, paid = 3)
        val records = listOf(record(nov), record(dec), record(jan))
        val janStatus = FixedPaymentSchedule.statusOf(p, jan, records, LocalDate.of(2027, 1, 30))
        assertEquals(6, janStatus.installmentNumber)
        assertTrue(janStatus.settlesCredit)
        assertTrue(FixedPaymentSchedule.isVisible(p, jan, records))
        assertTrue(!FixedPaymentSchedule.isVisible(p, YearMonth.of(2027, 2), records))
    }

    @Test
    fun `numero de cuota avanza con cada pago`() {
        val p = payment(total = 6, paid = 3)
        assertEquals(4, FixedPaymentSchedule.statusOf(p, nov, emptyList(), LocalDate.of(2026, 11, 1)).installmentNumber)
        assertEquals(5, FixedPaymentSchedule.statusOf(p, dec, listOf(record(nov)), LocalDate.of(2026, 12, 1)).installmentNumber)
        assertEquals(1, FixedPaymentSchedule.statusOf(p, dec, listOf(record(nov)), LocalDate.of(2026, 12, 1)).remainingAfter)
    }

    @Test
    fun `dia 31 en mes corto y antes del inicio no aparece`() {
        val p = payment(dueDay = 31, start = dec)
        assertEquals(LocalDate.of(2027, 2, 28), FixedPaymentSchedule.dueDate(p, YearMonth.of(2027, 2)))
        assertTrue(!FixedPaymentSchedule.isVisible(p, nov, emptyList()))
    }

    @Test
    fun `validacion de pago fijo y credito`() {
        val ok = FinanceValidator.validateFixedPayment(FixedPaymentInput("Sistecrédito", "200465", "25", 1L, true, "6", "3", ""))
        assertTrue(ok.isValid)
        assertEquals(6, ok.value!!.totalInstallments)
        val bad = FinanceValidator.validateFixedPayment(FixedPaymentInput("", "0", "40", null, true, "6", "6", ""))
        assertTrue(bad.errors.keys.containsAll(listOf(FormField.NAME, FormField.AMOUNT, FormField.DUE_DAY, FormField.CATEGORY, FormField.PAID_INSTALLMENTS)))
    }
}
