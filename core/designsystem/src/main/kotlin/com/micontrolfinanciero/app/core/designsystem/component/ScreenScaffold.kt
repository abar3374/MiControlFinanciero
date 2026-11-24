package com.micontrolfinanciero.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micontrolfinanciero.app.core.designsystem.R
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors

/** Barra superior de pantallas secundarias: volver, título y una acción opcional. */
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actionIcon: Int? = null,
    actionDescription: String? = null,
    actionTint: Color = FinanceColors.Red,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver", tint = FinanceColors.DarkBlue)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = FinanceColors.DarkBlue,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actionIcon != null && onAction != null) {
            IconButton(onClick = onAction) {
                Icon(painterResource(actionIcon), contentDescription = actionDescription, tint = actionTint)
            }
        }
    }
}

/** Botón principal grande de guardar, con indicador mientras guarda. */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    busy: Boolean = false,
    color: Color = FinanceColors.Blue,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(color = FinanceColors.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(text)
        }
    }
}

/**
 * Confirmación visual después de guardar (sección 8, paso 7), con la alerta de
 * presupuesto si corresponde.
 */
@Composable
fun SavedConfirmation(
    title: String,
    message: String,
    alert: String?,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String?,
    onSecondary: (() -> Unit)?,
    accent: Color = FinanceColors.Green,
    accentTint: Color = FinanceColors.GreenTint,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(accentTint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = accent, modifier = Modifier.size(36.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = FinanceColors.DarkBlue, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = FinanceColors.MediumGray, textAlign = TextAlign.Center)
        if (alert != null) {
            Text(
                text = alert,
                style = MaterialTheme.typography.bodyMedium,
                color = FinanceColors.Amber,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FinanceColors.AmberTint, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            )
        }
        PrimaryActionButton(text = primaryText, onClick = onPrimary, modifier = Modifier.padding(top = 8.dp))
        if (secondaryText != null && onSecondary != null) {
            OutlinedButton(
                onClick = onSecondary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text(secondaryText) }
        }
    }
}
