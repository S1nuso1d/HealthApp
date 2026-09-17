package com.example.healtapp.features.social.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.data.network.dto.social.FriendAchievementDto
import com.example.healtapp.data.network.dto.social.FriendActivityDto
import com.example.healtapp.data.network.dto.social.FeedPostDto
import com.example.healtapp.data.network.dto.social.UserCardDto
import com.example.healtapp.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val user: UserCardDto? = null,
    val activities: List<FriendActivityDto> = emptyList(),
    val achievements: List<FriendAchievementDto> = emptyList(),
    val posts: List<FeedPostDto> = emptyList(),
    val isFriend: Boolean = false,
    val isBlocked: Boolean = false,
    val actionMessage: String? = null,
)

@HiltViewModel
class FriendProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SocialRepository,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val userId: Int =
        savedStateHandle.get<Int>("userId")
            ?: savedStateHandle.get<String>("userId")?.toIntOrNull()
            ?: 0

    private val _uiState = MutableStateFlow(FriendProfileUiState())
    val uiState: StateFlow<FriendProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (userId <= 0) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Некорректный профиль")
                }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getUserProfile(userId)
                .onSuccess { dto ->
                    _uiState.value = FriendProfileUiState(
                        isLoading = false,
                        user = dto.user,
                        activities = dto.activities,
                        achievements = dto.achievements,
                        posts = dto.posts,
                        isFriend = dto.is_friend,
                        isBlocked = dto.is_blocked,
                    )
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message?.takeIf { msg ->
                                msg.contains("403") || msg.contains("404") || msg.contains("недоступ")
                            } ?: (e.message ?: "Профиль скрыт настройками приватности"),
                        )
                    }
                }
        }
    }

    fun requestFriend() {
        if (userId <= 0) return
        viewModelScope.launch {
            repository.requestFriend(userId)
                .onSuccess {
                    _uiState.update { it.copy(actionMessage = "Заявка отправлена", isFriend = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun blockUser() {
        if (userId <= 0) return
        viewModelScope.launch {
            repository.blockUser(userId)
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            actionMessage = "Пользователь заблокирован", 
                            isBlocked = true, 
                            isFriend = false, 
                            activities = emptyList(), 
                            achievements = emptyList(),
                            error = null
                        ) 
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun unblockUser() {
        if (userId <= 0) return
        viewModelScope.launch {
            repository.unblockUser(userId)
                .onSuccess {
                    _uiState.update { it.copy(actionMessage = "Пользователь разблокирован", isBlocked = false) }
                    load()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
