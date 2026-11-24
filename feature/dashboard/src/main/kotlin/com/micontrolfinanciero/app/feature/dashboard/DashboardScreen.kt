package com.micontrolfinanciero.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.component.BudgetProgressBar
import com.micontrolfinanciero.app.core.designsystem.component.EmptyState
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.component.FinanceFormat
import com.micontrolfinanciero.app.core.designsystem.component.MovementListItem
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.budget.BudgetMath
import com.micontrolfinanciero.app.core.model.MonthSummary
import com.micontrolfinanciero.app.core.model.MovementItem
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/** Punto de entrada que usa la navegación: conecta el ViewModel con la pantalla. */
@Composable
fun DashboardRoute(
    onOpenProfile: () -> Unit,
    onRegisterExpense: () -> Unit,
    onRegisterIncome: () -> Unit,
    onOpenMovements: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenMovement: (MovementItem) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Al volver a la app se actualiza el saludo (mañana, tarde, noche), la fecha y el mes.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshClock()
        onPauseOrDispose { }
    }

    DashboardScreen(
        state = state,
        onOpenProfile = onOpenProfile,
        onRegisterExpense = onRegisterExpense,
        onRegisterIncome = onRegisterIncome,
        onOpenMovements = onOpenMovements,
        onOpenBudget = onOpenBudget,
        onOpenMovement = onOpenMovement,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onOpenProfile: () -> Unit,
    onRegisterExpense: () -> Unit,
    onRegisterIncome: () -> Unit,
    onOpenMovements: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenMovement: (MovementItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DashboardHeader(onOpenProfile = onOpenProfile)

        FinanceCard(filled = true) {
            Text(state.greeting + ",", style = MaterialTheme.typography.bodyMedium, color = FinanceColors.MediumGray)
            Text(
                text = state.userName ?: stringResource(R.string.dashboard_welcome_no_name),
                style = MaterialTheme.typography.headlineMedium,
                color = FinanceColors.DarkBlue,
            )
            Text(
                text = state.formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = FinanceColors.MediumGray,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        val summary = state.summary
        val hasData = summary != null &&
            (!summary.spent.isZero || !summary.income.isZero || summary.budgets.isNotEmpty() || state.recent.isNotEmpty())

        if (summary == null || !hasData) {
            EmptyState(
                title = "Empecemos",
                message = "Registra tu primer gasto o ingreso, o crea tus rubros en Presupuesto. Aquí verás cuánto te queda, en qué se te va la plata y tus últimos movimientos.",
            )
            QuickActions(onRegisterExpense, onRegisterIncome)
            return@Column
        }

        BudgetHeroCard(summary = summary, onOpenBudget = onOpenBudget)
        IncomeVsExpenseCard(summary)
        QuickActions(onRegisterExpense, onRegisterIncome)
        if (summary.byCategory.isNotEmpty()) CategoryCard(summary)
        RecentCard(items = state.recent, onOpenMovements = onOpenMovements, onOpenMovement = onOpenMovement)
    }
}

@Composable
private fun DashboardHeader(onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(FinanceColors.Blue, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("F", color = FinanceColors.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
            Text(stringResource(R.string.dashboard_app_name), style = MaterialTheme.typography.titleLarge, color = FinanceColors.DarkBlue)
        }
        // El acceso a notificaciones se agrega en la Fase 6, cuando existan recordatorios.
        FilledIconButton(
            onClick = onOpenProfile,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = FinanceColors.BlueTint, contentColor = FinanceColors.Blue),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(painterResource(DesignR.drawable.ic_nav_profile), contentDescription = stringResource(R.string.dashboard_open_profile))
        }
    }
}

/** Tarjeta azul principal: disponible del presupuesto del mes. */
@Composable
private fun BudgetHeroCard(summary: MonthSummary, onOpenBudget: () -> Unit) {
    val percent = BudgetMath.percentUsed(summary.budgetTotal, summary.spentInBudgets)
    Surface(
        onClick = onOpenBudget,
        shape = RoundedCornerShape(22.dp),
        color = FinanceColors.Blue,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (summary.budgets.isEmpty()) {
                Text("Presupuesto · ${FinanceFormat.month(summary.month)}", color = FinanceColors.BlueTint, style = MaterialTheme.typography.bodyMedium)
                Text("Sin rubros todavía", color = FinanceColors.White, style = MaterialTheme.typography.headlineMedium)
                Text("Toca aquí para crear tus rubros del mes.", color = FinanceColors.BlueTint, style = MaterialTheme.typography.bodySmall)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Disponible del presupuesto · ${FinanceFormat.month(summary.month)}",
                        color = FinanceColors.BlueTint,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text("$percent % usado", color = FinanceColors.White, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    FinanceFormat.money(summary.budgetRemaining),
                    color = FinanceColors.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                BudgetProgressBar(percent = percent, trackColor = FinanceColors.White.copy(alpha = 0.25f))
                Text(
                    "Gastado ${FinanceFormat.money(summary.spentInBudgets)} de ${FinanceFormat.money(summary.budgetTotal)}",
                    color = FinanceColors.BlueTint,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun IncomeVsExpenseCard(summary: MonthSummary) {
    val spentShare = BudgetMath.percentUsed(summary.income, summary.spent)
    FinanceCard {
        Text("Ingresos vs. gastos", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue)
        Row(modifier = Modifier.padding(top = 10.dp)) {
            Text("Ingresos", style = MaterialTheme.typography.bodyLarge, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
            Text("+" + FinanceFormat.money(summary.income), style = MaterialTheme.typography.titleSmall, color = FinanceColors.Green)
        }
        Row(modifier = Modifier.padding(top = 6.dp)) {
            Text("Gastos", style = MaterialTheme.typography.bodyLarge, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
            Text("−" + FinanceFormat.money(summary.spent), style = MaterialTheme.typography.titleSmall, color = FinanceColors.DarkBlue)
        }
        if (!summary.income.isZero) {
            BudgetProgressBar(percent = spentShare, modifier = Modifier.padding(vertical = 10.dp))
            Text("Has gastado el $spentShare % de lo que entró este mes.", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
        }
        HorizontalDivider(color = FinanceColors.Border, modifier = Modifier.padding(vertical = 10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Te queda este mes", style = MaterialTheme.typography.titleSmall, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
            Text(
                FinanceFormat.money(summary.balance),
                style = MaterialTheme.typography.titleLarge,
                color = if (summary.balance.isNegative) FinanceColors.Red else FinanceColors.DarkBlue,
            )
        }
        Text(
            "Es lo que entró menos lo que gastaste según tus registros; no es el saldo de tu banco.",
            style = MaterialTheme.typography.bodySmall,
            color = FinanceColors.MediumGray,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun QuickActions(onRegisterExpense: () -> Unit, onRegisterIncome: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onRegisterExpense,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
        ) { Text("− Gasto") }
        Button(
            onClick = onRegisterIncome,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FinanceColors.Green),
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
        ) { Text("+ Ingreso") }
    }
}

/** ¿En qué se fue la plata? Barras por categoría (las gráficas completas llegan en la Fase 7). */
@Composable
private fun CategoryCard(summary: MonthSummary) {
    FinanceCard {
        Text("¿En qué se fue la plata?", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue)
        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            summary.byCategory.take(6).forEach { cat ->
                val share = BudgetMath.percentUsed(summary.spent, cat.total)
                Column {
                    Row {
                        Text(cat.name, style = MaterialTheme.typography.bodyMedium, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
                        Text("${FinanceFormat.money(cat.total)} · $share %", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(FinanceColors.LightGray, RoundedCornerShape(6.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(share.coerceIn(0, 100) / 100f)
                                .height(6.dp)
                                .background(FinanceColors.DarkBlue, RoundedCornerShape(6.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCard(items: List<MovementItem>, onOpenMovements: () -> Unit, onOpenMovement: (MovementItem) -> Unit) {
    FinanceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Actividad reciente", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenMovements) { Text("Ver todo") }
        }
        if (items.isEmpty()) {
            Text("Sin movimientos todavía.", style = MaterialTheme.typography.bodyMedium, color = FinanceColors.MediumGray)
        }
        items.forEachIndexed { index, item ->
            MovementListItem(item = item, onClick = { onOpenMovement(item) })
            if (index < items.lastIndex) HorizontalDivider(color = FinanceColors.Border)
        }
    }
}
