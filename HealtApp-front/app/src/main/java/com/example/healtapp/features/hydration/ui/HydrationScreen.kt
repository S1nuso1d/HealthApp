package com.example.healtapp.features.hydration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
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
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.features.hydration.presentation.HydrationViewModel

@Composable
fun HydrationScreen() {
    val viewModel: HydrationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var recordToEdit by remember { mutableStateOf<HydrationDto?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var recordToDelete by remember { mutableStateOf<HydrationDto?>(null) }

    androidx.compose.foundation.layout.Box {
        AppScreen(
        title = "Вода",
        subtitle = "Гидратация и быстрый ввод",
        headerIcon = Icons.Filled.WaterDrop,
        scrollable = true,
    ) {
        Text(
            text = "Отслеживай водный баланс и быстро добавляй выпитую воду.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AppCard {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Сегодня: ${uiState.waterToday} мл",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Цель: ${uiState.target} мл",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                }

                uiState.error?.let { errorText ->
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.addWater(200) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("+200 мл")
            }

            Button(
                onClick = { viewModel.addWater(250) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("+250 мл")
            }

            Button(
                onClick = { viewModel.addWater(500) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("+500 мл")
            }
        }

        SectionHeader("Записи за сегодня")

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
                            }) {
                                Text("Изменить")
                            }
                            TextButton(onClick = { recordToDelete = rec }) {
                                Text("Удалить", color = MaterialTheme.colorScheme.error)
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
            text = { Text("${rec.amount_ml} мл будет удалено.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(rec.id)
                        recordToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Отмена")
                }
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
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToEdit = null }) {
                    Text("Отмена")
                }
            },
        )
    }
    }
}
