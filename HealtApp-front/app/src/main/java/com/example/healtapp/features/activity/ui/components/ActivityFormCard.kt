package com.example.healtapp.features.activity.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField

@Composable
fun ActivityFormCard(
    type: String,
    start: String,
    end: String,
    steps: String,
    intensity: String,
    note: String,
    onTypeChange: (String) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onStepsChange: (String) -> Unit,
    onIntensityChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Добавить активность",
                style = MaterialTheme.typography.titleMedium
            )

            AppTextField(
                value = type,
                onValueChange = onTypeChange,
                label = "Тип (walk, workout, run)"
            )

            AppTextField(
                value = start,
                onValueChange = onStartChange,
                label = "Начало"
            )

            AppTextField(
                value = end,
                onValueChange = onEndChange,
                label = "Конец"
            )

            AppTextField(
                value = steps,
                onValueChange = onStepsChange,
                label = "Шаги"
            )

            AppTextField(
                value = intensity,
                onValueChange = onIntensityChange,
                label = "Интенсивность"
            )

            AppTextField(
                value = note,
                onValueChange = onNoteChange,
                label = "Заметка"
            )

            AppButton(
                text = "Сохранить запись",
                onClick = onSaveClick
            )
        }
    }
}