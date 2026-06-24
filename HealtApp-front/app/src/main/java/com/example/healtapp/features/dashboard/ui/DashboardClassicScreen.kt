package com.example.healtapp.features.dashboard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.healtapp.core.common.ShareProgressHelper
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.ErrorStateView
import com.example.healtapp.core.ui.components.FeatureGuideContent
import com.example.healtapp.core.ui.components.FeatureGuideOverlay
import com.example.healtapp.core.ui.components.FeatureGuidePrefs
import com.example.healtapp.core.ui.components.FeatureGuideScreen
import com.example.healtapp.core.ui.components.PullToRefreshContainer
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.dashboard.presentation.DashboardUiState
import com.example.healtapp.features.dashboard.presentation.DashboardViewModel
import com.example.healtapp.features.dashboard.ui.components.AnimatedDashboardSection
import com.example.healtapp.features.dashboard.ui.components.DashboardActionPlanPreview
import com.example.healtapp.features.dashboard.ui.components.DashboardGoalsCalendarBlock
import com.example.healtapp.features.dashboard.ui.components.DashboardHeroCard
import com.example.healtapp.features.dashboard.ui.components.DashboardHintsRow
import com.example.healtapp.features.dashboard.ui.components.DashboardMetricsGrid
import com.example.healtapp.features.dashboard.ui.components.DashboardMoodCheckInCard
import com.example.healtapp.features.dashboard.ui.components.DashboardQuickLinksRow
import com.example.healtapp.features.dashboard.ui.components.DashboardRecommendationsBlock
import com.example.healtapp.features.dashboard.ui.components.DashboardScoresCard
import com.example.healtapp.features.dashboard.ui.components.DashboardSkeleton
import com.example.healtapp.features.dashboard.ui.components.DashboardStreaksRow
import com.example.healtapp.features.dashboard.ui.components.DashboardVitalsStrip
import com.example.healtapp.features.dashboard.ui.components.DashboardWeeklySummaryBlock

@Composable
fun DashboardClassicScreen(
    uiState: DashboardUiState,
    dashboardViewModel: DashboardViewModel,
    onOpenSleep: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenRecommendations: () -> Unit,
    onOpenActionPlan: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenHealthVitals: () -> Unit,
) {
    val context = LocalContext.current
    val healthScore = uiState.scores.healthScore.takeIf { it > 0 }
    var showGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showGuide = FeatureGuidePrefs.shouldShow(context, FeatureGuideScreen.Dashboard)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dashboardViewModel.refreshLiveSteps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            PullToRefreshContainer(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { dashboardViewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                AppScreen(
                    scrollable = true,
                    scrollStateKey = "dashboard",
                    contentPadding = PaddingValues(20.dp),
                    extraBottomPadding = 80.dp,
                ) {
                    DashboardHeroCard(
                        greeting = uiState.greetingText,
                        subtitle = uiState.headerSubtitle,
                        healthScore = healthScore,
                        isRecommendationsLoading = uiState.recommendationsLoading && !uiState.hasLoadedOnce,
                        streak = uiState.currentStreak,
                    )

                    if (!uiState.isGuestMode) {
                        DashboardHintsRow(
                            hints = uiState.dashboardHints,
                            isLoading = uiState.hintsLoading,
                        )
                    }

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
                    } else if (
                        uiState.error != null &&
                        !uiState.hasLoadedOnce &&
                        uiState.sleepHours == 0f &&
                        uiState.waterMl == 0
                    ) {
                        ErrorStateView(
                            message = uiState.error ?: "Не удалось загрузить сводку",
                            onRetry = { dashboardViewModel.loadDashboard() },
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

                        DashboardQuickLinksRow(
                            onOpenAi = onOpenAiAssistant,
                            onOpenTimeline = onOpenTimeline,
                        )

                        AnimatedDashboardSection(visible = uiState.scores.healthScore > 0) {
                            DashboardScoresCard(scores = uiState.scores)
                        }

                        DashboardStreaksRow(
                            waterStreak = uiState.waterStreakDays,
                            stepsStreak = uiState.stepsStreakDays,
                        )

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

                        SectionHeader(
                            title = "Сегодня",
                            subtitle = "Четыре опоры здоровья — нажмите на плитку",
                        )

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
                            onShare = uiState.weeklySummary?.takeIf { it.hasAnyData }?.let { summary ->
                                {
                                    val steps = summary.metrics.find { m -> m.key == "steps" }?.averageDisplay
                                    val water = summary.metrics.find { m -> m.key == "water" }?.averageDisplay
                                    val sleep = summary.metrics.find { m -> m.key == "sleep" }?.averageDisplay
                                    ShareProgressHelper.shareWeeklySummary(
                                        context = context,
                                        periodLabel = summary.periodLabel,
                                        stepsAvg = steps,
                                        waterAvg = water,
                                        sleepAvg = sleep,
                                        healthScore = uiState.scores.healthScore.takeIf { it > 0 },
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
                        visible = uiState.hasLoadedOnce &&
                            (!uiState.recommendationsLoading || uiState.recommendations.isNotEmpty()),
                    ) {
                        DashboardRecommendationsBlock(
                            isLoading = uiState.recommendationsLoading,
                            error = uiState.recommendationsError,
                            recommendations = uiState.recommendations,
                            onRetry = { dashboardViewModel.refresh() },
                            onOpenAll = onOpenRecommendations,
                        )
                    }
                }
            }

            FeatureGuideOverlay(
                visible = showGuide,
                pages = FeatureGuideContent.dashboard,
                sectionLabel = "Главная",
                onDismiss = {
                    FeatureGuidePrefs.markSeen(context, FeatureGuideScreen.Dashboard)
                    showGuide = false
                },
            )
        }
    }
}
