package com.micontrolfinanciero.app.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.backup.AuditEntry
import com.micontrolfinanciero.app.core.domain.backup.AuditRepository
import com.micontrolfinanciero.app.core.domain.backup.BackupCrypto
import com.micontrolfinanciero.app.core.domain.backup.BackupException
import com.micontrolfinanciero.app.core.domain.backup.BackupInfo
import com.micontrolfinanciero.app.core.domain.backup.BackupRepository
import com.micontrolfinanciero.app.core.domain.finance.SettingsRepository
import com.micontrolfinanciero.app.core.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/** Diálogo abierto en la pantalla de seguridad. */
sealed interface BackupDialog {
    data object None : BackupDialog
    /** Pedir contraseña nueva para crear el respaldo. */
    data object CreatePassword : BackupDialog
    /** Pedir la contraseña del respaldo elegido para restaurar. */
    data class RestorePassword(val uri: Uri) : BackupDialog
    /** Confirmar que se reemplazarán todos los datos. */
    data class ConfirmRestore(val uri: Uri, val info: BackupInfo) : BackupDialog
}

data class SecurityState(
    val dialog: BackupDialog = BackupDialog.None,
    val busy: Boolean = false,
    val passwordError: String? = null,
    val message: String? = null,
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    auditRepository: AuditRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val audit: StateFlow<List<AuditEntry>> = auditRepository.observeLatest(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(SecurityState())
    val state: StateFlow<SecurityState> = _state.asStateFlow()

    /** Contraseña del respaldo en curso: solo en memoria y se borra al terminar. */
    private var pendingPassword: CharArray? = null

    fun setAutoLock(minutes: Int) {
        viewModelScope.launch { settingsRepository.update { it.copy(autoLockMinutes = minutes) } }
    }

    fun setSecureScreen(on: Boolean) {
        viewModelScope.launch { settingsRepository.update { it.copy(secureScreen = on) } }
    }

    fun backupFileName() = "finance-respaldo-${LocalDate.now()}.fbk"

    fun openCreate() {
        _state.update { it.copy(dialog = BackupDialog.CreatePassword, passwordError = null) }
    }
    fun dismiss() {
        clearPassword()
        _state.update { it.copy(dialog = BackupDialog.None, passwordError = null) }
    }
    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    /** Valida la contraseña; si sirve, la pantalla abre el selector para guardar el archivo. */
    fun confirmCreatePassword(password: String, confirmation: String): Boolean {
        BackupCrypto.passwordError(password, confirmation)?.let { error ->
            _state.update { it.copy(passwordError = error) }
            return false
        }
        pendingPassword = password.toCharArray()
        _state.update { it.copy(dialog = BackupDialog.None, passwordError = null) }
        return true
    }

    fun writeBackup(uri: Uri?, appVersion: String) {
        val password = pendingPassword
        if (uri == null || password == null) { clearPassword(); return }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching {
                val bytes = backupRepository.createBackup(password, appVersion)
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: error("sin archivo")
                }
            }.onSuccess {
                _state.update { it.copy(busy = false, message = "Respaldo cifrado guardado. Guarda también la contraseña: sin ella no se puede abrir.") }
            }.onFailure {
                _state.update { it.copy(busy = false, message = "No se pudo crear el respaldo.") }
            }
            clearPassword()
        }
    }

    fun onRestoreFileChosen(uri: Uri?) {
        if (uri != null) _state.update { it.copy(dialog = BackupDialog.RestorePassword(uri), passwordError = null) }
    }

    /** Abre el respaldo con la contraseña para mostrar qué contiene antes de reemplazar nada. */
    fun inspectRestore(password: String) {
        val dialog = _state.value.dialog as? BackupDialog.RestorePassword ?: return
        _state.update { it.copy(busy = true, passwordError = null) }
        viewModelScope.launch {
            runCatching { backupRepository.inspect(readUri(dialog.uri), password.toCharArray()) }
                .onSuccess { info ->
                    pendingPassword = password.toCharArray()
                    _state.update { it.copy(busy = false, dialog = BackupDialog.ConfirmRestore(dialog.uri, info)) }
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false, passwordError = (e as? BackupException)?.message ?: "No se pudo leer el archivo.") }
                }
        }
    }

    fun restore() {
        val dialog = _state.value.dialog as? BackupDialog.ConfirmRestore ?: return
        val password = pendingPassword ?: return
        _state.update { it.copy(busy = true, dialog = BackupDialog.None) }
        viewModelScope.launch {
            runCatching { backupRepository.restore(readUri(dialog.uri), password) }
                .onSuccess { info ->
                    _state.update { it.copy(busy = false, message = "Datos restaurados: ${info.rows} registros y ${info.files} archivos.") }
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false, message = (e as? BackupException)?.message ?: "No se pudo restaurar. Tus datos actuales no cambiaron.") }
                }
            clearPassword()
        }
    }

    private suspend fun readUri(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            // Límite de seguridad: un respaldo normal pesa poco; 200 MB evita llenar la memoria.
            val bytes = input.readBytes()
            if (bytes.size > 200 * 1024 * 1024) throw BackupException("El archivo es demasiado grande.")
            bytes
        } ?: throw BackupException("No se pudo abrir el archivo.")
    }

    private fun clearPassword() {
        pendingPassword?.fill('\u0000')
        pendingPassword = null
    }

    override fun onCleared() {
        clearPassword()
    }
}
