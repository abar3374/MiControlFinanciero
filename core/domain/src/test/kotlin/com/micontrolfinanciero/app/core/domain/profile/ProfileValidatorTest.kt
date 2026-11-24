package com.micontrolfinanciero.app.core.domain.profile

import com.micontrolfinanciero.app.core.domain.security.PasswordHasher
import com.micontrolfinanciero.app.core.model.PersonalData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidatorTest {

    @Test
    fun `datos validos se limpian y normalizan`() {
        val result = ProfileValidator.validatePersonalData(
            PersonalData("  Ana   María ", " Pérez Gómez ", "+57 300 123 4567", " Ana@Correo.COM "),
        )
        assertTrue(result.isValid)
        assertEquals(PersonalData("Ana María", "Pérez Gómez", "3001234567", "ana@correo.com"), result.value)
    }

    @Test
    fun `correo es opcional`() {
        val result = ProfileValidator.validatePersonalData(PersonalData("Ana", "Pérez", "3001234567", ""))
        assertTrue(result.isValid)
    }

    @Test
    fun `celular invalido da error claro`() {
        val result = ProfileValidator.validatePersonalData(PersonalData("Ana", "Pérez", "6015551234", ""))
        assertTrue(result.errors.containsKey(ProfileField.PHONE))
        assertNull(result.value)
    }

    @Test
    fun `formas aceptadas de celular`() {
        assertEquals("3001234567", ProfileValidator.normalizePhone("300-123-4567"))
        assertEquals("3001234567", ProfileValidator.normalizePhone("573001234567"))
        assertNull(ProfileValidator.normalizePhone("300123456"))
        assertEquals("300 123 4567", ProfileValidator.formatPhone("3001234567"))
    }

    @Test
    fun `nombre con numeros o vacio no se acepta`() {
        val result = ProfileValidator.validatePersonalData(PersonalData("Ana2", "", "3001234567", "x@"))
        assertTrue(result.errors.containsKey(ProfileField.FIRST_NAME))
        assertTrue(result.errors.containsKey(ProfileField.LAST_NAME))
        assertTrue(result.errors.containsKey(ProfileField.EMAIL))
    }

    @Test
    fun `credenciales validas`() {
        val result = ProfileValidator.validateCredentials(" Ana.Perez ", "clave2026".toCharArray(), "clave2026".toCharArray())
        assertTrue(result.isValid)
        assertEquals("ana.perez", result.value)
    }

    @Test
    fun `credenciales debiles o distintas`() {
        val result = ProfileValidator.validateCredentials("an", "corta".toCharArray(), "otra".toCharArray())
        assertTrue(result.errors.containsKey(ProfileField.USERNAME))
        assertTrue(result.errors.containsKey(ProfileField.PASSWORD))
        assertTrue(result.errors.containsKey(ProfileField.PASSWORD_CONFIRM))
        assertTrue(ProfileValidator.passwordError("solamenteletras".toCharArray()) != null)
        assertTrue(ProfileValidator.validateCredentials("ana perez", "clave2026".toCharArray(), "clave2026".toCharArray())
            .errors.containsKey(ProfileField.USERNAME))
    }

    @Test
    fun `hash de contrasena verifica y no guarda el texto`() {
        val stored = PasswordHasher.hash("clave2026".toCharArray(), iterations = 1_000)
        assertTrue(!stored.contains("clave2026"))
        assertTrue(PasswordHasher.verify("clave2026".toCharArray(), stored))
        assertTrue(!PasswordHasher.verify("clave2027".toCharArray(), stored))
        assertTrue(!PasswordHasher.verify("clave2026".toCharArray(), "basura"))
    }

    @Test
    fun `dos hash de la misma contrasena son distintos por la sal`() {
        val a = PasswordHasher.hash("clave2026".toCharArray(), iterations = 1_000)
        val b = PasswordHasher.hash("clave2026".toCharArray(), iterations = 1_000)
        assertTrue(a != b)
    }
}
