package com.example.healtapp.features.dashboard.ui.alt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AltPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
fun AltSectionLabel(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title.uppercase(Locale("ru")),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AltHeroBlock(
    greeting: String,
    subtitle: String,
    healthScore: Int?,
    streak: Int,
    onRevertLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateLabel = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru", "RU")))
        .replaceFirstChar { it.uppercase() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRevertLayout) {
                Text(
                    text = "Классика",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.secondary)
                .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(6.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    if (streak > 0) {
                        Text(
                            text = "Серия $streak дн.",
                            style = MaterialTheme.typography.labelLarge,
                            color = DashboardAltColors.Terracotta.copy(alpha = 0.95f),
                        )
                    }
                }
                healthScore?.let { score ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = score.toString(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp,
                            color = Color.White,
                        )
                        Text(
                            text = "ИНДЕКС",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AltMetricsBento(
    sleepHours: Float,
    sleepTargetHours: Float,
    waterMl: Int,
    waterTargetMl: Int,
    caloriesToday: Int,
    caloriesTarget: Int,
    stepsToday: Int,
    stepsGoal: Int,
    onOpenSleep: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenActivity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sleepFmt = DecimalFormat("#.#").format(sleepHours).replace('.', ',')
    val sleepTargetFmt = DecimalFormat("#.#").format(sleepTargetHours).replace('.', ',')
    val waterStr = "%,d".format(waterMl).replace(',', '\u00A0')
    val waterGoalStr = "%,d".format(waterTargetMl).replace(',', '\u00A0')
    val stepsStr = "%,d".format(stepsToday).replace(',', '\u00A0')

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AltMetricCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Bedtime,
                label = "Сон",
                value = "$sleepFmt ч",
                hint = "из $sleepTargetFmt",
                progress = if (sleepTargetHours > 0f) sleepHours / sleepTargetHours else 0f,
                onClick = onOpenSleep,
            )
            AltMetricCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.WaterDrop,
                label = "Вода",
                value = "$waterStr мл",
                hint = "цель $waterGoalStr",
                progress = if (waterTargetMl > 0) waterMl.toFloat() / waterTargetMl else 0f,
                onClick = onOpenHydration,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AltMetricCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Restaurant,
                label = "Питание",
                value = "$caloriesToday",
                hint = "из $caloriesTarget ккал",
                progress = if (caloriesTarget > 0) caloriesToday.toFloat() / caloriesTarget else 0f,
                onClick = onOpenNutrition,
            )
            AltMetricCell(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = "Шаги",
                value = stepsStr,
                hint = "цель $stepsGoal",
                progress = if (stepsGoal > 0) stepsToday.toFloat() / stepsGoal else 0f,
                onClick = onOpenActivity,
            )
        }
    }
}

@Composable
private fun AltMetricCell(
    icon: ImageVector,
    label: String,
    value: String,
    hint: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = label.uppercase(Locale("ru")),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
    }
}

@Composable
fun AltStreakStrip(
    waterStreak: Int,
    stepsStreak: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AltStreakChip(label = "Вода", days = waterStreak, modifier = Modifier.weight(1f))
        AltStreakChip(label = "Шаги", days = stepsStreak, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AltStreakChip(
    label: String,
    days: Int,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
            .border(1.5.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(Locale("ru")),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = "${days}д",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}

@Composable
fun AltInlineBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
