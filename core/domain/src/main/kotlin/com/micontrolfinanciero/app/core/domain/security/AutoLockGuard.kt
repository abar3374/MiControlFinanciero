package com.micontrolfinanciero.app.core.domain.security

/**
 * Evita que el bloqueo automático salte cuando la propia app abre otra pantalla de Android
 * (selector de archivos, cámara, guardar reporte o respaldo). Sin esto, al volver el resultado
 * se perdería detrás de la pantalla de bloqueo. Solo cubre UNA vuelta a la app.
 */
object AutoLockGuard {
    @Volatile private var skipUntil: Long = 0L

    /** Llamar justo antes de abrir el selector o la cámara. */
    fun allowExternalScreen(nowMillis: Long = System.currentTimeMillis()) {
        skipUntil = nowMillis + MAX_EXTERNAL_MILLIS
    }

    /** true si la vuelta a la app viene de una pantalla que abrió la propia app. Se consume una vez. */
    fun consume(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val allowed = nowMillis <= skipUntil
        skipUntil = 0L
        return allowed
    }

    /** Tiempo máximo fuera de la app que se acepta sin pedir clave (10 minutos). */
    private const val MAX_EXTERNAL_MILLIS = 10 * 60_000L
}
