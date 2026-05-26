package com.example.healtapp.features.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.common.BmiHelper
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.theme.bmiCategoryColor
import com.example.healtapp.core.ui.theme.chipSelectedColor
import com.example.healtapp.core.ui.theme.screenBackgroundGradient
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.core.ui.theme.themedCardMint
import com.example.healtapp.features.onboarding.presentation.OnboardingEvent
import com.example.healtapp.features.onboarding.presentation.OnboardingViewModel
import com.example.healtapp.features.onboarding.ui.components.ActivityLevelSelector
import com.example.healtapp.features.onboarding.ui.components.GoalSelector
import com.example.healtapp.features.profile.ProfileRus
import java.text.DecimalFormat

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.consumeSavedState()
            onFinish()
        }
    }

    val stepTitles = listOf(
        "Добро пожаловать",
        "Питание",
        "Тело",
        "Цели",
        "Итог",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(screenBackgroundGradient()))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Короткий опрос",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Ответь на несколько вопросов — рассчитаем ИМТ и ориентиры по калориям и БЖУ.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { (uiState.step + 1f) / uiState.totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            Text(
                text = "Шаг ${uiState.step + 1} из ${uiState.totalSteps}: ${stepTitles.getOrElse(uiState.step) { "" }}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (uiState.step) {
                    0 -> WelcomeStep()
                    1 -> DietStep(uiState, viewModel::onEvent)
                    2 -> BodyStep(uiState, viewModel::onEvent)
                    3 -> GoalsStep(uiState, viewModel::onEvent)
                    4 -> SummaryStep(uiState)
                }

                uiState.error?.let {
                    AppMessageBanner(text = it, type = AppMessageType.Error)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (uiState.step > 0) {
                        AppButton(
                            text = "Назад",
                            onClick = { viewModel.onEvent(OnboardingEvent.PrevStep) },
                            isSecondary = true,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AppButton(
                        text = when {
                            uiState.isLoading -> "Сохраняем..."
                            uiState.step == uiState.totalSteps - 1 -> "В приложение"
                            else -> "Далее"
                        },
                        onClick = {
                            if (uiState.step == uiState.totalSteps - 1) {
                                viewModel.onEvent(OnboardingEvent.Submit)
                            } else {
                                viewModel.onEvent(OnboardingEvent.NextStep)
                            }
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Text(
        text = "Расскажи о себе",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = "Это займёт пару минут. Данные можно изменить позже в профиле.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DietStep(
    uiState: com.example.healtapp.features.onboarding.presentation.OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
) {
    Text(text = "Питание и ограничения", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Text(text = "Вегетарианец?", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
            selected = !uiState.isVegetarian,
            onClick = { onEvent(OnboardingEvent.VegetarianChanged(false)) },
            label = { Text("Нет") },
        )
        FilterChip(
            selected = uiState.isVegetarian,
            onClick = { onEvent(OnboardingEvent.VegetarianChanged(true)) },
            label = { Text("Да") },
            leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Eco, contentDescription = null) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = chipSelectedColor(themedCardMint()),
            ),
        )
    }
    Text(text = "Есть пищевые аллергии?", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
            selected = !uiState.hasAllergies,
            onClick = { onEvent(OnboardingEvent.HasAllergiesChanged(false)) },
            label = { Text("Нет") },
        )
        FilterChip(
            selected = uiState.hasAllergies,
            onClick = { onEvent(OnboardingEvent.HasAllergiesChanged(true)) },
            label = { Text("Да") },
            leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Restaurant, contentDescription = null) },
        )
    }
    if (uiState.hasAllergies) {
        AppTextField(
            value = uiState.allergiesText,
            onValueChange = { onEvent(OnboardingEvent.AllergiesTextChanged(it)) },
            label = "На что аллергия / непереносимость",
        )
    }
}

@Composable
private fun BodyStep(
    uiState: com.example.healtapp.features.onboarding.presentation.OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
) {
    Text(text = "Параметры тела", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    AppTextField(uiState.age, { onEvent(OnboardingEvent.AgeChanged(it)) }, label = "Возраст")
    AppTextField(uiState.height, { onEvent(OnboardingEvent.HeightChanged(it)) }, label = "Рост (см)")
    AppTextField(uiState.weight, { onEvent(OnboardingEvent.WeightChanged(it)) }, label = "Вес (кг)")
    Text(text = "Пол", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
            selected = uiState.sex == Constants.Sex.MALE,
            onClick = { onEvent(OnboardingEvent.SexChanged(Constants.Sex.MALE)) },
            label = { Text("Мужской") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = chipSelectedColor(themedCardBlue())),
        )
        FilterChip(
            selected = uiState.sex == Constants.Sex.FEMALE,
            onClick = { onEvent(OnboardingEvent.SexChanged(Constants.Sex.FEMALE)) },
            label = { Text("Женский") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = chipSelectedColor(themedCardMint())),
        )
    }
    val bmi = BmiHelper.calculate(uiState.height.toFloatOrNull(), uiState.weight.toFloatOrNull())
    if (bmi != null) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bmiCategoryColor(bmi.category).copy(alpha = 0.12f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(
                    Icons.Filled.MonitorWeight,
                    contentDescription = null,
                    tint = bmiCategoryColor(bmi.category),
                )
                Column {
                    Text(
                        text = "ИМТ ${BmiHelper.formatValue(bmi.value)} — ${bmi.labelRu}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = bmi.hintRu,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalsStep(
    uiState: com.example.healtapp.features.onboarding.presentation.OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
) {
    Text(text = "Цель и активность", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    GoalSelector(selectedGoal = uiState.goal, onGoalSelected = { onEvent(OnboardingEvent.GoalChanged(it)) })
    ActivityLevelSelector(
        selected = uiState.activityLevel,
        onSelected = { onEvent(OnboardingEvent.ActivityLevelChanged(it)) },
    )
}

@Composable
private fun SummaryStep(
    uiState: com.example.healtapp.features.onboarding.presentation.OnboardingUiState,
) {
    val df = remember { DecimalFormat("#,###") }
    Text(text = "Ваши ориентиры", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Text(
        text = "Цель: ${ProfileRus.goalLabel(uiState.goal)} · ${ProfileRus.activityLevelLabel(uiState.activityLevel)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (uiState.isVegetarian) {
        Text("Вегетарианское питание", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
    if (uiState.hasAllergies && uiState.allergiesText.isNotBlank()) {
        Text(
            "Аллергии: ${uiState.allergiesText}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        text = "Не нужно искать КБЖУ вручную — мы рассчитали ориентиры по росту, весу и цели. Сохраним их в профиль.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val t = uiState.previewTargets
    if (t != null) {
        Spacer(Modifier.height(8.dp))
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "КБЖУ и цели на день",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                SummaryRow("Калории", "${df.format(t.calories)} ккал")
                SummaryRow("Белки", "${t.proteinG.toInt()} г")
                SummaryRow("Жиры", "${t.fatG.toInt()} г")
                SummaryRow("Углеводы", "${t.carbsG.toInt()} г")
                SummaryRow("Вода", "${df.format(t.waterMl.toInt())} мл")
                SummaryRow("Сон", "${t.sleepHours.toInt()} ч")
                SummaryRow("Шаги", df.format(t.steps))
            }
        }
    } else {
        Text(
            "Заполните рост, вес и возраст на предыдущих шагах",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
