package com.micontrolfinanciero.app.feature.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.component.ChartLegend
import com.micontrolfinanciero.app.core.designsystem.component.ChipOption
import com.micontrolfinanciero.app.core.designsystem.component.ChoiceChips
import com.micontrolfinanciero.app.core.designsystem.component.EmptyState
import com.micontrolfinanciero.app.core.designsystem.component.EvolutionChart
import com.micontrolfinanciero.app.core.designsystem.component.FieldLabel
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.component.FinanceFormat
import com.micontrolfinanciero.app.core.designsystem.component.GroupedBars
import com.micontrolfinanciero.app.core.designsystem.component.HorizontalBars
import com.micontrolfinanciero.app.core.designsystem.component.ScreenHeader
import com.micontrolfinanciero.app.core.designsystem.component.SplitBar
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.report.Report
import com.micontrolfinanciero.app.core.domain.report.ReportFilters
import com.micontrolfinanciero.app.core.domain.report.ReportPeriodType
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.PaymentMethod
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/**
 * Pestaña Reportes: periodo (día a año), filtros, gráficas con datos reales y exportación
 * a PDF o Excel. Todo se calcula en el teléfono.
 */
@Composable
fun ReportsRoute(modifier: Modifier = Modifier, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.PDF.mime)) { uri ->
        uri?.let { viewModel.export(ExportFormat.PDF, it) }
    }
    val xlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.EXCEL.mime)) { uri ->
        uri?.let { viewModel.export(ExportFormat.EXCEL, it) }
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.onMessageShown() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenHeader(title = "Reportes")

            ChoiceChips(
                options = ReportPeriodType.entries.map { ChipOption(it, it.label) },
                selected = state.period.type,
                onSelect = viewModel::setType,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::previous) {
                    Icon(painterResource(DesignR.drawable.ic_chevron_left), contentDescription = "Periodo anterior", tint = FinanceColors.DarkBlue)
                }
                Text(
                    state.period.label(),
                    style = MaterialTheme.typography.titleMedium,
                    color = FinanceColors.DarkBlue,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::next) {
                    Icon(painterResource(DesignR.drawable.ic_chevron_right), contentDescription = "Periodo siguiente", tint = FinanceColors.DarkBlue)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    viewModel.filtersText(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.filters.isActive) FinanceColors.Blue else FinanceColors.MediumGray,
                    modifier = Modifier.weight(1f),
                )
                if (state.filters.isActive) TextButton(onClick = viewModel::clearFilters) { Text("Quitar") }
                TextButton(onClick = { showFilters = !showFilters }) { Text(if (showFilters) "Ocultar filtros" else "Filtros") }
            }
            if (showFilters) FiltersCard(state, viewModel::setFilters)

            val report = state.report
            when {
                state.loading || report == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                }
                report.expenseCount == 0 && report.incomes.isEmpty() -> EmptyState(
                    title = "Sin movimientos en este periodo",
                    message = "Cuando registres gastos o ingresos en ${state.period.label().lowercase()} aparecerán aquí con sus gráficas.",
                    iconRes = DesignR.drawable.ic_nav_reports,
                )
                else -> ReportContent(report, state)
            }

            FinanceCard {
                Text("Exportar", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue)
                Text(
                    "Guarda este reporte (con los filtros aplicados) donde elijas. El archivo exportado no va cifrado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.MediumGray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                )
                val enabled = report != null && (report.expenseCount > 0 || report.incomes.isNotEmpty())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { pdfLauncher.launch(viewModel.suggestedFileName(ExportFormat.PDF)) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text("PDF") }
                    OutlinedButton(
                        onClick = { xlsxLauncher.launch(viewModel.suggestedFileName(ExportFormat.EXCEL)) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text("Excel") }
                }
            }
            Spacer(Modifier.size(8.dp))
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun FiltersCard(state: ReportsState, onChange: (ReportFilters) -> Unit) {
    val f = state.filters
    FinanceCard(filled = true) {
        Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Categoría")
            ChoiceChips(
                options = listOf(ChipOption<Long?>(null, "Todas")) + state.categories.map { ChipOption<Long?>(it.id, it.name) },
                selected = f.categoryId,
                onSelect = { onChange(f.copy(categoryId = it)) },
            )
            FieldLabel("Clasificación")
            ChoiceChips(
                options = listOf(ChipOption<ExpenseNature?>(null, "Todas")) + ExpenseNature.entries.map { ChipOption<ExpenseNature?>(it, it.label) },
                selected = f.nature,
                onSelect = { onChange(f.copy(nature = it)) },
            )
            FieldLabel("Método de pago")
            ChoiceChips(
                options = listOf(ChipOption<PaymentMethod?>(null, "Todos")) + PaymentMethod.entries.map { ChipOption<PaymentMethod?>(it, it.label) },
                selected = f.method,
                onSelect = { onChange(f.copy(method = it)) },
            )
            FieldLabel("Tipo de gasto")
            ChoiceChips(
                options = listOf(ChipOption<Boolean?>(null, "Todos"), ChipOption<Boolean?>(true, "Fijos"), ChipOption<Boolean?>(false, "Ocasionales")),
                selected = f.fixed,
                onSelect = { onChange(f.copy(fixed = it)) },
            )
        }
    }
}

