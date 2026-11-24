package com.micontrolfinanciero.app.feature.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.component.BackTopBar
import com.micontrolfinanciero.app.core.designsystem.component.ChipOption
import com.micontrolfinanciero.app.core.designsystem.component.ChoiceChips
import com.micontrolfinanciero.app.core.designsystem.component.ConfirmDeleteDialog
import com.micontrolfinanciero.app.core.designsystem.component.DateTimeRow
import com.micontrolfinanciero.app.core.designsystem.component.FieldLabel
import com.micontrolfinanciero.app.core.designsystem.component.FinanceFormat
import com.micontrolfinanciero.app.core.designsystem.component.FinanceTextField
import com.micontrolfinanciero.app.core.designsystem.component.MoneyField
import com.micontrolfinanciero.app.core.designsystem.component.PrimaryActionButton
import com.micontrolfinanciero.app.core.designsystem.component.SavedConfirmation
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.finance.FormField
import com.micontrolfinanciero.app.core.model.IncomeDestination
import com.micontrolfinanciero.app.core.model.IncomeType
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

private val QUICK_SOURCES = listOf("Salario", "Honorarios", "Venta", "Arriendo recibido", "Préstamo recibido", "Otro")

@Composable
fun IncomeFormRoute(
    onDone: () -> Unit,
    viewModel: IncomeFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.closed) { if (state.closed) onDone() }
    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        snackbar.showSnackbar(error)
        viewModel.onErrorShown()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val saved = state.savedMessage
        when {
            state.loading -> CircularProgressIndicator(color = FinanceColors.Green, modifier = Modifier.align(Alignment.Center))
            saved != null -> SavedConfirmation(
                title = if (state.isEdit) "Cambios guardados" else "¡Ingreso registrado!",
                message = saved,
                alert = null,
                primaryText = "Listo",
                onPrimary = onDone,
                secondaryText = if (state.isEdit) null else "Registrar otro ingreso",
                onSecondary = if (state.isEdit) null else viewModel::registerAnother,
            )
            else -> IncomeForm(state, viewModel, onDone)
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (state.confirmDelete) {
        ConfirmDeleteDialog(
            title = "¿Eliminar este ingreso?",
            message = "Dejará de contar en tu balance del mes. Queda registro en la auditoría.",
            onConfirm = viewModel::delete,
            onDismiss = viewModel::cancelDelete,
        )
    }
}

@Composable
private fun IncomeForm(state: IncomeFormState, viewModel: IncomeFormViewModel, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        BackTopBar(
            title = if (state.isEdit) "Editar ingreso" else "Registrar ingreso",
            onBack = onBack,
            actionIcon = if (state.isEdit) DesignR.drawable.ic_delete else null,
            actionDescription = "Eliminar ingreso",
            onAction = if (state.isEdit) viewModel::askDelete else null,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MoneyField(
                digits = state.amountDigits,
                onDigitsChange = viewModel::onAmountChange,
                label = "Valor recibido",
                error = state.errors[FormField.AMOUNT],
            )
            FinanceTextField(
                value = state.source,
                onValueChange = viewModel::onSourceChange,
                label = "¿De dónde viene? *",
                error = state.errors[FormField.SOURCE],
                supporting = "Obligatorio. Ejemplo: Salario, venta, honorarios.",
            )
            ChoiceChips(
                options = QUICK_SOURCES.map { ChipOption(it, it) },
                selected = QUICK_SOURCES.firstOrNull { it == state.source },
                onSelect = viewModel::onQuickSource,
            )

            FieldLabel("Tipo de ingreso")
            ChoiceChips(
                options = IncomeType.entries.map { ChipOption(it, it.label) },
                selected = state.type,
                onSelect = viewModel::onTypeChange,
            )
            Text(
                text = if (state.type == IncomeType.FIXED) {
                    "Fijo: se repite cada mes, como el salario. Regístralo cuando llegue (los recordatorios de ingresos fijos llegan en la Fase 5)."
                } else {
                    "Variable: cuenta solo en el mes en que lo recibes."
                },
                style = MaterialTheme.typography.bodySmall,
                color = FinanceColors.MediumGray,
            )

            FieldLabel("Recibido en")
            ChoiceChips(
                options = IncomeDestination.entries.map { ChipOption(it, it.label) },
                selected = state.destination,
                onSelect = viewModel::onDestinationChange,
            )

            FieldLabel("Fecha y hora")
            DateTimeRow(
                date = state.date,
                time = state.time,
                dateText = FinanceFormat.date(state.date),
                timeText = FinanceFormat.time(state.time),
                onDateChange = viewModel::onDateChange,
                onTimeChange = viewModel::onTimeChange,
                error = state.errors[FormField.DATE],
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
        }
        PrimaryActionButton(
            text = if (state.isEdit) "Guardar cambios" else "Guardar ingreso",
            onClick = viewModel::save,
            busy = state.saving,
            color = FinanceColors.Green,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}
