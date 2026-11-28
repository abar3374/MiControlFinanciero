package com.micontrolfinanciero.app.core.domain.backup

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** El respaldo no se pudo abrir: contraseña incorrecta o archivo dañado / ajeno. */
class BackupException(message: String) : Exception(message)

/**
 * Cifrado de los respaldos (sección 13 del proyecto).
 *
 * Formato del archivo:  "FINBK1" | iteraciones (4 bytes) | sal (16) | IV (12) | datos AES-256-GCM.
 * La clave sale de la contraseña del respaldo con PBKDF2-HMAC-SHA256. La contraseña no se guarda
 * en ningún lado: sin ella el respaldo no se puede abrir (ni siquiera el desarrollador).
 * GCM detecta cualquier cambio en el archivo.
 */
object BackupCrypto {

    private val MAGIC = "FINBK1".toByteArray(Charsets.US_ASCII)
    const val ITERATIONS = 210_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val MIN_PASSWORD = 8

    /** Mensaje de error para la contraseña del respaldo, o null si sirve. */
    fun passwordError(password: String, confirmation: String): String? = when {
        password.length < MIN_PASSWORD -> "Usa al menos $MIN_PASSWORD caracteres."
        password != confirmation -> "Las contraseñas no coinciden."
        else -> null
    }

    fun encrypt(plain: ByteArray, password: CharArray, iterations: Int = ITERATIONS, random: SecureRandom = SecureRandom()): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        val body = cipher.doFinal(plain)
        return MAGIC + intToBytes(iterations) + salt + iv + body
    }

    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        val header = MAGIC.size + 4 + SALT_BYTES + IV_BYTES
        if (data.size < header + 16 || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupException("El archivo no es un respaldo de Finance.")
        }
        var p = MAGIC.size
        val iterations = bytesToInt(data, p); p += 4
        if (iterations !in 10_000..5_000_000) throw BackupException("El respaldo está dañado.")
        val salt = data.copyOfRange(p, p + SALT_BYTES); p += SALT_BYTES
        val iv = data.copyOfRange(p, p + IV_BYTES); p += IV_BYTES
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        return try {
            cipher.doFinal(data, p, data.size - p)
        } catch (e: AEADBadTagException) {
            throw BackupException("Contraseña incorrecta o respaldo dañado.")
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun intToBytes(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())
    private fun bytesToInt(b: ByteArray, o: Int) =
        ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)
}

/** Resultado de leer un respaldo antes de restaurarlo. */
data class BackupInfo(val createdAt: Long, val appVersion: String, val schemaVersion: Int, val rows: Int, val files: Int)

/** Crea y restaura respaldos cifrados (implementado en :core:data). */
interface BackupRepository {
    /** Devuelve el archivo cifrado listo para guardar. */
    suspend fun createBackup(password: CharArray, appVersion: String): ByteArray

    /** Revisa la contraseña y el contenido sin tocar los datos actuales. */
    suspend fun inspect(data: ByteArray, password: CharArray): BackupInfo

    /** Reemplaza TODOS los datos por los del respaldo, en una sola transacción. */
    suspend fun restore(data: ByteArray, password: CharArray): BackupInfo
}

/** Un cambio registrado en el historial de auditoría. */
data class AuditEntry(val id: Long, val entity: String, val entityId: Long, val action: String, val details: String, val at: Long)

/** Lectura del historial de auditoría (solo lectura: nadie lo edita desde la app). */
interface AuditRepository {
    fun observeLatest(limit: Int): kotlinx.coroutines.flow.Flow<List<AuditEntry>>
}
