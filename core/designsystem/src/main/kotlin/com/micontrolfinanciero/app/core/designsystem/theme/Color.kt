package com.micontrolfinanciero.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de Finance (sección 5 del proyecto), la misma del boceto.
 * Todos los textos sobre fondo blanco cumplen contraste mínimo 4.5:1.
 */
object FinanceColors {
    /** Fondo principal. */
    val White = Color(0xFFFFFFFF)

    /** Fondos secundarios y superficies. */
    val LightGray = Color(0xFFF3F5F9)

    /** Bordes suaves de tarjetas y campos. */
    val Border = Color(0xFFE6EAF1)

    /** Botones principales, indicadores y elementos destacados. */
    val Blue = Color(0xFF2563EB)

    /** Fondo suave para elementos azules seleccionados. */
    val BlueTint = Color(0xFFEEF4FF)

    /** Títulos y textos importantes. */
    val DarkBlue = Color(0xFF0F1E46)

    /** Información secundaria. */
    val MediumGray = Color(0xFF5B6474)

    // Colores con significado (siempre acompañados de texto, nunca solos).
    /** Ingresos, pagado, todo bien. */
    val Green = Color(0xFF15803D)
    val GreenTint = Color(0xFFEAF7EF)

    /** Por vencer, alerta 75 %. */
    val Amber = Color(0xFFB45309)
    val AmberTint = Color(0xFFFFF4E6)

    /** Presupuesto superado, error. */
    val Red = Color(0xFFB91C1C)
    val RedTint = Color(0xFFFDECEC)
}
