package com.example.healtapp.features.meal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.RoundedSectionTabs
import com.example.healtapp.core.ui.components.RoundedTabItem
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.ai.MealPlanDayDto
import com.example.healtapp.data.network.dto.ai.MealPlanItemDto
import com.example.healtapp.features.meal.presentation.MealPlannerViewModel

@Composable
fun MealPlannerScreen(
    onBack: () -> Unit,
) {
    val viewModel: MealPlannerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppScreen(
        title = "AI План питания",
        subtitle = "Меню и список покупок по вашему профилю",
        headerIcon = Icons.Filled.Restaurant,
        onNavigateBack = onBack,
        scrollable = true,
        contentPadding = PaddingValues(16.dp),
    ) {
        uiState.info?.let {
            AppMessageBanner(text = it, type = AppMessageType.Info)
        }
        uiState.error?.let {
            AppMessageBanner(text = it, type = AppMessageType.Error)
        }

        if (uiState.plan == null && !uiState.isLoading) {
            SectionHeader(title = "Генерация меню", subtitle = "По целям и ограничениям из профиля")
            GradientFormPanel {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Сгенерируйте персональное меню",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Учитываем цели, аллергии и вегетарианство из профиля.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    PlanDaysSelector(
                        selected = uiState.planDays,
                        onSelect = viewModel::setPlanDays,
                    )
                    AppButton(
                        text = "Сгенерировать план (${uiState.planDays} дн.)",
                        onClick = viewModel::generatePlan,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Составляем меню…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        uiState.error?.let {
            AppMessageBanner(text = it, type = AppMessageType.Error)
        }

        uiState.plan?.let { plan ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppButton(
                    text = "Обновить план",
                    onClick = viewModel::generatePlan,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.weight(1f),
                    isSecondary = true,
                )
            }
            PlanDaysSelector(
                selected = uiState.planDays,
                onSelect = viewModel::setPlanDays,
            )

            SectionHeader(
                title = "Список покупок",
                subtitle = "${plan.grocery_list.size} позиций",
            )
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    val grouped = plan.grocery_list.groupBy { it.category }
                    grouped.forEach { (cat, items) ->
                        Text(
                            cat,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(item.name)
                                Text(
                                    item.amount,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            SectionHeader(title = "Меню", subtitle = "${plan.days.size} дней")
            plan.days.forEach { day ->
                MealPlanDayCard(day)
            }
        }

    }
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
private fun MealPlanDayCard(day: MealPlanDayDto) {
    AppCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                day.day_name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${day.total_calories} ккал", style = MaterialTheme.typography.labelMedium)
                Text("Б ${day.total_protein.toInt()} г", style = MaterialTheme.typography.labelMedium)
                Text("Ж ${day.total_fat.toInt()} г", style = MaterialTheme.typography.labelMedium)
                Text("У ${day.total_carbs.toInt()} г", style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            day.meals.forEach { meal ->
                MealPlanMealRow(meal)
            }
        }
    }
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
            )
            Text(
                "${meal.calories} ккал · Б${meal.protein_g.toInt()} Ж${meal.fat_g.toInt()} У${meal.carbs_g.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(meal.name, fontWeight = FontWeight.SemiBold)
        meal.recipe?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
