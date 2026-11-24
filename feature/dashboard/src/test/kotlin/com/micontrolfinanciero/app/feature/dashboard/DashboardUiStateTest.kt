package com.micontrolfinanciero.app.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DashboardUiStateTest {

    private val bogota = ZoneId.of("America/Bogota")

    @Test
    fun `saludo segun la hora`() {
        assertEquals("Buenas noches", DashboardUiState.greetingFor(4))
        assertEquals("Buenos días", DashboardUiState.greetingFor(5))
        assertEquals("Buenos días", DashboardUiState.greetingFor(11))
        assertEquals("Buenas tardes", DashboardUiState.greetingFor(12))
        assertEquals("Buenas tardes", DashboardUiState.greetingFor(18))
        assertEquals("Buenas noches", DashboardUiState.greetingFor(19))
    }

    @Test
    fun `fecha en espanol con mayuscula inicial`() {
        val date = ZonedDateTime.of(2026, 11, 24, 15, 28, 0, 0, bogota)
        val state = DashboardUiState.from(date)
        assertEquals("Martes, 24 de noviembre de 2026", state.formattedDate)
        assertEquals("Buenas tardes", state.greeting)
    }
}
