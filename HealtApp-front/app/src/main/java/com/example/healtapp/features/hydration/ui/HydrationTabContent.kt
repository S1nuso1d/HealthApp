package com.example.healtapp.features.hydration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppDialogMessage
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.progressCelebrateEffect
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.healtapp.core.ui.components.CollapsibleAppCard
import com.example.healtapp.core.ui.components.PendingSyncBadge
import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.features.hydration.presentation.HydrationViewModel

@Composable
fun HydrationTabContent() {
    val viewModel: HydrationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var recordToEdit by remember { mutableStateOf<HydrationDto?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var recordToDelete by remember { mutableStateOf<HydrationDto?>(null) }
    var customMlInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Отслеживай водный баланс и быстро добавляй выпитую воду.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PendingSyncBadge(count = uiState.pendingSyncCount)

        AppCard {
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .progressCelebrateEffect(uiState.progressCelebrateToken),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Сегодня: ${uiState.waterToday} мл",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Цель: ${uiState.target} мл",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                }
                uiState.error?.let { errorText ->
                    AppMessageBanner(text = errorText, type = AppMessageType.Error)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { viewModel.addWater(200) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
            ) { Text("+200 мл") }
            Button(
                onClick = { viewModel.addWater(250) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
            ) { Text("+250 мл") }
            Button(
                onClick = { viewModel.addWater(500) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
            ) { Text("+500 мл") }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Свой объём",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                AppTextField(
                    value = customMlInput,
                    onValueChange = { customMlInput = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = "Сколько миллилитров выпили?",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Number,
                )
                AppButton(
                    text = "Добавить",
                    onClick = {
                        val ml = customMlInput.toIntOrNull()
                        if (ml != null && ml > 0) {
                            viewModel.addWater(ml)
                            customMlInput = ""
                        }
                    },
                    enabled = customMlInput.toIntOrNull()?.let { it > 0 } == true && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        CollapsibleAppCard(
            title = "Записи за сегодня",
            subtitle = if (uiState.todayRecords.isEmpty()) "Пока нет записей" else "${uiState.todayRecords.size} записей",
            initiallyExpanded = false,
        ) {
            if (uiState.todayRecords.isEmpty()) {
                Text(
                    text = "Пока нет записей за сегодня.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
