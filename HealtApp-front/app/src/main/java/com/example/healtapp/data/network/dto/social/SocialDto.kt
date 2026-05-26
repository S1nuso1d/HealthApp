package com.example.healtapp.data.network.dto.social

data class PrivacySettingsDto(
    val profile_visibility: String = "friends",
    val feed_visibility: String = "friends",
    val show_activity_to_friends: Boolean = true,
    val show_achievements_to_friends: Boolean = true,
)

data class PrivacyUpdateDto(
    val profile_visibility: String,
    val feed_visibility: String,
    val show_activity_to_friends: Boolean,
    val show_achievements_to_friends: Boolean,
)

data class UserCardDto(
    val user_id: Int,
    val display_name: String,
    val goal: String? = null,
    val has_avatar: Boolean = false,
    val is_self: Boolean = false,
)

data class UsersSearchResponseDto(val users: List<UserCardDto> = emptyList())
data class FriendsListResponseDto(val friends: List<UserCardDto> = emptyList())

data class PendingFriendDto(
    val user_id: Int,
    val display_name: String,
    val goal: String? = null,
    val has_avatar: Boolean = false,
    val is_self: Boolean = false,
    val friendship_id: Int,
)

data class PendingFriendsResponseDto(val incoming: List<PendingFriendDto> = emptyList())

data class FriendRequestDto(val user_id: Int)

data class WeeklyChallengeEntryDto(
    val user_id: Int,
    val display_name: String,
    val steps: Int = 0,
    val is_me: Boolean = false,
    val rank: Int = 0,
)

data class WeeklyChallengeResponseDto(
    val metric: String = "steps",
    val period: String = "week",
    val entries: List<WeeklyChallengeEntryDto> = emptyList(),
)

data class FeedPostCreateDto(
    val body: String? = null,
    val media_url: String? = null,
    val media_type: String? = null,
    val activity_id: Int? = null,
    val visibility: String = "friends",
)

data class FeedActivityDto(
    val activity_type: String?,
    val duration_minutes: Int?,
    val calories_burned: Float?,
    val steps: Int?,
)

data class FeedPostDto(
    val id: Int,
    val author: UserCardDto,
    val body: String? = null,
    val media_url: String? = null,
    val media_type: String? = null,
    val activity: FeedActivityDto? = null,
    val created_at: String? = null,
)

data class FeedResponseDto(val posts: List<FeedPostDto> = emptyList())

data class FriendActivityDto(
    val id: Int,
    val activity_type: String?,
    val duration_minutes: Int?,
    val calories_burned: Float?,
    val steps: Int?,
    val start_time: String? = null,
)

data class FriendAchievementDto(
    val code: String,
    val title: String,
    val icon_key: String,
    val points: Int,
    val unlocked_at: String? = null,
)

data class FriendProfileResponseDto(
    val user: UserCardDto,
    val activities: List<FriendActivityDto> = emptyList(),
    val achievements: List<FriendAchievementDto> = emptyList(),
    val is_friend: Boolean = false,
)
