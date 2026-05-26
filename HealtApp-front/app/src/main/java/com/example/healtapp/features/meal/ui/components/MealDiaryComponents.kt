package com.example.healtapp.features.meal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.animation.AppMotion
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.ShimmerBox
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.chartSweepGradient
import com.example.healtapp.core.ui.theme.sliderAccentColor
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import com.example.healtapp.features.meal.presentation.MealViewModel
import com.example.healtapp.features.meal.util.FatSecretFoodHit
import com.example.healtapp.features.meal.util.FatSecretServingOption
import kotlin.math.min

@Composable
fun MealSummarySkeleton() {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(20.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) {
                        ShimmerBox(modifier = Modifier.width(48.dp).height(36.dp))
                    }
                }
            }
            ShimmerBox(modifier = Modifier.size(120.dp), shape = RoundedCornerShape(60.dp))
        }
    }
}

@Composable
fun MealDailySummaryCard(
    consumed: Int,
    target: Int,
    protein: Float,
    fat: Float,
    carbs: Float,
    caffeine: Float,
    progress: Float,
    targetProteinG: Float?,
    targetFatG: Float?,
    targetCarbsG: Float?,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = AppMotion.tweenMedium(),
        label = "kcal_progress",
    )
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Сводка за день",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Подробнее по БЖУ — в блоке «Ориентиры на день»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (caffeine > 0f) {
                    Text(
                        text = "Кофеин: ${"%.0f".format(caffeine)} мг",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedCalorieRing(consumed = consumed, target = target, progress = animatedProgress)
        }
    }
}

@Composable
private fun AnimatedCalorieRing(consumed: Int, target: Int, progress: Float) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val brush = Brush.sweepGradient(chartSweepGradient())
    Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$consumed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "из $target\nккал",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MealSlotSection(
    titleRu: String,
    meals: List<MealDto>,
    onAdd: () -> Unit,
    onEdit: (MealDto) -> Unit,
    onDelete: (MealDto) -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(text = titleRu, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (meals.isNotEmpty()) {
                        Text(
                            text = "${meals.sumOf { (it.calories ?: 0f).toInt() }} ккал",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                TextButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Добавить")
                }
            }
            AnimatedVisibility(
                visible = meals.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = "Пока пусто — нажмите «Добавить»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            meals.forEach { meal ->
                MealDiaryRowCompact(meal = meal, onEdit = { onEdit(meal) }, onDelete = { onDelete(meal) })
            }
        }
    }
}

@Composable
fun MealServingPicker(
    servings: List<FatSecretServingOption>,
    selectedIndex: Int,
    portionMultiplier: Float,
    onSelectServing: (Int) -> Unit,
    onMultiplierChange: (Float) -> Unit,
) {
    if (servings.isEmpty()) return
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Порция",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                servings.forEachIndexed { index, serving ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { onSelectServing(index) },
                        label = {
                            Text(
                                text = serving.description,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
            Text(
                text = "Количество: ${"%.2f".format(portionMultiplier)}×",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = portionMultiplier,
                onValueChange = onMultiplierChange,
                valueRange = 0.25f..4f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = sliderAccentColor(),
                    activeTrackColor = sliderAccentColor(),
                ),
            )
        }
    }
}

@Composable
fun MealFatSecretHitRow(hit: FatSecretFoodHit, onClick: () -> Unit) {
    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(brandingGradient())),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hit.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                hit.description?.takeIf { it.isNotBlank() }?.let { d ->
                    Text(
                        text = d,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = "Выбрать",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun MealDiaryRowCompact(
    meal: MealDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(brandingGradient())),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val kcal = meal.calories?.let { "${it.toInt()} ккал" } ?: "— ккал"
            Text(text = kcal, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            TextButton(onClick = onEdit, modifier = Modifier.padding(0.dp)) { Text("Изм.") }
            TextButton(onClick = onDelete, modifier = Modifier.padding(0.dp)) {
                Text("Удал.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun MealSavedDishesGrid(
    dishes: List<SavedDishDto>,
    onApply: (SavedDishDto) -> Unit,
    onDelete: (Int) -> Unit,
) {
    if (dishes.isEmpty()) {
        Text(
            text = "Сохраните блюдо из формы — оно появится здесь для быстрого ввода.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        dishes.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { dish ->
                    AppCard(modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = dish.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val kcal = dish.calories?.let { "${it.toInt()} ккал" } ?: "— ккал"
                            Text(
                                text = kcal,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(onClick = { onApply(dish) }) { Text("В форму") }
                                TextButton(onClick = { onDelete(dish.id) }) {
                                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
