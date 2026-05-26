package com.example.healtapp.features.onboarding.presentation

import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.common.NutritionTargetsCalculator

data class OnboardingUiState(
    val step: Int = 0,
    val totalSteps: Int = 5,
    val isVegetarian: Boolean = false,
    val hasAllergies: Boolean = false,
    val allergiesText: String = "",
    val age: String = "",
    val sex: String = Constants.Sex.MALE,
    val height: String = "",
    val weight: String = "",
    val goal: String = Constants.Goals.IMPROVE_ENERGY,
    val activityLevel: String = Constants.ActivityLevel.MEDIUM,
    val previewTargets: NutritionTargetsCalculator.Result? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
)
