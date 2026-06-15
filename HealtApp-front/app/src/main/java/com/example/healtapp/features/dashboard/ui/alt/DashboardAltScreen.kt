package com.example.healtapp.features.dashboard.ui.alt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.ShareProgressHelper
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.ErrorStateView
import com.example.healtapp.core.ui.components.PullToRefreshContainer
import com.example.healtapp.features.dashboard.presentation.DashboardUiState
import com.example.healtapp.features.dashboard.presentation.DashboardViewModel
import com.example.healtapp.features.dashboard.ui.components.AnimatedDashboardSection
import com.example.healtapp.features.dashboard.ui.components.DashboardActionPlanPreview
import com.example.healtapp.features.dashboard.ui.components.DashboardGoalsCalendarBlock
import com.example.healtapp.features.dashboard.ui.components.DashboardHintsRow
import com.example.healtapp.features.dashboard.ui.components.DashboardMoodCheckInCard
import com.example.healtapp.features.dashboard.ui.components.DashboardQuickLinksRow
import com.example.healtapp.features.dashboard.ui.components.DashboardRecommendationsBlock
import com.example.healtapp.features.dashboard.ui.components.DashboardScoresCard
import com.example.healtapp.features.dashboard.ui.components.DashboardSkeleton
import com.example.healtapp.features.dashboard.ui.components.DashboardVitalsStrip
import com.example.healtapp.features.dashboard.ui.components.DashboardWeeklySummaryBlock

@Composable
fun DashboardAltScreen(
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
    onRevertLayout: () -> Unit,
) {
    val context = LocalContext.current
    val healthScore = uiState.scores.healthScore.takeIf { it > 0 }
    val scrollState = rememberScrollState()

    DashboardAltTheme {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            PullToRefreshContainer(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { dashboardViewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(scrollState)
                        .padding(PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 96.dp)),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        AltHeroBlock(
                            greeting = uiState.greetingText,
                            subtitle = uiState.headerSubtitle,
                            healthScore = healthScore,
                            streak = uiState.currentStreak,
                            onRevertLayout = onRevertLayout,
                        )

                        if (!uiState.isGuestMode) {
                            AltPanel {
                                DashboardHintsRow(
                                    hints = uiState.dashboardHints,
                                    isLoading = uiState.hintsLoading,
                                )
                            }
                        }

                        if (uiState.isGuestMode) {
                            AltInlineBanner(
                                text = "Демо-режим: сводка и советы — пример. Войдите в аккаунт для данных с сервера.",
                            )
                        }

                        if (uiState.isOfflineCache) {
                            AppMessageBanner(
                                text = "Показаны сохранённые данные — нет связи с сервером",
                                type = AppMessageType.Warning,
                                title = "Офлайн",
                            )
                        }

                        if (uiState.isLoading && !uiState.hasLoadedOnce) {
                            AltSectionLabel(title = "Сегодня", subtitle = "Загружаем метрики…")
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
                                AltPanel {
                                    DashboardMoodCheckInCard(
                                        state = uiState.moodCheckIn,
                                        onMoodChange = dashboardViewModel::updateMood,
                                        onEnergyChange = dashboardViewModel::updateEnergy,
                                        onStressChange = dashboardViewModel::updateStress,
                                        onSubmit = dashboardViewModel::submitMoodCheckIn,
                                    )
                                }
                            }

                            AltPanel {
                                DashboardQuickLinksRow(
                                    onOpenAi = onOpenAiAssistant,
                                    onOpenTimeline = onOpenTimeline,
                                )
                            }

                            AnimatedDashboardSection(visible = uiState.scores.healthScore > 0) {
                                AltPanel {
                                    DashboardScoresCard(scores = uiState.scores)
                                }
                            }

                            AltStreakStrip(
                                waterStreak = uiState.waterStreakDays,
                                stepsStreak = uiState.stepsStreakDays,
                            )

                            if (!uiState.isGuestMode) {
                                AltPanel {
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
                            }

                            if (!uiState.isGuestMode) {
                                AltPanel {
                                    DashboardVitalsStrip(
                                        heartRateBpm = uiState.heartRateBpm,
                                        spo2Percent = uiState.spo2Percent,
                                        onOpenVitals = onOpenHealthVitals,
                                    )
                                }
                            }

                            AltSectionLabel(
                                title = "Сегодня",
                                subtitle = "Четыре опоры — нажмите на плитку",
                            )

                            AltMetricsBento(
                                sleepHours = uiState.sleepHours,
                                sleepTargetHours = uiState.sleepTargetHours,
                                waterMl = uiState.waterMl,
                                waterTargetMl = uiState.waterTargetMl,
                                caloriesToday = uiState.caloriesToday,
                                caloriesTarget = uiState.caloriesTarget,
                                stepsToday = uiState.stepsToday,
                                stepsGoal = uiState.stepsGoal,
                                onOpenSleep = onOpenSleep,
                                onOpenHydration = onOpenHydration,
                                onOpenNutrition = onOpenNutrition,
                                onOpenActivity = onOpenActivity,
                            )

                            AltPanel {
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
                            }

                            AltPanel {
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
                        }

                        Spacer(Modifier.height(4.dp))

                        AnimatedDashboardSection(
                            visible = uiState.hasLoadedOnce &&
                                (!uiState.recommendationsLoading || uiState.recommendations.isNotEmpty()),
                        ) {
                            AltPanel {
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
                }
            }
        }
    }
}
