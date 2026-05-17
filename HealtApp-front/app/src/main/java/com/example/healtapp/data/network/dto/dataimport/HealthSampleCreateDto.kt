package com.example.healtapp.data.network.dto.dataimport

data class HealthSampleCreateDto(
    val recorded_at: String,
    val period_end: String? = null,
    val metric: String,
    val value1: Double? = null,
    val value2: Double? = null,
    val text_value: String? = null,
    val source: String = "health_connect",
)
