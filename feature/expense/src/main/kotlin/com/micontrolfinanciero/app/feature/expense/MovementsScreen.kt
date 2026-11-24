package com.micontrolfinanciero.app.feature.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.component.BackTopBar
import com.micontrolfinanciero.app.core.designsystem.component.ChipOption
import com.micontrolfinanciero.app.core.designsystem.component.ChoiceChips
import com.micontrolfinanciero.app.core.designsystem.component.EmptyState
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.component.FinanceFormat
import com.micontrolfinanciero.app.core.designsystem.component.MovementListItem
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.model.MovementKind
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/** Historial de movimientos del mes: búsqueda por concepto, filtro por tipo, editar al tocar. */
@Composable
fun MovementsRoute(
    onBack: () -> Unit,
    onOpenExpense: (Long) -> Unit,
    onOpenIncome: (Long) -> Unit,
    viewModel: MovementsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = "Movimientos", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(painterResource(DesignR.drawable.ic_chevron_left), contentDescription = "Mes anterior", tint = FinanceColors.DarkBlue)
                    }
                    Text(
                        FinanceFormat.month(state.range.month),
                        style = MaterialTheme.typography.titleMedium,
                        color = FinanceColors.DarkBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(painterResource(DesignR.drawable.ic_chevron_right), contentDescription = "Mes siguiente", tint = FinanceColors.DarkBlue)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Buscar por concepto, lugar o categoría") },
                    leadingIcon = { Icon(painterResource(DesignR.drawable.ic_search), contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                ChoiceChips(
                    options = MovementTypeFilter.entries.map { ChipOption(it, it.label) },
                    selected = state.type,
                    onSelect = viewModel::onTypeChange,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            item {
                FinanceCard(filled = true) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Ingresos", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                            Text("+" + FinanceFormat.money(state.totalIncomes), style = MaterialTheme.typography.titleMedium, color = FinanceColors.Green)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Gastos", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                            Text("−" + FinanceFormat.money(state.totalExpenses), style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue)
                        }
                    }
                    Text(
                        "${state.items.size} movimientos en la lista",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceColors.MediumGray,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            if (!state.loading && state.items.isEmpty()) {
                item {
                    EmptyState(
                        title = if (state.query.isBlank()) "Sin movimientos este mes" else "No hay resultados",
                        message = if (state.query.isBlank()) {
                            "Usa el botón + Registrar para anotar tus gastos e ingresos."
                        } else {
                            "Prueba con otra palabra o cambia de mes."
                        },
                        iconRes = DesignR.drawable.ic_list,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            items(state.items, key = { "${it.kind}-${it.id}" }) { item ->
                MovementListItem(
                    item = item,
                    onClick = {
                        if (item.kind == MovementKind.EXPENSE) onOpenExpense(item.id) else onOpenIncome(item.id)
                    },
                )
                HorizontalDivider(color = FinanceColors.Border)
            }
        }
    }
}
