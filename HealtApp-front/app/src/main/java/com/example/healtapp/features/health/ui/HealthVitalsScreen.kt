package com.example.healtapp.features.health.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.data.network.dto.health.HealthSampleDto
import com.example.healtapp.features.health.presentation.HealthVitalsViewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun HealthVitalsScreen(
    onBack: () -> Unit,
) {
    val viewModel: HealthVitalsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val byMetric = remember(uiState.samples) {
        uiState.samples.groupBy { it.metric }.mapValues { (_, v) ->
            v.sortedBy { it.recorded_at }
        }
    }

    AppScreen(
        title = "Показатели",
        subtitle = "Health Connect и импорт — шкала времени и зоны нормы",
        headerIcon = Icons.Filled.MonitorHeart,
        onNavigateBack = onBack,
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.error?.let { err ->
                AppMessageBanner(text = err, type = AppMessageType.Error)
            }
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppButton(
                    text = "7 дней",
                    onClick = { viewModel.setChartPeriod(7) },
                    enabled = !uiState.isLoading && uiState.chartDays != 7,
                    isSecondary = uiState.chartDays != 7,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = "30 дней",
                    onClick = { viewModel.setChartPeriod(30) },
                    enabled = !uiState.isLoading && uiState.chartDays != 30,
                    isSecondary = uiState.chartDays != 30,
                    modifier = Modifier.weight(1f),
                )
            }

            AppButton(
                text = "Обновить",
                enabled = !uiState.isLoading,
                onClick = viewModel::refresh,
            )

            val order = listOf(
                "weight_kg" to "Вес, кг",
                "heart_rate_bpm" to "Пульс",
                "blood_pressure_mmhg" to "Давление (сист./диаст.)",
                "spo2_percent" to "Кислород в крови, %",
                "blood_glucose_mmol_l" to "Глюкоза, ммоль/л",
                "vo2_max" to "VO₂ max",
                "power_w" to "Мощность, Вт",
                "speed_m_s" to "Скорость, м/с",
                "distance_m" to "Расстояние, м",
                "active_calories_kcal" to "Активные калории, ккал",
                "total_calories_kcal" to "Калории всего, ккал",
                "body_fat_percent" to "% жира",
                "bmr_kcal" to "Базовый обмен, ккал/сут",
                "height_cm" to "Рост, см",
            )

            for ((metric, title) in order) {
                val series = byMetric[metric] ?: emptyList()
                if (series.isEmpty()) continue
                SectionHeader(title = title, subtitle = "${series.size} записей · ${uiState.chartDays} дн.")
                AppCard {
                    Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeSeriesChart(
                            samples = series,
                            dualValues = metric == "blood_pressure_mmhg",
                            chartDays = uiState.chartDays,
                            metric = metric,
                            secondaryLineColor = MaterialTheme.colorScheme.tertiary,
                        )
                        ZoneLegend(metric = metric)
                        val last = series.lastOrNull()
                        if (last != null) {
                            Text(
                                text = formatSampleSummary(metric, last),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            SectionHeader(
                title = "Тренировки",
                subtitle = if (uiState.activities.isEmpty()) "Нет записей за период" else "${uiState.activities.size} за ${uiState.chartDays} дн.",
            )
            if (uiState.activities.isNotEmpty()) {
                for (act in uiState.activities.take(25)) {
                    ActivityRow(act)
                }
            }

            if (!uiState.isLoading && uiState.samples.isEmpty()) {
                AppCard {
                    Text(
                        text = "Пока нет выборок. Синхронизируйте Health Connect в разделе «Интеграции».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(act: ActivityDto) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = act.activity_type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            val start = runCatching {
                OffsetDateTime.parse(act.start_time).format(
                    DateTimeFormatter.ofPattern("d MMM HH:mm", Locale("ru", "RU")),
                )
            }.getOrDefault(act.start_time)
            Text(
                text = "$start · ${act.duration_minutes} мин",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val bits = buildList {
                act.calories_burned?.let { add("~${it.toInt()} ккал") }
                act.steps?.let { add("$it шагов") }
                act.distance_km?.let { add("%.1f км".format(it)) }
                act.avg_heart_rate?.let { add("пульс ~$it") }
            }
            if (bits.isNotEmpty()) {
                Text(
                    text = bits.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ZoneLegend(metric: String) {
    val lines = when (metric) {
        "heart_rate_bpm" -> listOf("Ниже 60 — спокойнее среднего", "60–99 — типичный диапазон", "100+ — выше среднего")
        "spo2_percent" -> listOf("До 94% — внимание", "95–100% — обычно в норме")
        "weight_kg" -> listOf("Зелёный коридор — ±~2 кг от среднего за период")
        else -> emptyList()
    }
    if (lines.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.forEach { line ->
            Text(
                text = "· $line",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatSampleSummary(metric: String, s: HealthSampleDto): String {
    val t = runCatching {
        OffsetDateTime.parse(s.recorded_at).format(
            DateTimeFormatter.ofPattern("d MMM HH:mm", Locale("ru", "RU")),
        )
    }.getOrDefault(s.recorded_at)
    return when (metric) {
        "blood_pressure_mmhg" -> "Последнее: ${s.value1?.toInt() ?: "—"}/${s.value2?.toInt() ?: "—"} · $t"
        else -> "Последнее: ${s.value1?.let { "%.1f".format(it) } ?: "—"} · $t"
    }
}

private data class YBand(val from: Float, val to: Float, val color: Color)

private fun bandsForMetric(metric: String, values: List<Float>): List<YBand> {
    if (values.isEmpty()) return emptyList()
    return when (metric) {
        "heart_rate_bpm" -> listOf(
            YBand(0f, 59f, Color(0xFF64B5F6).copy(alpha = 0.32f)),
            YBand(60f, 99f, Color(0xFF66BB6A).copy(alpha = 0.32f)),
            YBand(100f, 220f, Color(0xFFFFB74D).copy(alpha = 0.32f)),
        )
        "spo2_percent" -> listOf(
            YBand(0f, 94f, Color(0xFFFFB74D).copy(alpha = 0.32f)),
            YBand(95f, 100f, Color(0xFF66BB6A).copy(alpha = 0.32f)),
        )
        "weight_kg" -> {
            val mean = values.average().toFloat()
            val w = 2f
            listOf(
                YBand(mean - 15f, mean - w, Color(0xFF64B5F6).copy(alpha = 0.22f)),
                YBand(mean - w, mean + w, Color(0xFF66BB6A).copy(alpha = 0.26f)),
                YBand(mean + w, mean + 15f, Color(0xFFFFB74D).copy(alpha = 0.22f)),
            )
        }
        else -> emptyList()
    }
}

private fun parseInstant(iso: String): Instant? =
    runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()

@Composable
private fun TimeSeriesChart(
    samples: List<HealthSampleDto>,
    dualValues: Boolean,
    chartDays: Int,
    metric: String,
    secondaryLineColor: Color,
) {
    val zone = ZoneId.systemDefault()
    val now = remember(samples, chartDays) { Instant.now() }
    val windowStart = remember(now, chartDays) {
        now.minus(chartDays.toLong(), ChronoUnit.DAYS)
    }

    val timedV1 = remember(samples, windowStart, now) {
        samples.mapNotNull { s ->
            val t = parseInstant(s.recorded_at) ?: return@mapNotNull null
            val v = s.value1?.toFloat() ?: return@mapNotNull null
            if (t.isBefore(windowStart)) return@mapNotNull null
            Triple(t, v, s.value2?.toFloat())
        }.sortedBy { it.first }
    }

    val v1 = timedV1.map { it.second }
    val v2 = if (dualValues) timedV1.mapNotNull { it.third } else emptyList()
    val all = v1 + v2
    if (all.isEmpty()) return

    val minV = all.minOrNull() ?: 0f
    val maxV = all.maxOrNull() ?: 1f
    val padFrac = 0.08f
    val rawSpan = max(maxV - minV, 1e-4f)
    val yMin = minV - rawSpan * padFrac
    val yMax = maxV + rawSpan * padFrac
    val span = max(yMax - yMin, 1e-4f)

    val rangeEnd = maxOf(now, timedV1.maxOfOrNull { it.first } ?: now)
    val rangeStart = minOf(windowStart, timedV1.minOfOrNull { it.first } ?: windowStart)
    val tSpan = max(ChronoUnit.MILLIS.between(rangeStart, rangeEnd), 60_000L).toFloat()

    val bands = remember(metric, v1) { bandsForMetric(metric, v1) }
    val lineGradient = brandingGradient()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp),
    ) {
        val w = size.width
        val h = size.height
        val pad = 10.dp.toPx()
        val innerH = h - 2 * pad
        val innerW = w - 2 * pad

        fun xAt(t: Instant): Float {
            val u = ChronoUnit.MILLIS.between(rangeStart, t).toFloat() / tSpan
            return pad + innerW * u.coerceIn(0f, 1f)
        }

        fun yAt(v: Float): Float {
            return h - pad - (v - yMin) / span * innerH
        }

        for (band in bands) {
            val overlapLow = max(band.from, yMin)
            val overlapHigh = min(band.to, yMax)
            if (overlapLow >= overlapHigh) continue
            val yTop = min(yAt(overlapLow), yAt(overlapHigh))
            val yBot = max(yAt(overlapLow), yAt(overlapHigh))
            drawRect(
                color = band.color,
                topLeft = Offset(pad, yTop),
                size = Size(innerW, yBot - yTop),
            )
        }

        val gridCount = min(chartDays, 6)
        for (i in 0..gridCount) {
            val gx = pad + innerW * i / gridCount
            drawLine(
                color = Color.Gray.copy(alpha = 0.15f),
                start = Offset(gx, pad),
                end = Offset(gx, h - pad),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val path = Path()
        timedV1.forEachIndexed { i, (t, v, _) ->
            val x = xAt(t)
            val y = yAt(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(lineGradient),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )

        if (dualValues) {
            val path2 = Path()
            var started2 = false
            timedV1.forEach { (t, _, v2) ->
                if (v2 == null) return@forEach
                val x = xAt(t)
                val y = yAt(v2)
                if (!started2) {
                    path2.moveTo(x, y)
                    started2 = true
                } else {
                    path2.lineTo(x, y)
                }
            }
            if (started2) {
                drawPath(
                    path = path2,
                    color = secondaryLineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }

    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("ru", "RU"))
    val startLabel = remember(rangeStart, zone) {
        OffsetDateTime.ofInstant(rangeStart, zone).format(fmt)
    }
    val endLabel = remember(rangeEnd, zone) {
        OffsetDateTime.ofInstant(rangeEnd, zone).format(fmt)
    }
    val midInstant = remember(rangeStart, rangeEnd) {
        rangeStart.plusMillis((ChronoUnit.MILLIS.between(rangeStart, rangeEnd) / 2).coerceAtLeast(0))
    }
    val midLabel = remember(midInstant, zone) {
        OffsetDateTime.ofInstant(midInstant, zone).format(fmt)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = midLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
