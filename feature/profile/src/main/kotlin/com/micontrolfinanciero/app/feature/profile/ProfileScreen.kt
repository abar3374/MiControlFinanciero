package com.micontrolfinanciero.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.micontrolfinanciero.app.core.designsystem.component.EmptyState
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.component.ScreenHeader
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceTheme
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.MoneyFormat
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/**
 * Pestaña Perfil y ajustes.
 * Fase 1: muestra la configuración regional inicial (Colombia) y la versión instalada.
 * Estos valores son reales: el formato de ejemplo se calcula con MoneyFormat.
 * En la Fase 3 se guardarán en AppSettings y se podrán modificar.
 */
@Composable
fun ProfileRoute(
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    val sample = MoneyFormat.COLOMBIA.copy(showDecimals = true).format(Money(123_456_789L))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.profile_title))

        SectionTitle(stringResource(R.string.profile_section_region))
        FinanceCard {
            InfoRow(stringResource(R.string.profile_country), stringResource(R.string.profile_country_value))
            InfoDivider()
            InfoRow(stringResource(R.string.profile_currency), stringResource(R.string.profile_currency_value))
            InfoDivider()
            InfoRow(stringResource(R.string.profile_number_format), sample)
            InfoDivider()
            InfoRow(stringResource(R.string.profile_timezone), "America/Bogota")
            InfoDivider()
            InfoRow(stringResource(R.string.profile_language), stringResource(R.string.profile_language_value))
        }

        EmptyState(
            title = stringResource(R.string.profile_pending_title),
            message = stringResource(R.string.profile_pending_message),
            badge = stringResource(R.string.profile_pending_badge),
            iconRes = DesignR.drawable.ic_nav_profile,
        )

        SectionTitle(stringResource(R.string.profile_section_about))
        FinanceCard {
            InfoRow(stringResource(R.string.profile_version), appVersion)
            InfoDivider()
            InfoRow(stringResource(R.string.profile_privacy), stringResource(R.string.profile_privacy_value))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = FinanceColors.MediumGray,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = FinanceColors.DarkBlue,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = FinanceColors.MediumGray,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(color = FinanceColors.Border)
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ProfileRoutePreview() {
    FinanceTheme { ProfileRoute(appVersion = "0.1.0 (1)") }
}
