package com.example.healtapp.features.activity.presentation

import com.example.healtapp.data.network.dto.activity.ActivityDto

data class ActivityUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,

    val stepsToday: Int = 0,
    val stepsGoal: Int = 10_000,
    val weeklySteps: List<DaySteps> = emptyList(),
    val trainingMinutesToday: Int = 0,
    val trainingCaloriesToday: Int = 0,
    val caloriesBurnedToday: Int = 0,
    val caloriesBurnGoal: Int = 450,
    val healthConnectStepsToday: Int? = null,

    val activityType: String = "Бег",
    val durationMinutes: String = "",
    val caloriesBurned: String = "",
    val distanceKm: String = "",
    val intensity: String = "Средняя",

    val trainingHistory: List<ActivityDto> = emptyList(),
    val healthConnectWorkouts: List<ActivityDto> = emptyList(),
    val todayWalkRecordId: Int? = null,

    val trainingNotes: String = "",
    val perceivedExertion: String = "",
    val progressCelebrateToken: Int = 0,
)
