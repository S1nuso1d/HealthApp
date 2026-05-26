package com.example.healtapp.features.achievements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.data.network.dto.gamification.AchievementItemDto
import com.example.healtapp.data.network.dto.gamification.AchievementRecentDto
import com.example.healtapp.domain.repository.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val guestMode: Boolean = false,
    val totalPoints: Int = 0,
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val achievements: List<AchievementItemDto> = emptyList(),
    val recent: List<AchievementRecentDto> = emptyList(),
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val repository: GamificationRepository,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.value = AchievementsUiState(
                    isLoading = false,
                    guestMode = true,
                    totalPoints = 45,
                    unlockedCount = 2,
                    totalCount = 6,
                    achievements = demoAchievements(),
                )
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null, guestMode = false) }
            repository.getMyAchievements()
                .onSuccess { dto ->
                    _uiState.value = AchievementsUiState(
                        isLoading = false,
                        totalPoints = dto.total_points,
                        unlockedCount = dto.unlocked_count,
                        totalCount = dto.total_count,
                        achievements = dto.achievements,
                    recent = dto.recent,
                    )
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Не удалось загрузить достижения")
                    }
                }
        }
    }

    private fun demoAchievements() = listOf(
        AchievementItemDto("steps_5k", "5 000 шагов", "Демо: прогулка", "steps", 15, true, null, progress_current = 5000f, progress_target = 5000f, progress_unit = "шагов"),
        AchievementItemDto("water_goal", "Норма воды", "Демо: вода", "water", 20, true, null, progress_current = 2500f, progress_target = 2500f, progress_unit = "мл"),
        AchievementItemDto("run_total_10k", "10 км бега суммарно", "Накопите 10 км пробежек", "run", 40, false, null, "journey", 3.2f, 10f, "км"),
        AchievementItemDto("fastest_km", "Самый быстрый километр", "Личный рекорд темпа", "speed", 35, false, null, "record", record_label = "—"),
        AchievementItemDto("steps_10k", "10 000 шагов", "Войдите в аккаунт", "steps", 30, false, null, progress_current = 0f, progress_target = 10000f, progress_unit = "шагов"),
        AchievementItemDto("first_workout", "Первая тренировка", "Запишите тренировку", "workout", 10, false, null),
    )

}
