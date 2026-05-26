package com.example.healtapp.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.core.common.NutritionTargetsCalculator
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.NextStep -> advanceStep()
            OnboardingEvent.PrevStep -> {
                val s = _uiState.value
                if (s.step > 0) {
                    _uiState.value = s.copy(step = s.step - 1, error = null)
                }
            }
            is OnboardingEvent.VegetarianChanged -> {
                _uiState.value = _uiState.value.copy(isVegetarian = event.value, error = null)
            }
            is OnboardingEvent.HasAllergiesChanged -> {
                _uiState.value = _uiState.value.copy(
                    hasAllergies = event.value,
                    allergiesText = if (!event.value) "" else _uiState.value.allergiesText,
                    error = null,
                )
            }
            is OnboardingEvent.AllergiesTextChanged -> {
                _uiState.value = _uiState.value.copy(allergiesText = event.value, error = null)
            }
            is OnboardingEvent.AgeChanged -> {
                _uiState.value = _uiState.value.copy(age = event.value, error = null)
                refreshPreview()
            }
            is OnboardingEvent.SexChanged -> {
                _uiState.value = _uiState.value.copy(sex = event.value, error = null)
                refreshPreview()
            }
            is OnboardingEvent.HeightChanged -> {
                _uiState.value = _uiState.value.copy(height = event.value, error = null)
                refreshPreview()
            }
            is OnboardingEvent.WeightChanged -> {
                _uiState.value = _uiState.value.copy(weight = event.value, error = null)
                refreshPreview()
            }
            is OnboardingEvent.GoalChanged -> {
                _uiState.value = _uiState.value.copy(goal = event.value, error = null)
                refreshPreview()
            }
            is OnboardingEvent.ActivityLevelChanged -> {
                _uiState.value = _uiState.value.copy(activityLevel = event.value, error = null)
                refreshPreview()
            }
            OnboardingEvent.Submit -> submit()
        }
    }

    private fun advanceStep() {
        val state = _uiState.value
        val err = validateStep(state.step, state)
        if (err != null) {
            _uiState.value = state.copy(error = err)
            return
        }
        if (state.step >= state.totalSteps - 1) {
            submit()
            return
        }
        val next = state.step + 1
        _uiState.value = state.copy(step = next, error = null)
        if (next == state.totalSteps - 1) {
            refreshPreview()
        }
    }

    private fun validateStep(step: Int, state: OnboardingUiState): String? = when (step) {
        0 -> null
        1 -> {
            if (state.hasAllergies && state.allergiesText.trim().length < 2) {
                "Укажите, на что у вас аллергия"
            } else null
        }
        2 -> {
            val age = state.age.toIntOrNull()
            val height = state.height.toFloatOrNull()
            val weight = state.weight.toFloatOrNull()
            when {
                age == null || age !in 10..120 -> "Введите возраст от 10 до 120"
                height == null || height !in 100f..250f -> "Введите рост от 100 до 250 см"
                weight == null || weight !in 30f..300f -> "Введите вес от 30 до 300 кг"
                else -> null
            }
        }
        3 -> null
        else -> null
    }

    private fun refreshPreview() {
        val state = _uiState.value
        val age = state.age.toIntOrNull() ?: return
        val height = state.height.toFloatOrNull() ?: return
        val weight = state.weight.toFloatOrNull() ?: return
        val preview = NutritionTargetsCalculator.calculate(
            age = age,
            sex = state.sex,
            heightCm = height,
            weightKg = weight,
            activityLevel = state.activityLevel,
            goal = state.goal,
        )
        _uiState.value = state.copy(previewTargets = preview)
    }

    private fun submit() {
        val state = _uiState.value
        val err = validateStep(2, state) ?: validateStep(1, state)
        if (err != null) {
            _uiState.value = state.copy(error = err)
            return
        }

        val age = state.age.toIntOrNull()!!
        val height = state.height.toFloatOrNull()!!
        val weight = state.weight.toFloatOrNull()!!
        val targets = state.previewTargets
            ?: NutritionTargetsCalculator.calculate(
                age, state.sex, height, weight, state.activityLevel, state.goal,
            )
        if (targets == null) {
            _uiState.value = state.copy(error = "Не удалось рассчитать цели питания")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)

            val allergiesText = if (state.hasAllergies) state.allergiesText.trim().take(500) else null

            val result = profileRepository.updateMyProfile(
                age = age,
                sex = state.sex,
                heightCm = height,
                weightKg = weight,
                goal = state.goal,
                activityLevel = state.activityLevel,
                targetSleepHours = targets.sleepHours,
                targetWaterMl = targets.waterMl,
                targetDailyCalories = targets.calories,
                targetProteinG = targets.proteinG,
                targetFatG = targets.fatG,
                targetCarbsG = targets.carbsG,
                targetSteps = targets.steps,
                isVegetarian = state.isVegetarian,
                hasAllergies = state.hasAllergies,
                allergiesText = allergiesText,
                onboardingCompleted = true,
            )

            result.onSuccess {
                AppRefreshBus.notifyDataChanged()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    isSaved = true,
                )
            }.onFailure { throwable ->
                if (throwable is HttpException && throwable.code() == 401) {
                    tokenStorage.clearToken()
                    AppRefreshBus.notifySessionExpired()
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = throwable.message ?: "Не удалось сохранить профиль",
                )
            }
        }
    }

    fun consumeSavedState() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }
}
