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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healtapp.core.common.BmiHelper
import com.example.healtapp.data.preferences.WeightEntry
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.bmiCategoryColor
import com.example.healtapp.core.ui.theme.brandingGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class BmiPoint(
    val date: String,
    val bmi: Float,
)

@Composable
fun ProfileBodyStatsCard(
    heightCm: String,
    weightKg: String,
    weightHistory: List<WeightEntry> = emptyList(),
    weightWeeklyReminder: String? = null,
    modifier: Modifier = Modifier,
) {
    val height = BmiHelper.parseMetric(heightCm)
    val weight = BmiHelper.parseMetric(weightKg)
    val bmi = BmiHelper.calculate(height, weight)
    val range = height?.let { BmiHelper.healthyWeightRangeKg(it) }
    val recentWeights = weightHistory.takeLast(8)
    val bmiPoints = buildBmiPoints(height, recentWeights, bmi?.value)
    val showWeightTrend = recentWeights.size >= 2

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppCard {
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    VerticalBmiChart(
                        points = bmiPoints,
                        currentCategory = bmi?.category,
                        modifier = Modifier.width(96.dp),
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MetricLine(label = "Рост", value = if (heightCm.isBlank()) "—" else "$heightCm см")
                        MetricLine(label = "Вес", value = if (weightKg.isBlank()) "—" else "$weightKg кг")
                        if (bmi != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = "ИМТ ${BmiHelper.formatValue(bmi.value)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = bmi.labelRu,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Text(
                                text = bmi.hintRu,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            range?.let { (lo, hi) ->
                                Text(
                                    text = "Норма веса: ${"%.0f".format(lo)}–${"%.0f".format(hi)} кг",
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Text(
                                text = "Укажите рост и вес в «Основных данных».",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (showWeightTrend) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(
                        title = "Изменение веса",
                        subtitle = "После обновления данных в профиле",
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

private fun buildBmiPoints(
    heightCm: Float?,
    weightEntries: List<WeightEntry>,
    currentBmi: Float?,
): List<BmiPoint> {
    if (heightCm == null || heightCm <= 0f) return emptyList()
    val fromHistory = weightEntries.mapNotNull { entry ->
        BmiHelper.calculate(heightCm, entry.weightKg)?.value?.let { value ->
            BmiPoint(entry.date, value)
        }
    }
    if (fromHistory.isNotEmpty()) return fromHistory
    return currentBmi?.let { listOf(BmiPoint(LocalDate.now().toString(), it)) } ?: emptyList()
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

private const val HEALTHY_BMI_LO = 18.5f
private const val HEALTHY_BMI_HI = 24.9f

private data class BmiChartRange(
    val min: Float,
    val max: Float,
) {
    val span: Float get() = (max - min).coerceAtLeast(1f)

    fun fraction(value: Float): Float = ((value - min) / span).coerceIn(0f, 1f)
}

private fun computeBmiChartRange(values: List<Float>): BmiChartRange {
    val dataMin = values.minOrNull() ?: HEALTHY_BMI_LO
    val dataMax = values.maxOrNull() ?: HEALTHY_BMI_HI
    val margin = 2.5f
    var min = minOf(dataMin, HEALTHY_BMI_LO) - margin
    var max = maxOf(dataMax, HEALTHY_BMI_HI) + margin
    if (max - min < 12f) {
        val center = (max + min) / 2f
        min = center - 6f
        max = center + 6f
    }
    return BmiChartRange(
        min = min.coerceAtLeast(12f),
        max = max.coerceAtMost(42f),
    )
}

@Composable
private fun VerticalBmiChart(
    points: List<BmiPoint>,
    currentCategory: BmiHelper.Category?,
    modifier: Modifier = Modifier,
) {
    val values = points.map { it.bmi }
    if (values.isEmpty()) {
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

    val chartRange = computeBmiChartRange(values)
    val currentBmi = values.last()
    val gradient = brandingGradient()
    val markerColor = when {
        currentCategory != null -> bmiCategoryColor(currentCategory)
        else -> gradient.first()
    }
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
    val axisLabels = buildBmiAxisLabels(chartRange)

    Surface(
        modifier = modifier.heightIn(min = 148.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Column(
            modifier = Modifier
                .height(148.dp)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = markerColor.copy(alpha = 0.18f),
            ) {
                Text(
                    text = BmiHelper.formatValue(currentBmi),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = markerColor,
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .width(30.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    axisLabels.forEach { label ->
                        Text(
                            text = label,
                            style = labelStyle,
                            color = axisColor,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 4.dp),
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val trackWidth = size.width * 0.42f
                        val trackLeft = (size.width - trackWidth) / 2f
                        val chartHeight = size.height

                        fun yFor(value: Float): Float =
                            chartHeight * (1f - chartRange.fraction(value))

                        drawRoundRect(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f),
                            topLeft = Offset(trackLeft, 0f),
                            size = Size(trackWidth, chartHeight),
                            cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f),
                        )

                        val healthyTop = yFor(HEALTHY_BMI_HI)
                        val healthyBottom = yFor(HEALTHY_BMI_LO)
                        drawRoundRect(
                            color = androidx.compose.ui.graphics.Color(0xFF7AD9B6).copy(alpha = 0.45f),
                            topLeft = Offset(trackLeft, healthyTop),
                            size = Size(trackWidth, (healthyBottom - healthyTop).coerceAtLeast(6f)),
                            cornerRadius = CornerRadius(8f, 8f),
                        )

                        listOf(HEALTHY_BMI_LO, HEALTHY_BMI_HI).forEach { tick ->
                            val y = yFor(tick)
                            drawLine(
                                color = axisColor.copy(alpha = 0.35f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f,
                            )
                        }

                        values.forEachIndexed { index, value ->
                            val y = yFor(value)
                            val isLast = index == values.lastIndex
                            drawCircle(
                                color = if (isLast) markerColor else gradient.last().copy(alpha = 0.8f),
                                radius = if (isLast) 6.dp.toPx() else 4.dp.toPx(),
                                center = Offset(size.width / 2f, y),
                            )
                            if (isLast) {
                                drawCircle(
                                    color = androidx.compose.ui.graphics.Color.White,
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(size.width / 2f, y),
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "ИМТ",
                style = labelStyle,
                color = axisColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun buildBmiAxisLabels(range: BmiChartRange): List<String> {
    return listOf(
        "%.0f".format(range.max),
        "%.0f".format(HEALTHY_BMI_HI),
        "%.0f".format(HEALTHY_BMI_LO),
        "%.0f".format(range.min),
    )
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
