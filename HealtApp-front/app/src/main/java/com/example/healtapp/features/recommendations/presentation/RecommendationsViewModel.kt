package com.example.healtapp.features.recommendations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RecommendationsUiState(isLoading = true)
    )
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    init {
        loadRecommendations()

        viewModelScope.launch {
            AppRefreshBus.events.collect {
                loadRecommendations()
            }
        }
    }

    fun loadRecommendations(days: Int = 7) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = aiRepository.getRecommendations(days)

            result.onSuccess { response ->
                val items = response.recommendations.map { recommendation ->
                    RecommendationUiItem(
                        category = recommendation.category,
                        title = recommendation.title,
                        description = recommendation.description,
                        priority = recommendation.priority,
                        status = recommendation.status,
                        confidence = recommendation.confidence,
                        action = recommendation.action,
                        personalizedTip = recommendation.personalized_tip,
                        progressLabel = recommendation.progress_label
                    )
                }

                _uiState.value = RecommendationsUiState(
                    isLoading = false,
                    error = null,
                    healthScore = response.health_score,
                    periodDays = response.period_days,
                    recommendations = items
                )
            }.onFailure { throwable ->
                _uiState.value = RecommendationsUiState(
                    isLoading = false,
                    error = throwable.message ?: "Не удалось загрузить рекомендации",
                    healthScore = 0,
                    periodDays = days,
                    recommendations = emptyList()
                )
            }
        }
    }
}