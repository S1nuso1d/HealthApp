package com.example.healtapp.features.activity.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ActivityViewModel(
    private val repository: ActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        loadTodayActivity()
    }

    fun loadTodayActivity() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            repository.getTodayActivity()
                .onSuccess { activity ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        todayActivity = activity,
                        error = null
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Не удалось загрузить активность"
                    )
                }
        }
    }

    fun updateActivityType(value: String) {
        _uiState.value = _uiState.value.copy(activityType = value)
    }

    fun updateDuration(value: String) {
        _uiState.value = _uiState.value.copy(durationMinutes = value)
    }

    fun updateSteps(value: String) {
        _uiState.value = _uiState.value.copy(steps = value)
    }

    fun updateCalories(value: String) {
        _uiState.value = _uiState.value.copy(caloriesBurned = value)
    }

    fun updateDistance(value: String) {
        _uiState.value = _uiState.value.copy(distanceKm = value)
    }

    fun updateIntensity(value: String) {
        _uiState.value = _uiState.value.copy(intensity = value)
    }

    fun saveActivity() {
        viewModelScope.launch {
            val state = _uiState.value

            val duration = state.durationMinutes.toIntOrNull()
            if (duration == null || duration <= 0) {
                _uiState.value = state.copy(error = "Введите корректную длительность")
                return@launch
            }

            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

            val request = ActivityCreateRequestDto(
                activity_type = state.activityType.ifBlank { "walk" },
                start_time = now.minusMinutes(duration.toLong()).format(formatter),
                end_time = now.format(formatter),
                duration_minutes = duration,
                steps = state.steps.toIntOrNull(),
                distance_km = state.distanceKm.toFloatOrNull(),
                calories_burned = state.caloriesBurned.toFloatOrNull(),
                intensity = state.intensity.ifBlank { null },
                activity_category = null,
                perceived_exertion = null,
                avg_heart_rate = null,
                minutes_before_sleep = null,
                is_evening_activity = null,
                notes = null,
                source = "manual"
            )

            _uiState.value = state.copy(
                isSaving = true,
                error = null
            )

            repository.createActivity(request)
                .onSuccess { activity ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        todayActivity = activity,
                        durationMinutes = "",
                        steps = "",
                        caloriesBurned = "",
                        distanceKm = "",
                        error = null
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = throwable.message ?: "Не удалось сохранить активность"
                    )
                }
        }
    }
}