package com.micontrolfinanciero.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors

/** Título grande de cada pestaña (Presupuesto, Reportes, Perfil…). */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = FinanceColors.DarkBlue,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}
