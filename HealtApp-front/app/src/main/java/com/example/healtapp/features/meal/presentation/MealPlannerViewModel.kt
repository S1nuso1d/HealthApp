package com.example.healtapp.features.meal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.dto.ai.MealPlanResponseDto
import com.example.healtapp.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MealPlannerUiState(
    val isLoading: Boolean = false,
    val plan: MealPlanResponseDto? = null,
    val error: String? = null,
    val info: String? = null,
    val planDays: Int = 3,
    val llmAvailable: Boolean? = null,
)

@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val aiRepository: AiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlannerUiState())
    val uiState: StateFlow<MealPlannerUiState> = _uiState.asStateFlow()

    init {
        refreshAiStatus()
    }

    fun setPlanDays(days: Int) {
        _uiState.update { it.copy(planDays = days.coerceIn(3, 7)) }
    }

    fun refreshAiStatus() {
        viewModelScope.launch {
            aiRepository.getAiStatus()
                .onSuccess { status ->
                    _uiState.update {
                        it.copy(
                            llmAvailable = status.llm_available,
                            info = if (!status.llm_available) {
                                "На сервере ИИ недоступен: ${status.message}"
                            } else {
                                null
                            },
                        )
                    }
                }
        }
    }

    fun generatePlan() {
        val days = _uiState.value.planDays
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            aiRepository.getMealPlan(days)
                .onSuccess { plan ->
                    val info = when (plan.source) {
                        "fallback" -> "Составлен шаблонный план. ИИ не смог сгенерировать меню (таймаут или формат ответа) — попробуйте ещё раз через минуту."
                        else -> null
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            plan = plan,
                            info = info,
                            llmAvailable = if (plan.source == "llm") true else it.llmAvailable,
                        )
                    }
                    if (plan.source == "llm") {
                        refreshAiStatus()
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Не удалось сгенерировать план",
                        )
                    }
                }
        }
    }
}
