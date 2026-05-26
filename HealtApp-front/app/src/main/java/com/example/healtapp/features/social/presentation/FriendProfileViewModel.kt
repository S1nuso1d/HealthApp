package com.example.healtapp.features.social.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.data.network.dto.social.FriendAchievementDto
import com.example.healtapp.data.network.dto.social.FriendActivityDto
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
    val isFriend: Boolean = false,
)

@HiltViewModel
class FriendProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SocialRepository,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val userId: Int = savedStateHandle.get<String>("userId")?.toIntOrNull() ?: 0

    private val _uiState = MutableStateFlow(FriendProfileUiState())
    val uiState: StateFlow<FriendProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode() || userId <= 0) {
                _uiState.value = FriendProfileUiState(
                    isLoading = false,
                    user = UserCardDto(userId.coerceAtLeast(102), "Иван***@gmail.com", "LOSE_WEIGHT"),
                    activities = listOf(
                        FriendActivityDto(1, "run", 35, 280f, null, "2026-05-19T07:00:00"),
                    ),
                    achievements = listOf(
                        FriendAchievementDto("steps_10k", "10 000 шагов", "steps", 30, null),
                    ),
                    isFriend = true,
                )
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
                        isFriend = dto.is_friend,
                    )
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Профиль недоступен")
                    }
                }
        }
    }
}
