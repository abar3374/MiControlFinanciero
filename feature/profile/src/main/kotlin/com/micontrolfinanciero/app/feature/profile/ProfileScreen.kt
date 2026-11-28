package com.micontrolfinanciero.app.feature.profile

import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.component.ScreenHeader
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.notifications.ReminderNotifications
import com.micontrolfinanciero.app.core.security.BiometricAuthenticator
import com.micontrolfinanciero.app.core.security.findFragmentActivity

/**
 * Pestaña Perfil y ajustes.
 * Datos personales, foto, usuario y contraseña, y huella se guardan en la base cifrada.
 * Región y formato se muestran con los valores iniciales (editables más adelante).
 */
@Composable
fun ProfileRoute(
    appVersion: String,
    onOpenSecurity: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.onMessageShown()
    }

    // Permiso de notificaciones (Android 13+). Si lo niega, se explica cómo activarlo después.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.checkRemindersNow()
            viewModel.showMessage("Listo: te avisaremos antes de cada vencimiento.")
        } else {
            viewModel.showMessage("Sin permiso no se pueden mostrar avisos. Puedes activarlo en los ajustes del teléfono.")
        }
    }

    // Selector de fotos de Android: no necesita permisos y solo entrega la foto elegida.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri.toString())
    }

    val promptTitle = stringResource(R.string.profile_biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.profile_biometric_prompt_subtitle)
    val promptCancel = stringResource(R.string.profile_cancel)

    val onBiometricToggle: (Boolean) -> Unit = { enable ->
        if (!enable) {
            viewModel.setBiometricEnabled(false)
        } else {
            // Para activar la huella, el dueño debe confirmarla una vez.
            val activity = context.findFragmentActivity()
            if (activity == null) {
                viewModel.showMessage("No se pudo abrir el lector de huella.")
            } else {
                BiometricAuthenticator.authenticate(
                    activity = activity,
                    title = promptTitle,
                    subtitle = promptSubtitle,
                    negativeButtonText = promptCancel,
                    onSuccess = { viewModel.setBiometricEnabled(true) },
                    onError = { viewModel.showMessage(it) },
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.loading) {
            CircularProgressIndicator(color = FinanceColors.Blue, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScreenHeader(title = stringResource(R.string.profile_title))

                ProfileHeaderCard(
                    profile = state.profile,
                    photoBusy = state.photoBusy,
                    onPickPhoto = {
                        com.micontrolfinanciero.app.core.domain.security.AutoLockGuard.allowExternalScreen()
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemovePhoto = viewModel::removePhoto,
                )

                PersonalDataSection(
                    profile = state.profile,
                    editing = state.editingPersonal,
                    form = state.personalForm,
                    errors = state.personalErrors,
                    saving = state.saving,
                    onEdit = viewModel::startEditPersonal,
                    onFormChange = viewModel::onPersonalFormChange,
                    onSave = viewModel::savePersonal,
                    onCancel = viewModel::cancelEditPersonal,
                )

                SecuritySection(
                    profile = state.profile,
                    biometricAvailability = BiometricAuthenticator.availability(context),
                    editing = state.editingCredentials,
                    form = state.credentialsForm,
                    errors = state.credentialsErrors,
                    saving = state.saving,
                    onEdit = viewModel::startEditCredentials,
                    onFormChange = viewModel::onCredentialsFormChange,
                    onSave = viewModel::saveCredentials,
                    onCancel = viewModel::cancelEditCredentials,
                    onBiometricToggle = onBiometricToggle,
                )

                SecurityLinkCard(onClick = onOpenSecurity)

                RemindersSection(
                    settings = state.settings,
                    onEnabledChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33 && !ReminderNotifications.canNotify(context)) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.setRemindersEnabled(enabled)
                    },
                    canNotify = ReminderNotifications.canNotify(context),
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openNotificationSettings(context)
                        }
                    },
                    onToggleDay = viewModel::toggleReminderDay,
                    onCustomChange = viewModel::setCustomReminder,
                    onHourChange = viewModel::setReminderHour,
                )

                UpdatesSection()

                RegionSection()

                AboutSection(appVersion = appVersion)
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

/** Abre la pantalla de notificaciones de la app en los ajustes del teléfono. */
internal fun openNotificationSettings(context: Context) {
    runCatching {
        val intent = if (Build.VERSION.SDK_INT >= 26) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + context.packageName))
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Acceso a bloqueo automático, ocultar pantalla, respaldo cifrado e historial (Fase 8). */
@Composable
private fun SecurityLinkCard(onClick: () -> Unit) {
    com.micontrolfinanciero.app.core.designsystem.component.FinanceCard(
        modifier = Modifier.clickable(onClickLabel = "Abrir seguridad y respaldo", onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bloqueo, respaldo e historial", style = MaterialTheme.typography.bodyLarge, color = com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors.DarkBlue)
                Text(
                    "Tiempo de bloqueo, ocultar pantalla, respaldo cifrado y registro de cambios.",
                    style = MaterialTheme.typography.bodySmall,
                    color = com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors.MediumGray,
                )
            }
            Icon(
                painterResource(com.micontrolfinanciero.app.core.designsystem.R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors.MediumGray,
            )
        }
    }
}
