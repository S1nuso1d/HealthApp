package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.social.FeedPostCreateDto
import com.example.healtapp.data.network.dto.social.MediaUploadResponseDto
import com.example.healtapp.data.network.dto.social.FeedReactionRequestDto
import com.example.healtapp.data.network.dto.social.FeedReactionResultDto
import com.example.healtapp.data.network.dto.social.FeedResponseDto
import com.example.healtapp.data.network.dto.social.FeedCommentsResponseDto
import com.example.healtapp.data.network.dto.social.FeedCommentCreateDto
import com.example.healtapp.data.network.dto.social.FeedCommentDto
import com.example.healtapp.data.network.dto.social.FeedStoriesResponseDto
import com.example.healtapp.data.network.dto.social.StoryViewResultDto
import com.example.healtapp.data.network.dto.social.LinkableActivitiesResponseDto
import com.example.healtapp.data.network.dto.social.FriendProfileResponseDto
import com.example.healtapp.data.network.dto.social.FriendRequestDto
import com.example.healtapp.data.network.dto.social.FriendsListResponseDto
import com.example.healtapp.data.network.dto.social.PendingFriendsResponseDto
import com.example.healtapp.data.network.dto.social.PrivacySettingsDto
import com.example.healtapp.data.network.dto.social.PrivacyUpdateDto
import com.example.healtapp.data.network.dto.social.UsersSearchResponseDto
import com.example.healtapp.data.network.dto.social.WeeklyChallengeResponseDto
import com.example.healtapp.data.network.dto.social.ChallengeLeaderboardResponseDto
import com.example.healtapp.data.network.dto.social.ChallengeResponseDto
import com.example.healtapp.data.network.dto.social.ClubCreateDto
import com.example.healtapp.data.network.dto.social.ClubResponseDto
import com.example.healtapp.data.network.dto.social.ClubMemberResponseDto
import com.example.healtapp.data.network.dto.social.ClubPostCreateDto
import com.example.healtapp.data.network.dto.social.ClubPostResponseDto
import com.example.healtapp.data.network.dto.social.ClubPollVoteDto
import com.example.healtapp.data.network.dto.social.ClubUpdateDto
import com.example.healtapp.data.network.dto.social.ClubMemberRoleUpdateDto
import com.example.healtapp.data.network.dto.social.ClubNotificationDto
import retrofit2.http.PATCH
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SocialApi {
    @GET("social/challenges")
    suspend fun getChallenges(): List<ChallengeResponseDto>

    @POST("social/challenges/{id}/join")
    suspend fun joinChallenge(@Path("id") challengeId: Int): Map<String, String>

    @POST("social/challenges/{id}/leave")
    suspend fun leaveChallenge(@Path("id") challengeId: Int): Map<String, String>

    @GET("social/challenges/{id}/leaderboard")
    suspend fun getChallengeLeaderboard(@Path("id") challengeId: Int): ChallengeLeaderboardResponseDto

    @GET("social/privacy")
    suspend fun getPrivacy(): PrivacySettingsDto

    @PUT("social/privacy")
    suspend fun updatePrivacy(@Body body: PrivacyUpdateDto): PrivacySettingsDto

    @GET("social/users/search")
    suspend fun searchUsers(@Query("q") query: String): UsersSearchResponseDto

    @GET("social/friends")
    suspend fun listFriends(): FriendsListResponseDto

    @GET("social/friends/pending")
    suspend fun pendingFriends(): PendingFriendsResponseDto

    @POST("social/friends/request")
    suspend fun requestFriend(@Body body: FriendRequestDto): Map<String, String>

    @POST("social/friends/{id}/accept")
    suspend fun acceptFriend(@Path("id") friendshipId: Int): Map<String, String>

    @POST("social/friends/{id}/decline")
    suspend fun declineFriend(@Path("id") friendshipId: Int): Map<String, String>

    @DELETE("social/friends/{userId}")
    suspend fun removeFriend(@Path("userId") userId: Int): Map<String, String>

    @GET("social/feed")
    suspend fun getFeed(): FeedResponseDto

    @POST("social/feed")
    suspend fun createPost(@Body body: FeedPostCreateDto): Map<String, Any>

    @retrofit2.http.Multipart
    @POST("social/feed/media")
    suspend fun uploadPostMedia(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): MediaUploadResponseDto

    @POST("social/stories")
    suspend fun createStory(@Body body: com.example.healtapp.data.network.dto.social.StoryCreateDto): com.example.healtapp.data.network.dto.social.FeedStoryItemDto

    @GET("social/stories")
    suspend fun getStories(): FeedStoriesResponseDto

    @POST("social/stories/{storyId}/view")
    suspend fun recordStoryView(@Path("storyId") storyId: Int): StoryViewResultDto

    @GET("social/activities/linkable")
    suspend fun getLinkableActivities(): LinkableActivitiesResponseDto

    @POST("social/feed/{postId}/reactions")
    suspend fun toggleReaction(
        @Path("postId") postId: Int,
        @Body body: FeedReactionRequestDto,
    ): FeedReactionResultDto

    @GET("social/feed/{postId}/comments")
    suspend fun getPostComments(@Path("postId") postId: Int): FeedCommentsResponseDto

    @POST("social/feed/{postId}/comments")
    suspend fun createPostComment(
        @Path("postId") postId: Int,
        @Body body: FeedCommentCreateDto,
    ): FeedCommentDto

    @DELETE("social/feed/{postId}")
    suspend fun deletePost(@Path("postId") postId: Int): Map<String, String>

    @GET("social/users/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: Int): FriendProfileResponseDto

    @POST("social/users/{userId}/block")
    suspend fun blockUser(@Path("userId") userId: Int): Map<String, String>

    @DELETE("social/users/{userId}/block")
    suspend fun unblockUser(@Path("userId") userId: Int): Map<String, String>

    @GET("social/challenges/weekly")
    suspend fun getWeeklyChallenge(): WeeklyChallengeResponseDto

    @GET("social/clubs")
    suspend fun getClubs(): List<ClubResponseDto>

    @POST("social/clubs")
    suspend fun createClub(@Body body: ClubCreateDto): ClubResponseDto

    @GET("social/clubs/{id}")
    suspend fun getClub(@Path("id") clubId: Int): ClubResponseDto

    @POST("social/clubs/{id}/join")
    suspend fun joinClub(@Path("id") clubId: Int): Map<String, String>

    @POST("social/clubs/{id}/leave")
    suspend fun leaveClub(@Path("id") clubId: Int): Map<String, String>

    @GET("social/clubs/{id}/members")
    suspend fun getClubMembers(@Path("id") clubId: Int): List<ClubMemberResponseDto>

    @PUT("social/clubs/{id}")
    suspend fun updateClub(@Path("id") clubId: Int, @Body body: ClubUpdateDto): ClubResponseDto

    @PATCH("social/clubs/{id}/members/{userId}")
    suspend fun updateClubMemberRole(
        @Path("id") clubId: Int,
        @Path("userId") userId: Int,
        @Body body: ClubMemberRoleUpdateDto,
    ): Map<String, String>

    @DELETE("social/clubs/{id}/members/{userId}")
    suspend fun removeClubMember(
        @Path("id") clubId: Int,
        @Path("userId") userId: Int,
    ): Map<String, String>

    @GET("social/clubs/notifications/recent")
    suspend fun getClubNotificationsRecent(): List<ClubNotificationDto>

    @POST("social/clubs/notifications/{id}/read")
    suspend fun markClubNotificationRead(@Path("id") notificationId: Int): Map<String, String>

    @GET("social/clubs/{id}/posts")
    suspend fun getClubPosts(@Path("id") clubId: Int): List<ClubPostResponseDto>

    @POST("social/clubs/{id}/posts")
    suspend fun createClubPost(@Path("id") clubId: Int, @Body body: ClubPostCreateDto): ClubPostResponseDto

    @POST("social/clubs/{id}/posts/{postId}/vote")
    suspend fun voteClubPoll(
        @Path("id") clubId: Int,
        @Path("postId") postId: Int,
        @Body body: ClubPollVoteDto,
    ): Map<String, String>
}
