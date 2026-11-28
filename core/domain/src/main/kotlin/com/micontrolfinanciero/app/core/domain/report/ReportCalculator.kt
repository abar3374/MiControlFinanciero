package com.micontrolfinanciero.app.core.domain.report

import com.micontrolfinanciero.app.core.domain.finance.DEFAULT_ZONE
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.PaymentMethod
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Periodos de reporte (sección 11 del proyecto). */
enum class ReportPeriodType(val label: String) {
    DAY("Día"), WEEK("Semana"), MONTH("Mes"), QUARTER("Trimestre"), SEMESTER("Semestre"), YEAR("Año")
}

/** Un periodo concreto: [start] incluido, [endExclusive] excluido (fechas locales). */
data class ReportPeriod(val type: ReportPeriodType, val start: LocalDate, val endExclusive: LocalDate) {

    fun startMillis(zone: ZoneId = DEFAULT_ZONE) = start.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endMillis(zone: ZoneId = DEFAULT_ZONE) = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()

    fun previous(): ReportPeriod = of(type, shift(-1))
    fun next(): ReportPeriod = of(type, shift(1))

    private fun shift(n: Long): LocalDate = when (type) {
        ReportPeriodType.DAY -> start.plusDays(n)
        ReportPeriodType.WEEK -> start.plusWeeks(n)
        ReportPeriodType.MONTH -> start.plusMonths(n)
        ReportPeriodType.QUARTER -> start.plusMonths(3 * n)
        ReportPeriodType.SEMESTER -> start.plusMonths(6 * n)
        ReportPeriodType.YEAR -> start.plusYears(n)
    }

    /** "24 nov 2026", "Semana del 23 nov", "Noviembre 2026", "4.º trimestre 2026", "2.º semestre 2026", "2026". */
    fun label(): String {
        val locale = Locale.forLanguageTag("es-CO")
        return when (type) {
            ReportPeriodType.DAY -> start.format(DateTimeFormatter.ofPattern("EEEE d MMM yyyy", locale)).replaceFirstChar { it.titlecase(locale) }
            ReportPeriodType.WEEK -> "Semana del " + start.format(DateTimeFormatter.ofPattern("d MMM", locale)) + " al " +
                endExclusive.minusDays(1).format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
            ReportPeriodType.MONTH -> start.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar { it.titlecase(locale) }
            ReportPeriodType.QUARTER -> "${(start.monthValue - 1) / 3 + 1}.º trimestre ${start.year}"
            ReportPeriodType.SEMESTER -> "${(start.monthValue - 1) / 6 + 1}.º semestre ${start.year}"
            ReportPeriodType.YEAR -> start.year.toString()
        }
    }

    companion object {
        fun of(type: ReportPeriodType, anchor: LocalDate): ReportPeriod = when (type) {
            ReportPeriodType.DAY -> ReportPeriod(type, anchor, anchor.plusDays(1))
            ReportPeriodType.WEEK -> {
                val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ReportPeriod(type, monday, monday.plusWeeks(1))
            }
            ReportPeriodType.MONTH -> anchor.withDayOfMonth(1).let { ReportPeriod(type, it, it.plusMonths(1)) }
            ReportPeriodType.QUARTER -> LocalDate.of(anchor.year, (anchor.monthValue - 1) / 3 * 3 + 1, 1).let { ReportPeriod(type, it, it.plusMonths(3)) }
            ReportPeriodType.SEMESTER -> LocalDate.of(anchor.year, if (anchor.monthValue <= 6) 1 else 7, 1).let { ReportPeriod(type, it, it.plusMonths(6)) }
            ReportPeriodType.YEAR -> LocalDate.of(anchor.year, 1, 1).let { ReportPeriod(type, it, it.plusYears(1)) }
        }
    }
}

