package com.micontrolfinanciero.app.feature.expense

import com.micontrolfinanciero.app.core.domain.security.AutoLockGuard
import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.micontrolfinanciero.app.core.designsystem.component.NewCategoryDialog
import com.micontrolfinanciero.app.core.designsystem.component.PrimaryActionButton
import com.micontrolfinanciero.app.core.designsystem.component.SavedConfirmation
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.finance.FinanceValidator
import com.micontrolfinanciero.app.core.domain.finance.FormField
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.CLASSIFICATIONS
import com.micontrolfinanciero.app.core.model.PaymentMethod
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/** Formulario de gasto: nuevo (id = 0) o edición. [onDone] cierra la pantalla. */
@Composable
fun ExpenseFormRoute(
    onDone: () -> Unit,
    viewModel: ExpenseFormViewModel = hiltViewModel(),
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
        val saved = state.saved
        when {
            state.loading -> CircularProgressIndicator(color = FinanceColors.Blue, modifier = Modifier.align(Alignment.Center))
            saved != null -> SavedConfirmation(
                title = if (state.isEdit) "Cambios guardados" else "¡Gasto registrado!",
                message = saved.message,
                alert = saved.alert,
                primaryText = "Listo",
                onPrimary = onDone,
                secondaryText = if (state.isEdit) null else "Registrar otro gasto",
                onSecondary = if (state.isEdit) null else viewModel::registerAnother,
            )
            else -> ExpenseForm(state = state, viewModel = viewModel, onBack = onDone)
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (state.confirmDelete) {
        ConfirmDeleteDialog(
            title = "¿Eliminar este gasto?",
            message = "Dejará de contar en tus presupuestos y reportes. Queda registro en la auditoría.",
            onConfirm = viewModel::delete,
            onDismiss = viewModel::cancelDelete,
        )
    }
}

@Composable
private fun ExpenseForm(state: ExpenseFormState, viewModel: ExpenseFormViewModel, onBack: () -> Unit) {
    var showNewCategory by remember { mutableStateOf(false) }
    if (showNewCategory) {
        NewCategoryDialog(
            natureOptions = CLASSIFICATIONS.map { ChipOption(it, it.label) },
            initialNature = ExpenseNature.OTHER,
            validate = { name -> FinanceValidator.categoryNameError(name, state.categories.map { it.name }) },
            onCreate = { name, nature ->
                viewModel.createCategory(name, nature)
                showNewCategory = false
            },
            onDismiss = { showNewCategory = false },
        )
    }
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        BackTopBar(
            title = if (state.isEdit) "Editar gasto" else "Registrar gasto",
            onBack = onBack,
            actionIcon = if (state.isEdit) DesignR.drawable.ic_delete else null,
            actionDescription = "Eliminar gasto",
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
                label = "Valor",
                error = state.errors[FormField.AMOUNT],
            )
            FinanceTextField(
                value = state.concept,
                onValueChange = viewModel::onConceptChange,
                label = "¿En qué gastaste? *",
                error = state.errors[FormField.CONCEPT],
                supporting = "Obligatorio. Ejemplo: Sistecrédito, mercado, gasolina.",
            )
            FinanceTextField(
                value = state.place,
                onValueChange = viewModel::onPlaceChange,
                label = "Lugar o entidad (opcional)",
                error = state.errors[FormField.PLACE],
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

            if (state.categoryId != null) {
                FieldLabel("Rubro del presupuesto")
                if (state.budgetsForCategory.isEmpty()) {
                    Text(
                        "No tienes rubros en esta categoría. El gasto cuenta en el total del mes, sin rubro. Puedes crear rubros en Presupuesto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceColors.MediumGray,
                    )
                } else {
                    ChoiceChips(
                        options = state.budgetsForCategory.map { ChipOption<Long?>(it.id, it.name) } + ChipOption<Long?>(null, "Sin rubro"),
                        selected = state.budgetId,
                        onSelect = viewModel::onBudgetChange,
                    )
                }
            }

            FieldLabel("Clasificación")
            ChoiceChips(
                options = CLASSIFICATIONS.map { ChipOption(it, it.label) },
                selected = state.nature,
                onSelect = viewModel::onNatureChange,
            )

            FieldLabel("Método de pago")
            ChoiceChips(
                options = PaymentMethod.entries.map { ChipOption(it, it.label) },
                selected = state.paymentMethod,
                onSelect = viewModel::onPaymentMethodChange,
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
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = "Descripción u observaciones (opcional)",
                error = state.errors[FormField.DESCRIPTION],
                singleLine = false,
                maxLength = 300,
                imeAction = ImeAction.Default,
            )
            AttachmentsSection(state = state, viewModel = viewModel)
        }
        PrimaryActionButton(
            text = if (state.isEdit) "Guardar cambios" else "Guardar gasto",
            onClick = viewModel::save,
            busy = state.saving,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

/** Comprobantes: foto o PDF, guardados en la carpeta privada de la app. */
@Composable
private fun AttachmentsSection(state: ExpenseFormState, viewModel: ExpenseFormViewModel) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            val name = runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull() ?: "comprobante"
            viewModel.onAttachmentPicked(uri.toString(), name, mime)
        }
    }
    FieldLabel("Comprobante (opcional)")
    state.attachments.forEach { a ->
        AttachmentRow(
            name = a.displayName,
            onOpen = { openPrivateFile(context, a.path, a.mimeType) },
            onRemove = { viewModel.removeAttachment(a.id) },
        )
    }
    state.pendingAttachments.forEach { p ->
        AttachmentRow(name = p.name + " (se guarda con el gasto)", onOpen = null, onRemove = { viewModel.removePending(p) })
    }
    OutlinedButton(
        onClick = { AutoLockGuard.allowExternalScreen(); picker.launch(arrayOf("image/*", "application/pdf")) },
        enabled = !state.attaching,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (state.attaching) "Adjuntando…" else "Adjuntar foto o PDF del comprobante") }
}

@Composable
private fun AttachmentRow(name: String, onOpen: (() -> Unit)?, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(name, style = MaterialTheme.typography.bodyMedium, color = FinanceColors.DarkBlue, maxLines = 1, modifier = Modifier.weight(1f))
        if (onOpen != null) TextButton(onClick = onOpen) { Text("Ver") }
        TextButton(onClick = onRemove) { Text("Quitar", color = FinanceColors.Red) }
    }
}

/** Abre un archivo privado con otra app (visor de fotos o PDF) mediante un permiso temporal. */
internal fun openPrivateFile(context: Context, path: String, mimeType: String) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", File(path))
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        AutoLockGuard.allowExternalScreen()
        context.startActivity(Intent.createChooser(intent, "Abrir comprobante"))
    }
}
