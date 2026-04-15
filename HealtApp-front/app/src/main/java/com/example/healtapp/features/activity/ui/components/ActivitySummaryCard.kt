package com.example.healtapp.features.activity.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard

@Composable
fun ActivitySummaryCard(
    totalStepsToday: Int,
    totalMinutesToday: Int,
    caloriesBurnedToday: Int
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Сводка активности",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Шаги: $totalStepsToday")
            Text("Активность: ${totalMinutesToday} мин")
            Text("Калории: $caloriesBurnedToday")
        }
    }
}