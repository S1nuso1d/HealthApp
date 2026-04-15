package com.example.healtapp.features.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.theme.AppBackgroundBottom
import com.example.healtapp.core.ui.theme.AppBackgroundTop
import com.example.healtapp.core.ui.theme.CardBlue
import com.example.healtapp.core.ui.theme.CardMint
import com.example.healtapp.di.AppModule
import com.example.healtapp.features.onboarding.presentation.OnboardingEvent
import com.example.healtapp.features.onboarding.presentation.OnboardingViewModel
import com.example.healtapp.features.onboarding.ui.components.ActivityLevelSelector
import com.example.healtapp.features.onboarding.ui.components.GoalSelector

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val repository = AppModule.provideProfileRepository(context)

    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(repository)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.consumeSavedState()
            onFinish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AppBackgroundTop, AppBackgroundBottom)
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Настроим профиль",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Заполни базовые данные, чтобы рекомендации стали персональными и точными",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Основные данные",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                AppTextField(
                    value = uiState.age,
                    onValueChange = { viewModel.onEvent(OnboardingEvent.AgeChanged(it)) },
                    label = "Возраст"
                )

                AppTextField(
                    value = uiState.height,
                    onValueChange = { viewModel.onEvent(OnboardingEvent.HeightChanged(it)) },
                    label = "Рост (см)"
                )

                AppTextField(
                    value = uiState.weight,
                    onValueChange = { viewModel.onEvent(OnboardingEvent.WeightChanged(it)) },
                    label = "Вес (кг)"
                )

                Text(
                    text = "Пол",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = uiState.sex == Constants.Sex.MALE,
                        onClick = {
                            viewModel.onEvent(OnboardingEvent.SexChanged(Constants.Sex.MALE))
                        },
                        label = { Text("Мужской") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CardBlue
                        )
                    )

                    FilterChip(
                        selected = uiState.sex == Constants.Sex.FEMALE,
                        onClick = {
                            viewModel.onEvent(OnboardingEvent.SexChanged(Constants.Sex.FEMALE))
                        },
                        label = { Text("Женский") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CardMint
                        )
                    )
                }

                Spacer(modifier = Modifier.padding(top = 2.dp))

                Text(
                    text = "Цели и привычки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                AppTextField(
                    value = uiState.targetSleep,
                    onValueChange = { viewModel.onEvent(OnboardingEvent.TargetSleepChanged(it)) },
                    label = "Цель сна (часы)"
                )

                AppTextField(
                    value = uiState.targetWater,
                    onValueChange = { viewModel.onEvent(OnboardingEvent.TargetWaterChanged(it)) },
                    label = "Цель воды (мл)"
                )

                GoalSelector(
                    selectedGoal = uiState.goal,
                    onGoalSelected = {
                        viewModel.onEvent(OnboardingEvent.GoalChanged(it))
                    }
                )

                ActivityLevelSelector(
                    selected = uiState.activityLevel,
                    onSelected = {
                        viewModel.onEvent(OnboardingEvent.ActivityLevelChanged(it))
                    }
                )

                uiState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.padding(top = 4.dp))

                AppButton(
                    text = if (uiState.isLoading) "Сохраняем..." else "Продолжить",
                    onClick = {
                        viewModel.onEvent(OnboardingEvent.Submit)
                    },
                    enabled = !uiState.isLoading
                )
            }
        }
    }
}