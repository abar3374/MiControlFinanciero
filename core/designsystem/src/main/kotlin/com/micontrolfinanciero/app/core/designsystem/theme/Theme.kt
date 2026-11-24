package com.micontrolfinanciero.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Esquema de color de Material 3 armado con la paleta de Finance.
 * Por ahora solo tema claro (como el boceto). El tema oscuro se puede agregar
 * aquí más adelante sin tocar las pantallas.
 */
private val FinanceLightColors = lightColorScheme(
    primary = FinanceColors.Blue,
    onPrimary = FinanceColors.White,
    primaryContainer = FinanceColors.BlueTint,
    onPrimaryContainer = FinanceColors.DarkBlue,
    secondary = FinanceColors.DarkBlue,
    onSecondary = FinanceColors.White,
    tertiary = FinanceColors.Green,
    onTertiary = FinanceColors.White,
    background = FinanceColors.White,
    onBackground = FinanceColors.DarkBlue,
    surface = FinanceColors.White,
    onSurface = FinanceColors.DarkBlue,
    surfaceVariant = FinanceColors.LightGray,
    onSurfaceVariant = FinanceColors.MediumGray,
    surfaceContainer = FinanceColors.LightGray,
    outline = FinanceColors.Border,
    outlineVariant = FinanceColors.Border,
    error = FinanceColors.Red,
    onError = FinanceColors.White,
    errorContainer = FinanceColors.RedTint,
    onErrorContainer = FinanceColors.Red,
)

/** Esquinas redondeadas, como en el boceto. */
val FinanceShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FinanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FinanceLightColors,
        typography = FinanceTypography,
        shapes = FinanceShapes,
        content = content,
    )
}
