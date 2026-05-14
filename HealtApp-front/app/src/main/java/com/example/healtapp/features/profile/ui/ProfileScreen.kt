package com.example.healtapp.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.theme.CardBlue
import com.example.healtapp.core.ui.theme.CardMint
import com.example.healtapp.core.ui.theme.MintPrimaryDark
import com.example.healtapp.core.ui.theme.SkyPrimaryDark
import com.example.healtapp.features.profile.presentation.ProfileEditViewModel

@Composable
fun ProfileScreen() {
    val viewModel: ProfileEditViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val initial = when {
        uiState.goal.isNotBlank() -> uiState.goal.first().uppercaseChar().toString()
        uiState.age.isNotBlank() -> uiState.age.first().toString()
        else -> "Я"
    }

    AppScreen(
        title = "Профиль",
        subtitle = "Персонализация и цели",
        headerIcon = Icons.Filled.Person,
        scrollable = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MintPrimaryDark, SkyPrimaryDark),
                    ),
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Твой аккаунт",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = if (uiState.goal.isNotBlank()) {
                        "Цель: ${uiState.goal}"
                    } else {
                        "Заполни данные — рекомендации станут точнее"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        uiState.success?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfileSectionTitle(
                    icon = Icons.Filled.Tune,
                    title = "Основные данные",
                )
                AppTextField(
                    value = uiState.age,
                    onValueChange = viewModel::updateAge,
                    label = "Возраст",
                )
                AppTextField(
                    value = uiState.height,
                    onValueChange = viewModel::updateHeight,
                    label = "Рост (см)",
                )
                AppTextField(
                    value = uiState.weight,
                    onValueChange = viewModel::updateWeight,
                    label = "Вес (кг)",
                )
                Text(
                    text = "Пол",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = uiState.sex == Constants.Sex.MALE,
                        onClick = { viewModel.updateSex(Constants.Sex.MALE) },
                        label = { Text("Мужской") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CardBlue.copy(alpha = 0.85f),
                        ),
                    )
                    FilterChip(
                        selected = uiState.sex == Constants.Sex.FEMALE,
                        onClick = { viewModel.updateSex(Constants.Sex.FEMALE) },
                        label = { Text("Женский") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CardMint.copy(alpha = 0.85f),
                        ),
                    )
                }
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfileSectionTitle(
                    icon = Icons.Filled.Flag,
                    title = "Цели и привычки",
                )
                AppTextField(
                    value = uiState.targetSleep,
                    onValueChange = viewModel::updateTargetSleep,
                    label = "Цель сна (часы)",
                )
                AppTextField(
                    value = uiState.targetWater,
                    onValueChange = viewModel::updateTargetWater,
                    label = "Цель воды (мл)",
                )
                AppTextField(
                    value = uiState.goal,
                    onValueChange = viewModel::updateGoal,
                    label = "Цель (например, энергия, сон, вес)",
                )
                AppTextField(
                    value = uiState.activityLevel,
                    onValueChange = viewModel::updateActivityLevel,
                    label = "Уровень активности",
                )
                AppButton(
                    text = when {
                        uiState.isSaving -> "Сохраняем..."
                        uiState.isLoading -> "Загрузка..."
                        else -> "Сохранить профиль"
                    },
                    enabled = !uiState.isSaving && !uiState.isLoading,
                    onClick = viewModel::save,
                )
            }
        }
    }
}

@Composable
private fun ProfileSectionTitle(
    icon: ImageVector,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
