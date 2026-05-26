package com.example.healtapp.features.profile.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AgeUtils
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.core.common.AvatarJpegBytes
import com.example.healtapp.core.export.HealthReportExporter
import com.example.healtapp.data.network.dto.profile.ProfileDto
import com.example.healtapp.core.ui.theme.ThemeMode
import com.example.healtapp.data.preferences.ThemePreferences
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.AuthRepository
import com.example.healtapp.data.preferences.WeightHistoryStore
import com.example.healtapp.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage,
    private val themePreferences: ThemePreferences,
    @ApplicationContext private val appContext: Context,
    private val weightHistoryStore: WeightHistoryStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    private var cachedProfile: ProfileDto? = null

    init {
        load()
        viewModelScope.launch {
            themePreferences.themeModeFlow().collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun load() {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.value = ProfileEditUiState(
                    isLoading = false,
                    guestMode = true,
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null, guestMode = false)

            val result = profileRepository.getMyProfile()
            result.onSuccess { profile ->
                cachedProfile = profile
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    guestMode = false,
                    error = null,
                    birthDate = profile.age?.let { AgeUtils.estimatedBirthDateFromAge(it) }.orEmpty(),
                    age = profile.age?.toString().orEmpty(),
                    sex = profile.sex ?: _uiState.value.sex,
                    height = profile.height_cm?.toString().orEmpty(),
                    weight = profile.weight_kg?.toString().orEmpty(),
                    goal = profile.goal ?: _uiState.value.goal,
                    activityLevel = profile.activity_level ?: _uiState.value.activityLevel,
                    targetSleep = profile.target_sleep_hours?.toString() ?: _uiState.value.targetSleep,
                    targetWater = profile.target_water_ml?.toInt()?.toString() ?: _uiState.value.targetWater,
                    targetSteps = profile.target_steps?.toString() ?: _uiState.value.targetSteps,
                    targetCalories = profile.target_daily_calories?.toString() ?: _uiState.value.targetCalories,
                    targetProtein = profile.target_protein_g?.let { "%.0f".format(it) }.orEmpty(),
                    targetFat = profile.target_fat_g?.let { "%.0f".format(it) }.orEmpty(),
                    targetCarbs = profile.target_carbs_g?.let { "%.0f".format(it) }.orEmpty(),
                    isVegetarian = profile.is_vegetarian == true,
                    hasAllergies = profile.has_allergies == true,
                    allergiesText = profile.allergies_text.orEmpty(),
                    hasAvatar = profile.hasAvatar,
                    weightHistory = weightHistoryStore.loadEntries(),
                    weightWeeklyReminder = if (weightHistoryStore.shouldShowWeeklyReminder()) {
                        "Раз в неделю обновляйте вес в «Основных данных» — так точнее ИМТ и КБЖУ."
                    } else {
                        null
                    },
                    avatarLoadNonce = if (profile.hasAvatar) {
                        _uiState.value.avatarLoadNonce + 1
                    } else {
                        _uiState.value.avatarLoadNonce
                    },
                )
            }.onFailure { t ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UserFacingMessages.fromThrowable(t, "Не удалось загрузить профиль"),
                )
            }
        }
    }

    fun uploadAvatarFromUri(uri: Uri) {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                update {
                    copy(error = "В демо-режиме фото на сервер не загружается. Войди в аккаунт.")
                }
                return@launch
            }
            _uiState.value = _uiState.value.copy(isUploadingAvatar = true, error = null, success = null)

            val uploadResult = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = AvatarJpegBytes.fromUri(appContext, uri)
                    profileRepository.uploadAvatar(bytes, "image/jpeg").getOrThrow()
                }
            }

            uploadResult.onSuccess { dto ->
                _uiState.value = _uiState.value.copy(
                    isUploadingAvatar = false,
                    hasAvatar = dto.hasAvatar,
                    avatarLoadNonce = _uiState.value.avatarLoadNonce + 1,
                    success = "Фото профиля обновлено",
                    error = null,
                )
            }.onFailure { t ->
                _uiState.value = _uiState.value.copy(
                    isUploadingAvatar = false,
                    error = UserFacingMessages.fromThrowable(t, "Не удалось загрузить фото"),
                )
            }
        }
    }

    fun deleteAvatar() {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                update { copy(error = "В демо-режиме изменения на сервере недоступны.") }
                return@launch
            }
            _uiState.value = _uiState.value.copy(isUploadingAvatar = true, error = null, success = null)

            val result = profileRepository.deleteAvatar()

            result.onSuccess { dto ->
                _uiState.value = _uiState.value.copy(
                    isUploadingAvatar = false,
                    hasAvatar = dto.hasAvatar,
                    avatarLoadNonce = _uiState.value.avatarLoadNonce + 1,
                    success = "Фото профиля удалено",
                    error = null,
                )
            }.onFailure { t ->
                _uiState.value = _uiState.value.copy(
                    isUploadingAvatar = false,
                    error = UserFacingMessages.fromThrowable(t, "Не удалось удалить фото"),
                )
            }
        }
    }

    fun exportHealthReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingReport = true, error = null) }
            runCatching {
                val profile = cachedProfile ?: profileRepository.getMyProfile().getOrNull()
                val text = HealthReportExporter.buildReportText(appContext, profile)
                HealthReportExporter.shareAsFile(appContext, text)
            }.onFailure { t ->
                _uiState.update {
                    it.copy(error = UserFacingMessages.fromThrowable(t, "Не удалось сформировать отчёт"))
                }
            }
            _uiState.update { it.copy(isExportingReport = false) }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    fun updateBirthDate(value: String) {
        val age = AgeUtils.ageFromBirthDate(value)
        update {
            copy(
                birthDate = value,
                age = age?.toString().orEmpty(),
                error = null,
                success = null,
            )
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
    fun updateTargetSteps(value: String) = update {
        copy(targetSteps = value.filter { ch -> ch.isDigit() }, error = null, success = null)
    }
    fun updateTargetCalories(value: String) = update { copy(targetCalories = value, error = null, success = null) }
    fun updateTargetProtein(value: String) = update { copy(targetProtein = value, error = null, success = null) }
    fun updateTargetFat(value: String) = update { copy(targetFat = value, error = null, success = null) }
    fun updateTargetCarbs(value: String) = update { copy(targetCarbs = value, error = null, success = null) }
    fun updateIsVegetarian(value: Boolean) = update { copy(isVegetarian = value, error = null, success = null) }
    fun updateHasAllergies(value: Boolean) = update {
        copy(
            hasAllergies = value,
            allergiesText = if (!value) "" else allergiesText,
            error = null,
            success = null,
        )
    }
    fun updateAllergiesText(value: String) = update { copy(allergiesText = value, error = null, success = null) }

    fun updateCurrentPassword(value: String) = update { copy(currentPassword = value, error = null, success = null) }
    fun updateNewPassword(value: String) = update { copy(newPassword = value, error = null, success = null) }
    fun updateConfirmPassword(value: String) = update { copy(confirmPassword = value, error = null, success = null) }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.value = state.copy(
                    isSaving = false,
                    error = "В демо-режиме профиль на сервер не сохраняется. Войди в аккаунт.",
                )
                return@launch
            }
            _uiState.value = state.copy(isSaving = true, error = null, success = null)

            val ageYears = AgeUtils.ageFromBirthDate(state.birthDate)
                ?: state.age.toIntOrNull()

            val result = profileRepository.updateMyProfile(
                age = ageYears,
                sex = state.sex,
                heightCm = state.height.toFloatOrNull(),
                weightKg = state.weight.toFloatOrNull(),
                goal = state.goal,
                activityLevel = state.activityLevel,
                targetSleepHours = state.targetSleep.toFloatOrNull(),
                targetWaterMl = state.targetWater.toFloatOrNull(),
                targetSteps = state.targetSteps.toIntOrNull()?.takeIf { it >= 1000 },
                targetDailyCalories = state.targetCalories.toIntOrNull(),
                targetProteinG = state.targetProtein.toFloatOrNull(),
                targetFatG = state.targetFat.toFloatOrNull(),
                targetCarbsG = state.targetCarbs.toFloatOrNull(),
                isVegetarian = state.isVegetarian,
                hasAllergies = state.hasAllergies,
                allergiesText = if (state.hasAllergies) state.allergiesText.trim().take(500).ifBlank { null } else null,
            )

            result.onSuccess { profile ->
                cachedProfile = profile
                state.weight.toFloatOrNull()?.let { weightHistoryStore.append(it) }
                weightHistoryStore.markWeeklyPromptShown()
                val ageStr = AgeUtils.ageFromBirthDate(state.birthDate)?.toString()
                    ?: profile.age?.toString()
                    ?: state.age
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    success = "Профиль успешно сохранён",
                    age = ageStr,
                    weightHistory = weightHistoryStore.loadEntries(),
                    weightWeeklyReminder = null,
                )
                AppRefreshBus.notifyDataChanged()
            }.onFailure { t ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = UserFacingMessages.fromThrowable(t, "Не удалось сохранить профиль"),
                )
            }
        }
    }

    fun changeAccountPassword() {
        val state = _uiState.value
        if (state.currentPassword.isBlank()) {
            update { copy(error = "Введите текущий пароль") }
            return
        }
        if (state.newPassword.length < 6) {
            update { copy(error = "Новый пароль — не короче 6 символов") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            update { copy(error = "Новый пароль и подтверждение не совпадают") }
            return
        }
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.value = _uiState.value.copy(error = "Смена пароля доступна после входа в аккаунт.")
                return@launch
            }
            _uiState.value = state.copy(isChangingPassword = true, error = null, success = null)

            val result = authRepository.changePassword(state.currentPassword, state.newPassword)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isChangingPassword = false,
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = "",
                    success = "Пароль обновлён",
                )
            }.onFailure { t ->
                _uiState.value = _uiState.value.copy(
                    isChangingPassword = false,
                    error = UserFacingMessages.fromThrowable(t, "Не удалось сменить пароль"),
                )
            }
        }
    }

    private inline fun update(block: ProfileEditUiState.() -> ProfileEditUiState) {
        _uiState.value = _uiState.value.block()
    }
}
