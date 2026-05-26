package com.example.healtapp.features.hydration.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.features.nutrition.ui.NutritionHubScreen

/** @deprecated Используйте [NutritionHubScreen] с вкладкой «Вода». */
@Composable
fun HydrationScreen() {
    NutritionHubScreen(initialTab = 2)
}
