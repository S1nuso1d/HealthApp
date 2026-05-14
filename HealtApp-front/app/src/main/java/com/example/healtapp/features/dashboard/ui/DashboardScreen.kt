package com.example.healtapp.features.dashboard.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.ErrorStateView
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.dashboard.presentation.DashboardViewModel
import com.example.healtapp.features.dashboard.ui.components.DashboardActivityCard
import com.example.healtapp.features.dashboard.ui.components.DashboardHeader
import com.example.healtapp.features.dashboard.ui.components.DashboardNutritionCard
import com.example.healtapp.features.dashboard.ui.components.DashboardSleepCard
import com.example.healtapp.features.dashboard.ui.components.DashboardSkeleton
import com.example.healtapp.features.dashboard.ui.components.DashboardWaterCard
import com.example.healtapp.features.recommendations.presentation.RecommendationsViewModel
import com.example.healtapp.features.recommendations.ui.components.RecommendationCard

@Composable
fun DashboardScreen() {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val recommendationsViewModel: RecommendationsViewModel = hiltViewModel()

    val uiState by dashboardViewModel.uiState.collectAsState()
    val recommendationsState by recommendationsViewModel.uiState.collectAsState()

    AppScreen(scrollable = true) {
        DashboardHeader(
            greeting = "Привет!",
            userName = "",
        )

        SectionHeader(
            title = "Сводка за сегодня",
            subtitle = "Ключевые метрики по четырём направлениям здоровья",
        )

        if (uiState.isLoading) {
            DashboardSkeleton()
            return@AppScreen
        }

        if (uiState.error != null) {
            ErrorStateView(
                message = uiState.error ?: "Не удалось загрузить сводку",
                onRetry = {
                    dashboardViewModel.loadDashboard()
                    recommendationsViewModel.loadRecommendations()
                }
            )
            return@AppScreen
        }

        DashboardSleepCard(
            sleepHours = uiState.sleepHours,
            sleepTargetHours = uiState.sleepTargetHours,
            sleepQuality = uiState.sleepQuality
        )

        DashboardWaterCard(
            waterMl = uiState.waterMl,
            waterTargetMl = uiState.waterTargetMl
        )

        DashboardNutritionCard(
            caloriesToday = uiState.caloriesToday,
            caloriesTarget = uiState.caloriesTarget,
            caffeineToday = uiState.caffeineToday.toInt()
        )

        DashboardActivityCard(
            stepsToday = uiState.stepsToday,
            activityMinutesToday = uiState.activityMinutesToday,
            caloriesBurnedToday = uiState.caloriesBurnedToday
        )

        SectionHeader(
            title = "Рекомендации дня",
            subtitle = "Персональные подсказки на основе твоих данных",
        )

        when {
            recommendationsState.isLoading -> {
                Text(
                    text = "Обновляем рекомендации...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            recommendationsState.error != null -> {
                Text(
                    text = recommendationsState.error ?: "Не удалось загрузить рекомендации",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            recommendationsState.recommendations.isEmpty() -> {
                Text(
                    text = "На сегодня рекомендаций пока нет. Добавь данные о сне, питании, гидратации и активности, чтобы я смог помочь точнее.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            else -> {
                recommendationsState.recommendations.take(3).forEach { recommendation ->
                    RecommendationCard(item = recommendation)
                }
            }
        }
    }
}