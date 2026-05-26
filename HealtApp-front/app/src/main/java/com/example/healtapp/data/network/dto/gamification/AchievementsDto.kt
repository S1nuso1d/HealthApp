package com.example.healtapp.data.network.dto.gamification

data class AchievementsResponseDto(
    val total_points: Int = 0,
    val unlocked_count: Int = 0,
    val total_count: Int = 0,
    val achievements: List<AchievementItemDto> = emptyList(),
    val recent: List<AchievementRecentDto> = emptyList(),
)

data class AchievementItemDto(
    val code: String,
    val title: String,
    val description: String,
    val icon_key: String,
    val points: Int,
    val unlocked: Boolean,
    val unlocked_at: String? = null,
    val kind: String = "daily",
    val progress_current: Float = 0f,
    val progress_target: Float = 0f,
    val progress_unit: String? = null,
    val record_value: Float? = null,
    val record_label: String? = null,
)

data class AchievementRecentDto(
    val code: String,
    val title: String,
    val description: String,
    val icon_key: String,
    val points: Int,
    val unlocked_at: String? = null,
    val kind: String = "daily",
    val record_label: String? = null,
)
