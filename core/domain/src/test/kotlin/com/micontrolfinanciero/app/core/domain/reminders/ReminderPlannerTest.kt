package com.micontrolfinanciero.app.core.domain.reminders

import com.micontrolfinanciero.app.core.domain.finance.FixedPaymentSchedule
import com.micontrolfinanciero.app.core.model.AppSettings
import com.micontrolfinanciero.app.core.model.FixedPayment
import com.micontrolfinanciero.app.core.model.FixedPaymentRecord
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReminderPlannerTest {

    private val nov = YearMonth.of(2026, 11)
    private val internet = FixedPayment(
        id = 7, name = "Internet", categoryId = 1, budgetId = null, amount = Money.ofUnits(120_000), dueDay = 29,
        totalInstallments = null, initialPaidInstallments = 0, startMonth = nov, paymentMethod = PaymentMethod.PSE,
        notes = "", active = true,
    )

    private fun plan(today: LocalDate, records: List<FixedPaymentRecord> = emptyList(), settings: AppSettings = AppSettings()) =
        ReminderPlanner.plan(listOf(FixedPaymentSchedule.statusOf(internet, nov, records, today)), settings, today)

    @Test
    fun `manana vence con el texto del proyecto`() {
        val r = plan(LocalDate.of(2026, 11, 28)).single()
        assertEquals(1, r.daysBefore)
        assertEquals("Mañana vence Internet", r.title)
        assertEquals("Recordatorio: mañana vence Internet por \$120.000 (29 de noviembre).", r.text)
    }

    @Test
    fun `si el telefono estuvo apagado el aviso de 3 dias llega a los 2`() {
        assertEquals(3, plan(LocalDate.of(2026, 11, 27)).single().daysBefore)
    }

    @Test
    fun `mas de 7 dias antes no avisa y pagado no avisa`() {
        assertTrue(plan(LocalDate.of(2026, 11, 20)).isEmpty())
        val paid = FixedPaymentRecord(7, nov, 1, Money.ofUnits(120_000), 0)
        assertTrue(plan(LocalDate.of(2026, 11, 28), listOf(paid)).isEmpty())
    }

    @Test
    fun `vencido avisa una vez con marca especial`() {
        assertEquals(ReminderPlanner.OVERDUE, plan(LocalDate.of(2026, 11, 30)).single().daysBefore)
    }

    @Test
    fun `respeta ajustes desactivados y personalizado`() {
        assertTrue(plan(LocalDate.of(2026, 11, 28), settings = AppSettings(remindersEnabled = false)).isEmpty())
        val custom = AppSettings(reminderDays = emptySet(), customReminderDays = 10)
        assertEquals(10, plan(LocalDate.of(2026, 11, 20), settings = custom).single().daysBefore)
    }

    @Test
    fun `identificador de notificacion estable`() {
        val a = plan(LocalDate.of(2026, 11, 28)).single()
        val b = plan(LocalDate.of(2026, 11, 28)).single()
        assertEquals(a.notificationId, b.notificationId)
    }
}
