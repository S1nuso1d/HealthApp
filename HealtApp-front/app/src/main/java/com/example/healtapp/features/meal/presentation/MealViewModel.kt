package com.example.healtapp.features.meal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MealViewModel @Inject constructor(
    private val repository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealUiState())
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    init {
        loadTodayMeal()
    }

    fun loadTodayMeal() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            repository.getTodayMeal()
                .onSuccess { meal ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        todayMeal = meal,
                        error = null
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Не удалось загрузить питание"
                    )
                }
        }
    }

    fun updateMealType(value: String) {
        _uiState.value = _uiState.value.copy(mealType = value)
    }

    fun updateMealName(value: String) {
        _uiState.value = _uiState.value.copy(mealName = value)
    }

    fun updateCalories(value: String) {
        _uiState.value = _uiState.value.copy(calories = value)
    }

    fun updateProtein(value: String) {
        _uiState.value = _uiState.value.copy(protein = value)
    }

    fun updateFat(value: String) {
        _uiState.value = _uiState.value.copy(fat = value)
    }

    fun updateCarbs(value: String) {
        _uiState.value = _uiState.value.copy(carbs = value)
    }

    fun updateCaffeine(value: String) {
        _uiState.value = _uiState.value.copy(caffeineMg = value)
    }

    fun saveMeal() {
        viewModelScope.launch {
            val state = _uiState.value

            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val now = LocalDateTime.now()

            val request = MealCreateRequestDto(
                meal_type = state.mealType.ifBlank { "breakfast" },
                name = state.mealName.ifBlank { "Прием пищи" },
                calories = state.calories.toFloatOrNull(),
                protein_g = state.protein.toFloatOrNull(),
                fat_g = state.fat.toFloatOrNull(),
                carbs_g = state.carbs.toFloatOrNull(),
                fiber_g = null,
                sugar_g = null,
                caffeine_mg = state.caffeineMg.toFloatOrNull(),
                water_ml = null,
                portion_g = null,
                glycemic_load = null,
                meal_category = null,
                minutes_before_sleep = null,
                is_late_meal = null,
                meal_time = now.format(formatter),
                notes = null,
                source = "manual"
            )

            _uiState.value = state.copy(
                isSaving = true,
                error = null
            )

            repository.createMeal(request)
                .onSuccess { meal ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        todayMeal = meal,
                        mealName = "",
                        calories = "",
                        protein = "",
                        fat = "",
                        carbs = "",
                        caffeineMg = "",
                        error = null
                    )
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = throwable.message ?: "Не удалось сохранить прием пищи"
                    )
                }
        }
    }
}