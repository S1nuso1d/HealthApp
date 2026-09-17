package com.example.healtapp.features.achievements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.dto.gamification.AchievementItemDto
import com.example.healtapp.data.network.dto.gamification.AchievementLeaderboardEntryDto
import com.example.healtapp.data.network.dto.gamification.AchievementPointRuleDto
import com.example.healtapp.data.network.dto.gamification.AchievementRecentDto
import com.example.healtapp.data.network.dto.gamification.AchievementTierDefDto
import com.example.healtapp.data.network.dto.gamification.AchievementTierDto
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
    val totalPoints: Int = 0,
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val seasonPoints: Int = 0,
    val seasonLabel: String? = null,
    val tier: AchievementTierDto? = null,
    val pointRules: List<AchievementPointRuleDto> = emptyList(),
    val tiers: List<AchievementTierDefDto> = emptyList(),
    val leaderboard: List<AchievementLeaderboardEntryDto> = emptyList(),
    val achievements: List<AchievementItemDto> = emptyList(),
    val recent: List<AchievementRecentDto> = emptyList(),
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val repository: GamificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getMyAchievements()
                .onSuccess { dto ->
                    _uiState.value = AchievementsUiState(
                        isLoading = false,
                        totalPoints = dto.total_points,
                        unlockedCount = dto.unlocked_count,
                        totalCount = dto.total_count,
                        seasonPoints = dto.season_points,
                        seasonLabel = dto.season_label,
                        tier = dto.tier,
                        pointRules = dto.point_rules,
                        tiers = dto.tiers,
                        leaderboard = dto.leaderboard,
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

    fun refresh() = load()
}
