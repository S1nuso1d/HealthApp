package com.example.healtapp.features.sleep.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ShimmerBox
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.cardHeaderGradient
import com.example.healtapp.core.ui.theme.chartBarFillGradientSoft
import com.example.healtapp.core.ui.theme.chartBarGuideColor
import com.example.healtapp.core.ui.theme.chartSweepGradient
import com.example.healtapp.core.ui.theme.themedCardLavender
import com.example.healtapp.features.sleep.presentation.DaySleep
import com.example.healtapp.features.sleep.presentation.SleepHelper
import kotlin.math.roundToInt

@Composable
fun SleepHeroCard(
    lastNightHours: Float,
    averageSleepHours: Float,
    targetSleepHours: Float,
    sleepQualityAverage: Int,
    consistencyPercent: Int,
    onEditGoalInProfile: () -> Unit,
) {
    val progressSource = if (lastNightHours > 0f) lastNightHours else averageSleepHours
    val progress = if (targetSleepHours > 0f) {
        (progressSource / targetSleepHours).coerceIn(0f, 1.15f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = AppMotion.tweenMedium(),
        label = "sleep_ring",
    )
    val percent = if (targetSleepHours > 0f) {
        ((progressSource / targetSleepHours) * 100f).roundToInt().coerceIn(0, 999)
    } else {
        0
    }
    val goalReached = progressSource >= targetSleepHours && targetSleepHours > 0f
    val iconGrad = cardHeaderGradient(themedCardLavender(), 0.45f)

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(iconGrad)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Последняя ночь",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (lastNightHours > 0f) {
                            "${SleepHelper.formatHours(lastNightHours)} ч сна"
                        } else {
                            "Нет записи — добавьте ниже"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (lastNightHours > 0f) {
                            SleepHelper.formatHours(lastNightHours) + " ч"
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = buildString {
                            append("Среднее за неделю: ${SleepHelper.formatHours(averageSleepHours)} ч")
                            append(" · цель ${SleepHelper.formatHours(targetSleepHours)} ч")
                            if (goalReached) append(" · отлично!")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Качество $sleepQualityAverage/100 · стабильность $consistencyPercent%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                SleepProgressRing(progress = animatedProgress, percent = percent)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onEditGoalInProfile) {
                    Icon(
                        Icons.Outlined.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Цель: ${SleepHelper.formatHours(targetSleepHours)} ч · изменить",
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepProgressRing(
    progress: Float,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val brush = Brush.sweepGradient(chartSweepGradient())
    val trackColor = themedCardLavender().copy(alpha = 0.9f)
    Box(
        modifier = modifier.size(88.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val pad = stroke / 2f
            val arc = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(pad, pad)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arc,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = brush,
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arc,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun WeeklySleepBarChart(
    days: List<DaySleep>,
    goalHours: Float,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    val maxHours = (days.maxOf { it.hours }.coerceAtLeast(goalHours)).coerceAtLeast(0.5f)
    val goalLineColor = chartBarGuideColor()
    val todayBarGradient = cardHeaderGradient(themedCardLavender(), 1f)
    val defaultBarGradient = chartBarFillGradientSoft(themedCardLavender())

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Динамика за неделю",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Пунктир — цель ${SleepHelper.formatHours(goalHours)} ч",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val goalY = size.height * (1f - goalHours / maxHours).coerceIn(0f, 1f)
                    drawLine(
                        color = goalLineColor,
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    days.forEach { day ->
                        val barFraction = day.hours / maxHours
                        val isToday = day.label == "Сегодня"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = if (day.hours > 0f) SleepHelper.formatHours(day.hours) else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp, vertical = 4.dp)
                                    .height((118 * barFraction.coerceIn(0.04f, 1f)).dp)
                                    .fillMaxWidth(0.6f)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            if (isToday) todayBarGradient else defaultBarGradient,
                                        ),
                                    ),
                            )
                            Text(
                                text = day.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleepScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(168.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(156.dp))
    }
}
