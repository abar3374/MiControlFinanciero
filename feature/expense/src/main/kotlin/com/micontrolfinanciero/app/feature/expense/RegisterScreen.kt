package com.micontrolfinanciero.app.feature.expense

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.micontrolfinanciero.app.core.designsystem.component.FinanceCard
import com.micontrolfinanciero.app.core.designsystem.component.ScreenHeader
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceTheme
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/**
 * Botón central "Registrar": una lista de tarjetas con lo que se puede registrar.
 * Cada tarjeta abre su formulario.
 */
@Composable
fun RegisterRoute(
    onNewExpense: () -> Unit,
    onNewIncome: () -> Unit,
    onOpenMovements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.register_title))
        Text(
            text = stringResource(R.string.register_question),
            style = MaterialTheme.typography.bodyLarge,
            color = FinanceColors.MediumGray,
        )

        RegisterOptionCard(
            iconRes = DesignR.drawable.ic_expense,
            accent = FinanceColors.Red,
            tint = FinanceColors.RedTint,
            title = stringResource(R.string.register_expense_title),
            description = stringResource(R.string.register_expense_description),
            onClick = onNewExpense,
        )
        RegisterOptionCard(
            iconRes = DesignR.drawable.ic_income,
            accent = FinanceColors.Green,
            tint = FinanceColors.GreenTint,
            title = stringResource(R.string.register_income_title),
            description = stringResource(R.string.register_income_description),
            onClick = onNewIncome,
        )
        RegisterOptionCard(
            iconRes = DesignR.drawable.ic_list,
            accent = FinanceColors.Blue,
            tint = FinanceColors.BlueTint,
            title = stringResource(R.string.register_history_title),
            description = stringResource(R.string.register_history_description),
            onClick = onOpenMovements,
        )

        FinanceCard(filled = true) {
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

@Composable
private fun RegisterOptionCard(
    @DrawableRes iconRes: Int,
    accent: Color,
    tint: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = FinanceColors.White,
        border = BorderStroke(1.dp, FinanceColors.Border),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(tint, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(iconRes), contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = FinanceColors.DarkBlue)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RegisterRoutePreview() {
    FinanceTheme { RegisterRoute(onNewExpense = {}, onNewIncome = {}, onOpenMovements = {}) }
}
