package com.micontrolfinanciero.app.core.domain.profile

import com.micontrolfinanciero.app.core.model.PersonalData

/** Campos del perfil que pueden tener error. */
enum class ProfileField {
    FIRST_NAME, LAST_NAME, PHONE, EMAIL,
    USERNAME, CURRENT_PASSWORD, PASSWORD, PASSWORD_CONFIRM,
}

/** Resultado de validar: datos limpios (si todo está bien) y mensajes de error por campo. */
data class ValidationResult<T>(
    val value: T?,
    val errors: Map<ProfileField, String>,
) {
    val isValid: Boolean get() = errors.isEmpty() && value != null
}

/**
 * Validación de los datos del perfil (sección 13: "validación de todos los datos ingresados").
 * Limpia espacios, normaliza el celular y el usuario, y explica cada error en español.
 */
object ProfileValidator {

    private val NAME_REGEX = Regex("^[\\p{L}][\\p{L} '\\-]*$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    private val USERNAME_REGEX = Regex("^[a-z0-9._]+$")

    const val NAME_MAX = 50
    const val USERNAME_MIN = 4
    const val USERNAME_MAX = 30
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX = 64

    fun validatePersonalData(input: PersonalData): ValidationResult<PersonalData> {
        val errors = mutableMapOf<ProfileField, String>()

        val firstName = cleanName(input.firstName)
        nameError(firstName, "el nombre")?.let { errors[ProfileField.FIRST_NAME] = it }

        val lastName = cleanName(input.lastName)
        nameError(lastName, "los apellidos")?.let { errors[ProfileField.LAST_NAME] = it }

        val phone = normalizePhone(input.phone)
        if (phone == null) {
            errors[ProfileField.PHONE] = "Escribe un celular colombiano de 10 dígitos que empiece por 3, por ejemplo 300 123 4567."
        }

        val email = input.email.trim()
        if (email.isNotEmpty() && (email.length > 100 || !EMAIL_REGEX.matches(email))) {
            errors[ProfileField.EMAIL] = "El correo no parece válido. Revisa que tenga la forma nombre@dominio.com, o déjalo vacío."
        }

        val clean = if (errors.isEmpty()) PersonalData(firstName, lastName, phone!!, email.lowercase()) else null
        return ValidationResult(clean, errors)
    }

    /**
     * Valida usuario y contraseña nuevos. [password] y [confirm] se reciben como CharArray
     * para poder borrarlos de la memoria después de usarlos.
     */
    fun validateCredentials(username: String, password: CharArray, confirm: CharArray): ValidationResult<String> {
        val errors = mutableMapOf<ProfileField, String>()

        val user = normalizeUsername(username)
        when {
            user.length < USERNAME_MIN || user.length > USERNAME_MAX ->
                errors[ProfileField.USERNAME] = "El usuario debe tener entre $USERNAME_MIN y $USERNAME_MAX caracteres."
            !USERNAME_REGEX.matches(user) ->
                errors[ProfileField.USERNAME] = "Usa solo letras sin tildes, números, punto o guion bajo. Sin espacios."
        }

        passwordError(password)?.let { errors[ProfileField.PASSWORD] = it }
        if (!password.contentEquals(confirm)) {
            errors[ProfileField.PASSWORD_CONFIRM] = "Las dos contraseñas no coinciden."
        }

        return ValidationResult(if (errors.isEmpty()) user else null, errors)
    }

    fun passwordError(password: CharArray): String? = when {
        password.size < PASSWORD_MIN -> "La contraseña debe tener al menos $PASSWORD_MIN caracteres."
        password.size > PASSWORD_MAX -> "La contraseña puede tener máximo $PASSWORD_MAX caracteres."
        password.none { it.isLetter() } || password.none { it.isDigit() } ->
            "La contraseña debe combinar letras y números."
        password.any { it.isWhitespace() } -> "La contraseña no puede tener espacios."
        else -> null
    }

    /** Quita espacios sobrantes: "  ana   maría " -> "ana maría". */
    fun cleanName(value: String): String = value.trim().replace(Regex("\\s+"), " ")

    /** Usuario sin espacios y en minúsculas, para que "Andres" y "andres" sean el mismo. */
    fun normalizeUsername(value: String): String = value.trim().lowercase()

    /**
     * Acepta "300 123 4567", "300-123-4567", "+57 3001234567" o "573001234567".
     * Devuelve los 10 dígitos o null si no es un celular colombiano válido.
     */
    fun normalizePhone(value: String): String? {
        var digits = value.filter { it.isDigit() }
        if (digits.length == 12 && digits.startsWith("57")) digits = digits.substring(2)
        return if (digits.length == 10 && digits.startsWith("3")) digits else null
    }

    /** 3001234567 -> "300 123 4567" para mostrarlo. */
    fun formatPhone(phone: String): String =
        if (phone.length == 10) "${phone.substring(0, 3)} ${phone.substring(3, 6)} ${phone.substring(6)}" else phone

    private fun nameError(value: String, what: String): String? = when {
        value.length < 2 -> "Escribe ${what} (mínimo 2 letras)."
        value.length > NAME_MAX -> "Máximo $NAME_MAX caracteres."
        !NAME_REGEX.matches(value) -> "Usa solo letras, espacios, guion o apóstrofo."
        else -> null
    }
}
