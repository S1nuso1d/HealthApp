package com.example.healtapp.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.theme.AppBackgroundBottom
import com.example.healtapp.core.ui.theme.AppBackgroundTop
import com.example.healtapp.core.ui.theme.CardBlue
import com.example.healtapp.core.ui.theme.CardMint
import com.example.healtapp.di.AppModule
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val repository = remember { AppModule.provideProfileRepository(context) }
    val scope = rememberCoroutineScope()

    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Constants.Sex.MALE) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf(Constants.Goals.BETTER_SLEEP) }
    var activityLevel by remember { mutableStateOf(Constants.ActivityLevel.MEDIUM) }
    var targetSleep by remember { mutableStateOf("8") }
    var targetWater by remember { mutableStateOf("2500") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var successText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            errorText = null

            val result = repository.getMyProfile()

            result.onSuccess { profile ->
                age = profile.age?.toString().orEmpty()
                sex = profile.sex ?: Constants.Sex.MALE
                height = profile.height_cm?.toString().orEmpty()
                weight = profile.weight_kg?.toString().orEmpty()
                goal = profile.goal ?: Constants.Goals.BETTER_SLEEP
                activityLevel = profile.activity_level ?: Constants.ActivityLevel.MEDIUM
                targetSleep = profile.target_sleep_hours?.toString() ?: "8"
                targetWater = profile.target_water_ml?.toInt()?.toString() ?: "2500"
            }.onFailure {
                errorText = it.message ?: "Не удалось загрузить профиль"
            }

            isLoading = false
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Профиль",
            style = MaterialTheme.typography.headlineMedium
        )

        if (errorText != null) {
            Text(
                text = errorText ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (successText != null) {
            Text(
                text = successText ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Основные данные",
                    style = MaterialTheme.typography.titleLarge
                )

                AppTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = "Возраст"
                )

                AppTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = "Рост (см)"
                )

                AppTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = "Вес (кг)"
                )

                Text(
                    text = "Пол",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = sex == Constants.Sex.MALE,
                        onClick = { sex = Constants.Sex.MALE },
                        label = { Text("Мужской") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CardBlue
                        )
                    )

                    FilterChip(
                        selected = sex == Constants.Sex.FEMALE,
                        onClick = { sex = Constants.Sex.FEMALE },
                        label = { Text("Женский") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CardMint
                        )
                    )
                }

                Text(
                    text = "Цели",
                    style = MaterialTheme.typography.titleMedium
                )

                AppTextField(
                    value = targetSleep,
                    onValueChange = { targetSleep = it },
                    label = "Цель сна (часы)"
                )

                AppTextField(
                    value = targetWater,
                    onValueChange = { targetWater = it },
                    label = "Цель воды (мл)"
                )

                AppTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = "Цель (better_sleep / lose_weight / gain_muscle / improve_energy)"
                )

                AppTextField(
                    value = activityLevel,
                    onValueChange = { activityLevel = it },
                    label = "Активность (low / medium / high)"
                )

                AppButton(
                    text = if (isSaving) "Сохраняем..." else if (isLoading) "Загрузка..." else "Сохранить профиль",
                    enabled = !isSaving && !isLoading,
                    onClick = {
                        scope.launch {
                            isSaving = true
                            errorText = null
                            successText = null

                            val result = repository.updateMyProfile(
                                age = age.toIntOrNull(),
                                sex = sex,
                                heightCm = height.toFloatOrNull(),
                                weightKg = weight.toFloatOrNull(),
                                goal = goal,
                                activityLevel = activityLevel,
                                targetSleepHours = targetSleep.toFloatOrNull(),
                                targetWaterMl = targetWater.toFloatOrNull()
                            )

                            result.onSuccess {
                                successText = "Профиль успешно сохранён"
                            }.onFailure {
                                errorText = it.message ?: "Не удалось сохранить профиль"
                            }

                            isSaving = false
                        }
                    }
                )
            }
        }
    }
}