package com.example.healtapp.features.meal.ui.components

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
fun MealNutritionTargetsSheet(
    visible: Boolean,
    targetCalories: String,
    targetProtein: String,
    targetFat: String,
    targetCarbs: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
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
                        text = "Ориентиры питания",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Рассчитаны по росту, весу, возрасту и цели — можно изменить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
            }

            Text(
                text = "Калории",
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
            AppTextField(targetCalories, onCaloriesChange, label = "Калории (ккал/день)")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AppTextField(
                    targetProtein,
                    onProteinChange,
                    label = "Белки (г)",
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    targetFat,
                    onFatChange,
                    label = "Жиры (г)",
                    modifier = Modifier.weight(1f),
                )
            }
            AppTextField(targetCarbs, onCarbsChange, label = "Углеводы (г)")

            AppButton(
                text = if (isSaving) "Сохраняем…" else "Сохранить ориентиры",
                onClick = onSave,
                enabled = !isSaving,
            )
        }
    }
}
