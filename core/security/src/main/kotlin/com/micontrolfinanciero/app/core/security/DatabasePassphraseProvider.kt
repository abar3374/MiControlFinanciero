package com.micontrolfinanciero.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entrega la clave con la que SQLCipher cifra la base de datos.
 *
 * Cómo se protege (sección 13 del proyecto):
 *  1. La primera vez se genera una clave aleatoria de 256 bits para la base.
 *  2. Esa clave se cifra con AES-GCM usando otra clave que vive DENTRO del Android
 *     Keystore (hardware seguro cuando el teléfono lo tiene) y que nunca sale de él.
 *  3. En disco solo queda la versión cifrada, en una carpeta que Android no respalda.
 *
 * Si alguien copia el archivo de la base de datos a otro equipo, no puede abrirlo.
 */
@Singleton
class DatabasePassphraseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val wrappedKeyFile: File get() = File(context.noBackupFilesDir, WRAPPED_KEY_FILE)

    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        val file = wrappedKeyFile
        if (file.exists()) return unwrap(file.readBytes())

        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val tmp = File(file.parentFile, "$WRAPPED_KEY_FILE.tmp")
        tmp.writeBytes(wrap(passphrase))
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw IllegalStateException("No se pudo guardar la clave cifrada de la base de datos")
        }
        return passphrase
    }

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    /** Formato: [largo del IV (1 byte)][IV][texto cifrado + etiqueta GCM]. */
    private fun wrap(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        return byteArrayOf(iv.size.toByte()) + iv + encrypted
    }

    private fun unwrap(stored: ByteArray): ByteArray {
        val ivLength = stored[0].toInt()
        val iv = stored.copyOfRange(1, 1 + ivLength)
        val encrypted = stored.copyOfRange(1 + ivLength, stored.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "finance_db_wrapping_key"
        const val WRAPPED_KEY_FILE = "finance_db.key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PASSPHRASE_BYTES = 32
    }
}
