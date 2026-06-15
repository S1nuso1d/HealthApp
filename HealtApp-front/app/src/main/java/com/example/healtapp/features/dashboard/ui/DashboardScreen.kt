package com.example.healtapp.features.dashboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.data.preferences.DashboardLayoutMode
import com.example.healtapp.data.preferences.DashboardUiPrefs
import com.example.healtapp.features.dashboard.presentation.DashboardViewModel
import com.example.healtapp.features.dashboard.ui.alt.DashboardAltScreen

@Composable
fun DashboardScreen(
    onOpenSleep: () -> Unit = {},
    onOpenHydration: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onOpenActivity: () -> Unit = {},
    onOpenRecommendations: () -> Unit = {},
    onOpenActionPlan: () -> Unit = {},
    onOpenTimeline: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    onOpenHealthVitals: () -> Unit = {},
) {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var layoutMode by remember {
        mutableStateOf(DashboardUiPrefs.getLayoutMode(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                layoutMode = DashboardUiPrefs.getLayoutMode(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val revertToClassic = {
        DashboardUiPrefs.setLayoutMode(context, DashboardLayoutMode.Classic)
        layoutMode = DashboardLayoutMode.Classic
    }

    when (layoutMode) {
        DashboardLayoutMode.Experimental -> DashboardAltScreen(
            uiState = uiState,
            dashboardViewModel = dashboardViewModel,
            onOpenSleep = onOpenSleep,
            onOpenHydration = onOpenHydration,
            onOpenNutrition = onOpenNutrition,
            onOpenActivity = onOpenActivity,
            onOpenRecommendations = onOpenRecommendations,
            onOpenActionPlan = onOpenActionPlan,
            onOpenTimeline = onOpenTimeline,
            onOpenAiAssistant = onOpenAiAssistant,
            onOpenHealthVitals = onOpenHealthVitals,
            onRevertLayout = revertToClassic,
        )

        DashboardLayoutMode.Classic -> DashboardClassicScreen(
            uiState = uiState,
            dashboardViewModel = dashboardViewModel,
            onOpenSleep = onOpenSleep,
            onOpenHydration = onOpenHydration,
            onOpenNutrition = onOpenNutrition,
            onOpenActivity = onOpenActivity,
            onOpenRecommendations = onOpenRecommendations,
            onOpenActionPlan = onOpenActionPlan,
            onOpenTimeline = onOpenTimeline,
            onOpenAiAssistant = onOpenAiAssistant,
            onOpenHealthVitals = onOpenHealthVitals,
        )
    }
}