/** Gasto con los datos que necesitan los reportes. */
data class ReportExpense(
    val id: Long,
    val concept: String,
    val amount: Money,
    val at: Long,
    val categoryId: Long,
    val categoryName: String,
    val nature: ExpenseNature,
    val method: PaymentMethod,
    /** Gasto fijo: viene de un pago fijo o su rubro es fijo. */
    val isFixed: Boolean,
)

data class ReportIncome(val id: Long, val source: String, val amount: Money, val at: Long, val isFixed: Boolean)

/** Filtros del reporte (sección 11). null = sin filtrar. */
data class ReportFilters(
    val categoryId: Long? = null,
    val nature: ExpenseNature? = null,
    val method: PaymentMethod? = null,
    /** true = solo fijos, false = solo ocasionales. */
    val fixed: Boolean? = null,
) {
    val isActive: Boolean get() = categoryId != null || nature != null || method != null || fixed != null

    fun matches(e: ReportExpense): Boolean =
        (categoryId == null || e.categoryId == categoryId) &&
            (nature == null || e.nature == nature) &&
            (method == null || e.method == method) &&
            (fixed == null || e.isFixed == fixed)
}

/** Una barra o un punto: etiqueta y valor. */
data class ChartPoint(val label: String, val value: Money)

data class Report(
    val period: ReportPeriod,
    val totalExpenses: Money,
    val totalIncome: Money,
    val expenseCount: Int,
    /** Evolución de gastos e ingresos dentro del periodo (por día u hora, o por mes). */
    val evolution: List<Pair<ChartPoint, ChartPoint>>,
    val byCategory: List<ChartPoint>,
    val byMethod: List<ChartPoint>,
    val byNature: List<ChartPoint>,
    val fixed: Money,
    val occasional: Money,
    /** Últimos 6 meses (hasta el fin del periodo): gastos e ingresos por mes. */
    val monthComparison: List<Pair<ChartPoint, ChartPoint>>,
    /** Categoría que más creció frente al periodo anterior del mismo tamaño (null si ninguna creció). */
    val topGrowth: Pair<String, Money>?,
    val expenses: List<ReportExpense>,
    val incomes: List<ReportIncome>,
) {
    val balance: Money get() = totalIncome - totalExpenses
}

/**
 * Arma un reporte con datos reales: recibe los movimientos de los últimos meses y calcula
 * totales, distribuciones, evolución y comparaciones. Función pura (se prueba sin Android).
 */
object ReportCalculator {

    private val locale = Locale.forLanguageTag("es-CO")
    private val shortMonth = DateTimeFormatter.ofPattern("MMM", locale)

    fun build(
        period: ReportPeriod,
        filters: ReportFilters,
        expenses: List<ReportExpense>,
        incomes: List<ReportIncome>,
        zone: ZoneId = DEFAULT_ZONE,
    ): Report {
        val start = period.startMillis(zone)
        val end = period.endMillis(zone)
        val filtered = expenses.filter(filters::matches)
        val inPeriod = filtered.filter { it.at in start until end }.sortedByDescending { it.at }
        // Con filtros de gasto, los ingresos se muestran igual (no tienen categoría ni método).
        val incomesIn = incomes.filter { it.at in start until end }.sortedByDescending { it.at }

        val byCategory = inPeriod.groupBy { it.categoryName }
            .map { (name, list) -> ChartPoint(name, Money.sum(list.map { it.amount })) }
            .sortedByDescending { it.value.cents }
        val byMethod = inPeriod.groupBy { it.method }
            .map { (m, list) -> ChartPoint(m.label, Money.sum(list.map { it.amount })) }
            .sortedByDescending { it.value.cents }
        val byNature = inPeriod.groupBy { it.nature }
            .map { (n, list) -> ChartPoint(n.label, Money.sum(list.map { it.amount })) }
            .sortedByDescending { it.value.cents }

        // Crecimiento por categoría frente al periodo anterior del mismo tipo.
        val prev = period.previous()
        val prevStart = prev.startMillis(zone)
        val prevByCat = filtered.filter { it.at in prevStart until start }
            .groupBy { it.categoryName }.mapValues { (_, l) -> l.sumOf { it.amount.cents } }
        val topGrowth = byCategory
            .map { it.label to Money(it.value.cents - (prevByCat[it.label] ?: 0L)) }
            .filter { it.second.cents > 0 }
            .maxByOrNull { it.second.cents }

        return Report(
            period = period,
            totalExpenses = Money.sum(inPeriod.map { it.amount }),
            totalIncome = Money.sum(incomesIn.map { it.amount }),
            expenseCount = inPeriod.size,
            evolution = evolution(period, inPeriod, incomesIn, zone),
            byCategory = byCategory,
            byMethod = byMethod,
            byNature = byNature,
            fixed = Money.sum(inPeriod.filter { it.isFixed }.map { it.amount }),
            occasional = Money.sum(inPeriod.filter { !it.isFixed }.map { it.amount }),
            monthComparison = monthComparison(period, filtered, incomes, zone),
            topGrowth = topGrowth,
            expenses = inPeriod,
            incomes = incomesIn,
        )
    }

