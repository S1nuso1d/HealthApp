package com.example.healtapp.data.network.dto.health

data class HealthSampleDto(
    val id: Int,
    val user_id: Int,
    val recorded_at: String,
    val period_end: String? = null,
    val metric: String,
    val value1: Double? = null,
    val value2: Double? = null,
    val text_value: String? = null,
    val source: String,
    val created_at: String? = null,
)
