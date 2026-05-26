package com.example.healtapp.features.meal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.components.progressCelebrateEffect

@Composable
fun MealNutritionSummaryCard(
    caloriesConsumed: Int,
    caloriesTarget: Int,
    proteinConsumed: Float,
    proteinTarget: Float?,
    fatConsumed: Float,
    fatTarget: Float?,
    carbsConsumed: Float,
    carbsTarget: Float?,
    caffeine: Float,
    kcalProgress: Float,
    targetsHint: String? = null,
    celebrateToken: Int = 0,
    onEditTargets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedKcal by animateFloatAsState(
        targetValue = kcalProgress.coerceIn(0f, 1f),
        animationSpec = AppMotion.tweenMedium(),
        label = "kcal",
    )

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(
                title = "Сводка",
                subtitle = "Прогресс за сегодня",
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .progressCelebrateEffect(celebrateToken),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MacroProgressRow("Калории", caloriesConsumed, caloriesTarget, "ккал", animatedKcal)
                MacroProgressRow(
                    "Белки",
                    proteinConsumed.toInt(),
                    proteinTarget?.toInt(),
                    "г",
                    progressRatio(proteinConsumed, proteinTarget),
                )
                MacroProgressRow(
                    "Жиры",
                    fatConsumed.toInt(),
                    fatTarget?.toInt(),
                    "г",
                    progressRatio(fatConsumed, fatTarget),
                )
                MacroProgressRow(
                    "Углеводы",
                    carbsConsumed.toInt(),
                    carbsTarget?.toInt(),
                    "г",
                    progressRatio(carbsConsumed, carbsTarget),
                )
            }

            if (caffeine > 0f) {
                Text(
                    text = "Кофеин сегодня: ${"%.0f".format(caffeine)} мг",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                val hintText = targetsHint ?: "Ориентиры КБЖУ"
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = " · изменить",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onEditTargets),
                )
            }
        }
    }
}

@Composable
private fun MacroProgressRow(
    label: String,
    consumed: Int,
    target: Int?,
    unit: String,
    progress: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                text = if (target != null && target > 0) {
                    "$consumed из $target $unit"
                } else {
                    "$consumed $unit"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (target != null && target > 0) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
            )
        }
    }
}

private fun progressRatio(consumed: Float, target: Float?): Float {
    val t = target ?: return 0f
    if (t <= 0f) return 0f
    return (consumed / t).coerceIn(0f, 1.15f)
}
