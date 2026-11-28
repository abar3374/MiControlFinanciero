package com.micontrolfinanciero.app.core.reports

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.micontrolfinanciero.app.core.domain.finance.DEFAULT_ZONE
import com.micontrolfinanciero.app.core.domain.report.Report
import com.micontrolfinanciero.app.core.domain.report.XlsxCell
import com.micontrolfinanciero.app.core.domain.report.XlsxSheet
import com.micontrolfinanciero.app.core.domain.report.XlsxWriter
import com.micontrolfinanciero.app.core.model.MoneyFormat
import java.io.OutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Exporta un reporte con datos reales a PDF o Excel. El archivo se escribe donde el usuario
 * elija (selector de Android); la app no lo envía a ningún lado.
 */
object ReportExporter {

    private val locale = Locale.forLanguageTag("es-CO")
    private val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", locale)
    private val money = MoneyFormat.COLOMBIA

    private fun formatAt(ms: Long) = Instant.ofEpochMilli(ms).atZone(DEFAULT_ZONE).format(dateTime)

    // ------------------------------------------------------------------ Excel

    fun writeXlsx(report: Report, filtersText: String, out: OutputStream) {
        val summary = mutableListOf<List<XlsxCell>>(
            listOf(XlsxCell.Text("Concepto"), XlsxCell.Text("Valor (COP)")),
            listOf(XlsxCell.Text("Periodo: ${report.period.label()}"), XlsxCell.Text(filtersText)),
            listOf(XlsxCell.Text("Total ingresos"), num(report.totalIncome.cents)),
            listOf(XlsxCell.Text("Total gastos"), num(report.totalExpenses.cents)),
            listOf(XlsxCell.Text("Balance"), num(report.balance.cents)),
            listOf(XlsxCell.Text("Gastos fijos"), num(report.fixed.cents)),
            listOf(XlsxCell.Text("Gastos ocasionales"), num(report.occasional.cents)),
        )
        val byCategory = listOf(listOf<XlsxCell>(XlsxCell.Text("Categoría"), XlsxCell.Text("Total (COP)"))) +
            report.byCategory.map { listOf(XlsxCell.Text(it.label), num(it.value.cents)) }
        val movements = listOf(
            listOf<XlsxCell>(
                XlsxCell.Text("Fecha"), XlsxCell.Text("Tipo"), XlsxCell.Text("Concepto"), XlsxCell.Text("Categoría"),
                XlsxCell.Text("Clasificación"), XlsxCell.Text("Método"), XlsxCell.Text("Fijo"), XlsxCell.Text("Valor (COP)"),
            ),
        ) + report.expenses.map {
            listOf(
                XlsxCell.Text(formatAt(it.at)), XlsxCell.Text("Gasto"), XlsxCell.Text(it.concept), XlsxCell.Text(it.categoryName),
                XlsxCell.Text(it.nature.label), XlsxCell.Text(it.method.label), XlsxCell.Text(if (it.isFixed) "Sí" else "No"),
                num(-it.amount.cents),
            )
        } + report.incomes.map {
            listOf(
                XlsxCell.Text(formatAt(it.at)), XlsxCell.Text("Ingreso"), XlsxCell.Text(it.source), XlsxCell.Text(""),
                XlsxCell.Text(""), XlsxCell.Text(""), XlsxCell.Text(if (it.isFixed) "Sí" else "No"), num(it.amount.cents),
            )
        }
        val months = listOf(listOf<XlsxCell>(XlsxCell.Text("Mes"), XlsxCell.Text("Gastos"), XlsxCell.Text("Ingresos"))) +
            report.monthComparison.map { (e, i) -> listOf(XlsxCell.Text(e.label), num(e.value.cents), num(i.value.cents)) }
        XlsxWriter.write(
            listOf(
                XlsxSheet("Resumen", summary),
                XlsxSheet("Por categoría", byCategory),
                XlsxSheet("Movimientos", movements),
                XlsxSheet("Comparativo meses", months),
            ),
            out,
        )
    }

    private fun num(cents: Long) = XlsxCell.Number(cents / 100.0)

    // ------------------------------------------------------------------ PDF

    fun writePdf(report: Report, filtersText: String, userName: String, out: OutputStream) {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        val title = Paint().apply { color = Color.rgb(15, 30, 70); textSize = 18f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val h2 = Paint().apply { color = Color.rgb(15, 30, 70); textSize = 13f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val body = Paint().apply { color = Color.rgb(40, 48, 64); textSize = 10f; isAntiAlias = true }
        val muted = Paint().apply { color = Color.rgb(91, 100, 116); textSize = 9f; isAntiAlias = true }
        val right = Paint(body).apply { textAlign = Paint.Align.RIGHT }
        val line = Paint().apply { color = Color.rgb(230, 234, 241); strokeWidth = 1f }

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            y = margin
            page!!.canvas.drawText("Finance · ${report.period.label()} · página $pageNumber", margin, pageHeight - 20f, muted)
        }
        fun ensure(space: Float) { if (page == null || y + space > pageHeight - 40f) newPage() }
        fun text(t: String, p: Paint, gap: Float = 16f) { ensure(gap); page!!.canvas.drawText(t, margin, y + p.textSize, p); y += gap }
        fun row(label: String, value: String) {
            ensure(16f)
            page!!.canvas.drawText(label.take(70), margin, y + 10f, body)
            page!!.canvas.drawText(value, pageWidth - margin, y + 10f, right)
            y += 16f
        }

        newPage()
        text("Reporte financiero", title, 26f)
        text("${report.period.label()}${if (userName.isNotBlank()) " · $userName" else ""}", body)
        text(filtersText, muted, 22f)

        text("Resumen", h2, 20f)
        row("Ingresos", money.format(report.totalIncome))
        row("Gastos (${report.expenseCount})", money.format(report.totalExpenses))
        row("Balance (ingresos − gastos)", money.format(report.balance))
        row("Gastos fijos", money.format(report.fixed))
        row("Gastos ocasionales", money.format(report.occasional))
        report.topGrowth?.let { row("Mayor crecimiento: ${it.first}", "+" + money.format(it.second)) }
        y += 10f

        text("Gastos por categoría", h2, 20f)
        report.byCategory.forEach { row(it.label, money.format(it.value)) }
        y += 10f

        text("Comparativo de los últimos 6 meses (gastos / ingresos)", h2, 20f)
        report.monthComparison.forEach { (e, i) -> row(e.label, "${money.format(e.value)} / ${money.format(i.value)}") }
        y += 10f

        text("Movimientos", h2, 20f)
        val all = report.expenses.map { Triple(it.at, "${it.concept} · ${it.categoryName}", "−" + money.format(it.amount)) } +
            report.incomes.map { Triple(it.at, "${it.source} · ingreso", "+" + money.format(it.amount)) }
        all.sortedByDescending { it.first }.forEach { (at, label, value) ->
            ensure(16f)
            page!!.canvas.drawLine(margin, y, pageWidth - margin, y, line)
            page!!.canvas.drawText(formatAt(at), margin, y + 11f, muted)
            page!!.canvas.drawText(label.take(55), margin + 90f, y + 11f, body)
            page!!.canvas.drawText(value, pageWidth - margin, y + 11f, right)
            y += 16f
        }
        y += 14f
        text("Generado por Finance con los datos guardados en el teléfono. No es un extracto bancario.", muted)

        page?.let { doc.finishPage(it) }
        doc.writeTo(out)
        doc.close()
    }
}
