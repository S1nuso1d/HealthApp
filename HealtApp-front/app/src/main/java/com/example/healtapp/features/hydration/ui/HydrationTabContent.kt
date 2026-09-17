package com.example.healtapp.features.hydration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.core.ui.components.AppDialogMessage
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.CollapsibleAppCard
import com.example.healtapp.core.ui.components.EmptyStateCard
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.PendingSyncBadge
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.core.ui.components.progressCelebrateEffect
import com.example.healtapp.core.ui.theme.heroBlockGradient
import com.example.healtapp.core.ui.theme.heroContentColor
import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.data.preferences.HydrationPrefs
import com.example.healtapp.features.dashboard.ui.components.WaterGlassMeter
import com.example.healtapp.features.hydration.presentation.HydrationViewModel
import com.example.healtapp.features.hydration.ui.components.HydrationCustomAmountKeypad
import com.example.healtapp.features.hydration.ui.components.QuickAddWaterButtons

@Composable
fun HydrationTabContent() {
    val viewModel: HydrationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var recordToEdit by remember { mutableStateOf<HydrationDto?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var recordToDelete by remember { mutableStateOf<HydrationDto?>(null) }
    var customMlInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PendingSyncBadge(count = uiState.pendingSyncCount)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(heroBlockGradient()))
                .padding(20.dp)
                .progressCelebrateEffect(uiState.progressCelebrateToken),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WaterGlassMeter(
                    progress = if (uiState.target > 0) {
                        (uiState.waterToday / uiState.target.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Вода сегодня",
                        style = MaterialTheme.typography.labelLarge,
                        color = heroContentColor().copy(alpha = 0.88f),
                    )
                    Text(
                        text = "${uiState.waterToday} мл",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = heroContentColor(),
                    )
                    Text(
                        text = if (uiState.target > 0 && uiState.waterToday >= uiState.target) {
                            "Цель на сегодня выполнена"
                        } else if (java.time.LocalTime.now().hour >= 20 &&
                            uiState.target > 0 &&
                            uiState.target - uiState.waterToday >= 700
                        ) {
                            "После 20:00 не догоняйте большой объём — это мешает сну"
                        } else if (uiState.target > 0) {
                            "ещё ${uiState.target - uiState.waterToday} мл до ${uiState.target} мл"
                        } else {
                            "Цель: ${uiState.target} мл"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = heroContentColor().copy(alpha = 0.9f),
                    )
                }
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }
        uiState.error?.let { errorText ->
            AppMessageBanner(text = errorText, type = AppMessageType.Error)
        }

        SectionHeader(title = "Быстро добавить")
        QuickAddWaterButtons(
            defaultAmounts = uiState.defaultQuickAmounts,
            customAmounts = uiState.customQuickAmounts,
            onAdd = viewModel::addWater,
            onAddCustomButton = viewModel::addCustomQuickAmount,
            onRemoveCustomButton = viewModel::removeCustomQuickAmount,
            canAddMoreCustom = uiState.customQuickAmounts.size < HydrationPrefs.MAX_CUSTOM_BUTTONS,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader(title = "Свой объём")
        GradientFormPanel {
            HydrationCustomAmountKeypad(
                value = customMlInput,
                onValueChange = { customMlInput = it.filter { ch -> ch.isDigit() }.take(5) },
                onAdd = {
                    val ml = customMlInput.toIntOrNull()
                    if (ml != null && ml > 0) {
                        viewModel.addWater(ml)
                        customMlInput = ""
                    }
                },
                addEnabled = customMlInput.toIntOrNull()?.let { it > 0 } == true && !uiState.isLoading,
            )
        }

        CollapsibleAppCard(
            title = "Записи за сегодня",
            subtitle = if (uiState.todayRecords.isEmpty()) "Пока нет записей" else "${uiState.todayRecords.size} записей",
            initiallyExpanded = false,
        ) {
            if (uiState.todayRecords.isEmpty()) {
                EmptyStateCard(text = "Пока нет записей за сегодня — добавьте воду кнопками выше.")
            } else {
                uiState.todayRecords.forEach { rec ->
                    AppCard {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "${rec.amount_ml} мл · ${rec.record_time}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = {
                                    recordToEdit = rec
                                    editAmount = rec.amount_ml.toString()
                                }) { Text("Изменить") }
                                TextButton(onClick = { recordToDelete = rec }) {
                                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    recordToDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Удалить запись?") },
            text = {
                AppDialogMessage(
                    warning = UserFacingMessages.DELETE_RECORD_WARNING,
                    body = "Запись ${rec.amount_ml} мл будет удалена.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(rec.id)
                        recordToDelete = null
                    },
                ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Отмена") }
            },
        )
    }

    recordToEdit?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordToEdit = null },
            title = { Text("Изменить объём") },
            text = {
                AppTextField(
                    value = editAmount,
                    onValueChange = { editAmount = it },
                    label = "Мл",
                    keyboardType = KeyboardType.Number,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ml = editAmount.toIntOrNull()
                        if (ml != null && ml > 0) {
                            viewModel.updateRecord(rec.id, ml, rec.record_time)
                        }
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
