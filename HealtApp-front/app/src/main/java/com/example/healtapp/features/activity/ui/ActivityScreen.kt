package com.example.healtapp.features.activity.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.features.activity.presentation.ActivityViewModel

@Composable
fun ActivityScreen() {
    val viewModel: ActivityViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsState()

    val activityTypes = listOf(
        "Бег",
        "Прогулка",
        "Ходьба",
        "Велосипед",
        "Силовая тренировка",
        "Йога",
        "Растяжка",
        "Плавание"
    )

    val intensityTypes = listOf(
        "Низкая",
        "Средняя",
        "Высокая"
    )

    var activityExpanded by remember { mutableStateOf(false) }
    var intensityExpanded by remember { mutableStateOf(false) }

    AppScreen(
        title = "Активность",
        subtitle = "Шаги и тренировки",
        headerIcon = Icons.AutoMirrored.Filled.DirectionsWalk,
        scrollable = true,
    ) {
        Text(
            text = "Добавляй тренировки и отслеживай ежедневную активность.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.activityType,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        label = { Text("Тип активности") },
                        placeholder = { Text("Выбери активность") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Открыть список"
                            )
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                activityExpanded = true
                            }
                    )
                }

                DropdownMenu(
                    expanded = activityExpanded,
                    onDismissRequest = { activityExpanded = false }
                ) {
                    activityTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                viewModel.updateActivityType(type)
                                activityExpanded = false
                            }
                        )
                    }
                }

                AppTextField(
                    value = uiState.durationMinutes,
                    onValueChange = viewModel::updateDuration,
                    label = "Длительность (мин)"
                )

                AppTextField(
                    value = uiState.steps,
                    onValueChange = viewModel::updateSteps,
                    label = "Шаги"
                )

                AppTextField(
                    value = uiState.distanceKm,
                    onValueChange = viewModel::updateDistance,
                    label = "Дистанция (км)"
                )

                AppTextField(
                    value = uiState.caloriesBurned,
                    onValueChange = viewModel::updateCalories,
                    label = "Сожжено калорий"
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.intensity,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        label = { Text("Интенсивность") },
                        placeholder = { Text("Выбери интенсивность") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Открыть список"
                            )
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                intensityExpanded = true
                            }
                    )
                }

                DropdownMenu(
                    expanded = intensityExpanded,
                    onDismissRequest = { intensityExpanded = false }
                ) {
                    intensityTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                viewModel.updateIntensity(type)
                                intensityExpanded = false
                            }
                        )
                    }
                }

                AppButton(
                    text = if (uiState.isSaving) "Сохраняем..." else "Сохранить активность",
                    onClick = { viewModel.saveActivity() },
                    enabled = !uiState.isSaving
                )

                uiState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.todayActivity?.let { activity ->
                    Text(
                        text = "Сегодня: ${activity.activity_type}, ${activity.duration_minutes} мин",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}