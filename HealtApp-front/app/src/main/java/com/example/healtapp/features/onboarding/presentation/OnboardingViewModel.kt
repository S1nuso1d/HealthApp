package com.example.healtapp.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healtapp.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.AgeChanged -> {
                _uiState.value = _uiState.value.copy(
                    age = event.value,
                    error = null
                )
            }

            is OnboardingEvent.SexChanged -> {
                _uiState.value = _uiState.value.copy(
                    sex = event.value,
                    error = null
                )
            }

            is OnboardingEvent.HeightChanged -> {
                _uiState.value = _uiState.value.copy(
                    height = event.value,
                    error = null
                )
            }

            is OnboardingEvent.WeightChanged -> {
                _uiState.value = _uiState.value.copy(
                    weight = event.value,
                    error = null
                )
            }

            is OnboardingEvent.TargetSleepChanged -> {
                _uiState.value = _uiState.value.copy(
                    targetSleep = event.value,
                    error = null
                )
            }

            is OnboardingEvent.TargetWaterChanged -> {
                _uiState.value = _uiState.value.copy(
                    targetWater = event.value,
                    error = null
                )
            }

            is OnboardingEvent.GoalChanged -> {
                _uiState.value = _uiState.value.copy(
                    goal = event.value,
                    error = null
                )
            }

            is OnboardingEvent.ActivityLevelChanged -> {
                _uiState.value = _uiState.value.copy(
                    activityLevel = event.value,
                    error = null
                )
            }

            OnboardingEvent.Submit -> {
                submit()
            }
        }
    }

    private fun submit() {
        val state = _uiState.value

        val age = state.age.toIntOrNull()
        val height = state.height.toFloatOrNull()
        val weight = state.weight.toFloatOrNull()
        val targetSleep = state.targetSleep.toFloatOrNull()
        val targetWater = state.targetWater.toFloatOrNull()

        if (age == null || age <= 0) {
            _uiState.value = state.copy(error = "Введите корректный возраст")
            return
        }

        if (height == null || height <= 0f) {
            _uiState.value = state.copy(error = "Введите корректный рост")
            return
        }

        if (weight == null || weight <= 0f) {
            _uiState.value = state.copy(error = "Введите корректный вес")
            return
        }

        if (targetSleep == null || targetSleep <= 0f) {
            _uiState.value = state.copy(error = "Введите корректную цель сна")
            return
        }

        if (targetWater == null || targetWater <= 0f) {
            _uiState.value = state.copy(error = "Введите корректную цель воды")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                error = null
            )

            val result = profileRepository.updateMyProfile(
                age = age,
                sex = state.sex,
                heightCm = height,
                weightKg = weight,
                goal = state.goal,
                activityLevel = state.activityLevel,
                targetSleepHours = targetSleep,
                targetWaterMl = targetWater
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    isSaved = true
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = throwable.message ?: "Не удалось сохранить профиль"
                )
            }
        }
    }

    fun consumeSavedState() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }

    companion object {
        fun factory(profileRepository: ProfileRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(profileRepository) as T
                }
            }
        }
    }
}