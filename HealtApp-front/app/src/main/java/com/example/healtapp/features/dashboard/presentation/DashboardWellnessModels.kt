package com.example.healtapp.features.dashboard.presentation

data class DailyBriefUi(
    val title: String,
    val summary: String,
    val keyPoints: List<String>,
)

data class ActionPlanItemUi(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val priority: String,
)

data class SmartReminderUi(
    val id: Int,
    val title: String,
    val message: String,
    val type: String,
    val status: String,
)

data class InsightUi(
    val title: String,
    val description: String,
    val category: String,
    val severity: String,
)

data class ScoreBreakdownUi(
    val healthScore: Int = 0,
    val sleepScore: Int = 0,
    val hydrationScore: Int = 0,
    val activityScore: Int = 0,
    val nutritionScore: Int = 0,
    val stateScore: Int = 0,
)

data class WeeklySummaryUi(
    val periodLabel: String,
    val metrics: List<WeeklyMetricUi>,
) {
    val hasAnyData: Boolean get() = metrics.any { it.daysLogged > 0 }
}

data class WeeklyMetricUi(
    val key: String,
    val label: String,
    val averageDisplay: String,
    val daysLogged: Int,
    val daysInPeriod: Int,
    val hint: String,
)

data class MoodCheckInUi(
    val mood: Int = 5,
    val energy: Int = 5,
    val stress: Int = 5,
    val isSaving: Boolean = false,
    val savedToday: Boolean = false,
)
