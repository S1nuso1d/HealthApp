package com.example.healtapp.features.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.di.AppModule
import com.example.healtapp.features.dashboard.presentation.DashboardViewModel
import com.example.healtapp.features.dashboard.ui.components.DashboardActivityCard
import com.example.healtapp.features.dashboard.ui.components.DashboardHeader
import com.example.healtapp.features.dashboard.ui.components.DashboardNutritionCard
import com.example.healtapp.features.dashboard.ui.components.DashboardSleepCard
import com.example.healtapp.features.dashboard.ui.components.DashboardWaterCard
import com.example.healtapp.features.dashboard.ui.components.QuickActionCard
import com.example.healtapp.features.dashboard.ui.components.RecommendationPreviewCard

@Composable
fun DashboardScreen() {
    val context = LocalContext.current

    val viewModel = remember {
        DashboardViewModel(
            profileRepository = AppModule.provideProfileRepository(context),
            sleepRepository = AppModule.provideSleepRepository(context),
            hydrationRepository = AppModule.provideHydrationRepository(context),
            activityRepository = AppModule.provideActivityRepository(context),
            mealRepository = AppModule.provideMealRepository(context)
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DashboardHeader(
            greeting = uiState.greetingText,
            userName = uiState.userName
        )

        SectionHeader("Сводка за сегодня")

        DashboardSleepCard(
            sleepHours = uiState.sleepHours,
            sleepTargetHours = uiState.sleepTargetHours,
            sleepQuality = uiState.sleepQuality
        )

        DashboardWaterCard(
            waterMl = uiState.waterMl,
            waterTargetMl = uiState.waterTargetMl
        )

        DashboardNutritionCard(
            caloriesToday = uiState.caloriesToday,
            caloriesTarget = uiState.caloriesTarget,
            caffeineToday = uiState.caffeineToday.toInt()
        )

        DashboardActivityCard(
            stepsToday = uiState.stepsToday,
            activityMinutesToday = uiState.activityMinutesToday,
            caloriesBurnedToday = uiState.caloriesBurnedToday
        )

        SectionHeader("Быстрые действия")

        uiState.quickActions.forEach { action ->
            QuickActionCard(
                title = action.title,
                subtitle = action.subtitle
            )
        }

        SectionHeader("Рекомендации дня")

        uiState.recommendations.forEach { recommendation ->
            RecommendationPreviewCard(
                title = recommendation.title,
                description = recommendation.description,
                priority = recommendation.priority
            )
        }
    }
}