package com.example.healtapp.data.network.dto.dashboard

data class GoalsCalendarMealItemDto(
    val name: String = "",
    val meal_type: String = "snack",
    val meal_time: String = "",
    val calories: Float = 0f,
    val protein_g: Float = 0f,
    val fat_g: Float = 0f,
    val carbs_g: Float = 0f,
)

data class GoalsCalendarDayDto(
    val date: String,
    val sleep_met: Boolean = false,
    val hydration_met: Boolean = false,
    val activity_met: Boolean = false,
    val nutrition_met: Boolean = false,
    val all_goals_met: Boolean = false,
    val has_any_data: Boolean = false,
    val sleep_progress: Float = 0f,
    val hydration_progress: Float = 0f,
    val activity_progress: Float = 0f,
    val nutrition_progress: Float = 0f,
    val sleep_hours: Float = 0f,
    val water_ml: Int = 0,
    val steps: Int = 0,
    val calories: Float = 0f,
    val calories_burned: Float = 0f,
    val calories_consumed: Float = 0f,
    val protein_g: Float = 0f,
    val fat_g: Float = 0f,
    val carbs_g: Float = 0f,
    val meals: List<GoalsCalendarMealItemDto> = emptyList(),
)

data class GoalsCalendarDto(
    val days: List<GoalsCalendarDayDto> = emptyList(),
)
