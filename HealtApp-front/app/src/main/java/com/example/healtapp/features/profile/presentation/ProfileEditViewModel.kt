package com.example.healtapp.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)

            val result = profileRepository.getMyProfile()
            result.onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    age = profile.age?.toString().orEmpty(),
                    sex = profile.sex ?: _uiState.value.sex,
                    height = profile.height_cm?.toString().orEmpty(),
                    weight = profile.weight_kg?.toString().orEmpty(),
                    goal = profile.goal ?: _uiState.value.goal,
                    activityLevel = profile.activity_level ?: _uiState.value.activityLevel,
                    targetSleep = profile.target_sleep_hours?.toString() ?: _uiState.value.targetSleep,
                    targetWater = profile.target_water_ml?.toInt()?.toString() ?: _uiState.value.targetWater,
                )
            }.onFailure { t ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = t.message ?: "Не удалось загрузить профиль",
                )
            }
        }
    }

    fun updateAge(value: String) = update { copy(age = value, error = null, success = null) }
    fun updateSex(value: String) = update { copy(sex = value, error = null, success = null) }
    fun updateHeight(value: String) = update { copy(height = value, error = null, success = null) }
    fun updateWeight(value: String) = update { copy(weight = value, error = null, success = null) }
    fun updateGoal(value: String) = update { copy(goal = value, error = null, success = null) }
    fun updateActivityLevel(value: String) = update { copy(activityLevel = value, error = null, success = null) }
    fun updateTargetSleep(value: String) = update { copy(targetSleep = value, error = null, success = null) }
    fun updateTargetWater(value: String) = update { copy(targetWater = value, error = null, success = null) }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null, success = null)

            val result = profileRepository.updateMyProfile(
                age = state.age.toIntOrNull(),
                sex = state.sex,
                heightCm = state.height.toFloatOrNull(),
                weightKg = state.weight.toFloatOrNull(),
                goal = state.goal,
                activityLevel = state.activityLevel,
                targetSleepHours = state.targetSleep.toFloatOrNull(),
                targetWaterMl = state.targetWater.toFloatOrNull(),
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    success = "Профиль успешно сохранён",
                )
            }.onFailure { t ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = t.message ?: "Не удалось сохранить профиль",
                )
            }
        }
    }

    private inline fun update(block: ProfileEditUiState.() -> ProfileEditUiState) {
        _uiState.value = _uiState.value.block()
    }
}

