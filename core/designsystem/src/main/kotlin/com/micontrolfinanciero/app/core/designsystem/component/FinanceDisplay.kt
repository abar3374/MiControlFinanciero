package com.micontrolfinanciero.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.micontrolfinanciero.app.core.designsystem.R
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.model.IncomeDestination
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.MoneyFormat
import com.micontrolfinanciero.app.core.model.MovementItem
import com.micontrolfinanciero.app.core.model.MovementKind
import com.micontrolfinanciero.app.core.model.PaymentMethod
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatos de pantalla en español de Colombia. */
object FinanceFormat {
    private val locale: Locale = Locale.forLanguageTag("es-CO")
    val zone: ZoneId = ZoneId.of("America/Bogota")
    private val money = MoneyFormat.COLOMBIA
    private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    private val shortDayFormat = DateTimeFormatter.ofPattern("d MMM", locale)
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", locale)
    private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", locale)

    fun money(value: Money): String = money.format(value)

    fun date(value: LocalDate): String = value.format(dayFormat)

    fun time(value: LocalTime): String = value.format(timeFormat)

    fun month(value: YearMonth): String = value.format(monthFormat).replaceFirstChar { it.titlecase(locale) }

    /** "Hoy 3:28 p. m.", "Ayer 7:15 a. m." o "22 nov 3:00 p. m.". */
    fun moment(epochMillis: Long, today: LocalDate = LocalDate.now(zone)): String {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val day = local.toLocalDate()
        val dayText = when (day) {
            today -> "Hoy"
            today.minusDays(1) -> "Ayer"
            else -> if (day.year == today.year) day.format(shortDayFormat) else day.format(dayFormat)
        }
        return "$dayText · ${local.toLocalTime().format(timeFormat)}"
    }

    fun detailLabel(item: MovementItem): String = when (item.kind) {
        MovementKind.EXPENSE -> PaymentMethod.entries.firstOrNull { it.name == item.detail }?.label.orEmpty()
        MovementKind.INCOME -> IncomeDestination.entries.firstOrNull { it.name == item.detail }?.label.orEmpty()
    }
}

/** Fila de un gasto o ingreso, como en el boceto. */
@Composable
fun MovementListItem(
    item: MovementItem,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isIncome = item.kind == MovementKind.INCOME
    val clickable = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(clickable)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (isIncome) FinanceColors.GreenTint else FinanceColors.RedTint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isIncome) R.drawable.ic_income else R.drawable.ic_expense),
                contentDescription = null,
                tint = if (isIncome) FinanceColors.Green else FinanceColors.Red,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = FinanceColors.DarkBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(item.subtitle, FinanceFormat.moment(item.at), FinanceFormat.detailLabel(item))
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = FinanceColors.MediumGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = (if (isIncome) "+" else "−") + FinanceFormat.money(item.amount),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) FinanceColors.Green else FinanceColors.DarkBlue,
        )
    }
}

/**
 * Barra de avance de un presupuesto. Color según los umbrales de alerta:
 * azul < 75 %, ámbar desde 75 %, rojo desde 90 %.
 */
@Composable
fun BudgetProgressBar(percent: Int, modifier: Modifier = Modifier, height: Int = 8, trackColor: Color = FinanceColors.Border) {
    val color = when {
        percent >= 90 -> FinanceColors.Red
        percent >= 75 -> FinanceColors.Amber
        else -> FinanceColors.Blue
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                .height(height.dp)
                .clip(RoundedCornerShape(height.dp))
                .background(color),
        )
    }
}
