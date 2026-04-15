package com.example.healtapp.features.activity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.di.AppModule
import com.example.healtapp.features.activity.presentation.ActivityViewModel

@Composable
fun ActivityScreen() {
    val context = LocalContext.current
    val viewModel = androidx.compose.runtime.remember {
        ActivityViewModel(AppModule.provideActivityRepository(context))
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Активность",
            style = MaterialTheme.typography.headlineMedium
        )

        AppTextField(
            value = uiState.activityType,
            onValueChange = viewModel::updateActivityType,
            label = "Тип активности"
        )

        AppTextField(
            value = uiState.durationMinutes,
            onValueChange = viewModel::updateDuration,
            label = "Длительность (мин)"
        )

        AppTextField(
            value = uiState.steps,
            onValueChange = viewModel::updateSteps,
            label = "Шаги"
        )

        AppTextField(
            value = uiState.distanceKm,
            onValueChange = viewModel::updateDistance,
            label = "Дистанция (км)"
        )

        AppTextField(
            value = uiState.caloriesBurned,
            onValueChange = viewModel::updateCalories,
            label = "Сожжено калорий"
        )

        AppTextField(
            value = uiState.intensity,
            onValueChange = viewModel::updateIntensity,
            label = "Интенсивность"
        )

        AppButton(
            text = if (uiState.isSaving) "Сохраняем..." else "Сохранить активность",
            onClick = { viewModel.saveActivity() },
            enabled = !uiState.isSaving
        )

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        uiState.todayActivity?.let { activity ->
            Text(
                text = "Сегодня: ${activity.activity_type}, ${activity.duration_minutes} мин",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}