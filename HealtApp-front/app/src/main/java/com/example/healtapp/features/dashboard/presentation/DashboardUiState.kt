package com.example.healtapp.features.dashboard.presentation

data class DashboardUiState(
    val isLoading: Boolean = true,
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

    val scores: ScoreBreakdownUi = ScoreBreakdownUi(),
    val dailyBrief: DailyBriefUi? = null,
    val actionPlanItems: List<ActionPlanItemUi> = emptyList(),
    val smartReminders: List<SmartReminderUi> = emptyList(),
    val topInsights: List<InsightUi> = emptyList(),
    val moodCheckIn: MoodCheckInUi = MoodCheckInUi(),
    val waterStreakDays: Int = 0,
    val stepsStreakDays: Int = 0,
)
