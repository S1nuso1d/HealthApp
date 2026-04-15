package com.example.healtapp.features.hydration.ui.components

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
fun HydrationFormCard(
    amount: String,
    drinkType: String,
    time: String,
    note: String,
    onAmountChange: (String) -> Unit,
    onDrinkTypeChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Добавить запись",
                style = MaterialTheme.typography.titleMedium
            )

            AppTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = "Количество (мл)"
            )

            AppTextField(
                value = drinkType,
                onValueChange = onDrinkTypeChange,
                label = "Тип напитка (water, tea, coffee)"
            )

            AppTextField(
                value = time,
                onValueChange = onTimeChange,
                label = "Время (например 14:20)"
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