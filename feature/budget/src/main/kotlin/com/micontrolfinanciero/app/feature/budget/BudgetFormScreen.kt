package com.micontrolfinanciero.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import com.micontrolfinanciero.app.core.designsystem.component.FieldError
import com.micontrolfinanciero.app.core.designsystem.component.FieldLabel
import com.micontrolfinanciero.app.core.designsystem.component.FinanceFormat
import com.micontrolfinanciero.app.core.designsystem.component.FinanceTextField
import com.micontrolfinanciero.app.core.designsystem.component.MoneyField
import com.micontrolfinanciero.app.core.designsystem.component.PrimaryActionButton
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.finance.FormField
import com.micontrolfinanciero.app.core.model.CLASSIFICATIONS
import com.micontrolfinanciero.app.core.model.ExpenseFrequencyType
import java.time.LocalTime
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/** Crear o editar un rubro. Guardar o eliminar cierra la pantalla. */
@Composable
fun BudgetFormRoute(
    onDone: () -> Unit,
    viewModel: BudgetFormViewModel = hiltViewModel(),
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
        if (state.loading) {
            CircularProgressIndicator(color = FinanceColors.Blue, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                BackTopBar(
                    title = if (state.isEdit) "Editar rubro" else "Nuevo rubro",
                    onBack = onDone,
                    actionIcon = if (state.isEdit) DesignR.drawable.ic_delete else null,
                    actionDescription = "Eliminar rubro",
                    onAction = if (state.isEdit) viewModel::askDelete else null,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    FieldLabel("Categoría", required = true)
                    ChoiceChips(
                        options = state.categories.map { ChipOption(it.id, it.name) },
                        selected = state.categoryId,
                        onSelect = viewModel::onCategoryChange,
                    )
                    FieldError(state.errors[FormField.CATEGORY])

                    FinanceTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = "Nombre del rubro *",
                        error = state.errors[FormField.NAME],
                        supporting = "Ejemplo: Arriendo, Mercado, Gasolina.",
                        maxLength = 40,
                    )
                    MoneyField(
                        digits = state.amountDigits,
                        onDigitsChange = viewModel::onAmountChange,
                        label = "Valor presupuestado por mes",
                        error = state.errors[FormField.AMOUNT],
                    )

                    FieldLabel("Tipo de gasto")
                    ChoiceChips(
                        options = ExpenseFrequencyType.entries.map { ChipOption(it, it.label) },
                        selected = state.frequencyType,
                        onSelect = viewModel::onFrequencyChange,
                    )
                    Text(
                        "Para gastos fijos, el día de pago, los vencimientos y los recordatorios llegan en la Fase 5.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceColors.MediumGray,
                    )

                    FieldLabel("Clasificación")
                    ChoiceChips(
                        options = CLASSIFICATIONS.map { ChipOption(it, it.label) },
                        selected = state.nature,
                        onSelect = viewModel::onNatureChange,
                    )

                    FieldLabel("Fecha de inicio")
                    DateTimeRow(
                        date = state.startDate,
                        time = LocalTime.MIDNIGHT,
                        dateText = FinanceFormat.date(state.startDate),
                        timeText = "",
                        onDateChange = viewModel::onStartDateChange,
                        onTimeChange = {},
                        error = null,
                        showTime = false,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rubro activo", style = MaterialTheme.typography.bodyLarge, color = FinanceColors.DarkBlue)
                            Text(
                                "Si lo desactivas, deja de contar en el presupuesto del mes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FinanceColors.MediumGray,
                            )
                        }
                        Switch(checked = state.active, onCheckedChange = viewModel::onActiveChange)
                    }

                    FinanceTextField(
                        value = state.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = "Descripción (opcional)",
                        error = state.errors[FormField.DESCRIPTION],
                        maxLength = 300,
                    )
                    FinanceTextField(
                        value = state.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = "Observaciones (opcional)",
                        singleLine = false,
                        maxLength = 300,
                        imeAction = ImeAction.Default,
                    )
                }
                PrimaryActionButton(
                    text = if (state.isEdit) "Guardar cambios" else "Crear rubro",
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
            title = "¿Eliminar este rubro?",
            message = "Los gastos que ya registraste se conservan, pero quedan sin rubro. Si solo quieres pausarlo, mejor desactívalo.",
            onConfirm = viewModel::delete,
            onDismiss = viewModel::cancelDelete,
        )
    }
}
