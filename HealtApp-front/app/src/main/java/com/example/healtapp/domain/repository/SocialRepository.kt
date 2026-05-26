package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.social.FeedPostCreateDto
import com.example.healtapp.data.network.dto.social.FeedResponseDto
import com.example.healtapp.data.network.dto.social.FriendProfileResponseDto
import com.example.healtapp.data.network.dto.social.FriendsListResponseDto
import com.example.healtapp.data.network.dto.social.PendingFriendsResponseDto
import com.example.healtapp.data.network.dto.social.PrivacySettingsDto
import com.example.healtapp.data.network.dto.social.PrivacyUpdateDto
import com.example.healtapp.data.network.dto.social.UsersSearchResponseDto
import com.example.healtapp.data.network.dto.social.WeeklyChallengeResponseDto

interface SocialRepository {
    suspend fun getPrivacy(): Result<PrivacySettingsDto>
    suspend fun updatePrivacy(body: PrivacyUpdateDto): Result<PrivacySettingsDto>
    suspend fun searchUsers(query: String): Result<UsersSearchResponseDto>
    suspend fun listFriends(): Result<FriendsListResponseDto>
    suspend fun pendingFriends(): Result<PendingFriendsResponseDto>
    suspend fun requestFriend(userId: Int): Result<Map<String, String>>
    suspend fun acceptFriend(friendshipId: Int): Result<Map<String, String>>
    suspend fun getFeed(): Result<FeedResponseDto>
    suspend fun createPost(body: FeedPostCreateDto): Result<Map<String, Any>>
    suspend fun getUserProfile(userId: Int): Result<FriendProfileResponseDto>
    suspend fun getWeeklyChallenge(): Result<WeeklyChallengeResponseDto>
}
