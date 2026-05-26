package com.example.healtapp.data.network.api

import com.example.healtapp.data.network.dto.social.FeedPostCreateDto
import com.example.healtapp.data.network.dto.social.FeedResponseDto
import com.example.healtapp.data.network.dto.social.FriendProfileResponseDto
import com.example.healtapp.data.network.dto.social.FriendRequestDto
import com.example.healtapp.data.network.dto.social.FriendsListResponseDto
import com.example.healtapp.data.network.dto.social.PendingFriendsResponseDto
import com.example.healtapp.data.network.dto.social.PrivacySettingsDto
import com.example.healtapp.data.network.dto.social.PrivacyUpdateDto
import com.example.healtapp.data.network.dto.social.UsersSearchResponseDto
import com.example.healtapp.data.network.dto.social.WeeklyChallengeResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SocialApi {
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

    @GET("social/feed")
    suspend fun getFeed(): FeedResponseDto

    @POST("social/feed")
    suspend fun createPost(@Body body: FeedPostCreateDto): Map<String, Any>

    @GET("social/users/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: Int): FriendProfileResponseDto

    @GET("social/challenges/weekly")
    suspend fun getWeeklyChallenge(): WeeklyChallengeResponseDto
}
