package com.micontrolfinanciero.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Título pequeño de cada campo del formulario. */
@Composable
fun FieldLabel(text: String, required: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = FinanceColors.DarkBlue)
        if (required) Text(" *", style = MaterialTheme.typography.labelLarge, color = FinanceColors.Red)
    }
}

/** Mensaje de error bajo un grupo de opciones. */
@Composable
fun FieldError(text: String?) {
    if (text != null) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = FinanceColors.Red)
    }
}

/**
 * Campo de valor en pesos: solo dígitos, se muestra con punto de miles ("200.465").
 * [digits] son los pesos sin formato ("200465").
 */
@Composable
fun MoneyField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    maxDigits: Int = 12,
) {
    OutlinedTextField(
        value = digits,
        onValueChange = { text -> onDigitsChange(text.filter { it.isDigit() }.trimStart('0').take(maxDigits)) },
        label = { Text(label) },
        prefix = { Text("$ ", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = FinanceColors.MediumGray) },
        textStyle = MaterialTheme.typography.headlineMedium.copy(color = FinanceColors.DarkBlue),
        visualTransformation = ThousandsSeparatorTransformation(),
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Muestra "200465" como "200.465" sin cambiar lo que se guarda. */
class ThousandsSeparatorTransformation(private val separator: Char = '.') : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val out = StringBuilder()
        val originalToTransformed = IntArray(digits.length + 1)
        digits.forEachIndexed { index, c ->
            originalToTransformed[index] = out.length
            out.append(c)
            val fromEnd = digits.length - index - 1
            if (fromEnd > 0 && fromEnd % 3 == 0) out.append(separator)
        }
        originalToTransformed[digits.length] = out.length
        val transformedToOriginal = IntArray(out.length + 1)
        var count = 0
        for (t in 0..out.length) {
            transformedToOriginal[t] = count
            if (t < out.length && out[t] != separator) count++
        }
        return TransformedText(
            AnnotatedString(out.toString()),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = originalToTransformed[offset.coerceIn(0, digits.length)]
                override fun transformedToOriginal(offset: Int) = transformedToOriginal[offset.coerceIn(0, out.length)]
            },
        )
    }
}

/** Campo de texto de una línea con error o ayuda debajo. */
@Composable
fun FinanceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    supporting: String? = null,
    singleLine: Boolean = true,
    maxLength: Int = 80,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Next,
    modifier: Modifier = Modifier,
) {
    val helper = error ?: supporting
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        label = { Text(label) },
        isError = error != null,
        supportingText = if (helper != null) {
            { Text(helper) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = imeAction),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Una opción de un grupo de chips. */
data class ChipOption<T>(val value: T, val label: String)

/** Grupo de chips de selección única que se acomodan en varias líneas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(
    options: List<ChipOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    /** Si no es null, agrega al final un chip "+ …" (por ejemplo "+ Nueva categoría"). */
    addLabel: String? = null,
    onAdd: (() -> Unit)? = null,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option.value) },
                label = { Text(option.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FinanceColors.BlueTint,
                    selectedLabelColor = FinanceColors.Blue,
                ),
                modifier = Modifier.heightIn(min = 40.dp),
            )
        }
        if (addLabel != null && onAdd != null) {
            AssistChip(
                onClick = onAdd,
                label = { Text(addLabel, color = FinanceColors.Blue) },
                modifier = Modifier.heightIn(min = 40.dp),
            )
        }
    }
}

/**
 * Diálogo para crear una categoría sin salir del formulario.
 * [validate] devuelve el mensaje de error o null si el nombre sirve.
 */
@Composable
fun <T> NewCategoryDialog(
    natureOptions: List<ChipOption<T>>,
    initialNature: T,
    validate: (String) -> String?,
    onCreate: (name: String, nature: T) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Nueva categoría",
    initialName: String = "",
    confirmText: String = "Crear",
) {
    var name by remember { mutableStateOf(initialName) }
    var nature by remember { mutableStateOf(initialNature) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanceTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = "Nombre (ejemplo: Mascotas)",
                    error = error,
                    maxLength = 40,
                    imeAction = ImeAction.Done,
                )
                if (natureOptions.isNotEmpty()) {
                    FieldLabel("Clasificación habitual")
                    ChoiceChips(options = natureOptions, selected = nature, onSelect = { nature = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val problem = validate(name)
                if (problem != null) error = problem else onCreate(name.trim(), nature)
            }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Fecha y hora con sus selectores de Android. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeRow(
    date: LocalDate,
    time: LocalTime,
    dateText: String,
    timeText: String,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    error: String?,
    showTime: Boolean = true,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                Text(dateText)
            }
            if (showTime) {
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Text(timeText)
                }
            }
        }
        FieldError(error)
    }

    if (showDatePicker) {
        // El selector trabaja con medianoche UTC; se convierte a fecha local sin desfases.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(LocalTime.of(state.hour, state.minute))
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            text = { TimePicker(state = state, modifier = Modifier.padding(top = 8.dp)) },
        )
    }
}

/** Confirmación antes de borrar (sección 17: "confirmación antes de eliminar"). */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Eliminar", color = FinanceColors.Red) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
