package com.micontrolfinanciero.app.core.domain.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Convierte una contraseña en un "hash" que no se puede revertir (PBKDF2-HMAC-SHA256
 * con sal aleatoria). Se guarda solo el hash: si alguien obtiene la base de datos, no
 * puede leer la contraseña.
 *
 * Formato guardado: `pbkdf2-sha256$<iteraciones>$<sal base64>$<hash base64>`.
 * Las iteraciones quedan dentro del texto, así se pueden subir en el futuro sin
 * romper contraseñas viejas.
 */
object PasswordHasher {

    private const val ALGORITHM_ID = "pbkdf2-sha256"
    private const val JCA_NAME = "PBKDF2WithHmacSHA256"
    const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256

    private val random = SecureRandom()

    fun hash(password: CharArray, iterations: Int = DEFAULT_ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = derive(password, salt, iterations)
        val b64 = Base64.getEncoder()
        return listOf(ALGORITHM_ID, iterations.toString(), b64.encodeToString(salt), b64.encodeToString(hash))
            .joinToString("$")
    }

    /** Compara en tiempo constante para no dar pistas por cuánto tarda. */
    fun verify(password: CharArray, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 4 || parts[0] != ALGORITHM_ID) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        if (iterations < 1) return false
        return try {
            val b64 = Base64.getDecoder()
            val salt = b64.decode(parts[2])
            val expected = b64.decode(parts[3])
            MessageDigest.isEqual(expected, derive(password, salt, iterations))
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, HASH_BITS)
        try {
            return SecretKeyFactory.getInstance(JCA_NAME).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
