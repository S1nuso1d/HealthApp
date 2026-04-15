package com.example.healtapp.features.sleep.ui.components

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
fun SleepFormCard(
    sleepStart: String,
    sleepEnd: String,
    quality: String,
    note: String,
    onSleepStartChange: (String) -> Unit,
    onSleepEndChange: (String) -> Unit,
    onQualityChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Добавить сон",
                style = MaterialTheme.typography.titleMedium
            )

            AppTextField(
                value = sleepStart,
                onValueChange = onSleepStartChange,
                label = "Время сна (например 23:30)"
            )

            AppTextField(
                value = sleepEnd,
                onValueChange = onSleepEndChange,
                label = "Время пробуждения (например 07:30)"
            )

            AppTextField(
                value = quality,
                onValueChange = onQualityChange,
                label = "Качество сна (0-100)"
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