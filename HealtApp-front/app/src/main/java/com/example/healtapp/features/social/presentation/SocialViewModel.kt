package com.example.healtapp.features.social.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.ApiServerConfig
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
import com.example.healtapp.data.preferences.TokenStorage
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
    val guestMode: Boolean = false,
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
    val clubMembers: Map<Int, List<com.example.healtapp.data.network.dto.social.ClubMemberResponseDto>> = emptyMap(),
    val isSearching: Boolean = false,
    val searchHint: String? = null,
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val tokenStorage: TokenStorage,
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
        _uiState.update { it.copy(searchQuery = q, searchHint = null) }
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
            if (tokenStorage.isGuestMode()) {
                _uiState.value = SocialUiState(
                    isLoading = false,
                    guestMode = true,
                    feed = demoFeed(),
                    stories = demoStories(),
                    linkableActivities = demoLinkableActivities(),
                    friends = demoFriends(),
                    weeklyChallenge = demoWeeklyChallenge(),
                    challenges = demoChallenges(),
                    clubs = demoClubs(),
                )
                return@launch
            }
            val wasLoaded = !_uiState.value.isLoading && _uiState.value.feed.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !wasLoaded,
                    isRefreshing = wasLoaded,
                    error = null,
                    guestMode = false,
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
            
            _uiState.value = SocialUiState(
                isLoading = false,
                isRefreshing = false,
                feed = feed.posts,
                stories = storiesResult.getOrNull()?.stories.orEmpty(),
                linkableActivities = activitiesResult.getOrNull().orEmpty(),
                friends = friends.friends,
                pending = pending.incoming,
                weeklyChallenge = challenge,
                challenges = challengesList,
                clubs = clubsList,
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
            if (tokenStorage.isGuestMode()) return@launch
            repository.getChallengeLeaderboard(challengeId).onSuccess { res ->
                _uiState.update { it.copy(challengeLeaderboards = it.challengeLeaderboards + (challengeId to res.entries)) }
            }
        }
    }

    fun joinChallenge(challengeId: Int) {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.update { it.copy(message = "Войдите в аккаунт для участия") }
                return@launch
            }
            repository.joinChallenge(challengeId).onSuccess {
                refresh()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun leaveChallenge(challengeId: Int) {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) return@launch
            repository.leaveChallenge(challengeId).onSuccess {
                refresh()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun joinClub(clubId: Int) {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.update { it.copy(message = "Войдите в аккаунт для вступления") }
                return@launch
            }
            repository.joinClub(clubId).onSuccess {
                refresh()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun leaveClub(clubId: Int) {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) return@launch
            repository.leaveClub(clubId).onSuccess {
                refresh()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun loadClubMembers(clubId: Int) {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) return@launch
            repository.getClubMembers(clubId).onSuccess { members ->
                _uiState.update { it.copy(clubMembers = it.clubMembers + (clubId to members)) }
            }
        }
    }

    private fun loadLinkableActivities() {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.update { it.copy(linkableActivities = demoLinkableActivities()) }
                return@launch
            }
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
            if (tokenStorage.isGuestMode()) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = demoFriends().filter { f -> f.display_name.contains(q, true) },
                    )
                }
                return@launch
            }
            repository.searchUsers(q)
                .onSuccess { r ->
                    _uiState.update { it.copy(isSearching = false, searchResults = r.users) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSearching = false, error = e.message) }
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
            if (tokenStorage.isGuestMode()) {
                _uiState.update { it.copy(message = "Войдите в аккаунт для создания клуба") }
                return@launch
            }
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
            if (tokenStorage.isGuestMode()) {
                _uiState.update { it.copy(message = "Войдите в аккаунт для добавления друзей") }
                return@launch
            }
            repository.requestFriend(userId)
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun acceptFriend(friendshipId: Int) {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) return@launch
            repository.acceptFriend(friendshipId)
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun declineFriend(friendshipId: Int) {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) return@launch
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
            if (tokenStorage.isGuestMode()) return@launch
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
            if (tokenStorage.isGuestMode()) {
                _uiState.update { it.copy(message = "Демо: публикации доступны после входа") }
                return@launch
            }
            
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
            if (tokenStorage.isGuestMode()) {
                _uiState.update { it.copy(message = "Демо: истории доступны после входа") }
                return@launch
            }
            
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
            if (tokenStorage.isGuestMode()) {
                _uiState.update { state ->
                    val updated = state.feed.map { post ->
                        if (post.id != postId) return@map post
                        val wasMine = post.my_reaction == emoji
                        val newMine = if (wasMine) null else emoji
                        val counts = post.reactions.toMutableList()
                        fun bump(e: String, delta: Int) {
                            val i = counts.indexOfFirst { it.emoji == e }
                            if (i >= 0) {
                                val c = (counts[i].count + delta).coerceAtLeast(0)
                                if (c == 0) counts.removeAt(i) else counts[i] = FeedReactionDto(e, c)
                            } else if (delta > 0) {
                                counts.add(FeedReactionDto(e, delta))
                            }
                        }
                        post.my_reaction?.let { bump(it, -1) }
                        if (!wasMine) bump(emoji, 1)
                        post.copy(
                            my_reaction = newMine,
                            reactions = counts,
                            reaction_total = counts.sumOf { it.count },
                        )
                    }
                    state.copy(feed = updated)
                }
                return@launch
            }
            repository.toggleReaction(postId, emoji)
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun savePrivacy() {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.update { it.copy(message = "Настройки сохранятся после входа") }
                return@launch
            }
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
            if (tokenStorage.isGuestMode()) {
                _uiState.update {
                    it.copy(
                        selectedPostForDelete = null,
                        message = "Демо: удаление доступно после входа",
                    )
                }
                return@launch
            }
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
            if (tokenStorage.isGuestMode()) {
                val demoComments = listOf(
                    FeedCommentDto(1, postId, demoFriends()[0], "Отлично!"),
                )
                _uiState.update { it.copy(postComments = it.postComments + (postId to demoComments)) }
                return@launch
            }
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
            if (tokenStorage.isGuestMode()) {
                _uiState.update { it.copy(message = "Демо: комментарии доступны после входа") }
                return@launch
            }
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

    private fun demoFriends() = listOf(
        UserCardDto(101, "Анна", nickname = "anna_fit", goal = "MAINTAIN"),
        UserCardDto(102, "Иван Петров", nickname = "ivan_run", goal = "LOSE_WEIGHT", has_avatar = true),
    )

    private fun demoWeeklyChallenge() = listOf(
        WeeklyChallengeEntryDto(user_id = 0, display_name = "Вы", steps = 6_420, is_me = true, rank = 1),
        WeeklyChallengeEntryDto(user_id = 101, display_name = "Анна", steps = 5_100, rank = 2),
        WeeklyChallengeEntryDto(user_id = 102, display_name = "Иван Петров", steps = 4_800, rank = 3),
    )

    private fun demoLinkableActivities() = listOf(
        FeedActivityDto(
            id = 1,
            activity_type = "walk",
            duration_minutes = 45,
            calories_burned = 210f,
            steps = 5200,
            distance_km = 3.8f,
            start_time = "2026-05-26T08:00:00",
        ),
        FeedActivityDto(
            id = 2,
            activity_type = "run",
            duration_minutes = 32,
            calories_burned = 380f,
            steps = 4100,
            distance_km = 5.2f,
            start_time = "2026-05-25T19:30:00",
        ),
    )

    private fun demoStories() = listOf(
        FeedStoryDto(
            author = UserCardDto(102, "Иван Петров", nickname = "ivan_run", goal = "LOSE_WEIGHT"),
            preview_url = "https://picsum.photos/seed/walk1/400/600",
            has_unseen = true,
            items = listOf(
                com.example.healtapp.data.network.dto.social.FeedStoryItemDto(
                    id = 1,
                    media_url = "https://picsum.photos/seed/walk1/400/600",
                    created_at = "2026-05-26T07:00:00",
                    expires_at = "2026-05-27T07:00:00"
                )
            )
        ),
        FeedStoryDto(
            author = UserCardDto(101, "Анна", nickname = "anna_fit", goal = "MAINTAIN"),
            preview_url = "https://picsum.photos/seed/walk2/400/600",
            has_unseen = true,
            items = listOf(
                com.example.healtapp.data.network.dto.social.FeedStoryItemDto(
                    id = 2,
                    media_url = "https://picsum.photos/seed/walk2/400/600",
                    created_at = "2026-05-25T18:00:00",
                    expires_at = "2026-05-26T18:00:00"
                )
            )
        ),
    )

    private fun demoFeed() = listOf(
        FeedPostDto(
            id = 1,
            author = UserCardDto(102, "Иван Петров", nickname = "ivan_run", goal = "LOSE_WEIGHT"),
            body = "Утренняя прогулка по парку — отличное настроение!",
            media_url = "https://picsum.photos/seed/walkfeed/800/500",
            media_type = "image",
            activity = FeedActivityDto(
                id = 1,
                activity_type = "walk",
                duration_minutes = 45,
                calories_burned = 210f,
                steps = 5200,
                distance_km = 3.8f,
            ),
            activity_id = 1,
            created_at = "2026-05-26T08:15:00",
            reactions = listOf(FeedReactionDto("👍", 3), FeedReactionDto("🔥", 1)),
            reaction_total = 4,
            comments_count = 1,
        ),
        FeedPostDto(
            id = 2,
            author = UserCardDto(101, "Анна", nickname = "anna_fit", goal = "MAINTAIN"),
            body = "Рецепт ПП: овсянка с ягодами и арахисовой пастой — 320 ккал, готовится за 10 минут 🥣",
            media_url = "https://picsum.photos/seed/oatmeal/800/500",
            media_type = "image",
            created_at = "2026-05-25T20:30:00",
            reactions = listOf(FeedReactionDto("❤️", 2)),
            reaction_total = 2,
            my_reaction = "❤️",
            comments_count = 0,
        ),
        FeedPostDto(
            id = 3,
            author = UserCardDto(103, "Мария", nickname = "maria_go", goal = "GAIN_MUSCLE"),
            body = "Лайфхак: ставлю бутылку воды на видное место — так легче пить норму в течение дня 💧",
            created_at = "2026-05-25T12:00:00",
            reactions = listOf(FeedReactionDto("👍", 5), FeedReactionDto("🔥", 2)),
            reaction_total = 7,
            comments_count = 2,
        ),
    )

    private fun demoChallenges() = listOf(
        com.example.healtapp.data.network.dto.social.ChallengeResponseDto(
            id = 1,
            title = "10,000 шагов в день",
            description = "Пройдите 10 000 шагов за день",
            challenge_type = "steps",
            target_value = 10000,
            is_group = true,
            creator_id = 101,
            start_date = "2026-05-25T00:00:00",
            end_date = "2026-06-01T00:00:00",
            participants_count = 5,
            my_progress = null,
        )
    )

    private fun demoClubs() = listOf(
        com.example.healtapp.data.network.dto.social.ClubResponseDto(
            id = 1,
            name = "Бегуны 🏃‍♂️",
            description = "Клуб любителей утренних пробежек",
            avatar_url = null,
            rules = "Уважайте друг друга",
            creator_id = 101,
            members_count = 12,
            is_member = false
        ),
        com.example.healtapp.data.network.dto.social.ClubResponseDto(
            id = 2,
            name = "Здоровое питание 🥗",
            description = "Обмен рецептами и планами питания",
            avatar_url = null,
            rules = "Без фастфуда",
            creator_id = 102,
            members_count = 34,
            is_member = true
        )
    )
}
