package com.example.healtapp.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.core.common.LocalDemoData
import com.example.healtapp.data.healthconnect.HealthConnectReader
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.ActivityRepository
import com.example.healtapp.domain.repository.HydrationRepository
import com.example.healtapp.domain.repository.MealRepository
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.domain.repository.SleepRepository
import com.example.healtapp.domain.repository.WellnessRepository
import com.example.healtapp.features.activity.presentation.ActivityStepsHelper
import com.example.healtapp.features.activity.presentation.isWalkLikeApi
import com.example.healtapp.features.profile.ProfileRus
import com.example.healtapp.widget.refreshHealthWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val tokenStorage: TokenStorage,
    private val profileRepository: ProfileRepository,
    private val sleepRepository: SleepRepository,
    private val hydrationRepository: HydrationRepository,
    private val activityRepository: ActivityRepository,
    private val mealRepository: MealRepository,
    private val wellnessRepository: WellnessRepository,
    private val healthConnectReader: HealthConnectReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        viewModelScope.launch {
            AppRefreshBus.events.collect { loadDashboard() }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.value = LocalDemoData.dashboardUiState()
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null, isGuestMode = false) }

            runCatching {
                coroutineScope {
                    val profileDeferred = async { profileRepository.getMyProfile() }
                    val sleepDeferred = async { sleepRepository.getSleepHistory() }
                    val hydrationDeferred = async { hydrationRepository.getTodayHydrationSummary() }
                    val activityDeferred = async { activityRepository.getActivityHistory() }
                    val mealDeferred = async { mealRepository.getTodayMeal() }
                    val homeDeferred = async { wellnessRepository.getDashboardHome(7) }
                    val remindersDeferred = async { wellnessRepository.activeReminders() }
                    val statesDeferred = async { wellnessRepository.listUserStates() }

                    val profileResult = profileDeferred.await()
                    val sleepResult = sleepDeferred.await()
                    val hydrationResult = hydrationDeferred.await()
                    val activityResult = activityDeferred.await()
                    val mealResult = mealDeferred.await()
                    val homeResult = homeDeferred.await()
                    val remindersResult = remindersDeferred.await()
                    val statesResult = statesDeferred.await()

                    val failures = listOf(
                        profileResult,
                        sleepResult,
                        hydrationResult,
                        activityResult,
                        mealResult,
                    ).filter { it.isFailure }

                    if (failures.size == 5) {
                        throw failures.first().exceptionOrNull()
                            ?: IllegalStateException("Не удалось загрузить сводку")
                    }

                    val profile = profileResult.getOrNull()
                    val sleepList = sleepResult.getOrNull().orEmpty()
                    val hydration = hydrationResult.getOrNull()
                    val activityHistory = activityResult.getOrNull().orEmpty()
                    val meal = mealResult.getOrNull()
                    val home = homeResult.getOrNull()
                    val reminders = remindersResult.getOrNull().orEmpty()
                    val states = statesResult.getOrNull().orEmpty()

                    val todayKey = LocalDate.now().toString()
                    val yesterdayKey = LocalDate.now().minusDays(1).toString()
                    val todayActivities = activityHistory.filter { it.start_time.take(10) == todayKey }
                    val hcSteps = runCatching { healthConnectReader.readTodaySteps() }.getOrNull()
                    val stepsFromDb = ActivityStepsHelper.sumStepsForDate(activityHistory, todayKey)
                    val stepsToday = hcSteps?.takeIf { it > 0 } ?: stepsFromDb

                    val latestSleep = sleepList
                        .filter {
                            val key = it.sleep_start.take(10)
                            key == todayKey || key == yesterdayKey
                        }
                        .maxByOrNull { it.sleep_end }
                        ?: sleepList.maxByOrNull { it.sleep_end }

                    val goalLabel = profile?.goal?.takeIf { it.isNotBlank() }?.let { ProfileRus.goalLabel(it) }
                    val headerSubtitle = buildString {
                        append("Сводка за сегодня")
                        goalLabel?.let { append(" · цель: $it") }
                    }

                    val summary = home?.analytics?.summary
                    val scores = ScoreBreakdownUi(
                        healthScore = summary?.healthScore ?: 0,
                        sleepScore = summary?.sleepScore ?: 0,
                        hydrationScore = summary?.hydrationScore ?: 0,
                        activityScore = summary?.activityScore ?: 0,
                        nutritionScore = summary?.nutritionScore ?: 0,
                        stateScore = summary?.stateScore ?: 0,
                    )

                    val brief = home?.dailyBrief?.let {
                        DailyBriefUi(
                            title = it.title,
                            summary = it.summary,
                            keyPoints = it.keyPoints,
                        )
                    }

                    val plan = home?.actionPlan.orEmpty().map {
                        ActionPlanItemUi(
                            id = it.id,
                            title = it.title,
                            description = it.description,
                            category = it.category,
                            status = it.status,
                            priority = it.priority,
                        )
                    }

                    val smartReminders = reminders.map {
                        SmartReminderUi(
                            id = it.id,
                            title = it.title,
                            message = it.message,
                            type = it.reminderType,
                            status = it.status,
                        )
                    }

                    val insights = home?.analytics?.insights.orEmpty().take(3).map {
                        InsightUi(
                            title = it.title,
                            description = it.description,
                            category = it.category,
                            severity = it.severity,
                        )
                    }

                    val savedToday = states.any { state ->
                        runCatching {
                            LocalDateTime.parse(state.recordTime.take(19)).toLocalDate() == LocalDate.now()
                        }.getOrElse { state.recordTime.startsWith(todayKey) }
                    }

                    val waterStreak = computeWaterStreak(activityHistory, hydration?.total_ml ?: 0, profile?.target_water_ml?.toInt() ?: 2500)
                    val stepsStreak = computeStepsStreak(activityHistory, stepsToday, profile?.target_steps ?: 10_000)

                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        error = failures.singleOrNull()?.exceptionOrNull()?.message?.takeIf { failures.size >= 3 },
                        isGuestMode = false,
                        isOfflineCache = homeResult.isFailure && home != null,
                        greetingText = DashboardGreeting.forNow(),
                        headerSubtitle = headerSubtitle,
                        sleepHours = latestSleep?.duration_hours ?: 0f,
                        sleepTargetHours = profile?.target_sleep_hours?.takeIf { it > 0f } ?: 8f,
                        sleepQuality = latestSleep?.quality_score?.toString() ?: "—",
                        waterMl = hydration?.total_ml ?: 0,
                        waterTargetMl = (profile?.target_water_ml ?: 2500f).toInt(),
                        caloriesToday = (meal?.calories ?: 0f).toInt(),
                        caloriesTarget = profile?.target_daily_calories?.takeIf { it > 0 } ?: 2200,
                        caffeineToday = meal?.caffeine_mg ?: 0f,
                        stepsToday = stepsToday,
                        stepsGoal = profile?.target_steps?.takeIf { it > 0 } ?: 10_000,
                        activityMinutesToday = todayActivities
                            .filter { !isWalkLikeApi(it.activity_type) }
                            .sumOf { it.duration_minutes.toLong() }.toInt(),
                        caloriesBurnedToday = todayActivities.sumOf { (it.calories_burned ?: 0f).toDouble() }.toInt(),
                        scores = scores,
                        dailyBrief = brief,
                        actionPlanItems = plan,
                        smartReminders = smartReminders,
                        topInsights = insights,
                        moodCheckIn = MoodCheckInUi(savedToday = savedToday),
                        waterStreakDays = waterStreak,
                        stepsStreakDays = stepsStreak,
                    )
                    if (homeResult.isSuccess) {
                        refreshHealthWidget(appContext)
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось загрузить сводку",
                    )
                }
            }
        }
    }

    fun updateMood(mood: Int) {
        _uiState.update { it.copy(moodCheckIn = it.moodCheckIn.copy(mood = mood.coerceIn(1, 10))) }
    }

    fun updateEnergy(energy: Int) {
        _uiState.update { it.copy(moodCheckIn = it.moodCheckIn.copy(energy = energy.coerceIn(1, 10))) }
    }

    fun updateStress(stress: Int) {
        _uiState.update { it.copy(moodCheckIn = it.moodCheckIn.copy(stress = stress.coerceIn(1, 10))) }
    }

    fun submitMoodCheckIn() {
        val check = _uiState.value.moodCheckIn
        viewModelScope.launch {
            _uiState.update { it.copy(moodCheckIn = it.moodCheckIn.copy(isSaving = true)) }
            wellnessRepository.createUserState(
                mood = check.mood,
                energy = check.energy,
                stress = check.stress,
                focus = null,
                notes = null,
            ).onSuccess {
                _uiState.update {
                    it.copy(moodCheckIn = it.moodCheckIn.copy(isSaving = false, savedToday = true))
                }
                AppRefreshBus.notifyDataChanged()
                loadDashboard()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        moodCheckIn = it.moodCheckIn.copy(isSaving = false),
                        error = e.message,
                    )
                }
            }
        }
    }

    fun completeReminder(id: Int) {
        viewModelScope.launch {
            wellnessRepository.completeReminder(id).onSuccess { loadDashboard() }
        }
    }

    fun dismissReminder(id: Int) {
        viewModelScope.launch {
            wellnessRepository.dismissReminder(id).onSuccess { loadDashboard() }
        }
    }

    fun toggleActionPlanStatus(item: ActionPlanItemUi) {
        val next = when (item.status) {
            "done" -> "pending"
            else -> "done"
        }
        viewModelScope.launch {
            wellnessRepository.updateActionPlanStatus(item.id, next).onSuccess {
                AppRefreshBus.notifyDataChanged()
                loadDashboard()
            }
        }
    }

    private fun computeWaterStreak(
        @Suppress("UNUSED_PARAMETER") activityHistory: List<com.example.healtapp.data.network.dto.activity.ActivityDto>,
        todayWater: Int,
        target: Int,
    ): Int {
        if (todayWater >= target) return 1
        return 0
    }

    private fun computeStepsStreak(
        activityHistory: List<com.example.healtapp.data.network.dto.activity.ActivityDto>,
        todaySteps: Int,
        goal: Int,
    ): Int {
        var streak = if (todaySteps >= goal) 1 else 0
        val today = LocalDate.now()
        for (offset in 1 until 7) {
            val key = today.minusDays(offset.toLong()).toString()
            val steps = ActivityStepsHelper.sumStepsForDate(activityHistory, key)
            if (steps >= goal) streak++ else break
        }
        return streak
    }
}
