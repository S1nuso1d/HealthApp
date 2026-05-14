package com.example.healtapp.features.dashboard.presentation

data class DashboardQuickAction(
    val title: String,
    val subtitle: String
)

data class DashboardRecommendation(
    val title: String,
    val description: String,
    val priority: String
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    val greetingText: String = "Добро пожаловать",



    val sleepHours: Float = 0f,
    val sleepTargetHours: Float = 8f,
    val sleepQuality: String = "—",

    val waterMl: Int = 0,
    val waterTargetMl: Int = 2500,

    val caloriesToday: Int = 0,
    val caloriesTarget: Int = 2200,
    val caffeineToday: Float = 0f,

    val stepsToday: Int = 0,
    val activityMinutesToday: Int = 0,
    val caloriesBurnedToday: Int = 0,

    val quickActions: List<DashboardQuickAction> = listOf(
        DashboardQuickAction(
            title = "Добавить сон",
            subtitle = "Запиши время сна и качество"
        ),
        DashboardQuickAction(
            title = "Добавить воду",
            subtitle = "Быстро зафиксируй выпитую воду"
        ),
        DashboardQuickAction(
            title = "Открыть профиль",
            subtitle = "Обнови цели и параметры"
        )
    ),

    val recommendations: List<DashboardRecommendation> = emptyList()
)