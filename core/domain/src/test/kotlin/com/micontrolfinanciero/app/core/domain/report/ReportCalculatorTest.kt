package com.micontrolfinanciero.app.core.domain.report

import com.micontrolfinanciero.app.core.domain.finance.LocalTimes
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipInputStream

class ReportCalculatorTest {

    private fun at(y: Int, m: Int, d: Int) = LocalTimes.toMillis(LocalDate.of(y, m, d), LocalTime.of(12, 0))

    private fun exp(id: Long, pesos: Long, ms: Long, cat: String, nature: ExpenseNature = ExpenseNature.NEED, fixed: Boolean = false, method: PaymentMethod = PaymentMethod.CASH) =
        ReportExpense(id, "x$id", Money.ofUnits(pesos), ms, cat.hashCode().toLong(), cat, nature, method, fixed)

    private val data = listOf(
        exp(1, 900_000, at(2026, 11, 1), "Vivienda", ExpenseNature.OBLIGATION, fixed = true),
        exp(2, 186_500, at(2026, 11, 24), "Alimentación"),
        exp(3, 48_000, at(2026, 11, 22), "Ocio", ExpenseNature.LEISURE, method = PaymentMethod.CREDIT_CARD),
        exp(4, 100_000, at(2026, 10, 15), "Alimentación"),
    )
    private val incomes = listOf(ReportIncome(1, "Salario", Money.ofUnits(3_800_000), at(2026, 11, 30), true))

    @Test
    fun `periodos se calculan bien`() {
        val d = LocalDate.of(2026, 11, 24)
        assertEquals(LocalDate.of(2026, 11, 23), ReportPeriod.of(ReportPeriodType.WEEK, d).start)
        assertEquals(LocalDate.of(2026, 10, 1), ReportPeriod.of(ReportPeriodType.QUARTER, d).start)
        assertEquals(LocalDate.of(2026, 7, 1), ReportPeriod.of(ReportPeriodType.SEMESTER, d).start)
        assertEquals("4.º trimestre 2026", ReportPeriod.of(ReportPeriodType.QUARTER, d).label())
        assertEquals(LocalDate.of(2026, 10, 1), ReportPeriod.of(ReportPeriodType.MONTH, d).previous().start)
    }

    @Test
    fun `reporte mensual con totales distribuciones y crecimiento`() {
        val r = ReportCalculator.build(ReportPeriod.of(ReportPeriodType.MONTH, LocalDate.of(2026, 11, 24)), ReportFilters(), data, incomes)
        assertEquals(Money.ofUnits(1_134_500), r.totalExpenses)
        assertEquals(Money.ofUnits(3_800_000), r.totalIncome)
        assertEquals("Vivienda", r.byCategory.first().label)
        assertEquals(Money.ofUnits(900_000), r.fixed)
        assertEquals(Money.ofUnits(234_500), r.occasional)
        assertEquals(30, r.evolution.size)
        assertEquals(6, r.monthComparison.size)
        assertEquals("Vivienda", r.topGrowth?.first)
        assertEquals(Money.ofUnits(100_000), r.monthComparison[4].first.value)
    }

    @Test
    fun `filtros por clasificacion metodo y fijo`() {
        val p = ReportPeriod.of(ReportPeriodType.MONTH, LocalDate.of(2026, 11, 24))
        assertEquals(Money.ofUnits(48_000), ReportCalculator.build(p, ReportFilters(nature = ExpenseNature.LEISURE), data, incomes).totalExpenses)
        assertEquals(Money.ofUnits(48_000), ReportCalculator.build(p, ReportFilters(method = PaymentMethod.CREDIT_CARD), data, incomes).totalExpenses)
        assertEquals(Money.ofUnits(900_000), ReportCalculator.build(p, ReportFilters(fixed = true), data, incomes).totalExpenses)
    }

    @Test
    fun `excel generado es un zip valido con sus partes`() {
        val out = ByteArrayOutputStream()
        XlsxWriter.write(
            listOf(XlsxSheet("Movimientos", listOf(listOf(XlsxCell.Text("Concepto <&>"), XlsxCell.Number(200465.0))))),
            out,
        )
        val names = mutableListOf<String>()
        var sheet = ""
        ZipInputStream(out.toByteArray().inputStream()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                names += e.name
                if (e.name == "xl/worksheets/sheet1.xml") sheet = zip.readBytes().toString(Charsets.UTF_8)
                e = zip.nextEntry
            }
        }
        assertTrue(names.containsAll(listOf("[Content_Types].xml", "xl/workbook.xml", "xl/worksheets/sheet1.xml", "xl/styles.xml")))
        assertTrue(sheet.contains("Concepto &lt;&amp;&gt;"))
        assertTrue(sheet.contains("<v>200465.0</v>"))
        assertEquals("AA", XlsxWriter.columnName(26))
    }
}
