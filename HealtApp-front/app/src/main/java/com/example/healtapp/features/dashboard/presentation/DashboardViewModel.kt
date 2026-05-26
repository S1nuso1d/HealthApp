package com.example.healtapp.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.core.common.LocalDemoData
import com.example.healtapp.core.common.WeeklySummaryCalculator
import com.example.healtapp.data.healthconnect.HealthConnectReader
import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.domain.repository.ActivityRepository
import com.example.healtapp.domain.repository.HydrationRepository
import com.example.healtapp.domain.repository.MealRepository
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.domain.repository.SleepRepository
import com.example.healtapp.core.common.ActionPlanAutoComplete
import com.example.healtapp.core.common.CalorieBurnCalculator
import com.example.healtapp.core.common.DailyAdviceBuilder
import com.example.healtapp.data.network.api.DashboardApi
import com.example.healtapp.data.network.api.HealthApi
import com.example.healtapp.data.network.dto.ai.AIRecommendationDto
import com.example.healtapp.domain.repository.AiRepository
import com.example.healtapp.data.preferences.DashboardCache
import com.example.healtapp.data.preferences.ProfileCache
import com.example.healtapp.data.preferences.WidgetSnapshot
import com.example.healtapp.data.preferences.WidgetSnapshotStore
import com.example.healtapp.features.recommendations.presentation.RecommendationUiItem
import com.example.healtapp.data.network.dto.dashboard.GoalsCalendarDayDto
import com.example.healtapp.domain.repository.WellnessRepository
import com.example.healtapp.features.activity.presentation.ActivityStepsHelper
import com.example.healtapp.features.activity.presentation.isWalkLikeApi
import com.example.healtapp.features.sleep.presentation.SleepHelper
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

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
    private val aiRepository: AiRepository,
    private val dashboardApi: DashboardApi,
    private val healthApi: HealthApi,
    private val healthConnectReader: HealthConnectReader,
    private val widgetSnapshotStore: WidgetSnapshotStore,
    private val dashboardCache: DashboardCache,
    private val profileCache: ProfileCache,
) : ViewModel() {

    private var lastCalendarMonth: YearMonth? = null
    private var cachedActivityHistory: List<ActivityDto> = emptyList()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            showCachedBootstrapIfAny()
            loadDashboard(showFullLoading = !_uiState.value.hasLoadedOnce)
        }
        viewModelScope.launch {
            AppRefreshBus.events
                .debounce(900)
                .collect {
                    loadDashboard(showFullLoading = false)
                    loadGoalsCalendar(_uiState.value.goalsCalendarMonth)
                }
        }
    }

    fun shiftGoalsCalendarMonth(delta: Int) {
        val next = _uiState.value.goalsCalendarMonth.plusMonths(delta.toLong())
        _uiState.update { it.copy(goalsCalendarMonth = next, goalsCalendarSelectedDate = null) }
        loadGoalsCalendar(next)
    }

    fun onGoalsCalendarDayClick(date: LocalDate) {
        _uiState.update {
            it.copy(
                goalsCalendarSelectedDate = date,
                goalsCalendarDetailDate = date,
            )
        }
    }

    fun dismissGoalsCalendarDayDetail() {
        _uiState.update { it.copy(goalsCalendarDetailDate = null) }
    }

    private fun loadGoalsCalendar(month: YearMonth) {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.update {
                    it.copy(goalsCalendarDays = emptyList(), goalsCalendarLoading = false)
                }
                return@launch
            }
            val monthChanged = lastCalendarMonth != month
            if (monthChanged || _uiState.value.goalsCalendarDays.isEmpty()) {
                _uiState.update { it.copy(goalsCalendarLoading = true, goalsCalendarMonth = month) }
            }
            lastCalendarMonth = month
            runCatching {
                dashboardApi.getGoalsCalendar(year = month.year, month = month.monthValue)
            }.onSuccess { resp ->
                _uiState.update {
                    it.copy(
                        goalsCalendarDays = resp.days,
                        goalsCalendarLoading = false,
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(goalsCalendarLoading = false) }
            }
        }
    }

    fun refresh() {
        loadDashboard(showFullLoading = false)
        loadGoalsCalendar(_uiState.value.goalsCalendarMonth)
    }

    fun loadDashboard(showFullLoading: Boolean = false) {
        viewModelScope.launch {
            if (tokenStorage.isGuestMode()) {
                _uiState.value = LocalDemoData.dashboardUiState()
                return@launch
            }

            val showSkeleton = (showFullLoading || !_uiState.value.hasLoadedOnce) && !_uiState.value.isOfflineCache
            _uiState.update {
                it.copy(
                    isLoading = showSkeleton,
                    isRefreshing = !showSkeleton && it.hasLoadedOnce,
                    error = null,
                    isGuestMode = false,
                )
            }

            try {
                val phase1 = coroutineScope {
                    val profileDeferred = async { profileRepository.getMyProfile() }
                    val hydrationTodayDeferred = async { hydrationRepository.getTodayHydrationSummary() }
                    val activityDeferred = async { activityRepository.getActivityHistory() }
                    val mealDeferred = async { mealRepository.getTodayMeal() }
                    Phase1Results(
                        profileResult = profileDeferred.await(),
                        hydrationResult = hydrationTodayDeferred.await(),
                        activityResult = activityDeferred.await(),
                        mealResult = mealDeferred.await(),
                    )
                }

                val failures = listOf(
                    phase1.profileResult,
                    phase1.hydrationResult,
                    phase1.activityResult,
                    phase1.mealResult,
                ).filter { it.isFailure }

                if (failures.size == 4) {
                    throw failures.first().exceptionOrNull()
                        ?: IllegalStateException("Не удалось загрузить сводку")
                }

                applyPhase1(phase1, failures)

                launch { loadDashboardHome() }
                launch { loadAiRecommendations() }
                launch { refreshStepsFromHealthConnect() }
                launch { loadDashboardExtras(phase1.profileResult.getOrNull()) }
                loadGoalsCalendar(_uiState.value.goalsCalendarMonth)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Не удалось загрузить сводку",
                    )
                }
            }
        }
    }

    private suspend fun showCachedBootstrapIfAny(): Boolean {
        val home = dashboardCache.load() ?: return false
        val profile = profileCache.load()
        val widget = widgetSnapshotStore.load()
        val summary = home.analytics.summary
        val waterTarget = profile?.target_water_ml?.toInt() ?: widget?.waterGoalMl ?: 2500
        val stepsGoal = profile?.target_steps?.takeIf { it > 0 } ?: widget?.stepsGoal ?: 10_000
        val planRaw = home.actionPlan.map {
            ActionPlanItemUi(
                id = it.id,
                title = it.title,
                description = it.description,
                category = it.category,
                status = it.status,
                priority = it.priority,
            )
        }
        _uiState.value = DashboardUiState(
            isLoading = false,
            hasLoadedOnce = true,
            isOfflineCache = true,
            greetingText = DashboardGreeting.forNow(),
            headerSubtitle = "Сводка за сегодня",
            waterMl = widget?.waterMl ?: 0,
            waterTargetMl = waterTarget,
            stepsToday = widget?.stepsToday ?: 0,
            stepsGoal = stepsGoal,
            scores = ScoreBreakdownUi(
                healthScore = summary.healthScore,
                sleepScore = summary.sleepScore,
                hydrationScore = summary.hydrationScore,
                activityScore = summary.activityScore,
                nutritionScore = summary.nutritionScore,
                stateScore = summary.stateScore,
            ),
            actionPlanItems = planRaw,
            topInsights = home.analytics.insights.take(3).map {
                InsightUi(
                    title = it.title,
                    description = it.description,
                    category = it.category,
                    severity = it.severity,
                )
            },
            recommendationsLoading = true,
        )
        return true
    }

    private suspend fun applyPhase1(phase1: Phase1Results, failures: List<Result<*>>) {
        val previous = _uiState.value
        val profile = phase1.profileResult.getOrNull()
        val hydration = phase1.hydrationResult.getOrNull()
        val activityHistory = phase1.activityResult.getOrNull().orEmpty()
        cachedActivityHistory = activityHistory
        val meal = phase1.mealResult.getOrNull()
        val todayKey = LocalDate.now().toString()
        val todayActivities = activityHistory.filter { it.start_time.take(10) == todayKey }
        val stepsFromDb = ActivityStepsHelper.sumStepsForDate(activityHistory, todayKey)
        val stepsToday = _uiState.value.stepsToday.takeIf { it > 0 && stepsFromDb == 0 } ?: stepsFromDb

        val waterTarget = profile?.target_water_ml?.toInt() ?: 2500
        val stepsGoal = profile?.target_steps?.takeIf { it > 0 } ?: 10_000
        val burnGoal = CalorieBurnCalculator.dailyBurnGoal(stepsGoal, profile?.goal)
        val caloriesBurned = CalorieBurnCalculator.totalBurnedToday(todayActivities, stepsToday)
        val plan = ActionPlanAutoComplete.apply(
            items = previous.actionPlanItems,
            waterMl = hydration?.total_ml ?: 0,
            waterTargetMl = waterTarget,
            stepsToday = stepsToday,
            stepsGoal = stepsGoal,
            caloriesBurnedToday = caloriesBurned,
            caloriesBurnGoal = burnGoal,
            sleepHours = _uiState.value.sleepHours,
            sleepTargetHours = profile?.target_sleep_hours?.takeIf { it > 0f } ?: 8f,
            caloriesToday = (meal?.calories ?: 0f).toInt(),
            caloriesTarget = profile?.target_daily_calories?.takeIf { it > 0 } ?: 2200,
            mealCount = 0,
            activityMinutesToday = todayActivities
                .filter { !isWalkLikeApi(it.activity_type) }
                .sumOf { it.duration_minutes.toLong() }.toInt(),
            moodSavedToday = _uiState.value.moodCheckIn.savedToday,
        )

        val calendarMonth = _uiState.value.goalsCalendarMonth
        val calendarDays = _uiState.value.goalsCalendarDays
        val calendarLoading = _uiState.value.goalsCalendarLoading
        val calendarSelected = _uiState.value.goalsCalendarSelectedDate
        val calendarDetail = _uiState.value.goalsCalendarDetailDate

        _uiState.value = DashboardUiState(
            isLoading = false,
            isRefreshing = false,
            hasLoadedOnce = true,
            error = failures.singleOrNull()?.exceptionOrNull()?.message?.takeIf { failures.size >= 3 },
            isGuestMode = false,
            isOfflineCache = previous.isOfflineCache,
            greetingText = DashboardGreeting.forNow(),
            headerSubtitle = "Сводка за сегодня",
            sleepHours = previous.sleepHours,
            sleepTargetHours = profile?.target_sleep_hours?.takeIf { it > 0f } ?: 8f,
            sleepQuality = previous.sleepQuality,
            waterMl = hydration?.total_ml ?: 0,
            waterTargetMl = waterTarget,
            caloriesToday = (meal?.calories ?: 0f).toInt(),
            caloriesTarget = profile?.target_daily_calories?.takeIf { it > 0 } ?: 2200,
            caffeineToday = meal?.caffeine_mg ?: 0f,
            stepsToday = stepsToday,
            stepsGoal = stepsGoal,
            activityMinutesToday = todayActivities
                .filter { !isWalkLikeApi(it.activity_type) }
                .sumOf { it.duration_minutes.toLong() }.toInt(),
            caloriesBurnedToday = caloriesBurned,
            caloriesBurnGoal = burnGoal,
            scores = previous.scores,
            dailyBrief = buildDailyBrief(
                home = null,
                sleepHours = previous.sleepHours,
                sleepTargetHours = profile?.target_sleep_hours?.takeIf { it > 0f } ?: 8f,
                waterMl = hydration?.total_ml ?: 0,
                waterTargetMl = waterTarget,
                caloriesToday = (meal?.calories ?: 0f).toInt(),
                caloriesTarget = profile?.target_daily_calories?.takeIf { it > 0 } ?: 2200,
                stepsToday = stepsToday,
                stepsGoal = stepsGoal,
            ),
            heartRateBpm = previous.heartRateBpm,
            spo2Percent = previous.spo2Percent,
            actionPlanItems = plan,
            weeklySummary = previous.weeklySummary,
            topInsights = previous.topInsights,
            moodCheckIn = previous.moodCheckIn,
            waterStreakDays = previous.waterStreakDays,
            stepsStreakDays = previous.stepsStreakDays,
            goalsCalendarMonth = calendarMonth,
            goalsCalendarDays = calendarDays,
            goalsCalendarLoading = calendarLoading,
            goalsCalendarSelectedDate = calendarSelected,
            goalsCalendarDetailDate = calendarDetail,
            recommendations = filterAchievedRecommendations(previous.recommendations),
            recommendationsLoading = previous.recommendations.isEmpty(),
            recommendationsError = previous.recommendationsError,
        )
        widgetSnapshotStore.save(
            WidgetSnapshot(
                stepsToday = stepsToday,
                stepsGoal = stepsGoal,
                waterMl = hydration?.total_ml ?: 0,
                waterGoalMl = waterTarget,
            ),
        )
        refreshHealthWidget(appContext)
    }

    private suspend fun loadDashboardHome() {
        val result = wellnessRepository.getDashboardHome(7)
        result.onSuccess { home ->
            val summary = home.analytics.summary
            val insights = home.analytics.insights.take(3).map {
                InsightUi(
                    title = it.title,
                    description = it.description,
                    category = it.category,
                    severity = it.severity,
                )
            }
            _uiState.update { current ->
                val planRaw = home.actionPlan.map {
                    ActionPlanItemUi(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        category = it.category,
                        status = it.status,
                        priority = it.priority,
                    )
                }
                val plan = ActionPlanAutoComplete.apply(
                    items = planRaw,
                    waterMl = current.waterMl,
                    waterTargetMl = current.waterTargetMl,
                    stepsToday = current.stepsToday,
                    stepsGoal = current.stepsGoal,
                    caloriesBurnedToday = current.caloriesBurnedToday,
                    caloriesBurnGoal = current.caloriesBurnGoal,
                    sleepHours = current.sleepHours,
                    sleepTargetHours = current.sleepTargetHours,
                    caloriesToday = current.caloriesToday,
                    caloriesTarget = current.caloriesTarget,
                    mealCount = 0,
                    activityMinutesToday = current.activityMinutesToday,
                    moodSavedToday = current.moodCheckIn.savedToday,
                )
                current.copy(
                    isOfflineCache = false,
                    scores = ScoreBreakdownUi(
                        healthScore = summary.healthScore,
                        sleepScore = summary.sleepScore,
                        hydrationScore = summary.hydrationScore,
                        activityScore = summary.activityScore,
                        nutritionScore = summary.nutritionScore,
                        stateScore = summary.stateScore,
                    ),
                    dailyBrief = buildDailyBrief(
                        home = home,
                        sleepHours = current.sleepHours,
                        sleepTargetHours = current.sleepTargetHours,
                        waterMl = current.waterMl,
                        waterTargetMl = current.waterTargetMl,
                        caloriesToday = current.caloriesToday,
                        caloriesTarget = current.caloriesTarget,
                        stepsToday = current.stepsToday,
                        stepsGoal = current.stepsGoal,
                    ),
                    actionPlanItems = plan,
                    topInsights = insights,
                )
            }
        }.onFailure { e ->
            _uiState.update { current ->
                current.copy(
                    recommendationsLoading = false,
                    recommendationsError = e.message?.takeIf { current.recommendations.isEmpty() },
                )
            }
        }
    }

    private suspend fun loadDashboardExtras(profile: com.example.healtapp.data.network.dto.profile.ProfileDto?) {
        val extras = coroutineScope {
            val sleepDeferred = async { sleepRepository.getSleepHistory() }
            val hydrationHistoryDeferred = async { hydrationRepository.getHydrationHistory() }
            val mealHistoryDeferred = async { mealRepository.getMealHistory() }
            val statesDeferred = async { wellnessRepository.listUserStates() }
            val vitalsDeferred = async {
                runCatching {
                    healthApi.listSamples(days = 3, metrics = "heart_rate_bpm,spo2_percent")
                }
            }
            ExtrasResults(
                sleepList = sleepDeferred.await().getOrNull().orEmpty(),
                hydrationHistory = hydrationHistoryDeferred.await().getOrNull().orEmpty(),
                mealHistory = mealHistoryDeferred.await().getOrNull().orEmpty(),
                states = statesDeferred.await().getOrNull().orEmpty(),
                vitalsSamples = vitalsDeferred.await().getOrNull().orEmpty(),
            )
        }
        val todayKey = LocalDate.now().toString()
        val latestSleep = extras.sleepList
            .filter { SleepHelper.wakeDateKey(it.sleep_end) == todayKey }
            .maxByOrNull { it.sleep_end }
        val todayState = extras.states
            .filter { state ->
                runCatching {
                    LocalDateTime.parse(state.recordTime.take(19)).toLocalDate() == LocalDate.now()
                }.getOrElse { state.recordTime.startsWith(todayKey) }
            }
            .maxByOrNull { it.recordTime }
        val vitalsHeartRate = extras.vitalsSamples
            .filter { it.metric == "heart_rate_bpm" }
            .maxByOrNull { it.recorded_at }
            ?.value1
            ?.toInt()
            ?.takeIf { it in 30..220 }
        val vitalsSpo2 = extras.vitalsSamples
            .filter { it.metric == "spo2_percent" }
            .maxByOrNull { it.recorded_at }
            ?.value1
            ?.toFloat()
            ?.takeIf { it in 70f..100f }
        val activityHistory = cachedActivityHistory
        val weekly = WeeklySummaryCalculator.compute(
            sleeps = extras.sleepList,
            hydrationHistory = extras.hydrationHistory,
            activityHistory = activityHistory,
            mealHistory = extras.mealHistory,
        )
        val weeklySummary = WeeklySummaryUi(
            periodLabel = weekly.periodLabel,
            metrics = weekly.metrics.map { m ->
                WeeklyMetricUi(
                    key = m.key,
                    label = m.label,
                    averageDisplay = m.averageDisplay,
                    daysLogged = m.daysLogged,
                    daysInPeriod = m.daysInPeriod,
                    hint = m.hint,
                )
            },
        )
        val waterTarget = profile?.target_water_ml?.toInt() ?: _uiState.value.waterTargetMl
        val stepsGoal = profile?.target_steps?.takeIf { it > 0 } ?: _uiState.value.stepsGoal
        val stepsToday = _uiState.value.stepsToday
        val mealCountToday = extras.mealHistory.count { it.meal_time.take(10) == todayKey }
        val todayActivities = activityHistory.filter { it.start_time.take(10) == todayKey }
        val burnGoal = CalorieBurnCalculator.dailyBurnGoal(stepsGoal, profile?.goal)
        val caloriesBurned = CalorieBurnCalculator.totalBurnedToday(todayActivities, stepsToday)
        val plan = ActionPlanAutoComplete.apply(
            items = _uiState.value.actionPlanItems,
            waterMl = _uiState.value.waterMl,
            waterTargetMl = waterTarget,
            stepsToday = stepsToday,
            stepsGoal = stepsGoal,
            caloriesBurnedToday = caloriesBurned,
            caloriesBurnGoal = burnGoal,
            sleepHours = latestSleep?.duration_hours ?: 0f,
            sleepTargetHours = profile?.target_sleep_hours?.takeIf { it > 0f } ?: 8f,
            caloriesToday = _uiState.value.caloriesToday,
            caloriesTarget = profile?.target_daily_calories?.takeIf { it > 0 } ?: 2200,
            mealCount = mealCountToday,
            activityMinutesToday = todayActivities
                .filter { !isWalkLikeApi(it.activity_type) }
                .sumOf { it.duration_minutes.toLong() }.toInt(),
            moodSavedToday = todayState != null,
        )

        _uiState.update { current ->
            current.copy(
                isOfflineCache = false,
                recommendations = filterAchievedRecommendations(current.recommendations),
                sleepHours = latestSleep?.duration_hours ?: current.sleepHours,
                sleepQuality = latestSleep?.quality_score?.toString() ?: current.sleepQuality,
                actionPlanItems = plan,
                weeklySummary = weeklySummary,
                moodCheckIn = MoodCheckInUi(
                    mood = todayState?.mood?.coerceIn(1, 10) ?: current.moodCheckIn.mood,
                    energy = todayState?.energy?.coerceIn(1, 10) ?: current.moodCheckIn.energy,
                    stress = todayState?.stress?.coerceIn(1, 10) ?: current.moodCheckIn.stress,
                    savedToday = todayState != null,
                ),
                waterStreakDays = computeWaterStreak(extras.hydrationHistory, waterTarget),
                stepsStreakDays = computeStepsStreak(activityHistory, stepsToday, stepsGoal),
                heartRateBpm = vitalsHeartRate,
                spo2Percent = vitalsSpo2,
                dailyBrief = current.dailyBrief,
            )
        }
    }

    private suspend fun refreshStepsFromHealthConnect() {
        val hcSteps = runCatching { healthConnectReader.readTodaySteps() }.getOrNull() ?: return
        if (hcSteps <= 0) return
        val current = _uiState.value
        if (hcSteps <= current.stepsToday) return
        _uiState.update { it.copy(stepsToday = hcSteps) }
        widgetSnapshotStore.save(
            WidgetSnapshot(
                stepsToday = hcSteps,
                stepsGoal = current.stepsGoal,
                waterMl = current.waterMl,
                waterGoalMl = current.waterTargetMl,
            ),
        )
        refreshHealthWidget(appContext)
    }

    private suspend fun loadAiRecommendations(days: Int = 7) {
        if (tokenStorage.isGuestMode()) return
        _uiState.update {
            it.copy(
                recommendationsLoading = it.recommendations.isEmpty(),
                recommendationsError = null,
            )
        }
        aiRepository.getRecommendations(days).onSuccess { response ->
            val mapped = mapAiRecommendations(response.recommendations)
            _uiState.update { current ->
                current.copy(
                    recommendations = filterAchievedRecommendations(mapped),
                    recommendationsLoading = false,
                    recommendationsError = null,
                )
            }
        }.onFailure { e ->
            _uiState.update { current ->
                current.copy(
                    recommendationsLoading = false,
                    recommendationsError = e.message?.takeIf { current.recommendations.isEmpty() },
                )
            }
        }
    }

    private fun mapAiRecommendations(items: List<AIRecommendationDto>): List<RecommendationUiItem> =
        items
            .filter { rec ->
                rec.status.equals("active", ignoreCase = true) || rec.status.isBlank()
            }
            .map { rec ->
                RecommendationUiItem(
                    category = rec.category,
                    title = rec.title,
                    description = rec.description,
                    priority = rec.priority,
                    status = rec.status,
                    confidence = rec.confidence,
                    action = rec.action,
                    personalizedTip = rec.personalized_tip,
                    progressLabel = rec.progress_label,
                )
            }

    private fun filterAchievedRecommendations(
        items: List<RecommendationUiItem>,
    ): List<RecommendationUiItem> {
        val state = _uiState.value
        return items.filter { rec ->
            if (!rec.status.equals("active", ignoreCase = true) && rec.status.isNotBlank()) {
                return@filter false
            }
            when (rec.category.lowercase()) {
                "hydration" -> state.waterMl < state.waterTargetMl
                "activity" -> state.stepsToday < state.stepsGoal
                "sleep" -> state.sleepHours < state.sleepTargetHours - 0.05f
                "meals", "nutrition" -> state.caloriesToday < (state.caloriesTarget * 0.98f).toInt()
                "state" -> !state.moodCheckIn.savedToday
                else -> true
            }
        }
    }

    private data class Phase1Results(
        val profileResult: Result<com.example.healtapp.data.network.dto.profile.ProfileDto>,
        val hydrationResult: Result<com.example.healtapp.data.network.dto.hydration.HydrationSummaryDto>,
        val activityResult: Result<List<ActivityDto>>,
        val mealResult: Result<com.example.healtapp.data.network.dto.meal.MealDto?>,
    )

    private data class ExtrasResults(
        val sleepList: List<com.example.healtapp.data.network.dto.sleep.SleepDto>,
        val hydrationHistory: List<HydrationDto>,
        val mealHistory: List<com.example.healtapp.data.network.dto.meal.MealDto>,
        val states: List<com.example.healtapp.data.network.dto.wellness.UserStateDto>,
        val vitalsSamples: List<com.example.healtapp.data.network.dto.health.HealthSampleDto>,
    )

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
                loadDashboard(showFullLoading = false)
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

    fun toggleActionPlanStatus(item: ActionPlanItemUi) {
        val next = when (item.status) {
            "done" -> "pending"
            else -> "done"
        }
        viewModelScope.launch {
            val prev = _uiState.value.actionPlanItems
            _uiState.update { state ->
                state.copy(
                    actionPlanItems = state.actionPlanItems.map {
                        if (it.id == item.id) it.copy(status = next) else it
                    },
                )
            }
            wellnessRepository.updateActionPlanStatus(item.id, next).onFailure {
                _uiState.update { it.copy(actionPlanItems = prev) }
            }
        }
    }

    private fun computeWaterStreak(history: List<HydrationDto>, target: Int): Int {
        if (target <= 0) return 0
        val byDay = history.groupBy { it.record_time.take(10) }
        var streak = 0
        var day = LocalDate.now()
        while (true) {
            val key = day.toString()
            val total = byDay[key]?.sumOf { it.amount_ml } ?: 0
            if (total >= target) {
                streak++
                day = day.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    private fun computeStepsStreak(
        activityHistory: List<ActivityDto>,
        todaySteps: Int,
        goal: Int,
    ): Int {
        if (goal <= 0 || todaySteps < goal) return 0
        var streak = 1
        var day = LocalDate.now().minusDays(1)
        while (true) {
            val steps = ActivityStepsHelper.sumStepsForDate(activityHistory, day.toString())
            if (steps >= goal) {
                streak++
                day = day.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    private fun buildDailyBrief(
        home: com.example.healtapp.data.network.dto.wellness.DashboardHomeDto?,
        sleepHours: Float,
        sleepTargetHours: Float,
        waterMl: Int,
        waterTargetMl: Int,
        caloriesToday: Int,
        caloriesTarget: Int,
        stepsToday: Int,
        stepsGoal: Int,
    ): DailyBriefUi {
        home?.dailyBrief?.let { brief ->
            return DailyBriefUi(
                title = brief.title,
                summary = brief.summary,
                keyPoints = brief.keyPoints,
            )
        }
        val advice = DailyAdviceBuilder.build(
            DailyAdviceBuilder.Input(
                sleepHours = sleepHours,
                sleepTargetHours = sleepTargetHours,
                waterMl = waterMl,
                waterTargetMl = waterTargetMl,
                caloriesToday = caloriesToday,
                caloriesTarget = caloriesTarget,
                stepsToday = stepsToday,
                stepsGoal = stepsGoal,
            ),
        ) ?: return DailyBriefUi(
            title = "Совет дня",
            summary = "Добавьте данные за сегодня.",
            keyPoints = emptyList(),
        )
        return DailyBriefUi(
            title = advice.title,
            summary = advice.summary,
            keyPoints = advice.keyPoints,
        )
    }
}
