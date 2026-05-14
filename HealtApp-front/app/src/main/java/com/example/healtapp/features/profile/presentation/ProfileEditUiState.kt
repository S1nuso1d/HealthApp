package com.example.healtapp.features.profile.presentation

import com.example.healtapp.core.common.Constants

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: String? = null,

    val age: String = "",
    val sex: String = Constants.Sex.MALE,
    val height: String = "",
    val weight: String = "",
    val goal: String = Constants.Goals.IMPROVE_ENERGY,
    val activityLevel: String = Constants.ActivityLevel.MEDIUM,
    val targetSleep: String = "8",
    val targetWater: String = "2500",
)

