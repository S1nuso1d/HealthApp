package com.example.healtapp.data.network.dto.sleep

data class SleepDto(
    val id: Int,
    val user_id: Int,
    val sleep_start: String,
    val sleep_end: String,
    val duration_hours: Float,
    val quality_score: Int? = null,
    val notes: String? = null,
    val created_at: String? = null
)