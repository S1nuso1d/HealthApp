package com.example.healtapp.features.dashboard.presentation

import com.example.healtapp.data.network.dto.dashboard.GoalsCalendarDayDto
import com.example.healtapp.features.recommendations.presentation.RecommendationUiItem
import java.time.LocalDate
import java.time.YearMonth

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val error: String? = null,
    val isGuestMode: Boolean = false,
    val isOfflineCache: Boolean = false,

    val greetingText: String = "Добро пожаловать",
    val headerSubtitle: String = "Сводка сна, питания, воды и активности",

    val sleepHours: Float = 0f,
    val sleepTargetHours: Float = 8f,
    val sleepQuality: String = "—",

    val waterMl: Int = 0,
    val waterTargetMl: Int = 2500,

    val caloriesToday: Int = 0,
    val caloriesTarget: Int = 2200,
    val caffeineToday: Float = 0f,

    val stepsToday: Int = 0,
    val stepsGoal: Int = 10_000,
    val activityMinutesToday: Int = 0,
    val caloriesBurnedToday: Int = 0,
    val caloriesBurnGoal: Int = 450,

    val scores: ScoreBreakdownUi = ScoreBreakdownUi(),
    val dailyBrief: DailyBriefUi? = null,
    val actionPlanItems: List<ActionPlanItemUi> = emptyList(),
    val weeklySummary: WeeklySummaryUi? = null,
    val topInsights: List<InsightUi> = emptyList(),
    val moodCheckIn: MoodCheckInUi = MoodCheckInUi(),
    val waterStreakDays: Int = 0,
    val stepsStreakDays: Int = 0,

    val goalsCalendarMonth: YearMonth = YearMonth.now(),
    val goalsCalendarDays: List<GoalsCalendarDayDto> = emptyList(),
    val goalsCalendarLoading: Boolean = false,
    val goalsCalendarSelectedDate: LocalDate? = null,
    val goalsCalendarDetailDate: LocalDate? = null,

    val heartRateBpm: Int? = null,
    val spo2Percent: Float? = null,

    val recommendations: List<RecommendationUiItem> = emptyList(),
    val recommendationsLoading: Boolean = false,
    val recommendationsError: String? = null,
)
