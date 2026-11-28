package com.micontrolfinanciero.app.feature.credit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.component.BackTopBar
import com.micontrolfinanciero.app.core.designsystem.component.BudgetProgressBar
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.component.ChipOption
import com.micontrolfinanciero.app.core.designsystem.component.ChoiceChips
import com.micontrolfinanciero.app.core.designsystem.component.ConfirmDeleteDialog
import com.micontrolfinanciero.app.core.designsystem.component.DateTimeRow
import com.micontrolfinanciero.app.core.designsystem.component.FieldError
import com.micontrolfinanciero.app.core.designsystem.component.FieldLabel
import com.micontrolfinanciero.app.core.designsystem.component.FinanceFormat
import com.micontrolfinanciero.app.core.designsystem.component.FinanceTextField
import com.micontrolfinanciero.app.core.designsystem.component.MoneyField
import com.micontrolfinanciero.app.core.designsystem.component.NewCategoryDialog
import com.micontrolfinanciero.app.core.designsystem.component.PrimaryActionButton
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.finance.FinanceValidator
import com.micontrolfinanciero.app.core.domain.finance.FormField
import com.micontrolfinanciero.app.core.model.CLASSIFICATIONS
import com.micontrolfinanciero.app.core.model.CreditSummary
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.FixedPaymentRecord
import com.micontrolfinanciero.app.core.model.MonthInterval
import com.micontrolfinanciero.app.core.model.PaymentMethod
import java.time.LocalTime
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/** Crear o editar un pago fijo (arriendo, servicio) o un crédito con cuotas. */
@Composable
fun FixedPaymentFormRoute(
    onDone: () -> Unit,
    viewModel: FixedPaymentFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showNewCategory by remember { mutableStateOf(false) }

    LaunchedEffect(state.closed) { if (state.closed) onDone() }
    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        snackbar.showSnackbar(error)
        viewModel.onErrorShown()
    }

    if (showNewCategory) {
        NewCategoryDialog(
            natureOptions = CLASSIFICATIONS.map { ChipOption(it, it.label) },
            initialNature = ExpenseNature.OBLIGATION,
            validate = { name -> FinanceValidator.categoryNameError(name, state.categories.map { it.name }) },
            onCreate = { name, nature ->
                viewModel.createCategory(name, nature)
                showNewCategory = false
            },
            onDismiss = { showNewCategory = false },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.loading) {
            CircularProgressIndicator(color = FinanceColors.Blue, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                BackTopBar(
                    title = if (state.isEdit) "Editar pago fijo" else "Nuevo pago fijo",
                    onBack = onDone,
                    actionIcon = if (state.isEdit) DesignR.drawable.ic_delete else null,
                    actionDescription = "Quitar pago fijo",
                    onAction = if (state.isEdit) viewModel::askDelete else null,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    FinanceTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = "¿Qué pagas cada mes? *",
                        error = state.errors[FormField.NAME],
                        supporting = "Ejemplo: Sistecrédito, Arriendo, Internet. Será el concepto del gasto.",
                    )
                    MoneyField(
                        digits = state.amountDigits,
                        onDigitsChange = viewModel::onAmountChange,
                        label = "Valor de cada pago o cuota",
                        error = state.errors[FormField.AMOUNT],
                    )
                    NumberField(
                        value = state.dueDayText,
                        onValueChange = viewModel::onDueDayChange,
                        label = "Día de pago (1 a 31) *",
                        error = state.errors[FormField.DUE_DAY],
                    )

                    FieldLabel("Frecuencia")
                    ChoiceChips(
                        options = MonthInterval.entries.map { ChipOption(it, it.label) },
                        selected = state.interval,
                        onSelect = viewModel::onIntervalChange,
                    )
                    Text(
                        "Para pagos quincenales, crea dos pagos fijos (por ejemplo día 15 y día 30).",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceColors.MediumGray,
                    )

                    FieldLabel("Categoría", required = true)
                    ChoiceChips(
                        options = state.categories.map { ChipOption(it.id, it.name) },
                        selected = state.categoryId,
                        onSelect = viewModel::onCategoryChange,
                        addLabel = "+ Nueva categoría",
                        onAdd = { showNewCategory = true },
                    )
                    FieldError(state.errors[FormField.CATEGORY])

                    if (state.budgetsForCategory.isNotEmpty()) {
                        FieldLabel("Rubro del presupuesto")
                        ChoiceChips(
                            options = state.budgetsForCategory.map { ChipOption<Long?>(it.id, it.name) } + ChipOption<Long?>(null, "Sin rubro"),
                            selected = state.budgetId,
                            onSelect = viewModel::onBudgetChange,
                        )
                    }

                    FieldLabel("Método de pago habitual")
                    ChoiceChips(
                        options = PaymentMethod.entries.map { ChipOption(it, it.label) },
                        selected = state.paymentMethod,
                        onSelect = viewModel::onMethodChange,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Es un crédito con cuotas", style = MaterialTheme.typography.bodyLarge, color = FinanceColors.DarkBlue)
                            Text(
                                "Cuando pagues la última cuota quedará saldado y dejará de aparecer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinanceColors.MediumGray,
                            )
                        }
                        Switch(checked = state.isCredit, onCheckedChange = viewModel::onCreditChange)
                    }
                    if (state.isCredit) {
                        state.creditSummary?.let { CreditSummaryCard(it, state.history) }
                        FinanceTextField(
                            value = state.entity,
                            onValueChange = viewModel::onEntityChange,
                            label = "Entidad o establecimiento (opcional)",
                            error = state.errors[FormField.ENTITY],
                            maxLength = 40,
                        )
                        MoneyField(
                            digits = state.originalAmountDigits,
                            onDigitsChange = viewModel::onOriginalAmountChange,
                            label = "Valor inicial del crédito (opcional)",
                            error = state.errors[FormField.ORIGINAL_AMOUNT],
                        )
                        NumberField(
                            value = state.interestText,
                            onValueChange = viewModel::onInterestChange,
                            label = "Tasa de interés mensual % (opcional)",
                            error = state.errors[FormField.INTEREST],
                            decimal = true,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberField(
                                value = state.totalInstallmentsText,
                                onValueChange = viewModel::onTotalChange,
                                label = "Total de cuotas *",
                                error = state.errors[FormField.INSTALLMENTS],
                                modifier = Modifier.weight(1f),
                            )
                            NumberField(
                                value = state.paidInstallmentsText,
                                onValueChange = viewModel::onPaidChange,
                                label = "Ya pagadas",
                                error = state.errors[FormField.PAID_INSTALLMENTS],
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    FieldLabel("Empieza en el mes de")
                    DateTimeRow(
                        date = state.startMonth.atDay(1),
                        time = LocalTime.NOON,
                        dateText = FinanceFormat.month(state.startMonth),
                        timeText = "",
                        onDateChange = viewModel::onStartDateChange,
                        onTimeChange = {},
                        error = null,
                        showTime = false,
                    )

                    FinanceTextField(
                        value = state.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = "Observaciones (opcional)",
                        error = state.errors[FormField.DESCRIPTION],
                        singleLine = false,
                        maxLength = 300,
                        imeAction = ImeAction.Default,
                    )
                    Text(
                        "Los recordatorios antes del vencimiento llegan con las notificaciones (Fase 6).",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceColors.MediumGray,
                    )
                }
                PrimaryActionButton(
                    text = if (state.isEdit) "Guardar cambios" else "Guardar pago fijo",
                    onClick = viewModel::save,
                    busy = state.saving,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (state.confirmDelete) {
        ConfirmDeleteDialog(
            title = "¿Quitar este pago fijo?",
            message = "Dejará de aparecer en los pagos del mes. Los pagos que ya hiciste se conservan en tus gastos e historial.",
            onConfirm = viewModel::delete,
            onDismiss = viewModel::cancelDelete,
        )
    }
}

/** Resumen del crédito: pagado, saldo estimado, avance e historial de cuotas. */
@Composable
private fun CreditSummaryCard(summary: CreditSummary, history: List<FixedPaymentRecord>) {
    FinanceCard(filled = true) {
        Text("Estado del crédito", style = MaterialTheme.typography.titleSmall, color = FinanceColors.DarkBlue)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total pagado", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                Text(FinanceFormat.money(summary.totalPaid), style = MaterialTheme.typography.titleMedium, color = FinanceColors.Green)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Saldo pendiente (estimado)", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                Text(FinanceFormat.money(summary.estimatedBalance), style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue)
            }
        }
        BudgetProgressBar(percent = summary.percentPaid, modifier = Modifier.padding(vertical = 8.dp))
        Text(
            if (summary.isSettled) "¡Saldado! Pagaste las ${summary.totalInstallments} cuotas."
            else "${summary.paidInstallments} de ${summary.totalInstallments} cuotas pagadas (${summary.percentPaid} %) · faltan ${summary.remainingInstallments}",
            style = MaterialTheme.typography.bodySmall,
            color = if (summary.isSettled) FinanceColors.Green else FinanceColors.MediumGray,
        )
        if (history.isNotEmpty()) {
            Text("Historial de pagos", style = MaterialTheme.typography.labelLarge, color = FinanceColors.DarkBlue, modifier = Modifier.padding(top = 10.dp))
            history.take(24).forEach { record ->
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(FinanceFormat.month(record.month), style = MaterialTheme.typography.bodySmall, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
                    Text(FinanceFormat.money(record.amount), style = MaterialTheme.typography.bodySmall, color = FinanceColors.DarkBlue)
                }
            }
        }
        Text(
            "El saldo es una estimación (cuotas que faltan × valor de la cuota). Tu entidad puede liquidar intereses distinto.",
            style = MaterialTheme.typography.bodySmall,
            color = FinanceColors.MediumGray,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}
