package com.micontrolfinanciero.app.feature.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.micontrolfinanciero.app.core.model.AppSettings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.domain.profile.ProfileField
import com.micontrolfinanciero.app.core.domain.profile.ProfileValidator
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.MoneyFormat
import com.micontrolfinanciero.app.core.model.UserProfile
import com.micontrolfinanciero.app.core.security.BiometricAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------- Encabezado con foto

@Composable
internal fun ProfileHeaderCard(
    profile: UserProfile,
    photoBusy: Boolean,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    FinanceCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProfileAvatar(profile = profile, busy = photoBusy)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = profile.fullName.ifBlank { stringResource(R.string.profile_no_name) },
                    style = MaterialTheme.typography.titleMedium,
                    color = FinanceColors.DarkBlue,
                )
                Text(
                    text = profile.username?.let { "@$it" } ?: stringResource(R.string.profile_no_username),
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.MediumGray,
                )
                if (profile.phone.isNotEmpty()) {
                    Text(
                        text = ProfileValidator.formatPhone(profile.phone),
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceColors.MediumGray,
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onPickPhoto, enabled = !photoBusy) {
                Text(
                    stringResource(
                        if (profile.photoPath == null) R.string.profile_photo_add else R.string.profile_photo_change,
                    ),
                )
            }
            if (profile.photoPath != null) {
                TextButton(onClick = onRemovePhoto, enabled = !photoBusy) {
                    Text(stringResource(R.string.profile_photo_remove), color = FinanceColors.Red)
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: UserProfile, busy: Boolean) {
    val path = profile.photoPath
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = if (path == null) {
            null
        } else {
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
        }
    }
    val description = stringResource(R.string.profile_photo_description)
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(FinanceColors.BlueTint),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        when {
            busy -> CircularProgressIndicator(color = FinanceColors.Blue, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            image != null -> Image(
                bitmap = image,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(84.dp),
            )
            else -> Text(
                text = initials(profile),
                color = FinanceColors.Blue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
            )
        }
    }
}

private fun initials(profile: UserProfile): String {
    val letters = listOf(profile.firstName, profile.lastName).mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
    return if (letters.isEmpty()) "?" else letters.joinToString("")
}

// ---------------------------------------------------------------- Datos personales

@Composable
internal fun PersonalDataSection(
    profile: UserProfile,
    editing: Boolean,
    form: PersonalForm,
    errors: Map<ProfileField, String>,
    saving: Boolean,
    onEdit: () -> Unit,
    onFormChange: (PersonalForm) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    SectionTitle(stringResource(R.string.profile_section_personal))
    FinanceCard {
        if (editing) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(
                    value = form.firstName,
                    onValueChange = { onFormChange(form.copy(firstName = it)) },
                    label = stringResource(R.string.profile_first_name),
                    error = errors[ProfileField.FIRST_NAME],
                    capitalization = KeyboardCapitalization.Words,
                )
                FormField(
                    value = form.lastName,
                    onValueChange = { onFormChange(form.copy(lastName = it)) },
                    label = stringResource(R.string.profile_last_name),
                    error = errors[ProfileField.LAST_NAME],
                    capitalization = KeyboardCapitalization.Words,
                )
                FormField(
                    value = form.phone,
                    onValueChange = { onFormChange(form.copy(phone = it.take(20))) },
                    label = stringResource(R.string.profile_phone),
                    error = errors[ProfileField.PHONE],
                    keyboardType = KeyboardType.Phone,
                    supporting = stringResource(R.string.profile_phone_hint),
                )
                FormField(
                    value = form.email,
                    onValueChange = { onFormChange(form.copy(email = it.take(100))) },
                    label = stringResource(R.string.profile_email),
                    error = errors[ProfileField.EMAIL],
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    supporting = stringResource(R.string.profile_optional),
                )
                FormButtons(saving = saving, showCancel = profile.hasPersonalData, onSave = onSave, onCancel = onCancel)
            }
        } else if (!profile.hasPersonalData) {
            Text(
                text = stringResource(R.string.profile_complete_message),
                style = MaterialTheme.typography.bodyMedium,
                color = FinanceColors.MediumGray,
            )
            Button(onClick = onEdit, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.profile_complete_button))
            }
        } else {
            InfoRow(stringResource(R.string.profile_first_name), profile.firstName)
            InfoDivider()
            InfoRow(stringResource(R.string.profile_last_name), profile.lastName)
            InfoDivider()
            InfoRow(stringResource(R.string.profile_phone), ProfileValidator.formatPhone(profile.phone))
            InfoDivider()
            InfoRow(stringResource(R.string.profile_email), profile.email.ifBlank { "—" })
            TextButton(onClick = onEdit, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.profile_edit))
            }
        }
    }
}

