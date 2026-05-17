package com.example.healtapp.features.activity.ui.components

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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Sync
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
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ShimmerBox
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.cardHeaderGradient
import com.example.healtapp.core.ui.theme.chartBarFillGradient
import com.example.healtapp.core.ui.theme.chartBarFillGradientSoft
import com.example.healtapp.core.ui.theme.chartBarGuideColor
import com.example.healtapp.core.ui.theme.chartSweepGradientWithSurface
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.features.activity.presentation.DaySteps
import kotlin.math.min

fun formatStepsCount(steps: Int): String =
    "%,d".format(steps).replace(',', '\u00A0')

@Composable
fun ActivityStepsHeroCard(
    stepsToday: Int,
    stepsGoal: Int,
    trainingMinutesToday: Int,
    trainingCaloriesToday: Int,
    healthConnectSteps: Int?,
    isSaving: Boolean,
    onSyncHealthConnect: () -> Unit,
    onEditGoalInProfile: () -> Unit,
) {
    val progress = if (stepsGoal > 0) (stepsToday / stepsGoal.toFloat()).coerceIn(0f, 1.15f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = AppMotion.tweenMedium(),
        label = "steps_ring",
    )
    val remaining = (stepsGoal - stepsToday).coerceAtLeast(0)
    val percent = if (stepsGoal > 0) ((stepsToday * 100) / stepsGoal).coerceAtMost(999) else 0
    val goalReached = stepsToday >= stepsGoal && stepsGoal > 0
    val iconGrad = cardHeaderGradient(themedCardBlue(), 0.55f)

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
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Шаги сегодня",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (goalReached) "Цель выполнена" else "Данные из Health Connect",
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = formatStepsCount(stepsToday),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = buildString {
                            append("из ${formatStepsCount(stepsGoal)} · $percent%")
                            if (goalReached) append(" · отлично!")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!goalReached && stepsGoal > 0) {
                        Text(
                            text = "Осталось ${formatStepsCount(remaining)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                StepsProgressRing(progress = animatedProgress, percent = percent)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActivityMiniStat(
                    label = "Тренировки",
                    value = "$trainingMinutesToday мин",
                    modifier = Modifier.weight(1f),
                )
                ActivityMiniStat(
                    label = "Сожжено",
                    value = "$trainingCaloriesToday ккал",
                    modifier = Modifier.weight(1f),
                )
            }

            healthConnectSteps?.let { hc ->
                Text(
                    text = "Health Connect: ${formatStepsCount(hc)} шагов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    Text("  Цель: ${formatStepsCount(stepsGoal)} · изменить")
                }
            }

            AppButton(
                text = if (isSaving) "Синхронизация…" else "Синхронизировать шаги",
                onClick = onSyncHealthConnect,
                enabled = !isSaving,
                isSecondary = true,
            )
        }
    }
}

@Composable
private fun ActivityMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StepsProgressRing(progress: Float, percent: Int) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val brush = Brush.sweepGradient(chartSweepGradientWithSurface(themedCardBlue()))
    Box(modifier = Modifier.size(116.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val arcSize = min(size.width, size.height) - stroke
            val topLeft = Offset((size.width - arcSize) / 2, (size.height - arcSize) / 2)
            val arc = Size(arcSize, arcSize)
            drawArc(
                color = track,
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
fun WeeklyStepsBarChart(
    days: List<DaySteps>,
    goal: Int,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    val maxSteps = (days.maxOf { it.steps }.coerceAtLeast(goal)).coerceAtLeast(1)
    val todayBarGradient = chartBarFillGradient()
    val defaultBarGradient = chartBarFillGradientSoft(themedCardBlue())

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Динамика за неделю",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Пунктир — цель ${formatStepsCount(goal)} шагов",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val goalLineColor = chartBarGuideColor()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val goalY = size.height * (1f - goal.toFloat() / maxSteps).coerceIn(0f, 1f)
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
                        val barFraction = day.steps.toFloat() / maxSteps
                        val isToday = day.label == "Сегодня"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = if (day.steps > 0) formatShortSteps(day.steps) else "",
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

private fun formatShortSteps(steps: Int): String = when {
    steps >= 1000 -> "%.1fк".format(steps / 1000f).replace('.', ',')
    else -> steps.toString()
}

@Composable
fun ActivityStepsSkeleton() {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.45f).height(32.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.65f).height(14.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp))
            }
            ShimmerBox(modifier = Modifier.size(108.dp), shape = RoundedCornerShape(54.dp))
        }
    }
}
