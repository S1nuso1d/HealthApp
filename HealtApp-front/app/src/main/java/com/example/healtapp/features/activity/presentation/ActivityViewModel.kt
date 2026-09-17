package com.example.healtapp.features.activity.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.core.common.CalorieBurnCalculator
import com.example.healtapp.data.healthconnect.HealthConnectForegroundSync
import com.example.healtapp.data.healthconnect.HealthConnectReader
import com.example.healtapp.data.preferences.TrainingPrefs
import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.domain.repository.ActivityRepository
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.domain.repository.WellnessRepository
import com.example.healtapp.features.activity.live.LiveWorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val profileRepository: ProfileRepository,
    private val wellnessRepository: WellnessRepository,
    private val healthConnectReader: HealthConnectReader,
    private val healthConnectForegroundSync: HealthConnectForegroundSync,
    private val trainingPrefs: TrainingPrefs,
    val liveSession: LiveWorkoutSession,
    private val workoutWaterBoostPrefs: com.example.healtapp.data.preferences.WorkoutWaterBoostPrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        viewModelScope.launch {
            trainingPrefs.favoritesFlow.collect { refreshQuickPicks() }
        }
        viewModelScope.launch {
            trainingPrefs.usageFlow.collect { refreshQuickPicks() }
        }
        viewModelScope.launch {
            AppRefreshBus.events.collect { loadAll() }
        }
    }

    private fun refreshQuickPicks() {
        viewModelScope.launch {
            val state = _uiState.value
            val favorites = trainingPrefs.favoritesSnapshot()
            val usage = trainingPrefs.usageSnapshot()
            val historySlugs = state.trainingHistory.map { it.activity_type.lowercase() }
            val slugs = computeQuickPickSlugs(favorites, usage, historySlugs)
            _uiState.update {
                it.copy(
                    favoriteTrainingSlugs = favorites,
                    quickPickTrainings = slugs.mapNotNull { slug -> trainingTypeBySlug(slug) },
                )
            }
        }
    }

    fun toggleTrainingFavorite(slug: String) {
        viewModelScope.launch {
            trainingPrefs.toggleFavorite(slug)
        }
    }

    fun clearSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val profile = profileRepository.getMyProfile().getOrNull()
            val goal = profile?.target_steps?.takeIf { it > 0 } ?: 10_000
            val burnGoal = CalorieBurnCalculator.dailyBurnGoal(
                targetSteps = goal,
                goal = profile?.goal,
            )

            val historyResult = repository.getActivityHistory()
            if (historyResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = historyResult.exceptionOrNull()?.message ?: "Ошибка загрузки",
                    )
                }
                return@launch
            }

            var all = historyResult.getOrNull().orEmpty()
            val duplicateIds = (
                ActivityStepsHelper.walkDuplicateIdsToRemove(all) +
                    ActivityStepsHelper.trainingDuplicateIdsToRemove(all)
                ).distinct()
            if (duplicateIds.isNotEmpty()) {
                duplicateIds.forEach { id -> repository.deleteActivity(id) }
                all = repository.getActivityHistory().getOrNull().orEmpty()
            }

            val hcSteps = runCatching { healthConnectReader.readTodaySteps() }.getOrNull()
            val stepsFromRecords = ActivityStepsHelper.stepsToday(all)
            val stepsToday = when {
                hcSteps != null && hcSteps > 0 -> hcSteps
                stepsFromRecords > 0 -> stepsFromRecords
                else -> 0
            }
            val todayKey = java.time.LocalDate.now().toString()
            val trainingToday = ActivityStepsHelper.trainingHistory(all)
                .filter { ActivityStepsHelper.activityDateKey(it.start_time) == todayKey }
            val trainingMinutes = trainingToday.sumOf { it.duration_minutes.toLong() }.toInt()
            val trainingKcal = trainingToday.sumOf { (it.calories_burned ?: 0f).toDouble() }.toInt()
            val trainingCountToday = trainingToday.size

            val weekStart = LocalDate.now().minusDays(6)
            val weekTrainings = ActivityStepsHelper.trainingHistory(all).filter { act ->
                runCatching { LocalDate.parse(ActivityStepsHelper.activityDateKey(act.start_time)) }
                    .getOrNull()
                    ?.let { !it.isBefore(weekStart) } == true
            }
            val trainingMinutesWeek = weekTrainings.sumOf { it.duration_minutes.toLong() }.toInt()
            val trainingCaloriesWeek = weekTrainings.sumOf { (it.calories_burned ?: 0f).toDouble() }.toInt()
            val trainingCountWeek = weekTrainings.size
            val totalBurned = CalorieBurnCalculator.totalBurnedToday(all, stepsToday)

            val hcStepsByDay = runCatching { healthConnectReader.readStepsAggregatedForLastDays(7) }
                .getOrDefault(emptyMap())
            val weeklySteps = ActivityStepsHelper.weeklySteps(all).map { day ->
                val fromHc = hcStepsByDay[day.dateKey] ?: 0
                val resolved = when (day.dateKey) {
                    todayKey -> stepsToday
                    else -> maxOf(day.steps, fromHc)
                }
                day.copy(steps = resolved)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    stepsToday = stepsToday,
                    stepsGoal = goal,
                    weeklySteps = weeklySteps,
                    healthConnectStepsToday = hcSteps,
                    trainingHistory = ActivityStepsHelper.trainingHistory(all),
                    healthConnectWorkouts = ActivityStepsHelper.syncedFromHealthConnect(all),
                    trainingMinutesToday = trainingMinutes,
                    trainingCaloriesToday = trainingKcal,
                    trainingCountToday = trainingCountToday,
                    trainingMinutesWeek = trainingMinutesWeek,
                    trainingCaloriesWeek = trainingCaloriesWeek,
                    trainingCountWeek = trainingCountWeek,
                    caloriesBurnedToday = totalBurned,
                    caloriesBurnGoal = burnGoal,
                    todayWalkRecordId = ActivityStepsHelper.findTodayWalkRecord(all)?.id,
                    error = null,
                )
            }
            refreshQuickPicks()
            refreshCircadianHints()
        }
    }

    private fun refreshCircadianHints() {
        viewModelScope.launch {
            val circadian = wellnessRepository.getCircadian().getOrNull() ?: return@launch
            _uiState.update { state ->
                val hour = circadian.usualBedtimeHour
                val minute = circadian.usualBedtimeMinute
                state.copy(
                    usualBedtimeHour = hour,
                    usualBedtimeMinute = minute,
                    usualBedtimeLabel = circadian.usualBedtime,
                    eveningWindowHint = CircadianTiming.eveningWindowHint(hour, minute),
                    bedtimeWarning = CircadianTiming.workoutNearBedtimeWarning(
                        hour,
                        minute,
                        state.intensity,
                    ),
                )
            }
        }
    }

    private fun applyBedtimeHints(state: ActivityUiState): ActivityUiState = state.copy(
        eveningWindowHint = CircadianTiming.eveningWindowHint(
            state.usualBedtimeHour,
            state.usualBedtimeMinute,
        ),
        bedtimeWarning = CircadianTiming.workoutNearBedtimeWarning(
            state.usualBedtimeHour,
            state.usualBedtimeMinute,
            state.intensity,
        ),
    )

    fun beginTraining(typeTitleRu: String) {
        val now = LocalDateTime.now()
        _uiState.update {
            applyBedtimeHints(
                it.copy(
                    activityType = typeTitleRu,
                    durationMinutes = "",
                    caloriesBurned = "",
                    distanceKm = "",
                    trainingNotes = "",
                    perceivedExertion = "",
                    trainingStartTime = "%02d:%02d".format(now.hour, now.minute),
                    trainingPhotoUri = null,
                    error = null,
                ),
            )
        }
    }

    fun resetTrainingForm() {
        _uiState.update {
            it.copy(
                durationMinutes = "",
                caloriesBurned = "",
                distanceKm = "",
                trainingNotes = "",
                perceivedExertion = "",
                trainingStartTime = "",
                trainingPhotoUri = null,
                error = null,
            )
        }
    }

    fun syncWorkoutsFromHealthConnect() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            healthConnectForegroundSync.importOnUserRequest()
                .onSuccess { res ->
                    val created = res.activities_created
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            snackMessage = if (created > 0) {
                                "Импортировано тренировок: $created"
                            } else {
                                "Новых тренировок в Health Connect нет"
                            },
                        )
                    }
                    loadAll()
                    if (created > 0) AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = e.message ?: "Не удалось импортировать из Health Connect",
                        )
                    }
                }
        }
    }

    fun syncStepsFromHealthConnect() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val hc = runCatching { healthConnectReader.readTodaySteps() }.getOrNull()
            if (hc == null || hc <= 0) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Нет шагов из Health Connect. Выдайте разрешения в интеграциях.",
                    )
                }
                return@launch
            }
            upsertTodayWalkSteps(hc)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            snackMessage = "Синхронизировано: $hc шагов",
                            progressCelebrateToken = it.progressCelebrateToken + 1,
                        )
                    }
                    loadAll()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = e.message ?: "Ошибка синхронизации")
                    }
                }
        }
    }

    private suspend fun upsertTodayWalkSteps(steps: Int): Result<Unit> {
        val state = _uiState.value
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val now = LocalDateTime.now()
        val request = ActivityCreateRequestDto(
            activity_type = "walk",
            start_time = now.withHour(8).withMinute(0).format(formatter),
            end_time = now.format(formatter),
            duration_minutes = (steps / 110).coerceIn(1, 600),
            steps = steps,
            distance_km = null,
            calories_burned = (steps * 0.04f).takeIf { it > 0f },
            intensity = "low",
            source = "health_connect",
        )
        val walkId = state.todayWalkRecordId
        return if (walkId != null) {
            repository.updateActivity(walkId, request).map { }
        } else {
            repository.createActivity(request).map { }
        }
    }

    fun updateActivityType(value: String) {
        _uiState.update { it.copy(activityType = value) }
    }

    fun updateDuration(value: String) {
        _uiState.update { it.copy(durationMinutes = value) }
    }

    fun updateCalories(value: String) {
        _uiState.update { it.copy(caloriesBurned = value) }
    }

    fun updateDistance(value: String) {
        _uiState.update { it.copy(distanceKm = value) }
    }

    fun updateIntensity(value: String) {
        _uiState.update { applyBedtimeHints(it.copy(intensity = value)) }
    }

    fun updateTrainingNotes(value: String) {
        _uiState.update { it.copy(trainingNotes = value) }
    }

    fun updateTrainingStartTime(value: String) {
        val cleaned = value.filter { ch -> ch.isDigit() || ch == ':' }.take(5)
        _uiState.update { it.copy(trainingStartTime = cleaned) }
    }

    fun updateTrainingPhotoUri(uri: String?) {
        _uiState.update { it.copy(trainingPhotoUri = uri) }
    }

    fun updatePerceivedExertion(value: String) {
        _uiState.update { it.copy(perceivedExertion = value.filter { it.isDigit() }.take(2)) }
    }

    fun saveTraining() {
        viewModelScope.launch {
            val state = _uiState.value
            val duration = state.durationMinutes.toIntOrNull()
            if (duration == null || duration <= 0) {
                _uiState.update { it.copy(error = "Введите длительность тренировки (мин)") }
                return@launch
            }

            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val apiType = activityApiSlug(state.activityType.ifBlank { "Бег" })

            val start = parseTrainingStart(state.trainingStartTime, duration, now)
            val end = start.plusMinutes(duration.toLong())

            val fields = trainingFormFieldsFor(state.activityType)
            val rawDist = state.distanceKm.toFloatOrNull()
            val distanceKm = when {
                !fields.showDistance -> null
                state.activityType == "Плавание" && rawDist != null -> rawDist / 1000f
                else -> rawDist
            }
            val calories = state.caloriesBurned.toFloatOrNull()
                ?: estimateTrainingCalories(state.activityType.ifBlank { "Бег" }, duration, distanceKm)
            val notes = buildString {
                append(state.trainingNotes.trim())
                state.trainingPhotoUri?.let { uri ->
                    if (isNotEmpty()) append('\n')
                    append("[photo]$uri")
                }
            }.ifBlank { null }
            val request = ActivityCreateRequestDto(
                activity_type = apiType,
                start_time = start.format(formatter),
                end_time = end.format(formatter),
                duration_minutes = duration,
                steps = null,
                distance_km = distanceKm,
                calories_burned = calories,
                intensity = state.intensity.ifBlank { null }.takeIf { fields.showIntensity },
                perceived_exertion = state.perceivedExertion.toIntOrNull()?.coerceIn(1, 10)
                    .takeIf { fields.showExertion },
                notes = notes,
                source = "manual",
            )

            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.createActivity(request)
                .onSuccess {
                    trainingPrefs.recordUsage(apiType)
                    val warning = CircadianTiming.workoutNearBedtimeWarning(
                        state.usualBedtimeHour,
                        state.usualBedtimeMinute,
                        state.intensity,
                    )
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            durationMinutes = "",
                            caloriesBurned = "",
                            distanceKm = "",
                            trainingNotes = "",
                            perceivedExertion = "",
                            trainingStartTime = "",
                            trainingPhotoUri = null,
                            snackMessage = if (warning != null) {
                                "Тренировка сохранена. $warning"
                            } else {
                                "Тренировка сохранена"
                            },
                            progressCelebrateToken = it.progressCelebrateToken + 1,
                        )
                    }
                    loadAll()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = e.message ?: "Не удалось сохранить",
                        )
                    }
                }
        }
    }

    private fun parseTrainingStart(
        raw: String,
        durationMinutes: Int,
        now: LocalDateTime,
    ): LocalDateTime {
        val parts = raw.trim().split(':')
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()
        return if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
            now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        } else {
            now.minusMinutes(durationMinutes.toLong())
        }
    }

    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.deleteActivity(id)
                .onSuccess {
                    _uiState.update { it.copy(snackMessage = "Запись удалена") }
                    loadAll()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Не удалось удалить")
                    }
                }
        }
    }

    fun updateTrainingRecord(
        activity: ActivityDto,
        durationMinutes: Int,
        calories: String?,
        distanceKm: String?,
        intensity: String?,
        activityType: String,
    ) {
        viewModelScope.launch {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val start = ActivityStepsHelper.parseStartTime(activity.start_time)
            val end = start.plusMinutes(durationMinutes.toLong())
            val slug = activityApiSlug(activityType.ifBlank { activityTitleFromApi(activity.activity_type) })

            val request = ActivityCreateRequestDto(
                activity_type = slug,
                start_time = start.format(formatter),
                end_time = end.format(formatter),
                duration_minutes = durationMinutes,
                steps = null,
                distance_km = distanceKm?.toFloatOrNull() ?: activity.distance_km,
                calories_burned = calories?.toFloatOrNull() ?: activity.calories_burned,
                intensity = intensity?.ifBlank { null } ?: activity.intensity,
                activity_category = activity.activity_category,
                perceived_exertion = activity.perceived_exertion,
                avg_heart_rate = activity.avg_heart_rate,
                minutes_before_sleep = activity.minutes_before_sleep,
                is_evening_activity = activity.is_evening_activity,
                notes = activity.notes,
                source = activity.source,
            )

            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.updateActivity(activity.id, request)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, snackMessage = "Тренировка обновлена") }
                    loadAll()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = e.message ?: "Не удалось обновить")
                    }
                }
        }
    }

    fun updateActivityNotes(activity: ActivityDto, notes: String) {
        viewModelScope.launch {
            val request = ActivityCreateRequestDto(
                activity_type = activity.activity_type,
                start_time = activity.start_time,
                end_time = activity.end_time,
                duration_minutes = activity.duration_minutes,
                steps = activity.steps,
                distance_km = activity.distance_km,
                calories_burned = activity.calories_burned,
                intensity = activity.intensity,
                activity_category = activity.activity_category,
                perceived_exertion = activity.perceived_exertion,
                avg_heart_rate = activity.avg_heart_rate,
                minutes_before_sleep = activity.minutes_before_sleep,
                is_evening_activity = activity.is_evening_activity,
                notes = notes,
                source = activity.source,
                avg_speed_m_s = activity.avg_speed_m_s,
            )
            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.updateActivity(activity.id, request)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, snackMessage = "Тренировка обновлена") }
                    loadAll()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = e.message ?: "Не удалось обновить")
                    }
                }
        }
    }

    fun saveLiveWorkout(
        activityTitleRu: String,
        startEpochMs: Long,
        endEpochMs: Long,
        durationMinutes: Int,
        distanceKm: Float,
        calories: Float,
        avgSpeedMs: Float?,
        notes: String? = null,
    ) {
        viewModelScope.launch {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val start = java.time.Instant.ofEpochMilli(startEpochMs)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
            val end = java.time.Instant.ofEpochMilli(endEpochMs)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
            val apiType = activityApiSlug(activityTitleRu)
            val request = ActivityCreateRequestDto(
                activity_type = apiType,
                start_time = start.format(formatter),
                end_time = end.format(formatter),
                duration_minutes = durationMinutes.coerceAtLeast(1),
                distance_km = distanceKm.takeIf { it > 0f },
                calories_burned = calories.takeIf { it > 0f },
                avg_speed_m_s = avgSpeedMs,
                notes = notes?.takeIf { it.isNotBlank() } ?: "GPS-запись",
                source = "gps",
            )
            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.createActivity(request)
                .onSuccess {
                    trainingPrefs.recordUsage(apiType)
                    workoutWaterBoostPrefs.setFromDistanceKm(distanceKm)
                    val extra = workoutWaterBoostPrefs.extraMlToday()
                    val sleepHint = if (CircadianTiming.hitsTrainingSleepWindow(
                            _uiState.value.usualBedtimeHour,
                            _uiState.value.usualBedtimeMinute,
                        )
                    ) {
                        " · к сну без тяжёлого ужина"
                    } else {
                        ""
                    }
                    val waterHint = if (extra > 0) " · пейте ещё $extra мл" else ""
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            snackMessage = "Тренировка сохранена · ${"%.2f".format(distanceKm)} км$waterHint$sleepHint",
                            progressCelebrateToken = it.progressCelebrateToken + 1,
                        )
                    }
                    loadAll()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = e.message ?: "Не удалось сохранить GPS-тренировку")
                    }
                }
        }
    }
}
