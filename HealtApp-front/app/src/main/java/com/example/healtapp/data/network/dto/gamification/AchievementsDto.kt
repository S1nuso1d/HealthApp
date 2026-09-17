package com.example.healtapp.data.network.dto.gamification

data class AchievementsResponseDto(
    val total_points: Int = 0,
    val unlocked_count: Int = 0,
    val total_count: Int = 0,
    val season_points: Int = 0,
    val season_label: String? = null,
    val tier: AchievementTierDto? = null,
    val point_rules: List<AchievementPointRuleDto> = emptyList(),
    val tiers: List<AchievementTierDefDto> = emptyList(),
    val leaderboard: List<AchievementLeaderboardEntryDto> = emptyList(),
    val achievements: List<AchievementItemDto> = emptyList(),
    val recent: List<AchievementRecentDto> = emptyList(),
)

data class AchievementTierDto(
    val code: String = "bronze",
    val title: String = "Бронза",
    val min_points: Int = 0,
    val next_code: String? = null,
    val next_title: String? = null,
    val points_to_next: Int? = null,
)

data class AchievementTierDefDto(
    val code: String = "",
    val title: String = "",
    val min_points: Int = 0,
)

data class AchievementPointRuleDto(
    val code: String = "",
    val title: String = "",
    val points: Int = 0,
    val unit: String? = null,
)

data class AchievementLeaderboardEntryDto(
    val rank: Int = 0,
    val user_id: Int = 0,
    val display_name: String = "",
    val season_points: Int = 0,
    val tier_code: String = "bronze",
    val tier_title: String = "Бронза",
    val is_self: Boolean = false,
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
