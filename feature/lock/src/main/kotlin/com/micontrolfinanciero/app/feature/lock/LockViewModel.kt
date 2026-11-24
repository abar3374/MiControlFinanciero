package com.micontrolfinanciero.app.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockUiState(
    val firstName: String = "",
    val biometricEnabled: Boolean = false,
    val username: String = "",
    val password: String = "",
    val checking: Boolean = false,
    val error: String? = null,
    /** Segundos de espera tras varios intentos fallidos (0 = puede intentar). */
    val waitSeconds: Int = 0,
) {
    val canSubmit: Boolean get() = username.isNotBlank() && password.isNotEmpty() && !checking && waitSeconds == 0
}

@HiltViewModel
class LockViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val lockManager: AppLockManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private var failedAttempts = 0
    private var countdown: Job? = null

    init {
        viewModelScope.launch {
            repository.observeProfile().collect { profile ->
                _uiState.update { it.copy(firstName = profile.firstName, biometricEnabled = profile.biometricEnabled) }
            }
        }
    }

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value.take(40), error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value.take(64), error = null) }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(checking = true, error = null) }
        viewModelScope.launch {
            val chars = state.password.toCharArray()
            val ok = try {
                repository.verifyCredentials(state.username, chars)
            } finally {
                chars.fill('\u0000')
            }
            if (ok) {
                failedAttempts = 0
                _uiState.update { it.copy(checking = false, password = "") }
                lockManager.unlock()
            } else {
                failedAttempts++
                val wait = waitAfter(failedAttempts)
                _uiState.update {
                    it.copy(
                        checking = false,
                        password = "",
                        error = if (wait > 0) "Demasiados intentos. Espera $wait segundos." else "Usuario o contraseña incorrectos.",
                    )
                }
                if (wait > 0) startCountdown(wait)
            }
        }
    }

    fun onBiometricSuccess() {
        failedAttempts = 0
        lockManager.unlock()
    }

    fun onBiometricError(message: String) = _uiState.update { it.copy(error = message) }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (left in seconds downTo 1) {
                _uiState.update { it.copy(waitSeconds = left) }
                delay(1_000)
            }
            _uiState.update { it.copy(waitSeconds = 0, error = null) }
        }
    }

    companion object {
        /** 5 fallos: 30 s; cada fallo extra duplica la espera, hasta 15 minutos. */
        fun waitAfter(failed: Int): Int {
            if (failed < 5) return 0
            val doubled = 30L shl (failed - 5).coerceAtMost(5)
            return doubled.coerceAtMost(900L).toInt()
        }
    }
}
