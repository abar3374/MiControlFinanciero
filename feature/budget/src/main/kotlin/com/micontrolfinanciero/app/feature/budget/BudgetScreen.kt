package com.micontrolfinanciero.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.micontrolfinanciero.app.core.designsystem.component.EmptyState
import com.micontrolfinanciero.app.core.designsystem.component.ScreenHeader
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceTheme
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/**
 * Pestaña Presupuesto.
 * Fase 1: estado vacío honesto. Fase 4: rubros y presupuesto global.
 * Fase 5: gastos fijos, ingresos fijos y acceso a Créditos y obligaciones.
 */
@Composable
fun BudgetRoute(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.budget_title))
        EmptyState(
            title = stringResource(R.string.budget_empty_title),
            message = stringResource(R.string.budget_empty_message),
            badge = stringResource(R.string.budget_empty_badge),
            iconRes = DesignR.drawable.ic_nav_budget,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BudgetRoutePreview() {
    FinanceTheme { BudgetRoute() }
}
