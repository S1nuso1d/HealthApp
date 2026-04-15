package com.example.healtapp.data.network.dto.activity

data class ActivityDto(
    val id: Int,
    val user_id: Int,
    val activity_type: String,
    val start_time: String,
    val end_time: String,
    val duration_minutes: Int,
    val steps: Int? = null,
    val distance_km: Float? = null,
    val calories_burned: Float? = null,
    val avg_heart_rate: Int? = null,
    val intensity: String? = null,
    val activity_category: String? = null,
    val perceived_exertion: Int? = null,
    val minutes_before_sleep: Int? = null,
    val is_evening_activity: Boolean? = null,
    val notes: String? = null,
    val source: String,
    val created_at: String? = null
)