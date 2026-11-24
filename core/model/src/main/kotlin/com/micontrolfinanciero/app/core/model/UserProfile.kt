package com.micontrolfinanciero.app.core.model

/**
 * Perfil del dueño de la app. Hay un solo perfil por teléfono.
 *
 * La contraseña NO está aquí: solo se sabe si existe ([hasPassword]).
 * Su huella criptográfica vive en la base de datos y nunca sale de la capa de datos.
 */
data class UserProfile(
    val firstName: String,
    val lastName: String,
    /** Celular normalizado: 10 dígitos, por ejemplo 3001234567. */
    val phone: String,
    /** Correo opcional; vacío si no se registró. */
    val email: String,
    /** Ruta del archivo de la foto en el almacenamiento privado de la app, o null. */
    val photoPath: String?,
    /** Usuario para entrar a la app, en minúsculas; null si aún no se creó. */
    val username: String?,
    val hasPassword: Boolean,
    val biometricEnabled: Boolean,
) {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    val hasPersonalData: Boolean get() = firstName.isNotBlank()

    companion object {
        val EMPTY = UserProfile(
            firstName = "",
            lastName = "",
            phone = "",
            email = "",
            photoPath = null,
            username = null,
            hasPassword = false,
            biometricEnabled = false,
        )
    }
}

/** Los datos personales que el usuario edita en el formulario de perfil. */
data class PersonalData(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String,
)
