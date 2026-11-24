package com.micontrolfinanciero.app.core.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** ¿Este teléfono puede usar huella (u otra biometría segura)? */
enum class BiometricAvailability {
    /** Tiene lector y hay al menos una huella registrada. */
    AVAILABLE,

    /** Tiene lector, pero no hay huellas registradas en los ajustes del teléfono. */
    NOT_ENROLLED,

    /** No tiene lector compatible o está deshabilitado. */
    UNAVAILABLE,
}

/**
 * Envoltura sencilla de BiometricPrompt, la ventana oficial de Android para la huella.
 * La app nunca ve ni guarda la huella: Android solo le dice "sí es el dueño" o "no".
 *
 * Se exige biometría fuerte (BIOMETRIC_STRONG): en la mayoría de teléfonos es la huella.
 */
object BiometricAuthenticator {

    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

    fun availability(context: Context): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }

    /**
     * Muestra la ventana de huella. [onError] solo se llama ante errores reales
     * (por ejemplo, demasiados intentos); si el usuario cancela, no se llama nada.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val cancelled = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                if (!cancelled) onError(errString.toString())
            }
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()
        prompt.authenticate(info)
    }
}

/** Busca la FragmentActivity detrás de un Context de Compose (necesaria para BiometricPrompt). */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
