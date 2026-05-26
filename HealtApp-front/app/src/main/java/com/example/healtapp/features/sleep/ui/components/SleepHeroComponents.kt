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
import androidx.compose.ui.unit.sp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ShimmerBox
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.iconTintColor
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
    todaySleepHours: Float,
    averageSleepHours: Float,
    targetSleepHours: Float,
    sleepQualityAverage: Int,
    consistencyPercent: Int,
    onEditGoalInProfile: () -> Unit,
) {
    val progressSource = todaySleepHours
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
                        tint = iconTintColor(),
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Сон сегодня",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (todaySleepHours > 0f) {
                            "${SleepHelper.formatHours(todaySleepHours)} ч (пробуждение сегодня)"
                        } else {
                            "Нет записи за сегодня — добавьте ниже"
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
                        text = if (todaySleepHours > 0f) {
                            SleepHelper.formatHours(todaySleepHours) + " ч"
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = buildString {
                            append("Среднее за неделю: ${SleepHelper.formatHours(averageSleepHours)} ч")
                            if (goalReached) append(" · отлично!")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Цель: ${SleepHelper.formatHours(targetSleepHours)} ч",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (todaySleepHours > 0f && sleepQualityAverage > 0) {
                        Text(
                            text = "Качество $sleepQualityAverage/100 · стабильность $consistencyPercent%",
                            style = MaterialTheme.typography.labelLarge,
                            color = contentPrimaryColor(),
                        )
                    }
                }
                SleepProgressRing(progress = animatedProgress, percent = percent)
            }

            TextButton(onClick = onEditGoalInProfile) {
                Icon(
                    Icons.Outlined.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Изменить цель сна в профиле",
                    modifier = Modifier.padding(start = 4.dp),
                )
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
            val valueSlotHeight = 18.dp
            val barAreaHeight = 120.dp
            val labelSlotHeight = 32.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(valueSlotHeight + barAreaHeight + labelSlotHeight),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(valueSlotHeight),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    days.forEach { day ->
                        val isToday = day.dateKey == java.time.LocalDate.now().toString()
                        Text(
                            text = if (day.hours > 0f) SleepHelper.formatHours(day.hours) else "",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) {
                                contentPrimaryColor()
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barAreaHeight),
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
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        days.forEach { day ->
                            val barFraction = day.hours / maxHours
                            val isToday = day.dateKey == java.time.LocalDate.now().toString()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .fillMaxWidth(0.65f)
                                        .height((barAreaHeight * barFraction.coerceIn(0.04f, 1f)).coerceAtLeast(4.dp))
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                if (isToday) todayBarGradient else defaultBarGradient,
                                            ),
                                        ),
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(labelSlotHeight),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    days.forEach { day ->
                        val isToday = day.dateKey == java.time.LocalDate.now().toString()
                        Text(
                            text = day.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (isToday) {
                                contentPrimaryColor()
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
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