@Composable
private fun ReportContent(report: Report, state: ReportsState) {
    val fmt: (Money) -> String = FinanceFormat::money

    // Resumen
    FinanceCard {
        Row(Modifier.fillMaxWidth()) {
            Stat("Ingresos", fmt(report.totalIncome), FinanceColors.Green, Modifier.weight(1f))
            Stat("Gastos", fmt(report.totalExpenses), FinanceColors.DarkBlue, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Stat("Balance", fmt(report.balance), if (report.balance.cents < 0) FinanceColors.Red else FinanceColors.Green, Modifier.weight(1f))
            Stat("Movimientos", "${report.expenseCount + report.incomes.size}", FinanceColors.DarkBlue, Modifier.weight(1f))
        }
        report.topGrowth?.let { (name, delta) ->
            Text(
                "Mayor aumento frente al periodo anterior: $name (+${fmt(delta)})",
                style = MaterialTheme.typography.bodySmall,
                color = FinanceColors.MediumGray,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }

    ChartCard("Evolución") {
        ChartLegend(listOf(FinanceColors.Blue to "Gastos", FinanceColors.Green to "Ingresos"))
        EvolutionChart(
            labels = report.evolution.map { it.first.label },
            expenses = report.evolution.map { it.first.value },
            incomes = report.evolution.map { it.second.value },
            description = "Evolución de gastos e ingresos en ${report.period.label()}",
            modifier = Modifier.padding(top = 10.dp),
        )
    }

    if (report.byCategory.isNotEmpty()) ChartCard("Gastos por categoría") {
        HorizontalBars(report.byCategory.map { it.label to it.value }, fmt)
    }

    state.monthSummary?.takeIf { it.budgets.isNotEmpty() && !state.filters.isActive }?.let { s ->
        ChartCard("Presupuesto vs real") {
            ChartLegend(listOf(androidx.compose.ui.graphics.Color(0xFF93C5FD) to "Presupuesto", FinanceColors.Blue to "Gastado"))
            val items = s.budgets.take(6)
            GroupedBars(
                labels = items.map { it.categoryName.take(8) },
                first = items.map { it.budget.amount },
                second = items.map { it.spent },
                firstColor = androidx.compose.ui.graphics.Color(0xFF93C5FD),
                secondColor = FinanceColors.Blue,
                description = "Presupuesto frente a gasto real por rubro",
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }

    if (report.expenseCount > 0) ChartCard("Fijos vs ocasionales") {
        SplitBar("Fijos" to report.fixed, "Ocasionales" to report.occasional, fmt)
    }

    if (report.byNature.isNotEmpty()) ChartCard("Por clasificación (necesidad, ocio…)") {
        HorizontalBars(report.byNature.map { it.label to it.value }, fmt)
    }

    if (report.byMethod.isNotEmpty()) ChartCard("Por método de pago") {
        HorizontalBars(report.byMethod.map { it.label to it.value }, fmt)
    }

    ChartCard("Comparativo de 6 meses") {
        ChartLegend(listOf(FinanceColors.Blue to "Gastos", FinanceColors.Green to "Ingresos"))
        GroupedBars(
            labels = report.monthComparison.map { it.first.label },
            first = report.monthComparison.map { it.first.value },
            second = report.monthComparison.map { it.second.value },
            firstColor = FinanceColors.Blue,
            secondColor = FinanceColors.Green,
            description = "Gastos e ingresos de los últimos seis meses",
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    FinanceCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}
