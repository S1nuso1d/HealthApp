package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.SocialApi
import com.example.healtapp.data.network.dto.social.FeedPostCreateDto
import com.example.healtapp.data.network.dto.social.PrivacyUpdateDto
import com.example.healtapp.domain.repository.SocialRepository
import javax.inject.Inject

class SocialRepositoryImpl @Inject constructor(
    private val api: SocialApi,
) : SocialRepository {
    override suspend fun getPrivacy() = runCatching { api.getPrivacy() }
    override suspend fun updatePrivacy(body: PrivacyUpdateDto) = runCatching { api.updatePrivacy(body) }
    override suspend fun searchUsers(query: String) = runCatching { api.searchUsers(query) }
    override suspend fun listFriends() = runCatching { api.listFriends() }
    override suspend fun pendingFriends() = runCatching { api.pendingFriends() }
    override suspend fun requestFriend(userId: Int) =
        runCatching { api.requestFriend(com.example.healtapp.data.network.dto.social.FriendRequestDto(userId)) }
    override suspend fun acceptFriend(friendshipId: Int) = runCatching { api.acceptFriend(friendshipId) }
    override suspend fun getFeed() = runCatching { api.getFeed() }
    override suspend fun createPost(body: FeedPostCreateDto) = runCatching { api.createPost(body) }
    override suspend fun getUserProfile(userId: Int) = runCatching { api.getUserProfile(userId) }
    override suspend fun getWeeklyChallenge() = runCatching { api.getWeeklyChallenge() }
}
