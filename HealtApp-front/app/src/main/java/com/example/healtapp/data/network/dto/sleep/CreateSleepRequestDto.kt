package com.example.healtapp.data.network.dto.sleep

data class CreateSleepRequestDto(
    val sleep_start: String,
    val sleep_end: String,
    val quality_score: Int? = null,
    val notes: String? = null,
    val source: String = "manual"
)