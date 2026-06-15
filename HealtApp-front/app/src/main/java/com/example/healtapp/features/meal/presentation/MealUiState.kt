package com.example.healtapp.features.meal.presentation

import com.example.healtapp.core.common.NutritionTargetsCalculator
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.network.dto.meal.SavedDishDto
import com.example.healtapp.data.network.dto.meal.FoodCatalogItemDto
import com.example.healtapp.features.meal.util.FatSecretServingOption

private val defaultTargets = NutritionTargetsCalculator.defaultTargets()

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

    val caloriesTarget: Int = defaultTargets.calories,
    val targetProteinG: Float = defaultTargets.proteinG,
    val targetFatG: Float = defaultTargets.fatG,
    val targetCarbsG: Float = defaultTargets.carbsG,
    /** Подсказка в блоке «Сводка»: откуда взяты ориентиры КБЖУ. */
    val nutritionTargetsHint: String? = "Ориентиры КБЖУ",
    val progressCelebrateToken: Int = 0,
    val pendingSyncCount: Int = 0,

    val dayCaloriesTotal: Int = 0,
    val dayProteinTotal: Float = 0f,
    val dayFatTotal: Float = 0f,
    val dayCarbsTotal: Float = 0f,
    val dayCaffeineTotal: Float = 0f,

    val foodSearchQuery: String = "",
    val foodSearchResults: List<FoodCatalogItemDto> = emptyList(),
    val isFoodSearchLoading: Boolean = false,
    val foodSearchError: String? = null,

    val selectedCatalogItem: FoodCatalogItemDto? = null,
    val showMacroCompletionSheet: Boolean = false,
    val showAddCustomFoodSheet: Boolean = false,
    val macroCompletionProtein: String = "",
    val macroCompletionFat: String = "",
    val macroCompletionCarbs: String = "",
    val isCatalogSaving: Boolean = false,
)
