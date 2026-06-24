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
    val nickname: String? = null,
    val first_name: String? = null,
    val last_name: String? = null,
    val goal: String? = null,
    val age: Int? = null,
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

data class MediaUploadResponseDto(val url: String)

data class FeedReactionDto(val emoji: String, val count: Int = 0)

data class FeedActivityDto(
    val id: Int? = null,
    val activity_type: String?,
    val duration_minutes: Int?,
    val calories_burned: Float?,
    val steps: Int?,
    val distance_km: Float? = null,
    val start_time: String? = null,
)

data class FeedPostDto(
    val id: Int,
    val author: UserCardDto,
    val body: String? = null,
    val media_url: String? = null,
    val media_type: String? = null,
    val activity: FeedActivityDto? = null,
    val activity_id: Int? = null,
    val created_at: String? = null,
    val reactions: List<FeedReactionDto> = emptyList(),
    val reaction_total: Int = 0,
    val my_reaction: String? = null,
    val comments_count: Int = 0,
)

data class FeedCommentDto(
    val id: Int,
    val post_id: Int,
    val author: UserCardDto,
    val text: String,
    val created_at: String? = null,
)

data class FeedCommentsResponseDto(val comments: List<FeedCommentDto> = emptyList())

data class FeedCommentCreateDto(val text: String)

data class FeedResponseDto(val posts: List<FeedPostDto> = emptyList())

data class StoryCreateDto(
    val media_url: String
)

data class FeedStoryItemDto(
    val id: Int,
    val media_url: String,
    val created_at: String? = null,
    val expires_at: String? = null,
    val view_count: Int = 0,
)

data class StoryViewResultDto(val view_count: Int = 0)

data class FeedStoryDto(
    val author: UserCardDto,
    val preview_url: String?,
    val has_unseen: Boolean = false,
    val items: List<FeedStoryItemDto> = emptyList(),
)

data class FeedStoriesResponseDto(val stories: List<FeedStoryDto> = emptyList())

data class LinkableActivitiesResponseDto(val activities: List<FeedActivityDto> = emptyList())

data class FeedReactionRequestDto(val emoji: String)

data class FeedReactionResultDto(
    val status: String,
    val my_reaction: String? = null,
)

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
    val posts: List<FeedPostDto> = emptyList(),
    val is_friend: Boolean = false,
    val is_blocked: Boolean = false,
)

data class ChallengeResponseDto(
    val id: Int,
    val title: String,
    val description: String?,
    val challenge_type: String,
    val target_value: Int,
    val is_group: Boolean,
    val creator_id: Int,
    val start_date: String,
    val end_date: String,
    val participants_count: Int,
    val my_progress: Int? = null,
)

data class ChallengeLeaderboardEntryDto(
    val user_id: Int,
    val display_name: String,
    val progress: Int,
    val is_me: Boolean,
    val rank: Int,
)

data class ChallengeLeaderboardResponseDto(
    val entries: List<ChallengeLeaderboardEntryDto> = emptyList()
)

data class ClubCreateDto(
    val name: String,
    val description: String? = null,
    val avatar_url: String? = null,
    val rules: String? = null
)

data class ClubResponseDto(
    val id: Int,
    val name: String,
    val description: String?,
    val avatar_url: String?,
    val rules: String?,
    val creator_id: Int,
    val members_count: Int,
    val is_member: Boolean
)

data class ClubMemberResponseDto(
    val id: Int,
    val club_id: Int,
    val user: UserCardDto,
    val role: String,
    val joined_at: String
)

data class ClubUpdateDto(
    val name: String? = null,
    val description: String? = null,
    val avatar_url: String? = null,
    val rules: String? = null,
)

data class ClubMemberRoleUpdateDto(val role: String)

data class ClubNotificationDto(
    val id: Int,
    val club_id: Int,
    val event_type: String,
    val title: String,
    val message: String,
    val club_name: String,
    val created_at: String,
)

data class ClubPostCreateDto(
    val post_type: String = "discussion",
    val body: String? = null,
    val poll_options: List<String>? = null,
)

data class ClubPostResponseDto(
    val id: Int,
    val club_id: Int,
    val user: UserCardDto,
    val post_type: String,
    val body: String?,
    val poll_options: List<String>? = null,
    val poll_votes: Map<String, Int>? = null,
    val my_vote: String? = null,
    val created_at: String,
)

data class ClubPollVoteDto(val option: String)
