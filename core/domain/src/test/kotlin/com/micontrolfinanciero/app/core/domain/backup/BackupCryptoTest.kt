package com.micontrolfinanciero.app.core.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    private val plain = """{"expense":[{"concept":"Sistecrédito","amount_cents":20046500}]}""".toByteArray()

    @Test
    fun `cifra y descifra con la misma contrasena`() {
        val enc = BackupCrypto.encrypt(plain, "clave-segura".toCharArray(), iterations = 20_000)
        assertTrue(!String(enc, Charsets.ISO_8859_1).contains("Sistecr"))
        assertEquals(String(plain), String(BackupCrypto.decrypt(enc, "clave-segura".toCharArray())))
    }

    @Test
    fun `contrasena incorrecta o archivo alterado se rechazan`() {
        val enc = BackupCrypto.encrypt(plain, "clave-segura".toCharArray(), iterations = 20_000)
        var error: String? = null
        try { BackupCrypto.decrypt(enc, "otra-clave".toCharArray()) } catch (e: BackupException) { error = e.message }
        assertEquals("Contraseña incorrecta o respaldo dañado.", error)

        val tampered = enc.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        error = null
        try { BackupCrypto.decrypt(tampered, "clave-segura".toCharArray()) } catch (e: BackupException) { error = e.message }
        assertNotNull(error)

        error = null
        try { BackupCrypto.decrypt("hola".toByteArray(), "x".toCharArray()) } catch (e: BackupException) { error = e.message }
        assertEquals("El archivo no es un respaldo de Finance.", error)
    }

    @Test
    fun `validacion de contrasena del respaldo`() {
        assertNotNull(BackupCrypto.passwordError("corta", "corta"))
        assertNotNull(BackupCrypto.passwordError("larga1234", "larga12345"))
        assertNull(BackupCrypto.passwordError("larga1234", "larga1234"))
    }
}
