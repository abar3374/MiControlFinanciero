package com.micontrolfinanciero.app.feature.credit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.component.BackTopBar
import com.micontrolfinanciero.app.core.designsystem.component.ChipOption
import com.micontrolfinanciero.app.core.designsystem.component.ChoiceChips
import com.micontrolfinanciero.app.core.designsystem.component.EmptyState
import com.micontrolfinanciero.app.core.designsystem.component.FieldLabel
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.component.FinanceFormat
import com.micontrolfinanciero.app.core.designsystem.component.MoneyField
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.model.IncomeDestination
import com.micontrolfinanciero.app.core.model.IncomeStatus
import com.micontrolfinanciero.app.core.model.MonthlyIncome
import com.micontrolfinanciero.app.core.model.MonthlyPayment
import com.micontrolfinanciero.app.core.model.PaymentMethod
import com.micontrolfinanciero.app.core.model.PaymentStatus
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/**
 * Pagos del mes: cada pago fijo aparece pendiente o pagado. Se paga con un toque
 * (se crea el gasto), se puede deshacer, y se cambia de mes con las flechas.
 */
@Composable
fun MonthlyPaymentsRoute(
    onBack: () -> Unit,
    onNewPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onNewIncome: () -> Unit,
    onEditIncome: (Long) -> Unit,
    viewModel: MonthlyPaymentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val receive by viewModel.receive.collectAsStateWithLifecycle()
    val confirmUndoIncome by viewModel.confirmUndoIncome.collectAsStateWithLifecycle()
    val pay by viewModel.pay.collectAsStateWithLifecycle()
    val confirmUndo by viewModel.confirmUndo.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        viewModel.onMessageShown()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackTopBar(
                title = "Pagos e ingresos del mes",
                onBack = onBack,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = viewModel::previousMonth) {
                            Icon(painterResource(DesignR.drawable.ic_chevron_left), contentDescription = "Mes anterior", tint = FinanceColors.DarkBlue)
                        }
                        Text(
                            FinanceFormat.month(state.month),
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
                if (!state.loading && state.items.isNotEmpty()) {
                    item {
                        FinanceCard(filled = true) {
                            Row {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pendiente", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                                    Text(FinanceFormat.money(state.pending), style = MaterialTheme.typography.titleLarge, color = FinanceColors.DarkBlue)
                                    Text("${state.pendingCount} por pagar", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Pagado", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                                    Text(FinanceFormat.money(state.paid), style = MaterialTheme.typography.titleLarge, color = FinanceColors.Green)
                                    Text("de ${FinanceFormat.money(state.total)}", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                                }
                            }
                        }
                    }
                }
                if (!state.loading && (state.items.isNotEmpty() || state.incomes.isNotEmpty())) {
                    item { PaymentCalendar(state.month, state.items, state.incomes) }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ingresos fijos", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
                        TextButton(onClick = onNewIncome) { Text("+ Ingreso fijo") }
                    }
                }
                if (state.incomes.isEmpty()) {
                    item {
                        Text(
                            "Agrega tu salario u otro ingreso que llega cada mes. Lo confirmas cuando llegue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinanceColors.MediumGray,
                        )
                    }
                }
                items(state.incomes, key = { "income-${it.expected.id}" }) { income ->
                    IncomeRow(
                        item = income,
                        onReceive = { viewModel.openReceive(income) },
                        onUndo = { viewModel.askUndoIncome(income) },
                        onEdit = { onEditIncome(income.expected.id) },
                    )
                }
                item {
                    Text("Pagos fijos y créditos", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue, modifier = Modifier.padding(top = 8.dp))
                }
                if (!state.loading && state.items.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Sin pagos fijos en este mes",
                            message = "Agrega lo que pagas cada mes (arriendo, internet, cuotas como Sistecrédito). Cada mes vuelve a aparecer pendiente hasta que lo pagues; un crédito saldado deja de aparecer.",
                            iconRes = DesignR.drawable.ic_nav_budget,
                        )
                    }
                }
                items(state.items, key = { it.payment.id }) { item ->
                    PaymentRow(
                        item = item,
                        onPay = { viewModel.openPay(item) },
                        onUndo = { viewModel.askUndo(item) },
                        onEdit = { onEditPayment(item.payment.id) },
                    )
                }
            }
        }

        Button(
            onClick = onNewPayment,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
                .fillMaxWidth(),
        ) { Text("+ Agregar pago fijo o crédito") }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 84.dp, start = 16.dp, end = 16.dp))
    }

    pay?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::closePay,
            title = { Text("Registrar pago") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        listOfNotNull(
                            dialog.item.payment.name,
                            dialog.item.installmentNumber?.let { "Cuota $it de ${dialog.item.payment.totalInstallments}" },
                            FinanceFormat.month(dialog.item.month),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FinanceColors.MediumGray,
                    )
                    MoneyField(digits = dialog.amountDigits, onDigitsChange = viewModel::onPayAmount, label = "Valor pagado", error = dialog.error)
                    FieldLabel("Método de pago")
                    ChoiceChips(
                        options = PaymentMethod.entries.map { ChipOption(it, it.label) },
                        selected = dialog.method,
                        onSelect = viewModel::onPayMethod,
                    )
                    Text(
                        "Se creará el gasto «${dialog.item.payment.name}» y se descontará de su rubro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceColors.MediumGray,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmPay, enabled = !dialog.saving) { Text("Pagar") }
            },
            dismissButton = { TextButton(onClick = viewModel::closePay) { Text("Cancelar") } },
        )
    }

    receive?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::closeReceive,
            title = { Text("Confirmar ingreso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${dialog.item.expected.source} · ${FinanceFormat.month(dialog.item.month)}", style = MaterialTheme.typography.bodyMedium, color = FinanceColors.MediumGray)
                    MoneyField(digits = dialog.amountDigits, onDigitsChange = viewModel::onReceiveAmount, label = "Valor recibido", error = dialog.error)
                    FieldLabel("Recibido en")
                    ChoiceChips(
                        options = IncomeDestination.entries.map { ChipOption(it, it.label) },
                        selected = dialog.destination,
                        onSelect = viewModel::onReceiveDestination,
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmReceive) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = viewModel::closeReceive) { Text("Cancelar") } },
        )
    }

    confirmUndoIncome?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::cancelUndoIncome,
            title = { Text("¿Deshacer la confirmación?") },
            text = { Text("${item.expected.source} volverá a quedar como esperado y se anulará el ingreso registrado (queda en la auditoría).") },
            confirmButton = { TextButton(onClick = viewModel::undoIncome) { Text("Deshacer", color = FinanceColors.Red) } },
            dismissButton = { TextButton(onClick = viewModel::cancelUndoIncome) { Text("Cancelar") } },
        )
    }

    confirmUndo?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::cancelUndo,
            title = { Text("¿Deshacer el pago?") },
            text = { Text("${item.payment.name} volverá a quedar pendiente en ${FinanceFormat.month(item.month)} y se anulará el gasto que se creó (queda en la auditoría).") },
            confirmButton = { TextButton(onClick = viewModel::undo) { Text("Deshacer", color = FinanceColors.Red) } },
            dismissButton = { TextButton(onClick = viewModel::cancelUndo) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PaymentRow(item: MonthlyPayment, onPay: () -> Unit, onUndo: () -> Unit, onEdit: () -> Unit) {
    val (statusColor, statusTint) = statusColors(item.status)
    FinanceCard(modifier = Modifier.clickable(role = Role.Button, onClick = onEdit)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.payment.name, style = MaterialTheme.typography.titleSmall, color = FinanceColors.DarkBlue)
                Text(
                    listOfNotNull(
                        "Vence ${FinanceFormat.date(item.dueDate)}",
                        item.installmentNumber?.let { "cuota $it de ${item.payment.totalInstallments}" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.MediumGray,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    FinanceFormat.money(item.record?.amount ?: item.payment.amount),
                    style = MaterialTheme.typography.titleSmall,
                    color = FinanceColors.DarkBlue,
                )
                Text(
                    item.status.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    modifier = Modifier
                        .background(statusTint, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        if (item.settlesCredit && item.isPaid) {
            Text(
                "¡Crédito saldado! Desde el próximo mes ya no aparece.",
                style = MaterialTheme.typography.bodySmall,
                color = FinanceColors.Green,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.isPaid) {
                OutlinedButton(onClick = onUndo, shape = RoundedCornerShape(12.dp)) { Text("Deshacer pago") }
            } else {
                Button(
                    onClick = onPay,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceColors.Blue),
                ) { Text("Pagar") }
            }
        }
    }
}

@Composable
private fun IncomeRow(item: MonthlyIncome, onReceive: () -> Unit, onUndo: () -> Unit, onEdit: () -> Unit) {
    val (color, tint) = when (item.status) {
        IncomeStatus.RECEIVED -> FinanceColors.Green to FinanceColors.GreenTint
        IncomeStatus.LATE -> FinanceColors.Amber to FinanceColors.AmberTint
        IncomeStatus.EXPECTED -> FinanceColors.Blue to FinanceColors.BlueTint
    }
    FinanceCard(modifier = Modifier.clickable(role = Role.Button, onClick = onEdit)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.expected.source, style = MaterialTheme.typography.titleSmall, color = FinanceColors.DarkBlue)
                Text("Llega el ${FinanceFormat.date(item.date)}", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("+" + FinanceFormat.money(item.record?.amount ?: item.expected.amount), style = MaterialTheme.typography.titleSmall, color = FinanceColors.Green)
                Text(
                    item.status.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    modifier = Modifier.background(tint, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        Row(modifier = Modifier.padding(top = 10.dp)) {
            if (item.isReceived) {
                OutlinedButton(onClick = onUndo, shape = RoundedCornerShape(12.dp)) { Text("Deshacer") }
            } else {
                Button(
                    onClick = onReceive,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceColors.Green),
                ) { Text("Confirmar que llegó") }
            }
        }
    }
}

/** Calendario del mes con un punto de color en cada día que vence un pago o llega un ingreso. */
@Composable
private fun PaymentCalendar(month: java.time.YearMonth, payments: List<MonthlyPayment>, incomes: List<MonthlyIncome>) {
    val dots = mutableMapOf<Int, MutableList<Color>>()
    payments.forEach { p -> dots.getOrPut(p.dueDate.dayOfMonth) { mutableListOf() }.add(statusColors(p.status).first) }
    incomes.forEach { i -> dots.getOrPut(i.date.dayOfMonth) { mutableListOf() }.add(FinanceColors.Green) }
    val today = java.time.LocalDate.now(FinanceFormat.zone)
    val offset = month.atDay(1).dayOfWeek.value - 1 // lunes = 0
    val cells = offset + month.lengthOfMonth()
    FinanceCard {
        Text("Calendario de pagos", style = MaterialTheme.typography.titleSmall, color = FinanceColors.DarkBlue)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { d ->
                Text(d, style = MaterialTheme.typography.labelMedium, color = FinanceColors.MediumGray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        for (week in 0 until (cells + 6) / 7) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                for (dow in 0 until 7) {
                    val day = week * 7 + dow - offset + 1
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (day in 1..month.lengthOfMonth()) {
                            val isToday = month.atDay(day) == today
                            Text(
                                day.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isToday) FinanceColors.White else FinanceColors.DarkBlue,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .background(if (isToday) FinanceColors.Blue else Color.Transparent, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
                                dots[day].orEmpty().take(3).forEach { c ->
                                    Box(modifier = Modifier.size(6.dp).background(c, androidx.compose.foundation.shape.CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            "Rojo vencido · ámbar vence pronto · azul pendiente · verde pagado o ingreso",
            style = MaterialTheme.typography.bodySmall,
            color = FinanceColors.MediumGray,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun statusColors(status: PaymentStatus): Pair<Color, Color> = when (status) {
    PaymentStatus.PAID -> FinanceColors.Green to FinanceColors.GreenTint
    PaymentStatus.OVERDUE -> FinanceColors.Red to FinanceColors.RedTint
    PaymentStatus.DUE_SOON -> FinanceColors.Amber to FinanceColors.AmberTint
    PaymentStatus.PENDING -> FinanceColors.Blue to FinanceColors.BlueTint
}
