package com.example.healtapp.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.domain.repository.ActivityRepository
import com.example.healtapp.domain.repository.HydrationRepository
import com.example.healtapp.domain.repository.MealRepository
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.domain.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val profileRepository: ProfileRepository,
    private val sleepRepository: SleepRepository,
    private val hydrationRepository: HydrationRepository,
    private val activityRepository: ActivityRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val profileResult = profileRepository.getMyProfile()
                val sleepResult = sleepRepository.getSleepHistory()
                val hydrationResult = hydrationRepository.getTodayHydrationSummary()
                val activityResult = activityRepository.getTodayActivity()
                val mealResult = mealRepository.getTodayMeal()

                val profile = profileResult.getOrNull()
                val sleepList = sleepResult.getOrNull().orEmpty()
                val hydration = hydrationResult.getOrNull()
                val activity = activityResult.getOrNull()
                val meal = mealResult.getOrNull()

                val latestSleep = sleepList
                    .sortedByDescending { it.sleep_start }
                    .firstOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,

                    greetingText = "Добро пожаловать",
                    userName = "друг",

                    sleepHours = latestSleep?.duration_hours ?: 0f,
                    sleepTargetHours = profile?.target_sleep_hours ?: 8f,
                    sleepQuality = latestSleep?.quality_score?.toString() ?: "—",

                    waterMl = hydration?.total_ml ?: 0,
                    waterTargetMl = (profile?.target_water_ml ?: 2500f).toInt(),

                    caloriesToday = (meal?.calories ?: 0f).toInt(),
                    caloriesTarget = 2200,
                    caffeineToday = meal?.caffeine_mg ?: 0f,

                    stepsToday = activity?.steps ?: 0,
                    activityMinutesToday = activity?.duration_minutes ?: 0,
                    caloriesBurnedToday = (activity?.calories_burned ?: 0f).toInt()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки dashboard"
                )
            }
        }
    }
}