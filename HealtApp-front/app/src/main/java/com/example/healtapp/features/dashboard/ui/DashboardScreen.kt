package com.example.healtapp.features.dashboard.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.ErrorStateView
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.dashboard.presentation.DashboardViewModel
import com.example.healtapp.features.dashboard.ui.components.AnimatedDashboardSection
import com.example.healtapp.features.dashboard.ui.components.DashboardActionPlanPreview
import com.example.healtapp.features.dashboard.ui.components.DashboardDailyBriefCard
import com.example.healtapp.features.dashboard.ui.components.DashboardHeroCard
import com.example.healtapp.features.dashboard.ui.components.DashboardMetricsGrid
import com.example.healtapp.features.dashboard.ui.components.DashboardMoodCheckInCard
import com.example.healtapp.features.dashboard.ui.components.DashboardQuickLinksRow
import com.example.healtapp.features.dashboard.ui.components.DashboardRecommendationsBlock
import com.example.healtapp.features.dashboard.ui.components.DashboardScoresCard
import com.example.healtapp.features.dashboard.ui.components.DashboardSkeleton
import com.example.healtapp.features.dashboard.ui.components.DashboardSmartRemindersBlock
import com.example.healtapp.features.dashboard.ui.components.DashboardStreaksRow
import com.example.healtapp.features.recommendations.presentation.RecommendationsViewModel

@Composable
fun DashboardScreen(
    onOpenSleep: () -> Unit = {},
    onOpenHydration: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onOpenActivity: () -> Unit = {},
    onOpenRecommendations: () -> Unit = {},
    onOpenActionPlan: () -> Unit = {},
    onOpenTimeline: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
) {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val recommendationsViewModel: RecommendationsViewModel = hiltViewModel()

    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val recState by recommendationsViewModel.uiState.collectAsStateWithLifecycle()

    val healthScore = recState.healthScore.takeIf { it > 0 && recState.error == null }
        ?: uiState.scores.healthScore.takeIf { it > 0 }

    AppScreen(scrollable = true, contentPadding = PaddingValues(20.dp)) {
        DashboardHeroCard(
            greeting = uiState.greetingText,
            subtitle = uiState.headerSubtitle,
            healthScore = healthScore,
            isRecommendationsLoading = recState.isLoading && !uiState.isLoading,
        )

        if (uiState.isGuestMode) {
            AppCard {
                Text(
                    text = "Демо-режим: сводка и советы — пример. Войдите в аккаунт для данных с сервера.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.isOfflineCache) {
            Text(
                text = "Показаны сохранённые данные — нет связи с сервером",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (uiState.isLoading) {
            SectionHeader(title = "Сегодня", subtitle = "Загружаем метрики…")
            DashboardSkeleton()
        } else if (uiState.error != null && uiState.sleepHours == 0f && uiState.waterMl == 0) {
            ErrorStateView(
                message = uiState.error ?: "Не удалось загрузить сводку",
                onRetry = {
                    dashboardViewModel.loadDashboard()
                    recommendationsViewModel.refresh()
                },
            )
        } else {
            uiState.error?.let { warn ->
                Text(text = warn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            AnimatedDashboardSection(visible = uiState.dailyBrief != null) {
                uiState.dailyBrief?.let { DashboardDailyBriefCard(brief = it) }
            }

            if (!uiState.isGuestMode) {
                DashboardMoodCheckInCard(
                    state = uiState.moodCheckIn,
                    onMoodChange = dashboardViewModel::updateMood,
                    onEnergyChange = dashboardViewModel::updateEnergy,
                    onStressChange = dashboardViewModel::updateStress,
                    onSubmit = dashboardViewModel::submitMoodCheckIn,
                )
            }

            DashboardQuickLinksRow(onOpenAi = onOpenAiAssistant, onOpenTimeline = onOpenTimeline)

            AnimatedDashboardSection(visible = uiState.scores.healthScore > 0) {
                DashboardScoresCard(scores = uiState.scores)
            }

            DashboardStreaksRow(waterStreak = uiState.waterStreakDays, stepsStreak = uiState.stepsStreakDays)

            SectionHeader(title = "Сегодня", subtitle = "Четыре опоры здоровья — нажмите на плитку")

            DashboardMetricsGrid(
                sleepHours = uiState.sleepHours,
                sleepTargetHours = uiState.sleepTargetHours,
                sleepQuality = uiState.sleepQuality,
                waterMl = uiState.waterMl,
                waterTargetMl = uiState.waterTargetMl,
                caloriesToday = uiState.caloriesToday,
                caloriesTarget = uiState.caloriesTarget,
                caffeineToday = uiState.caffeineToday.toInt(),
                stepsToday = uiState.stepsToday,
                stepsGoal = uiState.stepsGoal,
                activityMinutesToday = uiState.activityMinutesToday,
                onOpenSleep = onOpenSleep,
                onOpenHydration = onOpenHydration,
                onOpenNutrition = onOpenNutrition,
                onOpenActivity = onOpenActivity,
            )

            DashboardSmartRemindersBlock(
                reminders = uiState.smartReminders,
                onComplete = dashboardViewModel::completeReminder,
                onDismiss = dashboardViewModel::dismissReminder,
            )

            DashboardActionPlanPreview(
                items = uiState.actionPlanItems,
                onOpenAll = onOpenActionPlan,
                onToggle = dashboardViewModel::toggleActionPlanStatus,
            )
        }

        Spacer(Modifier.height(4.dp))

        DashboardRecommendationsBlock(
            isLoading = recState.isLoading,
            error = recState.error,
            recommendations = recState.recommendations,
            onRetry = { recommendationsViewModel.refresh() },
            onOpenAll = onOpenRecommendations,
        )

        Spacer(Modifier.height(80.dp))
    }
}
