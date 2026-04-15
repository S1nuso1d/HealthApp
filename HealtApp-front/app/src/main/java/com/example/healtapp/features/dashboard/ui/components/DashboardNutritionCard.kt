package com.example.healtapp.features.dashboard.ui.components

import androidx.compose.runtime.Composable

@Composable
fun DashboardNutritionCard(
    caloriesToday: Int,
    caloriesTarget: Int,
    caffeineToday: Int
) {
    DashboardSectionCard(
        title = "Питание",
        value = "$caloriesToday / $caloriesTarget ккал",
        subtitle = "Кофеин сегодня: $caffeineToday мг"
    )
}