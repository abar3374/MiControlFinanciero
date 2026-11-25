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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import com.micontrolfinanciero.app.core.designsystem.component.NewCategoryDialog
import com.micontrolfinanciero.app.core.model.ExpenseCategory
import com.micontrolfinanciero.app.core.model.ExpenseNature
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
    onOpenPayments: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        viewModel.onMessageShown()
    }

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
                PaymentsLinkCard(onOpenPayments)

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
                    categories = state.categories,
                    onAdd = viewModel::openCreateCategory,
                    onManage = viewModel::openManageCategory,
                )
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    when (val d = dialog) {
        CategoryDialog.None -> Unit
        CategoryDialog.Create -> NewCategoryDialog(
            natureOptions = CLASSIFICATIONS.map { ChipOption(it, it.label) },
            initialNature = ExpenseNature.OTHER,
            validate = { viewModel.nameError(it) },
            onCreate = viewModel::createCategory,
            onDismiss = viewModel::closeDialog,
        )
        is CategoryDialog.Manage -> ManageCategoryDialog(
            category = d.category,
            validate = { viewModel.nameError(it, exceptId = d.category.id) },
            onRename = { name -> viewModel.renameCategory(d.category.id, name) },
            onToggleHidden = { viewModel.setHidden(d.category.id, !d.category.archived) },
            onDismiss = viewModel::closeDialog,
        )
    }
}

@Composable
private fun ManageCategoryDialog(
    category: ExpenseCategory,
    validate: (String) -> String?,
    onRename: (String) -> Unit,
    onToggleHidden: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FinanceTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = "Nombre",
                    error = error,
                    maxLength = 40,
                )
                OutlinedButton(onClick = onToggleHidden, modifier = Modifier.fillMaxWidth()) {
                    Text(if (category.archived) "Volver a mostrar" else "Ocultar categoría")
                }
                Text(
                    if (category.archived) {
                        "Está oculta: no aparece al registrar gastos."
                    } else {
                        "Ocultarla no borra nada: los gastos que la usan la conservan y la puedes volver a mostrar."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.MediumGray,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val problem = if (name.trim().equals(category.name, ignoreCase = false)) null else validate(name)
                if (problem != null) error = problem else if (name.trim() != category.name) onRename(name.trim()) else onDismiss()
            }) { Text("Guardar nombre") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

@Composable
private fun PaymentsLinkCard(onOpenPayments: () -> Unit) {
    FinanceCard(modifier = Modifier.clickable(role = Role.Button, onClick = onOpenPayments)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pagos del mes", style = MaterialTheme.typography.titleSmall, color = FinanceColors.DarkBlue)
                Text(
                    "Arriendo, servicios y cuotas de créditos: cada mes vuelven a quedar pendientes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.MediumGray,
                )
            }
            Icon(painterResource(DesignR.drawable.ic_chevron_right), contentDescription = null, tint = FinanceColors.Blue)
        }
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
private fun CategoriesSection(
    categories: List<ExpenseCategory>,
    onAdd: () -> Unit,
    onManage: (ExpenseCategory) -> Unit,
) {
    val visible = categories.filter { !it.archived }
    val hidden = categories.filter { it.archived }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Categorías", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onAdd, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(40.dp)) { Text("+ Nueva") }
        }
        Text("Toca una categoría para renombrarla u ocultarla.", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visible.forEach { category -> CategoryLabel(category.name, hidden = false) { onManage(category) } }
        }
        if (hidden.isNotEmpty()) {
            Text("Ocultas", style = MaterialTheme.typography.labelLarge, color = FinanceColors.MediumGray, modifier = Modifier.padding(top = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                hidden.forEach { category -> CategoryLabel(category.name, hidden = true) { onManage(category) } }
            }
        }
    }
}

@Composable
private fun CategoryLabel(name: String, hidden: Boolean, onClick: () -> Unit) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyMedium,
        color = if (hidden) FinanceColors.MediumGray else FinanceColors.DarkBlue,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (hidden) FinanceColors.White else FinanceColors.LightGray, RoundedCornerShape(999.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
