package com.micontrolfinanciero.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.core.model.Money

/** Colores de las categorías en gráficas (distinguibles también por luminosidad). */
val ChartPalette = listOf(
    Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF93C5FD), Color(0xFFF59E0B),
    Color(0xFF8B5CF6), Color(0xFF15803D), Color(0xFFDC2626), Color(0xFF94A3B8),
)

/** Leyenda pequeña: cuadro de color + texto. */
@Composable
fun ChartLegend(items: List<Pair<Color, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
                Text(label, style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
            }
        }
    }
}

/**
 * Líneas de evolución: gastos (azul) e ingresos (verde) por día o por mes.
 * [labels] se muestran espaciadas para que no se amontonen.
 */
@Composable
fun EvolutionChart(
    labels: List<String>,
    expenses: List<Money>,
    incomes: List<Money>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val max = (expenses + incomes).maxOfOrNull { it.cents }?.coerceAtLeast(1L) ?: 1L
    val blue = FinanceColors.Blue
    val green = FinanceColors.Green
    val grid = FinanceColors.Border
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .semantics { contentDescription = description },
        ) {
            val n = labels.size.coerceAtLeast(2)
            for (i in 0..3) {
                val y = size.height * i / 3f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            fun series(values: List<Money>, color: Color) {
                if (values.isEmpty()) return
                val path = Path()
                values.forEachIndexed { i, v ->
                    val x = size.width * i / (n - 1).toFloat()
                    val y = size.height - (v.cents.toFloat() / max) * (size.height - 8f)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 5f, cap = StrokeCap.Round))
            }
            series(incomes, green)
            series(expenses, blue)
        }
        val step = (labels.size / 6).coerceAtLeast(1)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.filterIndexed { i, _ -> i % step == 0 || i == labels.lastIndex }.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = FinanceColors.MediumGray)
            }
        }
    }
}

/** Barras horizontales con nombre, valor y porcentaje del total. */
@Composable
fun HorizontalBars(
    items: List<Pair<String, Money>>,
    format: (Money) -> String,
    modifier: Modifier = Modifier,
    maxItems: Int = 8,
) {
    val total = items.sumOf { it.second.cents }.coerceAtLeast(1L)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.take(maxItems).forEachIndexed { index, (label, value) ->
            val share = (value.cents * 100 / total).toInt()
            Column {
                Row {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
                    Text("${format(value)} · $share %", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
                }
                Box(
                    Modifier.padding(top = 4.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(FinanceColors.LightGray),
                ) {
                    Box(
                        Modifier.fillMaxWidth(share / 100f).height(8.dp).clip(RoundedCornerShape(8.dp))
                            .background(ChartPalette[index % ChartPalette.size]),
                    )
                }
            }
        }
    }
}

/** Barras agrupadas (dos series) por grupo: presupuesto vs real, gastos vs ingresos por mes. */
@Composable
fun GroupedBars(
    labels: List<String>,
    first: List<Money>,
    second: List<Money>,
    firstColor: Color,
    secondColor: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val max = (first + second).maxOfOrNull { it.cents }?.coerceAtLeast(1L) ?: 1L
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = description },
        ) {
            val groups = labels.size.coerceAtLeast(1)
            val groupWidth = size.width / groups
            val barWidth = (groupWidth * 0.32f).coerceAtMost(28f)
            labels.indices.forEach { i ->
                val center = groupWidth * i + groupWidth / 2
                val h1 = (first.getOrNull(i)?.cents ?: 0L).toFloat() / max * size.height
                val h2 = (second.getOrNull(i)?.cents ?: 0L).toFloat() / max * size.height
                drawRoundRect(firstColor, Offset(center - barWidth - 2f, size.height - h1), Size(barWidth, h1), CornerRadius(6f, 6f))
                drawRoundRect(secondColor, Offset(center + 2f, size.height - h2), Size(barWidth, h2), CornerRadius(6f, 6f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = FinanceColors.MediumGray, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Barra dividida en dos partes (fijos vs ocasionales, necesidades vs ocio). */
@Composable
fun SplitBar(
    left: Pair<String, Money>,
    right: Pair<String, Money>,
    format: (Money) -> String,
    leftColor: Color = FinanceColors.DarkBlue,
    rightColor: Color = Color(0xFF93C5FD),
) {
    val total = (left.second.cents + right.second.cents).coerceAtLeast(1L)
    val share = (left.second.cents * 100 / total).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(8.dp)).background(rightColor)) {
            if (share > 0) Box(Modifier.fillMaxWidth(share / 100f).height(14.dp).background(leftColor))
        }
        Row {
            Text("${left.first}: ${format(left.second)} ($share %)", style = MaterialTheme.typography.bodySmall, color = FinanceColors.DarkBlue, modifier = Modifier.weight(1f))
            Text("${right.first}: ${format(right.second)} (${100 - share} %)", style = MaterialTheme.typography.bodySmall, color = FinanceColors.MediumGray)
        }
    }
}
