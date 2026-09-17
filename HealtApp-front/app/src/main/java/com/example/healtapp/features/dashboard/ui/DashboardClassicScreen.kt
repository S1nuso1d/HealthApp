package com.example.healtapp.features.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.example.healtapp.core.ui.theme.contentPrimaryColor
import com.example.healtapp.features.dashboard.presentation.DashboardUiState
import com.example.healtapp.features.dashboard.presentation.DashboardViewModel
import com.example.healtapp.features.dashboard.presentation.DayInsightBuilder
import com.example.healtapp.features.dashboard.ui.components.DashboardDayInsightCard
import com.example.healtapp.features.dashboard.ui.components.DashboardGoalsCalendarBlock
import com.example.healtapp.features.dashboard.ui.components.DashboardHabitExperimentCard
import com.example.healtapp.features.dashboard.ui.components.DashboardHeroCard
import com.example.healtapp.features.dashboard.ui.components.DashboardMetricsGrid
import com.example.healtapp.features.dashboard.ui.components.DashboardMoodCheckInCard
import com.example.healtapp.features.dashboard.ui.components.DashboardQuickLinksRow
import com.example.healtapp.features.dashboard.ui.components.DashboardSkeleton
import com.example.healtapp.features.dashboard.ui.components.DashboardTodayFocusCard
import com.example.healtapp.features.dashboard.ui.components.DashboardVitalsStrip
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun DashboardClassicScreen(
    uiState: DashboardUiState,
    dashboardViewModel: DashboardViewModel,
    onOpenSleep: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenHealthVitals: () -> Unit,
    onOpenWeeklyReview: () -> Unit = {},
    onOpenInfluenceFactors: () -> Unit = {},
) {
    val context = LocalContext.current
    val healthScore = uiState.scores.healthScore.takeIf { it > 0 }
    var showGuide by remember { mutableStateOf(false) }
    val isEvening = remember(uiState.tonightRisk?.isEvening) {
        LocalTime.now().hour >= 21 || uiState.tonightRisk?.isEvening == true
    }
    val showWeekClosed = remember(uiState.weeklySummary?.hasAnyData) {
        val today = LocalDate.now()
        val endOfWeek = today.dayOfWeek == DayOfWeek.SUNDAY ||
            (today.dayOfWeek == DayOfWeek.MONDAY && LocalTime.now().hour < 12)
        endOfWeek && uiState.weeklySummary?.hasAnyData == true
    }

    LaunchedEffect(Unit) {
        showGuide = FeatureGuidePrefs.shouldShow(context, FeatureGuideScreen.Dashboard)
    }

    LaunchedEffect(uiState.quickActionMessage) {
        if (uiState.quickActionMessage.isNullOrBlank()) return@LaunchedEffect
        delay(3200)
        dashboardViewModel.consumeQuickActionMessage()
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
                        healthScore = healthScore,
                        isRecommendationsLoading = uiState.recommendationsLoading && !uiState.hasLoadedOnce,
                        isEvening = isEvening,
                    )

                    if (uiState.isOfflineCache) {
                        AppMessageBanner(
                            text = "Показаны сохранённые данные — нет связи с сервером",
                            type = AppMessageType.Warning,
                            title = "Офлайн",
                        )
                    }

                    if (uiState.isLoading && !uiState.hasLoadedOnce) {
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

                        DashboardMoodCheckInCard(
                            state = uiState.moodCheckIn,
                            onMoodChange = dashboardViewModel::updateMood,
                            onEnergyChange = dashboardViewModel::updateEnergy,
                            onStressChange = dashboardViewModel::updateStress,
                            onWellbeingChange = dashboardViewModel::updateWellbeing,
                            onSubmit = dashboardViewModel::submitMoodCheckIn,
                        )

                        DashboardQuickLinksRow(
                            onOpenAi = onOpenAiAssistant,
                            onOpenWeeklyReview = onOpenWeeklyReview,
                            onOpenInfluenceFactors = onOpenInfluenceFactors,
                        )

                        uiState.quickActionMessage?.let { message ->
                            AppMessageBanner(text = message, type = AppMessageType.Success)
                        }

                        if (isEvening) {
                            DashboardEveningContext(
                                uiState = uiState,
                                onOpenTimeline = onOpenTimeline,
                                dashboardViewModel = dashboardViewModel,
                            )
                        }

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
                            compact = false,
                            isEvening = isEvening,
                        )

                        DayInsightBuilder.insight(uiState, isEvening)?.let { insight ->
                            DashboardDayInsightCard(insight = insight)
                        }

                        if (!isEvening) {
                            DashboardEveningContext(
                                uiState = uiState,
                                onOpenTimeline = onOpenTimeline,
                                dashboardViewModel = dashboardViewModel,
                            )
                        }

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
                                cyclePhaseTitle = uiState.cyclePhaseTitle,
                            )

                        DashboardVitalsStrip(
                                heartRateBpm = uiState.heartRateBpm,
                                spo2Percent = uiState.spo2Percent,
                                onOpenVitals = onOpenHealthVitals,
                            )

                        if (showWeekClosed) {
                            AppCard(onClick = onOpenWeeklyReview, highlight = true) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        Icons.Filled.AutoStories,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        "Неделя закрыта",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = contentPrimaryColor(),
                                    )
                                    Text(
                                        uiState.weeklySummary?.periodLabel.orEmpty() +
                                            " · средние по дням с записями",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "Открыть разбор недели",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        } else {
                            DashboardTodayFocusCard(
                                uiState = uiState,
                                isEvening = isEvening,
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
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

@Composable
private fun DashboardEveningContext(
    uiState: DashboardUiState,
    onOpenTimeline: () -> Unit,
    dashboardViewModel: DashboardViewModel,
) {
    uiState.habitExperiment?.let { experiment ->
        DashboardHabitExperimentCard(
            experiment = experiment,
            checkedToday = uiState.experimentCheckedToday,
            keptCount = uiState.experimentKeptCount,
            onCheckin = dashboardViewModel::markExperimentToday,
            onOpenFriends = onOpenTimeline,
        )
    }
}
