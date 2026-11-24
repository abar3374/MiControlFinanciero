package com.micontrolfinanciero.app.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.component.BudgetProgressBar
import com.micontrolfinanciero.app.core.designsystem.component.ChipOption
import com.micontrolfinanciero.app.core.designsystem.component.ChoiceChips
import com.micontrolfinanciero.app.core.designsystem.component.EmptyState
import com.micontrolfinanciero.app.core.designsystem.component.FieldLabel
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.component.FinanceFormat
import com.micontrolfinanciero.app.core.designsystem.component.FinanceTextField
import com.micontrolfinanciero.app.core.designsystem.component.ScreenHeader
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.budget.BudgetMath
import com.micontrolfinanciero.app.core.model.BudgetProgress
import com.micontrolfinanciero.app.core.model.CLASSIFICATIONS
import com.micontrolfinanciero.app.core.model.MonthSummary
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/** Pestaña Presupuesto: resumen del mes, rubros con su avance y categorías. */
@Composable
fun BudgetRoute(
    onNewBudget: () -> Unit,
    onOpenBudget: (Long) -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        val summary = state.summary
        if (state.loading || summary == null) {
            CircularProgressIndicator(color = FinanceColors.Blue, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScreenHeader(title = "Presupuesto")
                MonthSelector(
                    label = FinanceFormat.month(state.range.month),
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                )
                SummaryCard(summary)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rubros del mes", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
                    Button(onClick = onNewBudget, shape = RoundedCornerShape(12.dp)) { Text("+ Nuevo rubro") }
                }
                if (summary.budgets.isEmpty()) {
                    EmptyState(
                        title = "Aún no tienes rubros",
                        message = "Crea rubros como Arriendo, Mercado o Transporte con su valor del mes. Cada gasto de esa categoría se descuenta solo y te avisa al llegar al 50, 75, 90 y 100 %.",
                        iconRes = DesignR.drawable.ic_nav_budget,
                    )
                } else {
                    summary.budgets.sortedByDescending { BudgetMath.percentUsed(it.budget.amount, it.spent) }.forEach { progress ->
                        BudgetRow(progress = progress, onClick = { onOpenBudget(progress.budget.id) })
                    }
                }

                if (state.inactiveBudgets.isNotEmpty()) {
                    Text("Rubros inactivos", style = MaterialTheme.typography.titleSmall, color = FinanceColors.MediumGray)
                    state.inactiveBudgets.forEach { budget ->
                        FinanceCard(filled = true) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) { onOpenBudget(budget.id) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(budget.name, color = FinanceColors.MediumGray, modifier = Modifier.weight(1f))
                                Text(FinanceFormat.money(budget.amount), color = FinanceColors.MediumGray)
                            }
                        }
                    }
                }

                CategoriesSection(
                    names = state.categories.map { it.name },
                    onAdd = viewModel::openCategoryDialog,
                )
            }
        }
    }

    if (dialog.visible) {
        AlertDialog(
            onDismissRequest = viewModel::closeCategoryDialog,
            title = { Text("Nueva categoría") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinanceTextField(
                        value = dialog.name,
                        onValueChange = viewModel::onCategoryNameChange,
                        label = "Nombre (ejemplo: Mascotas)",
                        error = dialog.error,
                        maxLength = 40,
                    )
                    FieldLabel("Clasificación habitual")
                    ChoiceChips(
                        options = CLASSIFICATIONS.map { ChipOption(it, it.label) },
                        selected = dialog.nature,
                        onSelect = viewModel::onCategoryNatureChange,
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::saveCategory) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = viewModel::closeCategoryDialog) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun MonthSelector(label: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(DesignR.drawable.ic_chevron_left), contentDescription = "Mes anterior", tint = FinanceColors.DarkBlue)
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconButton(onClick = onNext) {
            Icon(painterResource(DesignR.drawable.ic_chevron_right), contentDescription = "Mes siguiente", tint = FinanceColors.DarkBlue)
        }
    }
}

@Composable
private fun SummaryCard(summary: MonthSummary) {
    val percent = BudgetMath.percentUsed(summary.budgetTotal, summary.spentInBudgets)
    FinanceCard(filled = true) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Presupuesto global del mes", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                Text(FinanceFormat.money(summary.budgetTotal), style = MaterialTheme.typography.headlineMedium, color = FinanceColors.DarkBlue)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Restante", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                Text(
                    FinanceFormat.money(summary.budgetRemaining),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (summary.budgetRemaining.isNegative) FinanceColors.Red else FinanceColors.Green,
                )
            }
        }
        BudgetProgressBar(percent = percent, height = 10, modifier = Modifier.padding(vertical = 10.dp))
        Text(
            "Gastado en rubros: ${FinanceFormat.money(summary.spentInBudgets)} ($percent %) · Alertas: 50 · 75 · 90 · 100 %",
            style = MaterialTheme.typography.bodySmall,
            color = FinanceColors.MediumGray,
        )
        val outside = summary.spent - summary.spentInBudgets
        if (!outside.isZero) {
            Text(
                "Gastos sin rubro este mes: ${FinanceFormat.money(outside)}",
                style = MaterialTheme.typography.bodySmall,
                color = FinanceColors.Amber,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun BudgetRow(progress: BudgetProgress, onClick: () -> Unit) {
    val percent = BudgetMath.percentUsed(progress.budget.amount, progress.spent)
    val (badge, badgeColor) = when {
        percent >= 100 -> "Superado" to FinanceColors.Red
        percent >= 90 -> "Alerta 90 %" to FinanceColors.Red
        percent >= 75 -> "Alerta 75 %" to FinanceColors.Amber
        else -> "En curso" to FinanceColors.Blue
    }
    FinanceCard(modifier = Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(progress.budget.name, style = MaterialTheme.typography.titleSmall, color = FinanceColors.DarkBlue)
                Text(
                    "${progress.categoryName} · ${progress.budget.frequencyType.label} · ${progress.budget.nature.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.MediumGray,
                )
            }
            Text(badge, style = MaterialTheme.typography.labelMedium, color = badgeColor)
        }
        BudgetProgressBar(percent = percent, modifier = Modifier.padding(vertical = 10.dp))
        Row {
            Text(
                "${FinanceFormat.money(progress.spent)} de ${FinanceFormat.money(progress.budget.amount)}",
                style = MaterialTheme.typography.bodySmall,
                color = FinanceColors.MediumGray,
                modifier = Modifier.weight(1f),
            )
            Text("$percent %", style = MaterialTheme.typography.labelLarge, color = FinanceColors.DarkBlue)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoriesSection(names: List<String>, onAdd: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Categorías", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onAdd, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(40.dp)) { Text("+ Nueva") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            names.forEach { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinanceColors.DarkBlue,
                    modifier = Modifier
                        .background(FinanceColors.LightGray, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
