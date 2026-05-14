package com.example.healtapp.features.activity.presentation

import com.example.healtapp.data.network.dto.activity.ActivityDto

data class ActivityUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,

    val activityType: String = "",
    val durationMinutes: String = "",
    val steps: String = "",
    val caloriesBurned: String = "",
    val distanceKm: String = "",
    val intensity: String = "",

    val todayActivity: ActivityDto? = null
)