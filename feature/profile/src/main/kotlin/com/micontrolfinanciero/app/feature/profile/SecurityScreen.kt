package com.micontrolfinanciero.app.feature.profile

import com.micontrolfinanciero.app.core.domain.security.AutoLockGuard
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.component.BackTopBar
import com.micontrolfinanciero.app.core.designsystem.component.ChipOption
import com.micontrolfinanciero.app.core.designsystem.component.ChoiceChips
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.component.FinanceFormat
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.backup.AuditEntry
import com.micontrolfinanciero.app.core.model.AppSettings
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Seguridad y respaldo (Fase 8): bloqueo automático, ocultar pantalla, respaldo cifrado,
 * restauración e historial de cambios.
 */
@Composable
fun SecurityRoute(appVersion: String, onBack: () -> Unit, viewModel: SecurityViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val audit by viewModel.audit.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        viewModel.writeBackup(uri, appVersion)
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onRestoreFileChosen(uri)
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.onMessageShown() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(title = "Seguridad y respaldo", onBack = onBack)
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { LockCard(settings, viewModel::setAutoLock, viewModel::setSecureScreen) }
                item {
                    BackupCard(
                        busy = state.busy,
                        onCreate = viewModel::openCreate,
                        onRestore = { AutoLockGuard.allowExternalScreen(); openLauncher.launch(arrayOf("*/*")) },
                    )
                }
                item {
                    Text("Historial de cambios", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue)
                    Text(
                        "Cada gasto, ingreso, rubro o ajuste que se crea, cambia o elimina queda registrado aquí. Últimos 200.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceColors.MediumGray,
                    )
                }
                if (audit.isEmpty()) {
                    item { Text("Aún no hay cambios registrados.", color = FinanceColors.MediumGray) }
                } else {
                    items(audit, key = { it.id }) { AuditRow(it) }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    when (val d = state.dialog) {
        BackupDialog.None -> Unit
        BackupDialog.CreatePassword -> PasswordDialog(
            title = "Contraseña del respaldo",
            text = "El respaldo se cifra con esta contraseña. Guárdala en un lugar seguro: sin ella no se puede abrir.",
            confirmText = "Continuar",
            withConfirmation = true,
            error = state.passwordError,
            busy = false,
            onConfirm = { p, c -> if (viewModel.confirmCreatePassword(p, c)) { AutoLockGuard.allowExternalScreen(); saveLauncher.launch(viewModel.backupFileName()) } },
            onDismiss = viewModel::dismiss,
        )
        is BackupDialog.RestorePassword -> PasswordDialog(
            title = "Abrir respaldo",
            text = "Escribe la contraseña con la que se creó el respaldo.",
            confirmText = "Abrir",
            withConfirmation = false,
            error = state.passwordError,
            busy = state.busy,
            onConfirm = { p, _ -> viewModel.inspectRestore(p) },
            onDismiss = viewModel::dismiss,
        )
        is BackupDialog.ConfirmRestore -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("¿Reemplazar todos los datos?") },
            text = {
                Text(
                    "Respaldo del ${formatAt(d.info.createdAt)} (Finance ${d.info.appVersion}): ${d.info.rows} registros y " +
                        "${d.info.files} archivos.\n\nTodo lo que hay ahora en la app se reemplazará por el respaldo, incluidos " +
                        "el usuario y la contraseña. Si algo falla, tus datos actuales no cambian.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::restore) { Text("Restaurar", color = FinanceColors.Red) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun LockCard(settings: AppSettings, onAutoLock: (Int) -> Unit, onSecure: (Boolean) -> Unit) {
    FinanceCard {
        Text("Bloqueo automático", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue)
        Text(
            "Pide contraseña o huella al volver a la app después de este tiempo. Solo aplica si creaste contraseña.",
            style = MaterialTheme.typography.bodySmall,
            color = FinanceColors.MediumGray,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        ChoiceChips(
            options = AppSettings.AUTO_LOCK_OPTIONS.map { ChipOption(it, if (it == 0) "Siempre" else "$it min") },
            selected = settings.autoLockMinutes,
            onSelect = onAutoLock,
        )
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = FinanceColors.Border)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ocultar pantalla", style = MaterialTheme.typography.bodyLarge, color = FinanceColors.DarkBlue)
                Text(
                    "Impide capturas de pantalla y oculta el contenido en la lista de apps recientes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.MediumGray,
                )
            }
            Switch(checked = settings.secureScreen, onCheckedChange = onSecure)
        }
    }
}

@Composable
private fun BackupCard(busy: Boolean, onCreate: () -> Unit, onRestore: () -> Unit) {
    FinanceCard {
        Text("Respaldo cifrado", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue)
        Text(
            "Guarda todos tus datos, la foto y los comprobantes en un archivo cifrado (AES-256) que tú eliges dónde " +
                "guardar. La app nunca lo sube a internet. Útil para cambiar de teléfono o reinstalar.",
            style = MaterialTheme.typography.bodySmall,
            color = FinanceColors.MediumGray,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onCreate,
                enabled = !busy,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinanceColors.Blue),
            ) { Text("Crear respaldo") }
            OutlinedButton(onClick = onRestore, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text("Restaurar")
            }
        }
    }
}

@Composable
private fun AuditRow(entry: AuditEntry) {
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(
                "${actionLabel(entry.action)} · ${entityLabel(entry.entity)}",
                style = MaterialTheme.typography.bodyMedium,
                color = FinanceColors.DarkBlue,
                modifier = Modifier.weight(1f),
            )
            Text(formatAt(entry.at), style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
        }
        Text(entry.details, style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray, maxLines = 3)
        HorizontalDivider(Modifier.padding(top = 8.dp), color = FinanceColors.Border)
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    text: String,
    confirmText: String,
    withConfirmation: Boolean,
    error: String?,
    busy: Boolean,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(128) },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                if (withConfirmation) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it.take(128) },
                        label = { Text("Repite la contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
                error?.let { Text(it, color = FinanceColors.Red, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password, confirmation) }, enabled = !busy && password.isNotEmpty()) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private val auditFormat = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.forLanguageTag("es-CO"))

private fun formatAt(ms: Long) = Instant.ofEpochMilli(ms).atZone(FinanceFormat.zone).format(auditFormat)

private fun actionLabel(action: String) = when (action) {
    "CREATE" -> "Creado"
    "UPDATE" -> "Modificado"
    "DELETE" -> "Eliminado"
    else -> action.lowercase().replaceFirstChar { it.uppercase() }
}

private fun entityLabel(entity: String) = when (entity) {
    "expense" -> "Gasto"
    "income" -> "Ingreso"
    "budget" -> "Rubro"
    "expense_category" -> "Categoría"
    "fixed_payment" -> "Pago fijo"
    "fixed_payment_record" -> "Pago del mes"
    "expected_income" -> "Ingreso esperado"
    "expected_income_record" -> "Ingreso recibido"
    "attachment" -> "Comprobante"
    "app_setting" -> "Ajuste"
    "user_profile" -> "Perfil"
    "backup" -> "Respaldo"
    else -> entity
}
