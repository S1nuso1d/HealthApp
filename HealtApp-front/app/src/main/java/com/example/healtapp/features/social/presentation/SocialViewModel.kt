package com.example.healtapp.features.social.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.ApiServerConfig
import com.example.healtapp.data.network.dto.social.ClubPostResponseDto
import com.example.healtapp.data.network.dto.social.FeedActivityDto
import com.example.healtapp.data.network.dto.social.FeedPostCreateDto
import com.example.healtapp.data.network.dto.social.FeedPostDto
import com.example.healtapp.data.network.dto.social.FeedCommentDto
import com.example.healtapp.data.network.dto.social.FeedCommentCreateDto
import com.example.healtapp.data.network.dto.social.FeedReactionDto
import com.example.healtapp.data.network.dto.social.FeedStoryDto
import com.example.healtapp.data.network.dto.social.PendingFriendDto
import com.example.healtapp.data.network.dto.social.PrivacyUpdateDto
import com.example.healtapp.data.network.dto.social.UserCardDto
import com.example.healtapp.data.network.dto.social.WeeklyChallengeEntryDto
import com.example.healtapp.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

val COMMUNITY_REACTIONS = listOf("👍", "❤️", "🔥", "👏", "😊")

data class SocialUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0,
    val feed: List<FeedPostDto> = emptyList(),
    val stories: List<FeedStoryDto> = emptyList(),
    val linkableActivities: List<FeedActivityDto> = emptyList(),
    val selectedActivityId: Int? = null,
    val friends: List<UserCardDto> = emptyList(),
    val pending: List<PendingFriendDto> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<UserCardDto> = emptyList(),
    val newPostText: String = "",
    val selectedMediaUri: android.net.Uri? = null,
    val showComposeSheet: Boolean = false,
    val showStoryComposeSheet: Boolean = false,
    val storyViewerIndex: Int? = null,
    val profileVisibility: String = "friends",
    val feedVisibility: String = "friends",
    val showActivity: Boolean = true,
    val showAchievements: Boolean = true,
    val message: String? = null,
    val weeklyChallenge: List<WeeklyChallengeEntryDto> = emptyList(),
    val selectedPostForComments: Int? = null,
    val selectedPostForDelete: FeedPostDto? = null,
    val postComments: Map<Int, List<FeedCommentDto>> = emptyMap(),
    val challenges: List<com.example.healtapp.data.network.dto.social.ChallengeResponseDto> = emptyList(),
    val challengeLeaderboards: Map<Int, List<com.example.healtapp.data.network.dto.social.ChallengeLeaderboardEntryDto>> = emptyMap(),
    val clubs: List<com.example.healtapp.data.network.dto.social.ClubResponseDto> = emptyList(),
    val clubFeedEntries: List<ClubFeedEntry> = emptyList(),
    val clubMembers: Map<Int, List<com.example.healtapp.data.network.dto.social.ClubMemberResponseDto>> = emptyMap(),
    val isSearching: Boolean = false,
    val searchHint: String? = null,
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val apiServerConfig: ApiServerConfig,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun updateSearchQuery(q: String) {
        val trimmed = q.trim()
        _uiState.update {
            it.copy(
                searchQuery = q,
                searchHint = null,
                searchResults = if (trimmed.length < 2) emptyList() else it.searchResults,
            )
        }
    }

    fun updateNewPostText(t: String) {
        _uiState.update { it.copy(newPostText = t) }
    }

    fun updateSelectedMediaUri(uri: android.net.Uri?) {
        _uiState.update { it.copy(selectedMediaUri = uri) }
    }

    fun setShowComposeSheet(show: Boolean) {
        _uiState.update { it.copy(showComposeSheet = show) }
        if (show && _uiState.value.linkableActivities.isEmpty()) {
            loadLinkableActivities()
        }
    }

    fun setShowStoryComposeSheet(show: Boolean) {
        _uiState.update { it.copy(showStoryComposeSheet = show) }
    }

    fun openStoryViewer(index: Int) {
        _uiState.update { it.copy(storyViewerIndex = index) }
    }

    fun closeStoryViewer() {
        _uiState.update { it.copy(storyViewerIndex = null) }
    }

    fun markStoryViewed(storyId: Int) {
        viewModelScope.launch {
            repository.recordStoryView(storyId).onSuccess { result ->
                _uiState.update { state ->
                    state.copy(
                        stories = state.stories.map { group ->
                            if (!group.author.is_self) return@map group
                            group.copy(
                                items = group.items.map { item ->
                                    if (item.id == storyId) item.copy(view_count = result.view_count) else item
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    fun selectActivityForPost(activityId: Int?) {
        _uiState.update {
            it.copy(selectedActivityId = if (it.selectedActivityId == activityId) null else activityId)
        }
    }

    fun setProfileVisibility(v: String) {
        _uiState.update { it.copy(profileVisibility = v) }
    }

    fun setFeedVisibility(v: String) {
        _uiState.update { it.copy(feedVisibility = v) }
    }

    fun setShowActivity(v: Boolean) {
        _uiState.update { it.copy(showActivity = v) }
    }

    fun setShowAchievements(v: Boolean) {
        _uiState.update { it.copy(showAchievements = v) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            val wasLoaded = !_uiState.value.isLoading && _uiState.value.feed.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !wasLoaded,
                    isRefreshing = wasLoaded,
                    error = null,
                )
            }
            val privacy = repository.getPrivacy().getOrNull()
            val feedResult = repository.getFeed()
            val storiesResult = repository.getStories()
            val friendsResult = repository.listFriends()
            val pendingResult = repository.pendingFriends()
            val activitiesResult = repository.getLinkableActivities()
            val challengesResult = repository.getChallenges()
            val clubsResult = repository.getClubs()
            
            val err = feedResult.exceptionOrNull()
                ?: friendsResult.exceptionOrNull()
                ?: pendingResult.exceptionOrNull()
            if (err != null) {
                setError(err)
                return@launch
            }
            val feed = feedResult.getOrThrow()
            val friends = friendsResult.getOrThrow()
            val pending = pendingResult.getOrThrow()
            val challenge = repository.getWeeklyChallenge().getOrNull()?.entries.orEmpty()
            val challengesList = challengesResult.getOrNull().orEmpty()
            val clubsList = clubsResult.getOrNull().orEmpty()
            val clubFeedEntries = loadClubFeedEntries(clubsList)
            
            _uiState.value = SocialUiState(
                isLoading = false,
                isRefreshing = false,
                feed = feed.posts,
                stories = filterActiveStories(storiesResult.getOrNull()?.stories.orEmpty()),
                linkableActivities = activitiesResult.getOrNull().orEmpty(),
                friends = friends.friends,
                pending = pending.incoming,
                weeklyChallenge = challenge,
                challenges = challengesList,
                clubs = clubsList,
                clubFeedEntries = clubFeedEntries,
                profileVisibility = privacy?.profile_visibility ?: "friends",
                feedVisibility = privacy?.feed_visibility ?: "friends",
                showActivity = privacy?.show_activity_to_friends ?: true,
                showAchievements = privacy?.show_achievements_to_friends ?: true,
            )
            
            challengesList.filter { it.my_progress != null }.forEach { loadChallengeLeaderboard(it.id) }
        }
    }

    private fun loadChallengeLeaderboard(challengeId: Int) {
        viewModelScope.launch {
            repository.getChallengeLeaderboard(challengeId).onSuccess { res ->
                _uiState.update { it.copy(challengeLeaderboards = it.challengeLeaderboards + (challengeId to res.entries)) }
            }
        }
    }

    fun joinChallenge(challengeId: Int) {
        viewModelScope.launch {
            repository.joinChallenge(challengeId).onSuccess {
                refresh()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun leaveChallenge(challengeId: Int) {
        viewModelScope.launch {
            repository.leaveChallenge(challengeId).onSuccess {
                refresh()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun joinClub(clubId: Int) {
        viewModelScope.launch {
            repository.joinClub(clubId).onSuccess {
                refresh()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun leaveClub(clubId: Int) {
        viewModelScope.launch {
            repository.leaveClub(clubId).onSuccess {
                refresh()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun loadClubMembers(clubId: Int) {
        viewModelScope.launch {
            repository.getClubMembers(clubId).onSuccess { members ->
                _uiState.update { it.copy(clubMembers = it.clubMembers + (clubId to members)) }
            }
        }
    }

    private fun loadLinkableActivities() {
        viewModelScope.launch {
            repository.getLinkableActivities()
                .onSuccess { list -> _uiState.update { it.copy(linkableActivities = list) } }
        }
    }

    private fun setError(e: Throwable) {
        _uiState.update {
            it.copy(isLoading = false, isRefreshing = false, error = e.message ?: "Ошибка загрузки")
        }
    }

    fun search() {
        val q = _uiState.value.searchQuery.trim()
        if (q.length < 2) {
            _uiState.update { it.copy(searchHint = "Введите минимум 2 символа") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchHint = null, error = null) }
            repository.searchUsers(q)
                .onSuccess { r ->
                    _uiState.update {
                        it.copy(isSearching = false, searchResults = r.users, error = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchResults = emptyList(),
                            error = e.message ?: "Ошибка поиска",
                        )
                    }
                }
        }
    }

    fun createClub(name: String, description: String?, rules: String?) {
        val trimmed = name.trim()
        if (trimmed.length < 3) {
            _uiState.update { it.copy(error = "Название клуба — минимум 3 символа") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.createClub(
                com.example.healtapp.data.network.dto.social.ClubCreateDto(
                    name = trimmed,
                    description = description?.trim()?.takeIf { it.isNotBlank() },
                    rules = rules?.trim()?.takeIf { it.isNotBlank() },
                ),
            ).onSuccess {
                _uiState.update { it.copy(message = "Клуб «$trimmed» создан", isLoading = false) }
                refresh()
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun requestFriend(userId: Int) {
        viewModelScope.launch {
            repository.requestFriend(userId)
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun acceptFriend(friendshipId: Int) {
        viewModelScope.launch {
            repository.acceptFriend(friendshipId)
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun declineFriend(friendshipId: Int) {
        viewModelScope.launch {
            repository.declineFriend(friendshipId)
                .onSuccess {
                    _uiState.update { it.copy(message = "Заявка отклонена") }
                    refresh()
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun removeFriend(userId: Int) {
        viewModelScope.launch {
            repository.removeFriend(userId)
                .onSuccess {
                    _uiState.update { it.copy(message = "Удалён из друзей") }
                    refresh()
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun publishPost(context: android.content.Context) {
        val state = _uiState.value
        val body = state.newPostText.trim().ifBlank { null }
        if (body == null && state.selectedMediaUri == null && state.selectedActivityId == null) return
        
        viewModelScope.launch {
            
            _uiState.update { it.copy(isLoading = true) }
            
            var mediaUrl: String? = null
            
            if (state.selectedMediaUri != null) {
                try {
                    val bytes = context.contentResolver.openInputStream(state.selectedMediaUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val mime = context.contentResolver.getType(state.selectedMediaUri) ?: "image/jpeg"
                        val requestBody = bytes.toRequestBody(mime.toMediaType())
                        val part = MultipartBody.Part.createFormData("file", "upload.jpg", requestBody)
                        val uploadResult = repository.uploadPostMedia(part)
                        if (uploadResult.isSuccess) {
                            val baseUrl = apiServerConfig.baseUrl().removeSuffix("/")
                            mediaUrl = baseUrl + uploadResult.getOrThrow().url
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = "Ошибка загрузки фото: ${uploadResult.exceptionOrNull()?.message}") }
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = "Ошибка чтения файла: ${e.message}") }
                    return@launch
                }
            }

            repository.createPost(
                FeedPostCreateDto(
                    body = body,
                    media_url = mediaUrl,
                    media_type = if (mediaUrl != null) "image" else null,
                    activity_id = state.selectedActivityId,
                    visibility = state.feedVisibility,
                ),
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            newPostText = "",
                            selectedMediaUri = null,
                            selectedActivityId = null,
                            showComposeSheet = false,
                            message = "Опубликовано",
                            isLoading = false
                        )
                    }
                    refresh()
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun publishStory(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val requestBody = bytes.toRequestBody(mime.toMediaType())
                    val part = MultipartBody.Part.createFormData("file", "story.jpg", requestBody)
                    val uploadResult = repository.uploadPostMedia(part)
                    if (uploadResult.isSuccess) {
                        val baseUrl = apiServerConfig.baseUrl().removeSuffix("/")
                        val mediaUrl = baseUrl + uploadResult.getOrThrow().url
                        
                        val createResult = repository.createStory(
                            com.example.healtapp.data.network.dto.social.StoryCreateDto(media_url = mediaUrl)
                        )
                        
                        if (createResult.isSuccess) {
                            _uiState.update {
                                it.copy(
                                    showStoryComposeSheet = false,
                                    message = "История опубликована",
                                    isLoading = false
                                )
                            }
                            refresh()
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = "Ошибка создания истории: ${createResult.exceptionOrNull()?.message}") }
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Ошибка загрузки истории: ${uploadResult.exceptionOrNull()?.message}") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Ошибка чтения файла: ${e.message}") }
            }
        }
    }

    fun toggleReaction(postId: Int, emoji: String) {
        viewModelScope.launch {
            repository.toggleReaction(postId, emoji)
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun savePrivacy() {
        viewModelScope.launch {
            val s = _uiState.value
            repository.updatePrivacy(
                PrivacyUpdateDto(
                    profile_visibility = s.profileVisibility,
                    feed_visibility = s.feedVisibility,
                    show_activity_to_friends = s.showActivity,
                    show_achievements_to_friends = s.showAchievements,
                ),
            )
                .onSuccess {
                    _uiState.update { it.copy(message = "Приватность сохранена") }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun openComments(postId: Int) {
        _uiState.update { it.copy(selectedPostForComments = postId) }
        loadComments(postId)
    }

    fun closeComments() {
        _uiState.update { it.copy(selectedPostForComments = null) }
    }

    fun openDeleteOverlay(post: FeedPostDto) {
        if (!post.author.is_self) return
        _uiState.update { it.copy(selectedPostForDelete = post) }
    }

    fun closeDeleteOverlay() {
        _uiState.update { it.copy(selectedPostForDelete = null) }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.deletePost(postId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            selectedPostForDelete = null,
                            feed = state.feed.filter { it.id != postId },
                            message = "Публикация удалена",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedPostForDelete = null,
                            error = e.message,
                        )
                    }
                }
        }
    }

    private fun loadComments(postId: Int) {
        viewModelScope.launch {
            repository.getPostComments(postId)
                .onSuccess { response ->
                    _uiState.update { it.copy(postComments = it.postComments + (postId to response.comments)) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun postComment(postId: Int, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.createPostComment(postId, FeedCommentCreateDto(trimmed))
                .onSuccess { newComment ->
                    _uiState.update { state ->
                        val currentComments = state.postComments[postId].orEmpty()
                        val updatedComments = currentComments + newComment
                        val updatedFeed = state.feed.map { 
                            if (it.id == postId) it.copy(comments_count = it.comments_count + 1) else it 
                        }
                        state.copy(
                            postComments = state.postComments + (postId to updatedComments),
                            feed = updatedFeed
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    private fun filterActiveStories(stories: List<FeedStoryDto>): List<FeedStoryDto> {
        val now = java.time.Instant.now()
        return stories.mapNotNull { group ->
            val activeItems = group.items.filter { item ->
                val expiresAt = item.expires_at?.let { raw ->
                    runCatching { java.time.Instant.parse(raw) }.getOrNull()
                }
                expiresAt == null || expiresAt.isAfter(now)
            }
            if (activeItems.isEmpty()) {
                null
            } else {
                group.copy(
                    items = activeItems,
                    preview_url = activeItems.lastOrNull()?.media_url ?: group.preview_url,
                )
            }
        }
    }

    private suspend fun loadClubFeedEntries(
        clubs: List<com.example.healtapp.data.network.dto.social.ClubResponseDto>,
    ): List<ClubFeedEntry> {
        return clubs
            .filter { it.is_member }
            .flatMap { club ->
                repository.getClubPosts(club.id).getOrNull().orEmpty().take(5).map { post ->
                    ClubFeedEntry(club, post)
                }
            }
            .sortedByDescending { it.post.created_at }
            .take(30)
    }
}
