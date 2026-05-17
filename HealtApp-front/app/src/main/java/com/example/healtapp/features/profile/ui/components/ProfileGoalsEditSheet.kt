package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileGoalsEditSheet(
    visible: Boolean,
    targetSleep: String,
    targetWater: String,
    targetSteps: String,
    targetCalories: String,
    isSaving: Boolean,
    guestMode: Boolean,
    onDismiss: () -> Unit,
    onSleepChange: (String) -> Unit,
    onWaterChange: (String) -> Unit,
    onStepsChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Твои цели",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Быстрое изменение дневных ориентиров",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
            }

            Text(
                text = "Шаги в день",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5_000, 8_000, 10_000, 12_000).forEach { preset ->
                    FilterChip(
                        selected = targetSteps.toIntOrNull() == preset,
                        onClick = { onStepsChange(preset.toString()) },
                        label = { Text("%,d".format(preset).replace(',', '\u00A0')) },
                    )
                }
            }

            AppTextField(targetSteps, onStepsChange, label = "Цель шагов")

            Text(
                text = "Сон и вода",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("7", "8", "9").forEach { h ->
                    FilterChip(
                        selected = targetSleep.toFloatOrNull()?.let { it == h.toFloat() } == true,
                        onClick = { onSleepChange(h) },
                        label = { Text("$h ч") },
                    )
                }
            }
            AppTextField(targetSleep, onSleepChange, label = "Цель сна (часы)")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2_000, 2_500, 3_000).forEach { ml ->
                    FilterChip(
                        selected = targetWater.toIntOrNull() == ml,
                        onClick = { onWaterChange(ml.toString()) },
                        label = { Text("%,d".format(ml).replace(',', '\u00A0')) },
                    )
                }
            }
            AppTextField(targetWater, onWaterChange, label = "Цель воды (мл)")

            Text(
                text = "Питание",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1_800, 2_200, 2_500).forEach { kcal ->
                    FilterChip(
                        selected = targetCalories.toIntOrNull() == kcal,
                        onClick = { onCaloriesChange(kcal.toString()) },
                        label = { Text("$kcal") },
                    )
                }
            }
            AppTextField(targetCalories, onCaloriesChange, label = "Цель калорий (ккал)")

            AppButton(
                text = when {
                    guestMode -> "Войдите для сохранения"
                    isSaving -> "Сохраняем…"
                    else -> "Сохранить цели"
                },
                onClick = onSave,
                enabled = !isSaving && !guestMode,
            )
        }
    }
}
