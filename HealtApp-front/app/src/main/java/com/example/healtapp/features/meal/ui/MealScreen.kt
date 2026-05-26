package com.example.healtapp.features.meal.ui

import androidx.compose.runtime.Composable
import com.example.healtapp.features.nutrition.ui.NutritionHubScreen

/** @deprecated Используйте [NutritionHubScreen] с вкладкой «Питание». */
@Composable
fun MealScreen(
    onOpenHydration: () -> Unit = {},
    onOpenHealthVitals: () -> Unit = {},
) {
    NutritionHubScreen(initialTab = 0)
}
