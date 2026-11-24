package com.micontrolfinanciero.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.component.EmptyState
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceTheme
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/** Punto de entrada que usa la navegación: conecta el ViewModel con la pantalla. */
@Composable
fun DashboardRoute(
    onOpenProfile: () -> Unit,
    onRegisterMovement: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Al volver a la app se actualiza el saludo (mañana, tarde, noche) y la fecha.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshClock()
        onPauseOrDispose { }
    }

    DashboardScreen(
        state = state,
        onOpenProfile = onOpenProfile,
        onRegisterMovement = onRegisterMovement,
    )
}

/** La pantalla solo dibuja el estado que recibe; no tiene lógica propia. */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onOpenProfile: () -> Unit,
    onRegisterMovement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DashboardHeader(onOpenProfile = onOpenProfile)

        FinanceCard(filled = true) {
            Text(
                text = state.greeting + ",",
                style = MaterialTheme.typography.bodyMedium,
                color = FinanceColors.MediumGray,
            )
            Text(
                text = state.userName ?: stringResource(R.string.dashboard_welcome_no_name),
                style = MaterialTheme.typography.headlineMedium,
                color = FinanceColors.DarkBlue,
            )
            Text(
                text = state.formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = FinanceColors.MediumGray,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        EmptyState(
            title = stringResource(R.string.dashboard_empty_title),
            message = stringResource(R.string.dashboard_empty_message),
            badge = stringResource(R.string.dashboard_empty_badge),
        )

        Button(
            onClick = onRegisterMovement,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text(stringResource(R.string.dashboard_register_button))
        }
    }
}

@Composable
private fun DashboardHeader(onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(FinanceColors.Blue, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("F", color = FinanceColors.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
            Text(
                text = stringResource(R.string.dashboard_app_name),
                style = MaterialTheme.typography.titleLarge,
                color = FinanceColors.DarkBlue,
            )
        }
        // El acceso a notificaciones se agrega en la Fase 6, cuando existan recordatorios.
        FilledIconButton(
            onClick = onOpenProfile,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = FinanceColors.BlueTint,
                contentColor = FinanceColors.Blue,
            ),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_nav_profile),
                contentDescription = stringResource(R.string.dashboard_open_profile),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DashboardScreenPreview() {
    FinanceTheme {
        DashboardScreen(
            state = DashboardUiState(
                greeting = "Buenas tardes",
                formattedDate = "Martes, 24 de noviembre de 2026",
                userName = null,
            ),
            onOpenProfile = {},
            onRegisterMovement = {},
        )
    }
}
