package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.healtapp.core.common.BmiHelper
import com.example.healtapp.data.preferences.WeightEntry
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.bmiCategoryColor
import com.example.healtapp.core.ui.theme.bmiScaleGradient
import com.example.healtapp.core.ui.theme.brandingGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileBodyStatsCard(
    heightCm: String,
    weightKg: String,
    weightHistory: List<WeightEntry> = emptyList(),
    weightWeeklyReminder: String? = null,
    modifier: Modifier = Modifier,
) {
    val height = heightCm.toFloatOrNull()
    val weight = weightKg.toFloatOrNull()
    val bmi = BmiHelper.calculate(height, weight)
    val range = height?.let { BmiHelper.healthyWeightRangeKg(it) }
    val recentWeights = weightHistory.takeLast(8)
    val distinctWeights = recentWeights.map { it.weightKg }.distinct()
    val showTrend = distinctWeights.size >= 2

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(
                title = "Показатели тела",
                subtitle = "Рост, вес и ИМТ",
            )

            weightWeeklyReminder?.let { reminder ->
                Text(
                    text = reminder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VerticalWeightBar(
                    entries = recentWeights,
                    healthyRange = range,
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight(),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricLine(label = "Рост", value = if (heightCm.isBlank()) "—" else "$heightCm см")
                    MetricLine(label = "Вес", value = if (weightKg.isBlank()) "—" else "$weightKg кг")
                    if (bmi != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        ) {
                            Text(
                                text = "ИМТ ${BmiHelper.formatValue(bmi.value)} · ${bmi.labelRu}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = bmi.hintRu,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        range?.let { (lo, hi) ->
                            Text(
                                text = "Норма: ${"%.0f".format(lo)}–${"%.0f".format(hi)} кг",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        BmiScaleBar(bmi = bmi.value, category = bmi.category)
                    } else {
                        Text(
                            text = "Укажите рост и вес в «Основных данных».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (showTrend) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Изменение веса",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WeightTrendChart(
                        entries = recentWeights,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VerticalWeightBar(
    entries: List<WeightEntry>,
    healthyRange: Pair<Float, Float>?,
    modifier: Modifier = Modifier,
) {
    val weights = entries.map { it.weightKg }
    if (weights.isEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val minW = (healthyRange?.first ?: weights.minOrNull()!!) - 2f
    val maxW = (healthyRange?.second ?: weights.maxOrNull()!!) + 2f
    val span = (maxW - minW).coerceAtLeast(1f)
    val gradient = brandingGradient()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    gradient.map { it.copy(alpha = 0.18f) },
                ),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
            val barWidth = size.width * 0.35f
            val barLeft = (size.width - barWidth) / 2f
            drawRoundRect(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f),
                topLeft = Offset(barLeft, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
            healthyRange?.let { (lo, hi) ->
                val top = size.height * (1f - ((hi - minW) / span).coerceIn(0f, 1f))
                val bottom = size.height * (1f - ((lo - minW) / span).coerceIn(0f, 1f))
                drawRoundRect(
                    color = androidx.compose.ui.graphics.Color(0xFF7AD9B6).copy(alpha = 0.35f),
                    topLeft = Offset(barLeft, top),
                    size = Size(barWidth, (bottom - top).coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(8f, 8f),
                )
            }
            weights.forEachIndexed { index, w ->
                val fraction = ((w - minW) / span).coerceIn(0f, 1f)
                val y = size.height * (1f - fraction)
                val isLast = index == weights.lastIndex
                drawCircle(
                    color = if (isLast) gradient.first() else gradient.last().copy(alpha = 0.85f),
                    radius = if (isLast) 7.dp.toPx() else 5.dp.toPx(),
                    center = Offset(size.width / 2f, y),
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = weights.lastOrNull()?.let { "%.1f".format(it) } ?: "—",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BmiScaleBar(
    bmi: Float,
    category: BmiHelper.Category,
) {
    val fraction = ((bmi - 15f) / (35f - 15f)).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.horizontalGradient(bmiScaleGradient())),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(bmiCategoryColor(category).copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun WeightTrendChart(
    entries: List<WeightEntry>,
    modifier: Modifier = Modifier,
) {
    if (entries.size < 2) return

    val weights = entries.map { it.weightKg }
    val minW = weights.minOrNull() ?: return
    val maxW = weights.maxOrNull() ?: return
    val midW = (minW + maxW) / 2f
    val range = (maxW - minW).coerceAtLeast(0.5f)
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM", Locale("ru", "RU"))

    fun formatDate(iso: String): String =
        runCatching { LocalDate.parse(iso).format(dateFormatter) }.getOrDefault(iso.takeLast(5))

    val xLabels = when {
        entries.size <= 3 -> entries.map { formatDate(it.date) }
        else -> listOf(
            formatDate(entries.first().date),
            formatDate(entries[entries.size / 2].date),
            formatDate(entries.last().date),
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Вес, кг",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .width(34.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("%.1f".format(maxW), style = labelStyle, color = axisColor, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Text("%.1f".format(midW), style = labelStyle, color = axisColor, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Text("%.1f".format(minW), style = labelStyle, color = axisColor, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 4.dp),
            ) {
                val leftPad = 4f
                val bottomPad = 8f
                val chartW = size.width - leftPad
                val chartH = size.height - bottomPad

                // Оси
                drawLine(
                    color = axisColor,
                    start = Offset(leftPad, 0f),
                    end = Offset(leftPad, chartH),
                    strokeWidth = 1.5f,
                )
                drawLine(
                    color = axisColor,
                    start = Offset(leftPad, chartH),
                    end = Offset(size.width, chartH),
                    strokeWidth = 1.5f,
                )

                // Горизонтальные направляющие
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val y = chartH * (1f - frac)
                    drawLine(
                        color = axisColor.copy(alpha = 0.25f),
                        start = Offset(leftPad, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }

                val stepX = chartW / (weights.size - 1).coerceAtLeast(1)
                val path = Path()
                weights.forEachIndexed { index, w ->
                    val x = leftPad + index * stepX
                    val y = chartH - ((w - minW) / range) * chartH
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y),
                    )
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 38.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            xLabels.forEach { label ->
                Text(label, style = labelStyle, color = axisColor)
            }
        }
        Text(
            text = "Дата",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 38.dp),
            textAlign = TextAlign.Center,
        )
    }
}
