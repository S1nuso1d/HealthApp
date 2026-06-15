package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.SocialApi
import com.example.healtapp.data.network.dto.social.FeedPostCreateDto
import com.example.healtapp.data.network.dto.social.FeedCommentCreateDto
import com.example.healtapp.data.network.dto.social.PrivacyUpdateDto
import com.example.healtapp.data.network.dto.social.ClubCreateDto
import com.example.healtapp.data.network.dto.social.ClubMemberRoleUpdateDto
import com.example.healtapp.data.network.dto.social.ClubPollVoteDto
import com.example.healtapp.data.network.dto.social.ClubPostCreateDto
import com.example.healtapp.data.network.dto.social.ClubUpdateDto
import com.example.healtapp.domain.repository.SocialRepository
import javax.inject.Inject

class SocialRepositoryImpl @Inject constructor(
    private val api: SocialApi,
) : SocialRepository {
    override suspend fun getChallenges() = runCatching { api.getChallenges() }
    override suspend fun joinChallenge(challengeId: Int) = runCatching { api.joinChallenge(challengeId) }
    override suspend fun leaveChallenge(challengeId: Int) = runCatching { api.leaveChallenge(challengeId) }
    override suspend fun getChallengeLeaderboard(challengeId: Int) = runCatching { api.getChallengeLeaderboard(challengeId) }
    override suspend fun getPrivacy() = runCatching { api.getPrivacy() }
    override suspend fun updatePrivacy(body: PrivacyUpdateDto) = runCatching { api.updatePrivacy(body) }
    override suspend fun searchUsers(query: String) = runCatching { api.searchUsers(query) }
    override suspend fun listFriends() = runCatching { api.listFriends() }
    override suspend fun pendingFriends() = runCatching { api.pendingFriends() }
    override suspend fun requestFriend(userId: Int) =
        runCatching { api.requestFriend(com.example.healtapp.data.network.dto.social.FriendRequestDto(userId)) }
    override suspend fun acceptFriend(friendshipId: Int) = runCatching { api.acceptFriend(friendshipId) }
    override suspend fun declineFriend(friendshipId: Int) = runCatching { api.declineFriend(friendshipId) }
    override suspend fun removeFriend(userId: Int) = runCatching { api.removeFriend(userId) }
    override suspend fun getFeed() = runCatching { api.getFeed() }
    override suspend fun getStories() = runCatching { api.getStories() }
    override suspend fun createStory(body: com.example.healtapp.data.network.dto.social.StoryCreateDto) = runCatching { api.createStory(body) }
    override suspend fun getLinkableActivities() =
        runCatching { api.getLinkableActivities().activities }
    override suspend fun toggleReaction(postId: Int, emoji: String) =
        runCatching {
            api.toggleReaction(
                postId,
                com.example.healtapp.data.network.dto.social.FeedReactionRequestDto(emoji),
            )
        }
    override suspend fun getPostComments(postId: Int) = runCatching { api.getPostComments(postId) }
    override suspend fun createPostComment(postId: Int, body: FeedCommentCreateDto) = runCatching { api.createPostComment(postId, body) }
    override suspend fun createPost(body: FeedPostCreateDto) = runCatching { api.createPost(body) }
    override suspend fun deletePost(postId: Int) = runCatching { api.deletePost(postId) }
    override suspend fun uploadPostMedia(file: okhttp3.MultipartBody.Part) = runCatching { api.uploadPostMedia(file) }
    override suspend fun getUserProfile(userId: Int) = runCatching { api.getUserProfile(userId) }
    override suspend fun blockUser(userId: Int) = runCatching { api.blockUser(userId) }
    override suspend fun unblockUser(userId: Int) = runCatching { api.unblockUser(userId) }
    override suspend fun getWeeklyChallenge() = runCatching { api.getWeeklyChallenge() }
    
    override suspend fun getClubs() = runCatching { api.getClubs() }
    override suspend fun createClub(body: ClubCreateDto) = runCatching { api.createClub(body) }
    override suspend fun getClub(clubId: Int) = runCatching { api.getClub(clubId) }
    override suspend fun joinClub(clubId: Int) = runCatching { api.joinClub(clubId) }
    override suspend fun leaveClub(clubId: Int) = runCatching { api.leaveClub(clubId) }
    override suspend fun getClubMembers(clubId: Int) = runCatching { api.getClubMembers(clubId) }
    override suspend fun updateClub(clubId: Int, body: ClubUpdateDto) = runCatching { api.updateClub(clubId, body) }
    override suspend fun updateClubMemberRole(clubId: Int, userId: Int, body: ClubMemberRoleUpdateDto) =
        runCatching { api.updateClubMemberRole(clubId, userId, body) }
    override suspend fun getClubPosts(clubId: Int) = runCatching { api.getClubPosts(clubId) }
    override suspend fun createClubPost(clubId: Int, body: ClubPostCreateDto) = runCatching { api.createClubPost(clubId, body) }
    override suspend fun voteClubPoll(clubId: Int, postId: Int, body: ClubPollVoteDto) =
        runCatching { api.voteClubPoll(clubId, postId, body) }
}
