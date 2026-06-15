package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.social.FeedActivityDto
import com.example.healtapp.data.network.dto.social.FeedPostCreateDto
import com.example.healtapp.data.network.dto.social.FeedReactionResultDto
import com.example.healtapp.data.network.dto.social.FeedResponseDto
import com.example.healtapp.data.network.dto.social.FeedCommentsResponseDto
import com.example.healtapp.data.network.dto.social.FeedCommentCreateDto
import com.example.healtapp.data.network.dto.social.FeedCommentDto
import com.example.healtapp.data.network.dto.social.FeedStoriesResponseDto
import com.example.healtapp.data.network.dto.social.FriendProfileResponseDto
import com.example.healtapp.data.network.dto.social.FriendsListResponseDto
import com.example.healtapp.data.network.dto.social.PendingFriendsResponseDto
import com.example.healtapp.data.network.dto.social.PrivacySettingsDto
import com.example.healtapp.data.network.dto.social.PrivacyUpdateDto
import com.example.healtapp.data.network.dto.social.UsersSearchResponseDto
import com.example.healtapp.data.network.dto.social.WeeklyChallengeResponseDto
import com.example.healtapp.data.network.dto.social.ChallengeResponseDto
import com.example.healtapp.data.network.dto.social.ChallengeLeaderboardResponseDto
import com.example.healtapp.data.network.dto.social.ClubCreateDto
import com.example.healtapp.data.network.dto.social.ClubResponseDto
import com.example.healtapp.data.network.dto.social.ClubMemberResponseDto
import com.example.healtapp.data.network.dto.social.ClubPostCreateDto
import com.example.healtapp.data.network.dto.social.ClubPostResponseDto
import com.example.healtapp.data.network.dto.social.ClubPollVoteDto
import com.example.healtapp.data.network.dto.social.ClubUpdateDto
import com.example.healtapp.data.network.dto.social.ClubMemberRoleUpdateDto

interface SocialRepository {
    suspend fun getChallenges(): Result<List<ChallengeResponseDto>>
    suspend fun joinChallenge(challengeId: Int): Result<Map<String, String>>
    suspend fun leaveChallenge(challengeId: Int): Result<Map<String, String>>
    suspend fun getChallengeLeaderboard(challengeId: Int): Result<ChallengeLeaderboardResponseDto>
    suspend fun getPrivacy(): Result<PrivacySettingsDto>
    suspend fun updatePrivacy(body: PrivacyUpdateDto): Result<PrivacySettingsDto>
    suspend fun searchUsers(query: String): Result<UsersSearchResponseDto>
    suspend fun listFriends(): Result<FriendsListResponseDto>
    suspend fun pendingFriends(): Result<PendingFriendsResponseDto>
    suspend fun requestFriend(userId: Int): Result<Map<String, String>>
    suspend fun acceptFriend(friendshipId: Int): Result<Map<String, String>>
    suspend fun declineFriend(friendshipId: Int): Result<Map<String, String>>
    suspend fun removeFriend(userId: Int): Result<Map<String, String>>
    suspend fun getFeed(): Result<FeedResponseDto>
    suspend fun getStories(): Result<FeedStoriesResponseDto>
    suspend fun createStory(body: com.example.healtapp.data.network.dto.social.StoryCreateDto): Result<com.example.healtapp.data.network.dto.social.FeedStoryItemDto>
    suspend fun getLinkableActivities(): Result<List<FeedActivityDto>>
    suspend fun toggleReaction(postId: Int, emoji: String): Result<FeedReactionResultDto>
    suspend fun getPostComments(postId: Int): Result<FeedCommentsResponseDto>
    suspend fun createPostComment(postId: Int, body: FeedCommentCreateDto): Result<FeedCommentDto>
    suspend fun createPost(body: FeedPostCreateDto): Result<Map<String, Any>>
    suspend fun deletePost(postId: Int): Result<Map<String, String>>
    suspend fun uploadPostMedia(file: okhttp3.MultipartBody.Part): Result<com.example.healtapp.data.network.dto.social.MediaUploadResponseDto>
    suspend fun getUserProfile(userId: Int): Result<FriendProfileResponseDto>
    suspend fun blockUser(userId: Int): Result<Map<String, String>>
    suspend fun unblockUser(userId: Int): Result<Map<String, String>>
    suspend fun getWeeklyChallenge(): Result<WeeklyChallengeResponseDto>

    suspend fun getClubs(): Result<List<ClubResponseDto>>
    suspend fun createClub(body: ClubCreateDto): Result<ClubResponseDto>
    suspend fun getClub(clubId: Int): Result<ClubResponseDto>
    suspend fun joinClub(clubId: Int): Result<Map<String, String>>
    suspend fun leaveClub(clubId: Int): Result<Map<String, String>>
    suspend fun getClubMembers(clubId: Int): Result<List<ClubMemberResponseDto>>
    suspend fun updateClub(clubId: Int, body: ClubUpdateDto): Result<ClubResponseDto>
    suspend fun updateClubMemberRole(clubId: Int, userId: Int, body: ClubMemberRoleUpdateDto): Result<Map<String, String>>
    suspend fun getClubPosts(clubId: Int): Result<List<ClubPostResponseDto>>
    suspend fun createClubPost(clubId: Int, body: ClubPostCreateDto): Result<ClubPostResponseDto>
    suspend fun voteClubPoll(clubId: Int, postId: Int, body: ClubPollVoteDto): Result<Map<String, String>>
}
