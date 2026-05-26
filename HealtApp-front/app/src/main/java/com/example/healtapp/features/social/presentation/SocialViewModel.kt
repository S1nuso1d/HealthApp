package com.example.healtapp.features.social.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.data.network.dto.social.FeedPostCreateDto
import com.example.healtapp.data.network.dto.social.FeedPostDto
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
import javax.inject.Inject

data class SocialUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val guestMode: Boolean = false,
    val selectedTab: Int = 0,
    val feed: List<FeedPostDto> = emptyList(),
    val friends: List<UserCardDto> = emptyList(),
    val pending: List<PendingFriendDto> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<UserCardDto> = emptyList(),
    val newPostText: String = "",
    val newPostMediaUrl: String = "",
    val profileVisibility: String = "friends",
    val feedVisibility: String = "friends",
    val showActivity: Boolean = true,
    val showAchievements: Boolean = true,
    val message: String? = null,
    val weeklyChallenge: List<WeeklyChallengeEntryDto> = emptyList(),
    val weeklyChallengeLoading: Boolean = false,
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val tokenStorage: TokenStorage,
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
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun updateNewPostText(t: String) {
        _uiState.update { it.copy(newPostText = t) }
    }

    fun updateNewPostMediaUrl(u: String) {
        _uiState.update { it.copy(newPostMediaUrl = u) }
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

    fun refresh() {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.value = SocialUiState(
                    isLoading = false,
                    guestMode = true,
                    feed = demoFeed(),
                    friends = demoFriends(),
                    weeklyChallenge = demoWeeklyChallenge(),
                )
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null, guestMode = false) }
            val privacy = repository.getPrivacy().getOrNull()
            val feedResult = repository.getFeed()
            val friendsResult = repository.listFriends()
            val pendingResult = repository.pendingFriends()
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
            _uiState.value = SocialUiState(
                isLoading = false,
                feed = feed.posts,
                friends = friends.friends,
                pending = pending.incoming,
                weeklyChallenge = challenge,
                profileVisibility = privacy?.profile_visibility ?: "friends",
                feedVisibility = privacy?.feed_visibility ?: "friends",
                showActivity = privacy?.show_activity_to_friends ?: true,
                showAchievements = privacy?.show_achievements_to_friends ?: true,
            )
        }
    }

    private fun setError(e: Throwable) {
        _uiState.update {
            it.copy(isLoading = false, error = e.message ?: "Ошибка загрузки")
        }
    }

    fun search() {
        val q = _uiState.value.searchQuery.trim()
        if (q.length < 2) return
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.update {
                    it.copy(searchResults = demoFriends().filter { f -> f.display_name.contains(q, true) })
                }
                return@launch
            }
            repository.searchUsers(q)
                .onSuccess { r -> _uiState.update { it.copy(searchResults = r.users) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
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

    fun publishPost() {
        val state = _uiState.value
        val body = state.newPostText.trim().ifBlank { null }
        val media = state.newPostMediaUrl.trim().ifBlank { null }
        if (body == null && media == null) return
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.update { it.copy(message = "Демо: публикации доступны после входа") }
                return@launch
            }
            repository.createPost(
                FeedPostCreateDto(
                    body = body,
                    media_url = media,
                    media_type = if (media != null) "image" else null,
                    visibility = state.feedVisibility,
                ),
            )
                .onSuccess {
                    _uiState.update { it.copy(newPostText = "", newPostMediaUrl = "", message = "Опубликовано") }
                    refresh()
                }
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

    private fun demoFriends() = listOf(
        UserCardDto(101, "Анна***@mail.ru", "MAINTAIN", false),
        UserCardDto(102, "Иван***@gmail.com", "LOSE_WEIGHT", true),
    )

    private fun demoWeeklyChallenge() = listOf(
        WeeklyChallengeEntryDto(user_id = 0, display_name = "Вы", steps = 6_420, is_me = true, rank = 1),
        WeeklyChallengeEntryDto(user_id = 101, display_name = "Анна***@mail.ru", steps = 5_100, rank = 2),
        WeeklyChallengeEntryDto(user_id = 102, display_name = "Иван***@gmail.com", steps = 4_800, rank = 3),
    )

    private fun demoFeed() = listOf(
        FeedPostDto(
            id = 1,
            author = UserCardDto(102, "Иван***@gmail.com", "LOSE_WEIGHT"),
            body = "Утренняя пробежка 5 км — отличное настроение!",
            media_url = null,
            created_at = "2026-05-19T08:00:00",
        ),
        FeedPostDto(
            id = 2,
            author = UserCardDto(101, "Анна***@mail.ru", "MAINTAIN"),
            body = "Закрыла норму воды 💧",
            created_at = "2026-05-18T20:30:00",
        ),
    )
}
