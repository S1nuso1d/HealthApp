package com.example.healtapp.features.activity.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.LocalDate
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.CollapsibleAppCard
import com.example.healtapp.core.ui.components.FeatureGuideContent
import com.example.healtapp.core.ui.components.FeatureGuideOverlay
import com.example.healtapp.core.ui.components.FeatureGuidePrefs
import com.example.healtapp.core.ui.components.FeatureGuideScreen
import com.example.healtapp.core.ui.components.AppDialogMessage
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.theme.brandingGradient
import com.example.healtapp.core.ui.theme.iconTintColor
import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.features.activity.presentation.ActivityViewModel
import com.example.healtapp.features.activity.presentation.activityTitleFromApi
import com.example.healtapp.features.activity.presentation.trainingFormFieldsFor
import com.example.healtapp.features.activity.ui.components.ActivityStepsHeroCard
import com.example.healtapp.features.activity.ui.components.ActivityStepsSkeleton
import com.example.healtapp.features.activity.ui.components.ActivityTrainingHistoryRow
import com.example.healtapp.features.activity.ui.components.ActivityTrainingCatalogScreen
import com.example.healtapp.features.activity.ui.components.ActivityTrainingLogScreen
import com.example.healtapp.features.activity.ui.components.ActivityTrainingPane
import com.example.healtapp.features.activity.ui.components.ActivityTrainingSection
import com.example.healtapp.features.activity.ui.components.WeeklyStepsBarChart

