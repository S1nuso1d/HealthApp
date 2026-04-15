package com.example.healtapp.features.meal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppCard

@Composable
fun NutritionSummaryCard(
    totalCaloriesToday: Int,
    targetCaloriesToday: Int,
    proteinToday: Int,
    caffeineToday: Int
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Сводка питания",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Калории: $totalCaloriesToday / $targetCaloriesToday")
            Text("Белок: ${proteinToday} г")
            Text("Кофеин: ${caffeineToday} мг")
        }
    }
}