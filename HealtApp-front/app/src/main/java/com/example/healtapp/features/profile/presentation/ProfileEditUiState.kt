package com.example.healtapp.features.profile.presentation

import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.theme.ThemeMode

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null,
    val success: String? = null,

    val hasAvatar: Boolean = false,
    /** Меняется после загрузки/смены фото, чтобы Coil не брал старый кэш. */
    val avatarLoadNonce: Int = 0,

    val age: String = "",
    val sex: String = Constants.Sex.MALE,
    val height: String = "",
    val weight: String = "",
    val goal: String = Constants.Goals.IMPROVE_ENERGY,
    val activityLevel: String = Constants.ActivityLevel.MEDIUM,
    val targetSleep: String = "8",
    val targetWater: String = "2500",
    val targetSteps: String = "10000",
    val targetCalories: String = "2200",
    val targetProtein: String = "",
    val targetFat: String = "",
    val targetCarbs: String = "",

    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isChangingPassword: Boolean = false,
    val isExportingReport: Boolean = false,

    /** Локальный демо-режим: экран открыт без JWT, сохранение на сервер недоступно. */
    val guestMode: Boolean = false,

    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
