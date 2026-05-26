package com.example.healtapp.features.activity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.CollapsibleAppCard
import com.example.healtapp.core.ui.components.AppDialogMessage
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.features.activity.presentation.ActivityViewModel
import com.example.healtapp.features.activity.presentation.activityTitleFromApi
import com.example.healtapp.features.activity.presentation.trainingActivityTypes
import com.example.healtapp.features.activity.ui.components.ActivityStepsHeroCard
import com.example.healtapp.features.activity.ui.components.ActivityStepsSkeleton
import com.example.healtapp.features.activity.ui.components.ActivityTrainingFormCard
import com.example.healtapp.features.activity.ui.components.ActivityTrainingHistoryRow
import com.example.healtapp.features.activity.ui.components.WeeklyStepsBarChart

@Composable
fun ActivityScreen(
    onOpenProfile: () -> Unit = {},
) {
    val viewModel: ActivityViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val intensityTypes = listOf("Низкая", "Средняя", "Высокая")

    var activityToDelete by remember { mutableStateOf<ActivityDto?>(null) }
    var activityToEdit by remember { mutableStateOf<ActivityDto?>(null) }
    var editDuration by remember { mutableStateOf("") }
    var editCal by remember { mutableStateOf("") }
    var editDist by remember { mutableStateOf("") }
    var editIntensity by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }

    LaunchedEffect(uiState.snackMessage) {
        uiState.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackMessage()
        }
    }

    val todayKey = remember { LocalDate.now().toString() }
    val weeklyStepsForChart = remember(uiState.weeklySteps, uiState.stepsToday, todayKey) {
        uiState.weeklySteps.map { day ->
            if (day.dateKey == todayKey) day.copy(steps = uiState.stepsToday) else day
        }
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
                headerIcon = Icons.AutoMirrored.Filled.DirectionsWalk,
                scrollable = true,
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
                }

                CollapsibleAppCard(
                    title = "Новая тренировка",
                    subtitle = "Ручной ввод",
                    initiallyExpanded = false,
                ) {
                    ActivityTrainingFormCard(
                        embeddedInCard = true,
                        activityType = uiState.activityType,
                        activityTypes = trainingActivityTypes,
                        onActivityTypeSelected = viewModel::updateActivityType,
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
                        isSaving = uiState.isSaving,
                        onSave = viewModel::saveTraining,
                    )
                }

                uiState.error?.let {
                    AppMessageBanner(text = it, type = AppMessageType.Error)
                }

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
                            text = "Добавьте тренировку вручную — она появится здесь.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.trainingHistory.forEach { activity ->
                                ActivityTrainingHistoryRow(
                                    activity = activity,
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

                Spacer(Modifier.height(72.dp))
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
        }
    }
}
