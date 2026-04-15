package com.example.healtapp.features.onboarding.presentation

import com.example.healtapp.core.common.Constants

data class OnboardingUiState(
    val age: String = "",
    val sex: String = Constants.Sex.MALE,
    val height: String = "",
    val weight: String = "",
    val targetSleep: String = "8",
    val targetWater: String = "2500",
    val goal: String = Constants.Goals.BETTER_SLEEP,
    val activityLevel: String = Constants.ActivityLevel.MEDIUM,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)