package com.example.healtapp.features.meal.presentation

import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import com.example.healtapp.features.meal.util.FatSecretFoodHit
import com.example.healtapp.features.meal.util.FatSecretServingOption

data class MealUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSavingTargets: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,

    val mealType: String = "Завтрак",
    val mealName: String = "",
    val calories: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val caffeineMg: String = "",

    val portionMultiplier: Float = 1f,
    val servingOptions: List<FatSecretServingOption> = emptyList(),
    val selectedServingIndex: Int = 0,

    val todayMeal: MealDto? = null,
    val mealHistory: List<MealDto> = emptyList(),
    val savedDishes: List<SavedDishDto> = emptyList(),

    val caloriesTarget: Int = 2200,
    val targetProteinG: Float? = null,
    val targetFatG: Float? = null,
    val targetCarbsG: Float? = null,
    /** Подсказка в блоке «Сводка»: откуда взяты ориентиры КБЖУ. */
    val nutritionTargetsHint: String? = null,
    val progressCelebrateToken: Int = 0,
    val pendingSyncCount: Int = 0,

    val dayCaloriesTotal: Int = 0,
    val dayProteinTotal: Float = 0f,
    val dayFatTotal: Float = 0f,
    val dayCarbsTotal: Float = 0f,
    val dayCaffeineTotal: Float = 0f,

    val foodSearchQuery: String = "",
    val foodSearchResults: List<FatSecretFoodHit> = emptyList(),
    val isFoodSearchLoading: Boolean = false,
    val foodSearchError: String? = null,
)
