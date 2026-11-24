package com.micontrolfinanciero.app.feature.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.security.BiometricAuthenticator
import com.micontrolfinanciero.app.core.security.BiometricAvailability
import com.micontrolfinanciero.app.core.security.findFragmentActivity

@Composable
fun LockRoute(viewModel: LockViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricReady = state.biometricEnabled &&
        BiometricAuthenticator.availability(context) == BiometricAvailability.AVAILABLE

    val promptTitle = stringResource(R.string.lock_biometric_title)
    val promptSubtitle = stringResource(R.string.lock_biometric_subtitle)
    val promptCancel = stringResource(R.string.lock_biometric_use_password)
    val askFingerprint: () -> Unit = {
        context.findFragmentActivity()?.let { activity ->
            BiometricAuthenticator.authenticate(
                activity = activity,
                title = promptTitle,
                subtitle = promptSubtitle,
                negativeButtonText = promptCancel,
                onSuccess = viewModel::onBiometricSuccess,
                onError = viewModel::onBiometricError,
            )
        }
    }

    // Si la huella está activa, se ofrece apenas aparece la pantalla (una sola vez).
    var autoPrompted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(biometricReady) {
        if (biometricReady && !autoPrompted) {
            autoPrompted = true
            askFingerprint()
        }
    }

    LockScreen(
        state = state,
        biometricReady = biometricReady,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onUseFingerprint = askFingerprint,
    )
}

@Composable
fun LockScreen(
    state: LockUiState,
    biometricReady: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onUseFingerprint: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceColors.White)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(FinanceColors.Blue, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("F", color = FinanceColors.White, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        }
        Text(
            text = if (state.firstName.isNotBlank()) {
                stringResource(R.string.lock_greeting_name, state.firstName)
            } else {
                stringResource(R.string.lock_greeting)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = FinanceColors.DarkBlue,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.lock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = FinanceColors.MediumGray,
            textAlign = TextAlign.Center,
        )

        OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.lock_username)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.lock_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.error != null) {
            Text(
                text = state.error,
                color = FinanceColors.Red,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            if (state.checking) {
                CircularProgressIndicator(color = FinanceColors.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    if (state.waitSeconds > 0) {
                        stringResource(R.string.lock_wait, state.waitSeconds)
                    } else {
                        stringResource(R.string.lock_enter)
                    },
                )
            }
        }

        if (biometricReady) {
            OutlinedButton(
                onClick = onUseFingerprint,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.lock_use_fingerprint))
            }
        }
    }
}

/** Pantalla mientras se abre la base de datos, o si falla. */
@Composable
fun LockStatusScreen(message: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceColors.White)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (message == null) {
            CircularProgressIndicator(color = FinanceColors.Blue)
        } else {
            Text(
                text = stringResource(R.string.lock_failed, message),
                color = FinanceColors.DarkBlue,
                textAlign = TextAlign.Center,
            )
        }
    }
}
