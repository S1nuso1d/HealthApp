package com.example.healtapp.features.meal.presentation

import com.example.healtapp.data.network.dto.meal.MealDto

data class MealUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,

    val mealType: String = "",
    val mealName: String = "",
    val calories: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val caffeineMg: String = "",

    val todayMeal: MealDto? = null
)