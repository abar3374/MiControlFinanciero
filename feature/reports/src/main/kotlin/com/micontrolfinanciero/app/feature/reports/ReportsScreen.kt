package com.micontrolfinanciero.app.feature.reports

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
 * Pestaña Reportes.
 * Fase 1: estado vacío honesto. Fase 7: reportes por periodo, filtros, gráficas y
 * exportación PDF / Excel, siempre con datos reales de la base de datos.
 */
@Composable
fun ReportsRoute(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.reports_title))
        EmptyState(
            title = stringResource(R.string.reports_empty_title),
            message = stringResource(R.string.reports_empty_message),
            badge = stringResource(R.string.reports_empty_badge),
            iconRes = DesignR.drawable.ic_nav_reports,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ReportsRoutePreview() {
    FinanceTheme { ReportsRoute() }
}