    /** Desde dónde hay que traer datos para armar el reporte (6 meses atrás para comparar). */
    fun dataStart(period: ReportPeriod): LocalDate =
        minOf(period.previous().start, YearMonth.from(period.endExclusive.minusDays(1)).minusMonths(5).atDay(1))

    private fun evolution(
        period: ReportPeriod,
        expenses: List<ReportExpense>,
        incomes: List<ReportIncome>,
        zone: ZoneId,
    ): List<Pair<ChartPoint, ChartPoint>> {
        fun localDate(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        return when (period.type) {
            ReportPeriodType.DAY -> (0 until 24 step 4).map { h ->
                val e = expenses.filter { Instant.ofEpochMilli(it.at).atZone(zone).hour in h until h + 4 }
                val i = incomes.filter { Instant.ofEpochMilli(it.at).atZone(zone).hour in h until h + 4 }
                ChartPoint("${h}h", Money.sum(e.map { it.amount })) to ChartPoint("${h}h", Money.sum(i.map { it.amount }))
            }
            ReportPeriodType.WEEK, ReportPeriodType.MONTH -> {
                val days = generateSequence(period.start) { it.plusDays(1) }.takeWhile { it < period.endExclusive }.toList()
                val eBy = expenses.groupBy { localDate(it.at) }
                val iBy = incomes.groupBy { localDate(it.at) }
                days.map { d ->
                    val label = d.dayOfMonth.toString()
                    ChartPoint(label, Money.sum(eBy[d].orEmpty().map { it.amount })) to ChartPoint(label, Money.sum(iBy[d].orEmpty().map { it.amount }))
                }
            }
            else -> {
                val months = generateSequence(YearMonth.from(period.start)) { it.plusMonths(1) }
                    .takeWhile { it.atDay(1) < period.endExclusive }.toList()
                months.map { m -> monthPoint(m, expenses, incomes, zone) }
            }
        }
    }

    private fun monthComparison(
        period: ReportPeriod,
        expenses: List<ReportExpense>,
        incomes: List<ReportIncome>,
        zone: ZoneId,
    ): List<Pair<ChartPoint, ChartPoint>> {
        val last = YearMonth.from(period.endExclusive.minusDays(1))
        return (5 downTo 0).map { monthPoint(last.minusMonths(it.toLong()), expenses, incomes, zone) }
    }

    private fun monthPoint(m: YearMonth, expenses: List<ReportExpense>, incomes: List<ReportIncome>, zone: ZoneId): Pair<ChartPoint, ChartPoint> {
        val s = m.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val e = m.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val label = m.atDay(1).format(shortMonth).replace(".", "").replaceFirstChar { it.titlecase(locale) }
        return ChartPoint(label, Money.sum(expenses.filter { it.at in s until e }.map { it.amount })) to
            ChartPoint(label, Money.sum(incomes.filter { it.at in s until e }.map { it.amount }))
    }
}
