package com.example.healtapp.features.meal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.RoundedSectionTabs
import com.example.healtapp.core.ui.components.RoundedTabItem
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.core.ui.theme.subtleFillGradient
import com.example.healtapp.data.network.dto.ai.MealPlanDayDto
import com.example.healtapp.data.network.dto.ai.MealPlanItemDto
import com.example.healtapp.features.meal.presentation.MealPlannerViewModel

@Composable
fun MealPlannerScreen(
    onBack: () -> Unit,
) {
    val viewModel: MealPlannerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeatureScreenShell(
        title = "ИИ план питания",
        subtitle = "Меню и список покупок по профилю",
        icon = Icons.Filled.Restaurant,
        onBack = onBack,
        scrollStateKey = "meal_planner",
        heroActions = {
            IconButton(onClick = viewModel::refreshAiStatus) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Обновить статус ИИ",
                    tint = heroContentColor(),
                )
            }
        },
        heroFooter = if (!uiState.isLoading || uiState.plan != null) {
            {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeatureHeroChip(label = "План: ${uiState.planDays} дн.")
                    uiState.plan?.let { plan ->
                        FeatureHeroChip(label = "Блюд: ${plan.days.sumOf { it.meals.size }}")
                        FeatureHeroChip(label = "Покупок: ${plan.grocery_list.size}")
                    }
                }
            }
        } else {
            null
        },
    ) {
        if (uiState.isLoading && uiState.plan == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Составляем меню…",
                        color = contentSecondaryColor(),
                    )
                    Text(
                        mealPlanLoadingHint(uiState.planDays),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentSecondaryColor(),
                    )
                }
            }
            return@FeatureScreenShell
        }

        uiState.info?.let { FeatureInlineNotice(text = it) }
        uiState.error?.let { FeatureInlineNotice(text = it, isError = true) }

        if (uiState.plan == null) {
            FeatureSectionTitle(
                title = "Генерация меню",
                subtitle = "КБЖУ пересчитываем по каталогу продуктов, не «на глаз»",
            )
            GradientFormPanel {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Персональное меню на несколько дней",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor(),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "ИИ подбирает блюда и порции, а калории и БЖУ считаются по продуктам из каталога.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentSecondaryColor(),
                        textAlign = TextAlign.Center,
                    )
                    PlanDaysSelector(
                        selected = uiState.planDays,
                        onSelect = viewModel::setPlanDays,
                    )
                    AppButton(
                        text = if (uiState.isLoading) {
                            "Составляем…"
                        } else {
                            "Сгенерировать план (${uiState.planDays} дн.)"
                        },
                        onClick = viewModel::generatePlan,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            EmptyStateCard(
                text = "Нажмите «Сгенерировать план», чтобы получить меню и список покупок.",
                icon = Icons.Filled.AutoAwesome,
            )
        } else {
            val plan = uiState.plan ?: return@FeatureScreenShell

            PlanDaysSelector(
                selected = uiState.planDays,
                onSelect = viewModel::setPlanDays,
            )
            AppButton(
                text = if (uiState.isLoading) "Обновляем…" else "Обновить план (${uiState.planDays} дн.)",
                onClick = viewModel::generatePlan,
                enabled = !uiState.isLoading,
                isSecondary = true,
                modifier = Modifier.fillMaxWidth(),
            )

            FeatureSectionTitle(
                title = "Список покупок",
                subtitle = "${plan.grocery_list.size} позиций · ${groceryPeriodLabel(plan.days.size)}",
            )
            if (plan.grocery_list.isEmpty()) {
                MealPlanListCard {
                    Text(
                        "Список покупок пуст",
                        color = contentSecondaryColor(),
                    )
                }
            } else {
                MealPlanListCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                groceryPeriodLabel(plan.days.size),
                                fontWeight = FontWeight.SemiBold,
                                color = contentPrimaryColor(),
                            )
                        }
                        val grouped = plan.grocery_list.groupBy { it.category }
                        grouped.forEach { (category, items) ->
                            Text(
                                category,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        item.name,
                                        color = contentPrimaryColor(),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        item.amount,
                                        color = contentSecondaryColor(),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FeatureSectionTitle(
                title = "Меню",
                subtitle = "${plan.days.size} дней · КБЖУ из каталога",
            )
            plan.days.forEach { day ->
                MealPlanDayCard(day)
            }
        }
    }
}

private fun groceryPeriodLabel(days: Int): String = when (days) {
    3 -> "На 3 дня"
    5 -> "На 5 дней"
    7 -> "На 7 дней"
    else -> "На $days дн."
}

private fun formatIngredientAmount(name: String, grams: Float): String {
    val lower = name.lowercase()
    if (lower.contains("яйц") || lower.contains("омлет") || lower.contains("яичниц")) {
        val count = (grams / 50f).toInt().coerceAtLeast(1)
        return "$count шт"
    }
    if (lower.contains("молоко") || lower.contains("кефир") || lower.contains("йогурт")) {
        return "${grams.toInt()} мл"
    }
    if (grams >= 1000f) {
        val kg = grams / 1000f
        return if (kg >= 10f) {
            "${kg.toInt()} кг"
        } else {
            val text = "%.1f".format(kg).trimEnd('0').trimEnd('.')
            "$text кг"
        }
    }
    return "${grams.toInt()} г"
}

private fun mealPlanLoadingHint(days: Int): String = when (days) {
    3 -> "Обычно 20–45 сек"
    5 -> "Обычно 45–90 сек"
    else -> "Обычно 1–2 мин"
}

@Composable
private fun PlanDaysSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    RoundedSectionTabs(
        tabs = listOf(
            RoundedTabItem(3, "3 дня"),
            RoundedTabItem(5, "5 дней"),
            RoundedTabItem(7, "7 дней"),
        ),
        selected = selected,
        onSelect = onSelect,
    )
}

@Composable
private fun MealPlanListCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        content()
    }
}

@Composable
private fun MealPlanDayCard(day: MealPlanDayDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(subtleFillGradient()))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            day.day_name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentPrimaryColor(),
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MacroChip("${day.total_calories} ккал")
            MacroChip("Б ${day.total_protein.toInt()} г")
            MacroChip("Ж ${day.total_fat.toInt()} г")
            MacroChip("У ${day.total_carbs.toInt()} г")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        day.meals.forEach { meal ->
            MealPlanMealRow(meal)
        }
    }
}

@Composable
private fun MacroChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun MealPlanMealRow(meal: MealPlanItemDto) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                meal.meal_type,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${meal.calories} ккал · Б${meal.protein_g.toInt()} Ж${meal.fat_g.toInt()} У${meal.carbs_g.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = contentSecondaryColor(),
            )
        }
        Text(
            meal.name,
            fontWeight = FontWeight.SemiBold,
            color = contentPrimaryColor(),
        )
        if (meal.ingredients.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                meal.ingredients.forEach { ingredient ->
                    Text(
                        text = "${ingredient.name} — ${formatIngredientAmount(ingredient.name, ingredient.grams_g)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentSecondaryColor(),
                    )
                }
            }
        }
        meal.recipe?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = contentSecondaryColor(),
            )
        }
    }
}
