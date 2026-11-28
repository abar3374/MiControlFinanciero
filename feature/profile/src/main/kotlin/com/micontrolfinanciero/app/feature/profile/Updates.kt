package com.micontrolfinanciero.app.feature.profile

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.designsystem.component.ChipOption
import com.micontrolfinanciero.app.core.designsystem.component.ChoiceChips
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.finance.SettingsRepository
import com.micontrolfinanciero.app.core.domain.security.AutoLockGuard
import com.micontrolfinanciero.app.core.domain.update.AvailableUpdate
import com.micontrolfinanciero.app.core.model.AppSettings
import com.micontrolfinanciero.app.core.model.UpdateChannel
import com.micontrolfinanciero.app.core.updates.AppUpdater
import com.micontrolfinanciero.app.core.updates.UpdateException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

/** Estado del actualizador. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val update: AvailableUpdate) : UpdateState
    data class Downloading(val update: AvailableUpdate, val progress: Float) : UpdateState
    data class Ready(val update: AvailableUpdate, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val updater = AppUpdater(context)

    val settings: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var job: Job? = null

    fun setChannel(channel: UpdateChannel) {
        viewModelScope.launch { settingsRepository.update { it.copy(updateChannel = channel) } }
        _state.value = UpdateState.Idle
    }

    fun check() {
        job?.cancel()
        _state.value = UpdateState.Checking
        job = viewModelScope.launch {
            _state.value = runCatching { updater.check(settings.value.updateChannel) }
                .fold(
                    onSuccess = { it?.let(UpdateState::Available) ?: UpdateState.UpToDate },
                    onFailure = { UpdateState.Failed(message(it)) },
                )
        }
    }

    fun download(update: AvailableUpdate) {
        job?.cancel()
        _state.value = UpdateState.Downloading(update, 0f)
        job = viewModelScope.launch {
            _state.value = runCatching {
                updater.download(update) { p -> _state.value = UpdateState.Downloading(update, p) }
            }.fold(
                onSuccess = { UpdateState.Ready(update, it) },
                onFailure = { UpdateState.Failed(message(it)) },
            )
        }
    }

    fun cancel() {
        job?.cancel()
        _state.value = UpdateState.Idle
    }

    /** Abre el instalador de Android (o primero el permiso, si falta). */
    fun install(file: File) {
        AutoLockGuard.allowExternalScreen()
        if (!updater.canRequestInstall()) {
            updater.openInstallPermissionSettings()
            return
        }
        runCatching { updater.launchInstaller(file) }
            .onFailure { _state.value = UpdateState.Failed("No se pudo abrir el instalador de Android.") }
    }

    fun canRequestInstall() = updater.canRequestInstall()

    private fun message(e: Throwable) = when (e) {
        is UpdateException -> e.message ?: "No se pudo actualizar."
        is kotlinx.coroutines.CancellationException -> "Cancelado."
        else -> "No se pudo conectar con GitHub. Revisa tu conexión."
    }
}

/** Tarjeta "Actualizaciones" del Perfil. Solo se conecta a internet cuando el usuario toca un botón. */
@Composable
internal fun UpdatesSection(viewModel: UpdatesViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Text("Actualizaciones", style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue)
    FinanceCard {
        Text(
            "Busca nuevas versiones en el repositorio oficial de Finance en GitHub. Antes de instalar se comprueban la " +
                "huella SHA-256 y la firma; tú confirmas la instalación y tus datos se conservan.",
            style = MaterialTheme.typography.bodySmall,
            color = FinanceColors.MediumGray,
        )
        Text("Canal", style = MaterialTheme.typography.labelLarge, color = FinanceColors.DarkBlue, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        ChoiceChips(
            options = listOf(
                ChipOption(UpdateChannel.STABLE, "Estable"),
                ChipOption(UpdateChannel.BETA, "Pruebas (beta)"),
            ),
            selected = settings.updateChannel,
            onSelect = viewModel::setChannel,
        )

        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (val s = state) {
                UpdateState.Idle -> PrimaryButton("Buscar actualización", viewModel::check)
                UpdateState.Checking -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Info("Consultando GitHub…")
                }
                UpdateState.UpToDate -> {
                    Info("Tienes la versión más reciente del canal ${settings.updateChannel.label.lowercase()}.", FinanceColors.Green)
                    SecondaryButton("Buscar de nuevo", viewModel::check)
                }
                is UpdateState.Available -> {
                    Info("Nueva versión ${s.update.versionName} (${s.update.versionCode})${if (s.update.prerelease) " · prueba" else ""} · ${sizeText(s.update.apk.size)}", FinanceColors.Blue)
                    if (s.update.notes.isNotBlank()) Info(s.update.notes.take(600))
                    PrimaryButton("Descargar", { viewModel.download(s.update) })
                }
                is UpdateState.Downloading -> {
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    Info("Descargando ${s.update.versionName}… ${(s.progress * 100).toInt()} %")
                    SecondaryButton("Cancelar", viewModel::cancel)
                }
                is UpdateState.Ready -> {
                    Info("Descarga verificada (SHA-256 y firma de Finance).", FinanceColors.Green)
                    if (!viewModel.canRequestInstall()) {
                        Info("Android pedirá permitir que Finance instale actualizaciones. Actívalo y vuelve aquí.", FinanceColors.Amber)
                    }
                    PrimaryButton("Instalar ${s.update.versionName}", { viewModel.install(s.file) })
                }
                is UpdateState.Failed -> {
                    Info(s.message, FinanceColors.Red)
                    SecondaryButton("Intentar de nuevo", viewModel::check)
                }
            }
        }
    }
}

@Composable
private fun Info(text: String, color: androidx.compose.ui.graphics.Color = FinanceColors.MediumGray) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Row {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FinanceColors.Blue),
        ) { Text(text) }
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(text) }
}

private fun sizeText(bytes: Long) = String.format(Locale.forLanguageTag("es-CO"), "%.1f MB", bytes / 1_048_576.0)
