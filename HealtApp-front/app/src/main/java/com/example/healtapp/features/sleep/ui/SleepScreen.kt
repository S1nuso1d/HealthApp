package com.example.healtapp.features.sleep.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.DatePickerField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.sleep.presentation.SleepRecordUi
import com.example.healtapp.features.sleep.presentation.SleepViewModel
import com.example.healtapp.features.sleep.ui.components.SleepFormCard
import com.example.healtapp.features.sleep.ui.components.SleepHcImportCard
import com.example.healtapp.features.sleep.ui.components.SleepHeroCard
import com.example.healtapp.features.sleep.ui.components.SleepHistoryRow
import com.example.healtapp.features.sleep.ui.components.SleepInsightCard
import com.example.healtapp.features.sleep.ui.components.SleepScreenSkeleton
import com.example.healtapp.features.sleep.ui.components.WeeklySleepBarChart

@Composable
fun SleepScreen(
    onOpenIntegrations: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
) {
    val viewModel: SleepViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var recordToDelete by remember { mutableStateOf<SleepRecordUi?>(null) }
    var recordToEdit by remember { mutableStateOf<SleepRecordUi?>(null) }
    var editDate by remember { mutableStateOf("") }
    var editStart by remember { mutableStateOf("") }
    var editEnd by remember { mutableStateOf("") }
    var editQuality by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }

    LaunchedEffect(uiState.snackMessage) {
        uiState.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackMessage()
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
                title = "Сон",
                subtitle = "Ночи, неделя и цель из профиля",
                headerIcon = Icons.Filled.Bedtime,
                scrollable = true,
            ) {
                if (uiState.isLoading && uiState.records.isEmpty()) {
                    SleepScreenSkeleton()
                } else {
                    SleepHeroCard(
                        lastNightHours = uiState.lastNightHours,
                        averageSleepHours = uiState.averageSleepHours,
                        targetSleepHours = uiState.targetSleepHours,
                        sleepQualityAverage = uiState.sleepQualityAverage,
                        consistencyPercent = uiState.consistencyPercent,
                        onEditGoalInProfile = onOpenProfile,
                    )

                    SectionHeader(
                        title = "Неделя",
                        subtitle = "Часы сна по дням засыпания",
                    )
                    WeeklySleepBarChart(
                        days = uiState.weeklySleep,
                        goalHours = uiState.targetSleepHours,
                    )

                    SleepInsightCard(text = uiState.insightText)
                }

                SectionHeader(
                    title = "Импорт",
                    subtitle = "Health Connect и разрешения",
                )
                SleepHcImportCard(
                    isImporting = uiState.isHcImporting,
                    importMessage = uiState.hcImportMessage,
                    onImport = viewModel::importFromHealthConnect,
                    onOpenIntegrations = onOpenIntegrations,
                )

                SectionHeader(
                    title = "Новая запись",
                    subtitle = "Если браслет не отдаёт сон в HC",
                )
                SleepFormCard(
                    sleepDate = uiState.sleepDateInput,
                    sleepStart = uiState.sleepStartInput,
                    sleepEnd = uiState.sleepEndInput,
                    quality = uiState.qualityInput,
                    note = uiState.noteInput,
                    isSaving = uiState.isSaving,
                    onSleepDateChange = viewModel::updateSleepDate,
                    onSleepStartChange = viewModel::updateSleepStart,
                    onSleepEndChange = viewModel::updateSleepEnd,
                    onQualityChange = viewModel::updateQuality,
                    onNoteChange = viewModel::updateNote,
                    onSaveClick = viewModel::saveSleepRecord,
                )

                uiState.error?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                SectionHeader(
                    title = "История",
                    subtitle = if (uiState.records.isEmpty()) {
                        "Пока нет ночей"
                    } else {
                        "${uiState.records.size} записей"
                    },
                )

                if (uiState.records.isEmpty() && !uiState.isLoading) {
                    Text(
                        text = "Импортируйте из Health Connect или добавьте ночь вручную — она появится здесь.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.records.forEach { record ->
                            SleepHistoryRow(
                                record = record,
                                onEdit = {
                                    recordToEdit = record
                                    editDate = record.date
                                    editStart = record.startTime
                                    editEnd = record.endTime
                                    editQuality = record.qualityScore.toString()
                                    editNote = record.note
                                },
                                onDelete = { recordToDelete = record },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(72.dp))
            }

            recordToDelete?.let { rec ->
                AlertDialog(
                    onDismissRequest = { recordToDelete = null },
                    title = { Text("Удалить запись?") },
                    text = { Text("Ночь за ${rec.date} будет удалена без восстановления.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteSleepRecord(rec.id)
                                recordToDelete = null
                            },
                        ) {
                            Text("Удалить", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { recordToDelete = null }) { Text("Отмена") }
                    },
                )
            }

            recordToEdit?.let { rec ->
                AlertDialog(
                    onDismissRequest = { recordToEdit = null },
                    title = { Text("Редактировать сон") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DatePickerField(
                                value = editDate,
                                onValueChange = { editDate = it },
                                label = "Дата засыпания",
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppTextField(
                                    editStart,
                                    { editStart = it },
                                    label = "Засыпание",
                                    modifier = Modifier.weight(1f),
                                )
                                AppTextField(
                                    editEnd,
                                    { editEnd = it },
                                    label = "Пробуждение",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            AppTextField(editQuality, { editQuality = it }, label = "Качество 0–100")
                            AppTextField(editNote, { editNote = it }, label = "Заметка")
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val q = editQuality.toIntOrNull() ?: rec.qualityScore
                                viewModel.updateSleepRecord(
                                    id = rec.id,
                                    sleepDate = editDate,
                                    sleepStart = editStart,
                                    sleepEnd = editEnd,
                                    quality = q,
                                    note = editNote,
                                )
                                recordToEdit = null
                            },
                        ) { Text("Сохранить") }
                    },
                    dismissButton = {
                        TextButton(onClick = { recordToEdit = null }) { Text("Отмена") }
                    },
                )
            }
        }
    }
}
