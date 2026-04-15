package com.example.healtapp.features.meal.ui.components

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
fun MealFormCard(
    title: String,
    time: String,
    calories: String,
    caffeine: String,
    note: String,
    onTitleChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onCaffeineChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Добавить прием пищи",
                style = MaterialTheme.typography.titleMedium
            )

            AppTextField(
                value = title,
                onValueChange = onTitleChange,
                label = "Название"
            )

            AppTextField(
                value = time,
                onValueChange = onTimeChange,
                label = "Время"
            )

            AppTextField(
                value = calories,
                onValueChange = onCaloriesChange,
                label = "Калории"
            )

            AppTextField(
                value = caffeine,
                onValueChange = onCaffeineChange,
                label = "Кофеин (мг)"
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