@Composable
fun ActivityScreen(
    onOpenProfile: () -> Unit = {},
    onImmersiveChanged: (Boolean) -> Unit = {},
) {
    val viewModel: ActivityViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showGuide by remember { mutableStateOf(false) }
    var trainingPane by remember { mutableStateOf(ActivityTrainingPane.Hub) }
    var menuExpanded by remember { mutableStateOf(false) }
    var summaryData by remember { mutableStateOf<WorkoutSummaryData?>(null) }
    var summarySourceActivity by remember { mutableStateOf<ActivityDto?>(null) }

    fun returnToActivityHub() {
        summaryData = null
        summarySourceActivity = null
        viewModel.resetTrainingForm()
        trainingPane = ActivityTrainingPane.Hub
    }

    LaunchedEffect(Unit) {
        showGuide = FeatureGuidePrefs.shouldShow(context, FeatureGuideScreen.Activity)
    }

    LaunchedEffect(trainingPane) {
        onImmersiveChanged(
            trainingPane == ActivityTrainingPane.Run || trainingPane == ActivityTrainingPane.Summary,
        )
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { onImmersiveChanged(false) }
    }

    BackHandler(enabled = trainingPane != ActivityTrainingPane.Hub) {
        when (trainingPane) {
            ActivityTrainingPane.Summary,
            ActivityTrainingPane.Form,
            ActivityTrainingPane.Catalog,
            -> returnToActivityHub()
            ActivityTrainingPane.Run -> {
                // Live-экран сам обрабатывает выход; если дошли сюда — на хаб.
                returnToActivityHub()
            }
            ActivityTrainingPane.Hub -> Unit
        }
    }

    val intensityTypes = listOf("Низкая", "Средняя", "Высокая")

    var activityToDelete by remember { mutableStateOf<ActivityDto?>(null) }
    var activityToEdit by remember { mutableStateOf<ActivityDto?>(null) }
    var editDuration by remember { mutableStateOf("") }
    var editCal by remember { mutableStateOf("") }
    var editDist by remember { mutableStateOf("") }
    var editIntensity by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        viewModel.updateTrainingPhotoUri(uri?.toString())
    }

    LaunchedEffect(uiState.snackMessage) {
        uiState.snackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            if (msg.startsWith("Тренировка сохранена") &&
                (trainingPane == ActivityTrainingPane.Form || trainingPane == ActivityTrainingPane.Summary)
            ) {
                trainingPane = ActivityTrainingPane.Hub
                summaryData = null
                summarySourceActivity = null
            }
            if (msg.startsWith("Тренировка обновлена") && trainingPane == ActivityTrainingPane.Summary) {
                trainingPane = ActivityTrainingPane.Hub
                summaryData = null
                summarySourceActivity = null
            }
            viewModel.clearSnackMessage()
        }
    }

    val todayKey = remember { LocalDate.now().toString() }
    val weeklyStepsForChart = remember(uiState.weeklySteps, uiState.stepsToday, todayKey) {
        uiState.weeklySteps.map { day ->
            if (day.dateKey == todayKey) day.copy(steps = uiState.stepsToday) else day
        }
    }

    val previousRuns = remember(uiState.trainingHistory) {
        uiState.trainingHistory
            .filter { it.activity_type.equals("run", ignoreCase = true) }
            .mapNotNull { act ->
                val dist = act.distance_km ?: return@mapNotNull null
                if (dist < 0.5f) return@mapNotNull null
                PreviousRunOption(
                    distanceKm = dist,
                    durationMin = act.duration_minutes,
                    label = "%.1f км · %d мин".format(dist, act.duration_minutes),
                )
            }
            .distinctBy { it.label }
            .take(2)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AppScreen(
                title = "Активность",
                subtitle = "Шаги, неделя и тренировки",
                headerLeading = {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(brandingGradient().map { it.copy(alpha = 0.28f) })),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = "Меню",
                                tint = iconTintColor(),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Добавить тренировку вручную") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.beginTraining(uiState.activityType.ifBlank { "Бег" })
                                    trainingPane = ActivityTrainingPane.Form
                                },
                            )
                        }
                    }
                },
                scrollable = true,
                scrollStateKey = "activity",
            ) {
                if (uiState.isLoading) {
                    ActivityStepsSkeleton()
                    Spacer(Modifier.height(12.dp))
                    ActivityStepsSkeleton()
                } else {
                    ActivityStepsHeroCard(
                        stepsToday = uiState.stepsToday,
                        stepsGoal = uiState.stepsGoal,
                        caloriesBurnedToday = uiState.caloriesBurnedToday,
                        caloriesBurnGoal = uiState.caloriesBurnGoal,
                        trainingMinutesToday = uiState.trainingMinutesToday,
                        trainingCaloriesToday = uiState.trainingCaloriesToday,
                        healthConnectSteps = uiState.healthConnectStepsToday,
                        isSaving = uiState.isSaving,
                        celebrateToken = uiState.progressCelebrateToken,
                        onSyncHealthConnect = viewModel::syncStepsFromHealthConnect,
                        onSyncWorkoutsFromHealthConnect = viewModel::syncWorkoutsFromHealthConnect,
                        onEditGoalInProfile = onOpenProfile,
                    )

                    SectionHeader(title = "Неделя", subtitle = null)
                    WeeklyStepsBarChart(
                        days = weeklyStepsForChart,
                        goal = uiState.stepsGoal,
                    )

                    uiState.eveningWindowHint?.let { hint ->
                        AppMessageBanner(text = hint, type = AppMessageType.Warning)
                    }
                }

                ActivityTrainingSection(
                    minutesToday = uiState.trainingMinutesToday,
                    countToday = uiState.trainingCountToday,
                    caloriesToday = uiState.trainingCaloriesToday,
                    minutesWeek = uiState.trainingMinutesWeek,
                    countWeek = uiState.trainingCountWeek,
                    caloriesWeek = uiState.trainingCaloriesWeek,
                    quickPicks = uiState.quickPickTrainings,
                    favoriteSlugs = uiState.favoriteTrainingSlugs,
                    onSelectType = { type ->
                        viewModel.beginTraining(type.titleRu)
                        trainingPane = if (trainingFormFieldsFor(type.titleRu).supportsLiveGps) {
                            ActivityTrainingPane.Run
                        } else {
                            ActivityTrainingPane.Form
                        }
                    },
                    onOpenCatalog = { trainingPane = ActivityTrainingPane.Catalog },
                    historyContent = {
                        CollapsibleAppCard(
                            title = "История",
                            subtitle = if (uiState.trainingHistory.isEmpty()) {
                                "Пока нет ручных записей"
                            } else {
                                "${uiState.trainingHistory.size} записей"
                            },
                            initiallyExpanded = false,
                        ) {
                            if (uiState.trainingHistory.isEmpty() && !uiState.isLoading) {
                                Text(
                                    text = "Добавьте тренировку — она появится здесь.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    uiState.trainingHistory.forEach { activity ->
                                        ActivityTrainingHistoryRow(
                                            activity = activity,
                                            onOpen = {
                                                summarySourceActivity = activity
                                                summaryData = activity.toWorkoutSummaryData()
                                                trainingPane = ActivityTrainingPane.Summary
                                            },
                                            onEdit = {
                                                activityToEdit = activity
                                                editDuration = activity.duration_minutes.toString()
                                                editCal = activity.calories_burned?.toString().orEmpty()
                                                editDist = activity.distance_km?.toString().orEmpty()
                                                editIntensity = activity.intensity.orEmpty()
                                                editType = activityTitleFromApi(activity.activity_type)
                                            },
                                            onDelete = { activityToDelete = activity },
                                        )
                                    }
                                }
                            }
                        }
                    },
                )

                uiState.error?.let {
                    AppMessageBanner(text = it, type = AppMessageType.Error)
                }
            }

            if (trainingPane == ActivityTrainingPane.Catalog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    AppScreen(scrollable = true) {
                        ActivityTrainingCatalogScreen(
                            favoriteSlugs = uiState.favoriteTrainingSlugs,
                            onBack = {
                                viewModel.resetTrainingForm()
                                trainingPane = ActivityTrainingPane.Hub
                            },
                            onSelectType = { type ->
                                viewModel.beginTraining(type.titleRu)
                                trainingPane = if (trainingFormFieldsFor(type.titleRu).supportsLiveGps) {
                                    ActivityTrainingPane.Run
                                } else {
                                    ActivityTrainingPane.Form
                                }
                            },
                            onToggleFavorite = viewModel::toggleTrainingFavorite,
                        )
                    }
                }
            }

            if (trainingPane == ActivityTrainingPane.Form) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    AppScreen(scrollable = true) {
                        ActivityTrainingLogScreen(
                            activityType = uiState.activityType,
                            durationMinutes = uiState.durationMinutes,
                            onDurationChange = viewModel::updateDuration,
                            calories = uiState.caloriesBurned,
                            onCaloriesChange = viewModel::updateCalories,
                            distanceKm = uiState.distanceKm,
                            onDistanceChange = viewModel::updateDistance,
                            intensity = uiState.intensity,
                            intensityOptions = intensityTypes,
                            onIntensitySelected = viewModel::updateIntensity,
                            notes = uiState.trainingNotes,
                            onNotesChange = viewModel::updateTrainingNotes,
                            perceivedExertion = uiState.perceivedExertion,
                            onPerceivedExertionChange = viewModel::updatePerceivedExertion,
                            startTime = uiState.trainingStartTime,
                            onStartTimeChange = viewModel::updateTrainingStartTime,
                            photoUri = uiState.trainingPhotoUri,
                            onPickPhoto = {
                                pickPhoto.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onClearPhoto = { viewModel.updateTrainingPhotoUri(null) },
                            isSaving = uiState.isSaving,
                            bedtimeWarning = uiState.bedtimeWarning,
                            onBack = {
                                viewModel.resetTrainingForm()
                                trainingPane = ActivityTrainingPane.Hub
                            },
                            onSave = viewModel::saveTraining,
                        )
                    }
                }
            }

            if (trainingPane == ActivityTrainingPane.Run) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    RunWorkoutScreen(
                        activityTitleRu = uiState.activityType.ifBlank { "Бег" },
                        session = viewModel.liveSession,
                        previousRuns = previousRuns,
                        bedtimeWarning = uiState.bedtimeWarning,
                        onBack = {
                            viewModel.resetTrainingForm()
                            trainingPane = ActivityTrainingPane.Hub
                        },
                        onFinished = { done ->
                            if (done.elapsedMs >= 20_000L || done.distanceMeters >= 40.0) {
                                summarySourceActivity = null
                                summaryData = WorkoutSummaryData(
                                    activityTitleRu = done.activityTitleRu.ifBlank {
                                        uiState.activityType.ifBlank { "Бег" }
                                    },
                                    startEpochMs = done.startedAtEpochMs,
                                    endEpochMs = System.currentTimeMillis(),
                                    durationMinutes = done.durationMinutes,
                                    distanceKm = done.distanceKm.toFloat(),
                                    calories = liveWorkoutCalories(done),
                                    avgPaceMinPerKm = done.avgPaceMinPerKm,
                                    cadenceSpm = done.cadenceSpm,
                                    avgSpeedMs = done.avgSpeedKmh?.div(3.6)?.toFloat(),
                                    lat = done.lastLat,
                                    lon = done.lastLon,
                                    mapPoints = done.points.size,
                                    isNew = true,
                                )
                                trainingPane = ActivityTrainingPane.Summary
                            } else {
                                viewModel.resetTrainingForm()
                                trainingPane = ActivityTrainingPane.Hub
                            }
                        },
                    )
                }
            }

            if (trainingPane == ActivityTrainingPane.Summary) {
                summaryData?.let { data ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        WorkoutSummaryScreen(
                            data = data,
                            isSaving = uiState.isSaving,
                            onBack = { returnToActivityHub() },
                            onSave = { updated ->
                                summaryData = updated
                                val notes = buildWorkoutNotes(updated)
                                val existing = summarySourceActivity
                                if (existing != null) {
                                    viewModel.updateActivityNotes(existing, notes)
                                } else {
                                    viewModel.saveLiveWorkout(
                                        activityTitleRu = updated.activityTitleRu,
                                        startEpochMs = updated.startEpochMs,
                                        endEpochMs = updated.endEpochMs,
                                        durationMinutes = updated.durationMinutes,
                                        distanceKm = updated.distanceKm,
                                        calories = updated.calories,
                                        avgSpeedMs = updated.avgSpeedMs,
                                        notes = notes,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            activityToDelete?.let { act ->
                AlertDialog(
                    onDismissRequest = { activityToDelete = null },
                    title = { Text("Удалить тренировку?") },
                    text = {
                        AppDialogMessage(
                            warning = UserFacingMessages.DELETE_RECORD_WARNING,
                            body = "${activityTitleFromApi(act.activity_type)}, ${act.duration_minutes} мин",
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteActivity(act.id)
                                activityToDelete = null
                            },
                        ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { activityToDelete = null }) { Text("Отмена") }
                    },
                )
            }

            activityToEdit?.let { act ->
                AlertDialog(
                    onDismissRequest = { activityToEdit = null },
                    title = { Text("Редактировать") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextField(editType, { editType = it }, label = "Тип")
                            AppTextField(editDuration, { editDuration = it }, label = "Минуты")
                            AppTextField(editCal, { editCal = it }, label = "Ккал")
                            AppTextField(editDist, { editDist = it }, label = "Км")
                            AppTextField(editIntensity, { editIntensity = it }, label = "Интенсивность")
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val duration = editDuration.toIntOrNull()?.takeIf { it > 0 }
                                if (duration != null) {
                                    viewModel.updateTrainingRecord(
                                        activity = act,
                                        durationMinutes = duration,
                                        calories = editCal,
                                        distanceKm = editDist,
                                        intensity = editIntensity,
                                        activityType = editType,
                                    )
                                }
                                activityToEdit = null
                            },
                        ) { Text("Сохранить") }
                    },
                    dismissButton = {
                        TextButton(onClick = { activityToEdit = null }) { Text("Отмена") }
                    },
                )
            }

            FeatureGuideOverlay(
                visible = showGuide,
                pages = FeatureGuideContent.activity,
                sectionLabel = "Активность",
                onDismiss = {
                    FeatureGuidePrefs.markSeen(context, FeatureGuideScreen.Activity)
                    showGuide = false
                },
            )
        }
    }
}

