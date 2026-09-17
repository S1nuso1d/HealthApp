package com.example.healtapp.features.dashboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.features.dashboard.presentation.DashboardViewModel

@Composable
fun DashboardScreen(
    onOpenSleep: () -> Unit = {},
    onOpenHydration: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onOpenActivity: () -> Unit = {},
    onOpenTimeline: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    onOpenHealthVitals: () -> Unit = {},
    onOpenWeeklyReview: () -> Unit = {},
    onOpenInfluenceFactors: () -> Unit = {},
) {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    DashboardClassicScreen(
        uiState = uiState,
        dashboardViewModel = dashboardViewModel,
        onOpenSleep = onOpenSleep,
        onOpenHydration = onOpenHydration,
        onOpenNutrition = onOpenNutrition,
        onOpenActivity = onOpenActivity,
        onOpenTimeline = onOpenTimeline,
        onOpenAiAssistant = onOpenAiAssistant,
        onOpenHealthVitals = onOpenHealthVitals,
        onOpenWeeklyReview = onOpenWeeklyReview,
        onOpenInfluenceFactors = onOpenInfluenceFactors,
    )
}
