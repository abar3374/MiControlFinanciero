package com.micontrolfinanciero.app.feature.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.micontrolfinanciero.app.core.designsystem.component.EmptyState
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.component.ScreenHeader
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceTheme
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/**
 * Botón central "Registrar".
 * Fase 1: explica qué se podrá registrar. Fase 4: formulario real de gasto o ingreso,
 * con el campo "¿En qué gastaste?" obligatorio.
 */
@Composable
fun RegisterRoute(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.register_title))
        EmptyState(
            title = stringResource(R.string.register_empty_title),
            message = stringResource(R.string.register_empty_message),
            badge = stringResource(R.string.register_empty_badge),
            iconRes = DesignR.drawable.ic_nav_add,
        )
        FinanceCard {
            Text(
                text = stringResource(R.string.register_rule_title),
                style = MaterialTheme.typography.titleSmall,
                color = FinanceColors.DarkBlue,
            )
            Text(
                text = stringResource(R.string.register_rule_message),
                style = MaterialTheme.typography.bodyMedium,
                color = FinanceColors.MediumGray,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RegisterRoutePreview() {
    FinanceTheme { RegisterRoute() }
}
