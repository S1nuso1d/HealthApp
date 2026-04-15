package com.example.healtapp.features.meal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MealRecordItem(
    mealType: String = "Приём пищи",
    calories: Int? = null,
    caffeineMg: Float? = null,
    notes: String? = null,
    recordTime: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = mealType,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val details = buildList {
                calories?.let { add("Калории: $it") }
                caffeineMg?.let { add("Кофеин: ${it.toInt()} мг") }
                if (!notes.isNullOrBlank()) add(notes)
            }.joinToString(" • ")

            Text(
                text = if (details.isBlank()) "Без деталей" else details,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = recordTime,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}