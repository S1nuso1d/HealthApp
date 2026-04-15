package com.example.healtapp.data.network.dto.activity

data class ActivitySummaryDto(
    val total_steps: Int = 0,
    val total_active_minutes: Int = 0,
    val total_calories_burned: Int = 0,
    val workouts_count: Int = 0
)