// ---------------------------------------------------------------- Seguridad

@Composable
internal fun SecuritySection(
    profile: UserProfile,
    biometricAvailability: BiometricAvailability,
    editing: Boolean,
    form: CredentialsForm,
    errors: Map<ProfileField, String>,
    saving: Boolean,
    onEdit: () -> Unit,
    onFormChange: (CredentialsForm) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
) {
    SectionTitle(stringResource(R.string.profile_section_security))
    FinanceCard {
        // Usuario y contraseña
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.profile_credentials),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinanceColors.DarkBlue,
                )
                Text(
                    text = if (profile.hasPassword) {
                        stringResource(R.string.profile_credentials_set, profile.username.orEmpty())
                    } else {
                        stringResource(R.string.profile_credentials_unset)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (profile.hasPassword) FinanceColors.Green else FinanceColors.Amber,
                )
            }
            if (!editing) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(if (profile.hasPassword) R.string.profile_change else R.string.profile_create))
                }
            }
        }

        if (editing) {
            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(
                    value = form.username,
                    onValueChange = { onFormChange(form.copy(username = it.take(ProfileValidator.USERNAME_MAX))) },
                    label = stringResource(R.string.profile_username),
                    error = errors[ProfileField.USERNAME],
                    keyboardType = KeyboardType.Ascii,
                    supporting = stringResource(R.string.profile_username_hint),
                )
                if (profile.hasPassword) {
                    FormField(
                        value = form.currentPassword,
                        onValueChange = { onFormChange(form.copy(currentPassword = it.take(ProfileValidator.PASSWORD_MAX))) },
                        label = stringResource(R.string.profile_current_password),
                        error = errors[ProfileField.CURRENT_PASSWORD],
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                    )
                }
                FormField(
                    value = form.newPassword,
                    onValueChange = { onFormChange(form.copy(newPassword = it.take(ProfileValidator.PASSWORD_MAX))) },
                    label = stringResource(R.string.profile_new_password),
                    error = errors[ProfileField.PASSWORD],
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    supporting = stringResource(R.string.profile_password_hint),
                )
                FormField(
                    value = form.confirmPassword,
                    onValueChange = { onFormChange(form.copy(confirmPassword = it.take(ProfileValidator.PASSWORD_MAX))) },
                    label = stringResource(R.string.profile_confirm_password),
                    error = errors[ProfileField.PASSWORD_CONFIRM],
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                )
                Text(
                    text = stringResource(R.string.profile_password_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.Amber,
                )
                FormButtons(saving = saving, showCancel = true, onSave = onSave, onCancel = onCancel)
            }
        }

        InfoDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Huella
        val canToggle = profile.hasPassword && biometricAvailability == BiometricAvailability.AVAILABLE
        val stateText = stringResource(if (profile.biometricEnabled) R.string.profile_on else R.string.profile_off)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.profile_biometric),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinanceColors.DarkBlue,
                )
                Text(
                    text = when {
                        !profile.hasPassword -> stringResource(R.string.profile_biometric_need_password)
                        biometricAvailability == BiometricAvailability.NOT_ENROLLED -> stringResource(R.string.profile_biometric_not_enrolled)
                        biometricAvailability == BiometricAvailability.UNAVAILABLE -> stringResource(R.string.profile_biometric_unavailable)
                        profile.biometricEnabled -> stringResource(R.string.profile_biometric_on_hint)
                        else -> stringResource(R.string.profile_biometric_off_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.MediumGray,
                )
            }
            Switch(
                checked = profile.biometricEnabled,
                onCheckedChange = onBiometricToggle,
                enabled = canToggle || profile.biometricEnabled,
                modifier = Modifier.semantics {
                    role = Role.Switch
                    stateDescription = stateText
                },
            )
        }
    }
}

