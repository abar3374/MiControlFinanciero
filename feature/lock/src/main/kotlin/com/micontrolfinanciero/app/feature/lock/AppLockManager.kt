package com.micontrolfinanciero.app.feature.lock

import com.micontrolfinanciero.app.core.domain.profile.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de bloqueo de toda la app. */
sealed interface LockState {
    /** Leyendo el perfil para saber si hay contraseña. */
    data object Checking : LockState

    /** Hay contraseña y el usuario aún no se ha identificado. */
    data object Locked : LockState

    /** Se puede usar la app. */
    data object Unlocked : LockState

    /** No se pudo abrir la base de datos cifrada. */
    data class Failed(val message: String) : LockState
}

/**
 * Decide cuándo se muestra la pantalla de bloqueo:
 *  - al abrir la app, si el usuario ya creó contraseña;
 *  - al volver a la app después de [AUTO_LOCK_MILLIS] en segundo plano.
 * Si no hay contraseña, la app abre directo (el usuario la crea en Perfil).
 */
@Singleton
class AppLockManager @Inject constructor(
    repository: UserProfileRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<LockState>(LockState.Checking)
    val state: StateFlow<LockState> = _state.asStateFlow()

    @Volatile private var hasPassword = false
    @Volatile private var backgroundedAt: Long? = null

    init {
        scope.launch {
            repository.observeProfile()
                .catch { error ->
                    _state.value = LockState.Failed(error.message ?: error.javaClass.simpleName)
                }
                .collect { profile ->
                    hasPassword = profile.hasPassword
                    if (_state.value == LockState.Checking) {
                        _state.value = if (profile.hasPassword) LockState.Locked else LockState.Unlocked
                    }
                }
        }
    }

    fun unlock() {
        _state.value = LockState.Unlocked
    }

    /** Llamar cuando la app deja de verse (onStop). */
    fun onAppBackgrounded(nowMillis: Long) {
        if (_state.value == LockState.Unlocked) backgroundedAt = nowMillis
    }

    /** Llamar cuando la app vuelve a verse (onStart). */
    fun onAppForegrounded(nowMillis: Long) {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (hasPassword && nowMillis - since >= AUTO_LOCK_MILLIS) {
            _state.value = LockState.Locked
        }
    }

    companion object {
        /** Bloqueo automático tras 1 minuto fuera de la app (configurable en la Fase 8). */
        const val AUTO_LOCK_MILLIS = 60_000L
    }
}
