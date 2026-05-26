package com.example.healtapp.features.dashboard.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.ShareProgressHelper
import com.example.healtapp.features.dashboard.ui.components.DashboardAiCoachCard
import com.example.healtapp.core.ui.components.PullToRefreshContainer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.ErrorStateView
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.dashboard.presentation.DashboardViewModel
import com.example.healtapp.features.dashboard.ui.components.AnimatedDashboardSection
import com.example.healtapp.features.dashboard.ui.components.DashboardActionPlanPreview
import com.example.healtapp.features.dashboard.ui.components.DashboardHeroCard
import com.example.healtapp.features.dashboard.ui.components.DashboardMetricsGrid
import com.example.healtapp.features.dashboard.ui.components.DashboardMoodCheckInCard
import com.example.healtapp.features.dashboard.ui.components.DashboardQuickLinksRow
import com.example.healtapp.features.dashboard.ui.components.DashboardRecommendationsBlock
import com.example.healtapp.features.dashboard.ui.components.DashboardScoresCard
import com.example.healtapp.features.dashboard.ui.components.DashboardSkeleton
import com.example.healtapp.features.dashboard.ui.components.DashboardGoalsCalendarBlock
import com.example.healtapp.features.dashboard.ui.components.DashboardVitalsStrip
import com.example.healtapp.features.dashboard.ui.components.DashboardWeeklySummaryBlock
import com.example.healtapp.features.dashboard.ui.components.DashboardStreaksRow

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
    onOpenHealthVitals: () -> Unit = {},
) {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val context = LocalContext.current

    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val coachItem = uiState.recommendations.firstOrNull()

    val healthScore = uiState.scores.healthScore.takeIf { it > 0 }

    val snackbarHostState = remember { SnackbarHostState() }
    var refreshTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isRefreshing) {
        if (uiState.isRefreshing && uiState.hasLoadedOnce) {
            refreshTriggered = true
        } else if (refreshTriggered && !uiState.isRefreshing) {
            snackbarHostState.showSnackbar("Обновлено")
            refreshTriggered = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { padding ->
        PullToRefreshContainer(
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                dashboardViewModel.refresh()
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            AppScreen(scrollable = true, contentPadding = PaddingValues(20.dp)) {
        DashboardHeroCard(
            greeting = uiState.greetingText,
            subtitle = uiState.headerSubtitle,
            healthScore = healthScore,
            isRecommendationsLoading = uiState.recommendationsLoading && !uiState.hasLoadedOnce,
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
            AppMessageBanner(
                text = "Показаны сохранённые данные — нет связи с сервером",
                type = AppMessageType.Warning,
                title = "Офлайн",
            )
        }

        if (uiState.isLoading && !uiState.hasLoadedOnce) {
            SectionHeader(title = "Сегодня", subtitle = "Загружаем метрики…")
            DashboardSkeleton()
        } else if (uiState.error != null && !uiState.hasLoadedOnce && uiState.sleepHours == 0f && uiState.waterMl == 0) {
            ErrorStateView(
                message = uiState.error ?: "Не удалось загрузить сводку",
                onRetry = {
                    dashboardViewModel.loadDashboard()
                },
            )
        } else {
            uiState.error?.let { warn ->
                AppMessageBanner(text = warn, type = AppMessageType.Error)
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

            if (!uiState.isGuestMode) {
                DashboardAiCoachCard(
                    title = coachItem?.title ?: "Совет коуча",
                    body = coachItem?.personalizedTip
                        ?: coachItem?.description
                        ?: "Задайте вопрос по питанию, сну и активности — коуч учтёт ваш дневник.",
                    isLoading = uiState.recommendationsLoading && coachItem == null,
                    onOpenAssistant = onOpenAiAssistant,
                )
            }

            AnimatedDashboardSection(visible = uiState.scores.healthScore > 0) {
                DashboardScoresCard(scores = uiState.scores)
            }

            DashboardStreaksRow(waterStreak = uiState.waterStreakDays, stepsStreak = uiState.stepsStreakDays)

            if (!uiState.isGuestMode) {
                DashboardGoalsCalendarBlock(
                    yearMonth = uiState.goalsCalendarMonth,
                    days = uiState.goalsCalendarDays,
                    isLoading = uiState.goalsCalendarLoading,
                    selectedDate = uiState.goalsCalendarSelectedDate,
                    detailDate = uiState.goalsCalendarDetailDate,
                    onPrevMonth = { dashboardViewModel.shiftGoalsCalendarMonth(-1) },
                    onNextMonth = { dashboardViewModel.shiftGoalsCalendarMonth(1) },
                    onDayClick = dashboardViewModel::onGoalsCalendarDayClick,
                    onDismissDetail = dashboardViewModel::dismissGoalsCalendarDayDetail,
                )
            }

            if (!uiState.isGuestMode) {
                DashboardVitalsStrip(
                    heartRateBpm = uiState.heartRateBpm,
                    spo2Percent = uiState.spo2Percent,
                    onOpenVitals = onOpenHealthVitals,
                )
            }

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
                caloriesBurnedToday = uiState.caloriesBurnedToday,
                caloriesBurnGoal = uiState.caloriesBurnGoal,
                onOpenSleep = onOpenSleep,
                onOpenHydration = onOpenHydration,
                onOpenNutrition = onOpenNutrition,
                onOpenActivity = onOpenActivity,
            )

            DashboardWeeklySummaryBlock(
                summary = uiState.weeklySummary,
                onShare = uiState.weeklySummary?.takeIf { it.hasAnyData }?.let {
                    {
                        val steps = it.metrics.find { m -> m.key == "steps" }?.averageDisplay
                        val water = it.metrics.find { m -> m.key == "water" }?.averageDisplay
                        val sleep = it.metrics.find { m -> m.key == "sleep" }?.averageDisplay
                        ShareProgressHelper.shareWeeklySummary(
                            context = context,
                            periodLabel = it.periodLabel,
                            stepsAvg = steps,
                            waterAvg = water,
                            sleepAvg = sleep,
                            healthScore = uiState.scores.healthScore.takeIf { s -> s > 0 },
                        )
                    }
                },
            )

            DashboardActionPlanPreview(
                items = uiState.actionPlanItems,
                onOpenAll = onOpenActionPlan,
                onToggle = dashboardViewModel::toggleActionPlanStatus,
                waterMl = uiState.waterMl,
                waterTargetMl = uiState.waterTargetMl,
                stepsToday = uiState.stepsToday,
                stepsGoal = uiState.stepsGoal,
                caloriesBurnedToday = uiState.caloriesBurnedToday,
                caloriesBurnGoal = uiState.caloriesBurnGoal,
                sleepHours = uiState.sleepHours,
                sleepTargetHours = uiState.sleepTargetHours,
                caloriesToday = uiState.caloriesToday,
                caloriesTarget = uiState.caloriesTarget,
                activityMinutesToday = uiState.activityMinutesToday,
                moodSavedToday = uiState.moodCheckIn.savedToday,
            )
        }

        Spacer(Modifier.height(4.dp))

        AnimatedDashboardSection(
            visible = uiState.hasLoadedOnce && (!uiState.recommendationsLoading || uiState.recommendations.isNotEmpty()),
        ) {
            DashboardRecommendationsBlock(
                isLoading = uiState.recommendationsLoading,
                error = uiState.recommendationsError,
                recommendations = uiState.recommendations,
                onRetry = { dashboardViewModel.refresh() },
                onOpenAll = onOpenRecommendations,
            )
        }

        Spacer(Modifier.height(80.dp))
            }
        }
    }
}
