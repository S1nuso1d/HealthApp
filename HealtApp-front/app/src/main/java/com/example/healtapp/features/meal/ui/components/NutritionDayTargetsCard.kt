package com.example.healtapp.features.meal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppCard
import kotlin.math.roundToInt

@Composable
fun NutritionDayTargetsCard(
    caloriesConsumed: Int,
    caloriesTarget: Int,
    proteinConsumed: Float,
    proteinTarget: Float?,
    fatConsumed: Float,
    fatTarget: Float?,
    carbsConsumed: Float,
    carbsTarget: Float?,
    onEditTargets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ориентиры на день",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Съедено сегодня и ваши цели",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEditTargets) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Изменить ориентиры")
                }
            }

            MacroProgressLine(
                label = "Калории",
                consumed = caloriesConsumed.toFloat(),
                target = caloriesTarget.toFloat().takeIf { it > 0f },
                unit = "ккал",
                formatWhole = true,
            )
            MacroProgressLine(
                label = "Белки",
                consumed = proteinConsumed,
                target = proteinTarget,
                unit = "г",
            )
            MacroProgressLine(
                label = "Жиры",
                consumed = fatConsumed,
                target = fatTarget,
                unit = "г",
            )
            MacroProgressLine(
                label = "Углеводы",
                consumed = carbsConsumed,
                target = carbsTarget,
                unit = "г",
            )
        }
    }
}

@Composable
private fun MacroProgressLine(
    label: String,
    consumed: Float,
    target: Float?,
    unit: String,
    formatWhole: Boolean = false,
) {
    val targetVal = target?.takeIf { it > 0f }
    val progress = if (targetVal != null) {
        (consumed / targetVal).coerceIn(0f, 1.15f)
    } else {
        0f
    }
    val animated by animateFloatAsState(
        targetValue = progress.coerceAtMost(1f),
        animationSpec = AppMotion.tweenMedium(),
        label = "macro_$label",
    )

    val consumedText = if (formatWhole) {
        consumed.roundToInt().toString()
    } else {
        "%.0f".format(consumed)
    }
    val targetText = targetVal?.let {
        if (formatWhole) it.roundToInt().toString() else "%.0f".format(it)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (targetText != null) {
                    "$consumedText из $targetText $unit"
                } else {
                    "$consumedText $unit · цель не задана"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (targetVal != null) {
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