// ---------------------------------------------------------------- Recordatorios (Fase 6)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RemindersSection(
    settings: AppSettings,
    canNotify: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onToggleDay: (Int) -> Unit,
    onCustomChange: (String) -> Unit,
    onHourChange: (Int) -> Unit,
) {
    SectionTitle("Recordatorios de pago")
    FinanceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Avisarme antes de cada vencimiento", style = MaterialTheme.typography.bodyLarge, color = FinanceColors.DarkBlue)
                Text(
                    "Por ejemplo: «Recordatorio: mañana vence Internet por \$120.000».",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceColors.MediumGray,
                )
            }
            Switch(checked = settings.remindersEnabled, onCheckedChange = onEnabledChange)
        }
        if (settings.remindersEnabled && !canNotify) {
            Column(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .background(FinanceColors.AmberTint, MaterialTheme.shapes.small)
                    .padding(12.dp),
            ) {
                Text("Las notificaciones están desactivadas para Finance.", style = MaterialTheme.typography.bodyMedium, color = FinanceColors.Amber)
                TextButton(onClick = onRequestPermission) { Text("Activar notificaciones") }
            }
        }
        if (settings.remindersEnabled) {
            Text("Avisar", style = MaterialTheme.typography.labelLarge, color = FinanceColors.DarkBlue, modifier = Modifier.padding(top = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(7 to "7 días antes", 3 to "3 días antes", 1 to "1 día antes", 0 to "El mismo día").forEach { (day, label) ->
                    FilterChip(
                        selected = day in settings.reminderDays,
                        onClick = { onToggleDay(day) },
                        label = { Text(label) },
                    )
                }
            }
            var custom by remember(settings.customReminderDays) { mutableStateOf(settings.customReminderDays?.toString().orEmpty()) }
            OutlinedTextField(
                value = custom,
                onValueChange = { v -> custom = v.filter { it.isDigit() }.take(2); onCustomChange(custom) },
                label = { Text("Recordatorio personalizado (días antes, opcional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text("Hora de la revisión diaria", style = MaterialTheme.typography.labelLarge, color = FinanceColors.DarkBlue, modifier = Modifier.padding(top = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(6, 7, 8, 9, 12, 18, 20).forEach { hour ->
                    FilterChip(
                        selected = settings.reminderHour == hour,
                        onClick = { onHourChange(hour) },
                        label = { Text(if (hour < 12) "$hour a. m." else if (hour == 12) "12 m." else "${hour - 12} p. m.") },
                    )
                }
            }
            Text(
                "Los avisos llegan aunque la app esté cerrada y siguen después de reiniciar el teléfono. Nunca se repite el mismo aviso. En la pantalla bloqueada no se muestran valores.",
                style = MaterialTheme.typography.bodySmall,
                color = FinanceColors.MediumGray,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- Región e información

@Composable
internal fun RegionSection() {
    val sample = MoneyFormat.COLOMBIA.copy(showDecimals = true).format(Money(123_456_789L))
    SectionTitle(stringResource(R.string.profile_section_region))
    FinanceCard {
        InfoRow(stringResource(R.string.profile_country), stringResource(R.string.profile_country_value))
        InfoDivider()
        InfoRow(stringResource(R.string.profile_currency), stringResource(R.string.profile_currency_value))
        InfoDivider()
        InfoRow(stringResource(R.string.profile_number_format), sample)
        InfoDivider()
        InfoRow(stringResource(R.string.profile_timezone), "America/Bogota")
        InfoDivider()
        InfoRow(stringResource(R.string.profile_language), stringResource(R.string.profile_language_value))
    }
}

@Composable
internal fun AboutSection(appVersion: String) {
    SectionTitle(stringResource(R.string.profile_section_about))
    FinanceCard {
        InfoRow(stringResource(R.string.profile_version), appVersion)
        InfoDivider()
        InfoRow(stringResource(R.string.profile_privacy), stringResource(R.string.profile_privacy_value))
    }
}

// ---------------------------------------------------------------- Piezas comunes

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = FinanceColors.MediumGray,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = FinanceColors.DarkBlue,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = FinanceColors.MediumGray,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun InfoDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(color = FinanceColors.Border, modifier = modifier)
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isPassword: Boolean = false,
    supporting: String? = null,
) {
    val helper = error ?: supporting
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (helper != null) {
            { Text(helper) }
        } else {
            null
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = capitalization,
            autoCorrectEnabled = !isPassword && keyboardType == KeyboardType.Text,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FormButtons(saving: Boolean, showCancel: Boolean, onSave: () -> Unit, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCancel) {
            OutlinedButton(onClick = onCancel, enabled = !saving) {
                Text(stringResource(R.string.profile_cancel))
            }
        }
        Button(onClick = onSave, enabled = !saving) {
            if (saving) {
                CircularProgressIndicator(color = FinanceColors.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text(stringResource(R.string.profile_save))
            }
        }
    }
}
