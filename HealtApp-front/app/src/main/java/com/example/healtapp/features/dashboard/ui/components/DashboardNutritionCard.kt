package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.Composable
import com.example.healtapp.core.ui.theme.CardMint

@Composable
fun DashboardNutritionCard(
    caloriesToday: Int,
    caloriesTarget: Int,
    caffeineToday: Int
) {
    DashboardSectionCard(
        title = "Питание",
        value = "$caloriesToday / $caloriesTarget ккал",
        subtitle = "Кофеин сегодня: $caffeineToday мг",
        icon = Icons.Filled.Restaurant,
        iconBackground = listOf(
            CardMint.copy(alpha = 0.95f),
            CardMint.copy(alpha = 0.5f),
        ),
    )
}