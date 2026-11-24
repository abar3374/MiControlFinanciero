package com.micontrolfinanciero.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors

/**
 * Tarjeta base de Finance: esquinas de 20 dp, borde suave y sombra mínima.
 * Con [filled] = true usa el gris claro de fondo y no lleva borde.
 */
@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    containerColor: Color = if (filled) FinanceColors.LightGray else FinanceColors.White,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = if (filled) null else BorderStroke(1.dp, FinanceColors.Border),
        shadowElevation = if (filled) 0.dp else 1.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}
