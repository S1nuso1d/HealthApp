package com.example.healtapp.features.insights.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.data.network.dto.analytics.CircadianProfileDto
import com.example.healtapp.data.network.dto.analytics.HabitExperimentDto
import com.example.healtapp.data.network.dto.analytics.InfluenceFactorsResponseDto
import com.example.healtapp.domain.repository.WellnessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InfluenceFactorsUiState(
    val isLoading: Boolean = true,
    val data: InfluenceFactorsResponseDto? = null,
    val error: String? = null,
    val activeExperiment: HabitExperimentDto? = null,
    val recentExperiments: List<HabitExperimentDto> = emptyList(),
    val circadian: CircadianProfileDto? = null,
    val startingFactorId: String? = null,
    val notice: String? = null,
)

@HiltViewModel
class InfluenceFactorsViewModel @Inject constructor(
    private val wellnessRepository: WellnessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InfluenceFactorsUiState())
    val uiState: StateFlow<InfluenceFactorsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            wellnessRepository.getInfluenceFactors(14).fold(
                onSuccess = { data ->
                    _uiState.update { it.copy(isLoading = false, data = data) }
                },
                onFailure = { t ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UserFacingMessages.fromThrowable(t, "Не удалось загрузить факторы"),
                        )
                    }
                },
            )
            wellnessRepository.getHabitExperiments().onSuccess { experiments ->
                _uiState.update { current ->
                    current.copy(
                        activeExperiment = experiments.active,
                        recentExperiments = experiments.items
                            .filter { item -> item.status.equals("completed", ignoreCase = true) }
                            .take(3),
                    )
                }
            }
            wellnessRepository.getCircadian().onSuccess { circadian ->
                _uiState.update { current -> current.copy(circadian = circadian) }
            }
        }
    }

    fun consumeNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun startExperiment(factorId: String, title: String? = null) {
        if (factorId.isBlank() || _uiState.value.startingFactorId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(startingFactorId = factorId, notice = null) }
            wellnessRepository.startHabitExperiment(factorId, title).fold(
                onSuccess = { item ->
                    _uiState.update {
                        it.copy(
                            startingFactorId = null,
                            activeExperiment = item,
                            notice = "Эксперимент на 7 дней начат: ${item.title.orEmpty()}",
                        )
                    }
                },
                onFailure = { t ->
                    _uiState.update {
                        it.copy(
                            startingFactorId = null,
                            notice = UserFacingMessages.fromThrowable(t, "Не удалось начать эксперимент"),
                        )
                    }
                },
            )
        }
    }
}