private fun ActivityDto.toWorkoutSummaryData(): WorkoutSummaryData {
    val payload = com.example.healtapp.features.activity.live.WorkoutNotesCodec.decode(notes)
    val start = com.example.healtapp.features.activity.presentation.ActivityStepsHelper.parseStartTime(start_time)
    val end = runCatching {
        com.example.healtapp.features.activity.presentation.ActivityStepsHelper.parseStartTime(end_time)
    }.getOrElse { start.plusMinutes(duration_minutes.toLong()) }
    val zone = java.time.ZoneId.systemDefault()
    return WorkoutSummaryData(
        activityId = id,
        activityTitleRu = activityTitleFromApi(activity_type),
        startEpochMs = start.atZone(zone).toInstant().toEpochMilli(),
        endEpochMs = end.atZone(zone).toInstant().toEpochMilli(),
        durationMinutes = duration_minutes,
        distanceKm = distance_km ?: 0f,
        calories = calories_burned ?: 0f,
        avgPaceMinPerKm = payload.meta?.avgPaceMinPerKm,
        cadenceSpm = payload.meta?.cadenceSpm,
        avgSpeedMs = avg_speed_m_s,
        weather = payload.meta?.weather,
        note = payload.note,
        photoUri = payload.meta?.photoUri,
        photoFocusY = 0f,
        mapPoints = payload.meta?.mapPoints ?: 0,
        isNew = false,
    )
}
