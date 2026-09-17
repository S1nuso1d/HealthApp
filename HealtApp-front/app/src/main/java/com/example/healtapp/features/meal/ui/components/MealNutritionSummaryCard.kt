package com.example.healtapp.features.meal.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ProgressRing
import com.example.healtapp.core.ui.components.progressCelebrateEffect
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.heroContentColor

@Composable
fun MealNutritionSummaryCard(
    caloriesConsumed: Int,
    caloriesTarget: Int,
    proteinConsumed: Float,
    proteinTarget: Float,
    fatConsumed: Float,
    fatTarget: Float,
    carbsConsumed: Float,
    carbsTarget: Float,
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

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(brandingGradient()))
                .progressCelebrateEffect(celebrateToken)
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProgressRing(
                    progress = animatedKcal,
                    text = "$caloriesConsumed",
                    color = heroContentColor(),
                    trackColor = heroContentColor().copy(alpha = 0.28f),
                    textColor = heroContentColor(),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Калории сегодня",
                        style = MaterialTheme.typography.labelLarge,
                        color = heroContentColor().copy(alpha = 0.86f),
                    )
                    Text(
                        if (caloriesTarget > 0) "из $caloriesTarget ккал" else "цель не задана",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = heroContentColor(),
                    )
                    if (caffeine > 0f) {
                        Text(
                            "Кофеин ${"%.0f".format(caffeine)} мг",
                            style = MaterialTheme.typography.bodySmall,
                            color = heroContentColor().copy(alpha = 0.86f),
                        )
                    }
                    targetsHint?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = heroContentColor().copy(alpha = 0.78f),
                        )
                    }
                }
                IconButton(onClick = onEditTargets) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Изменить ориентиры",
                        tint = heroContentColor(),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MacroChip("Белки", proteinConsumed.toInt(), proteinTarget.toInt(), "г", Modifier.weight(1f))
            MacroChip("Жиры", fatConsumed.toInt(), fatTarget.toInt(), "г", Modifier.weight(1f))
            MacroChip("Углеводы", carbsConsumed.toInt(), carbsTarget.toInt(), "г", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MacroChip(
    label: String,
    consumed: Int,
    target: Int,
    unit: String,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "$consumed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentPrimaryColor(),
            )
            Text(
                if (target > 0) "из $target $unit" else unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (target > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((consumed / target.toFloat()).coerceIn(0.04f, 1f))
                            .height(6.dp)
                            .background(Brush.horizontalGradient(brandingGradient()), RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
    }
}
