package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.DatePickerField
import com.example.healtapp.core.ui.theme.chipSelectedColor
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.core.ui.theme.themedCardMint
import com.example.healtapp.features.onboarding.ui.components.ActivityLevelSelector
import com.example.healtapp.features.onboarding.ui.components.GoalSelector
import com.example.healtapp.features.profile.presentation.ProfileEditUiState

@Composable
fun ProfilePersonalDataFields(
    uiState: ProfileEditUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSexChange: (String) -> Unit,
    onIsVegetarianChange: (Boolean) -> Unit,
    onHasAllergiesChange: (Boolean) -> Unit,
    onAllergiesTextChange: (String) -> Unit,
    onSave: () -> Unit,
    includeSave: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Имя и фамилия обязательны — по ним вас найдут друзья в поиске.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppTextField(
            value = uiState.firstName,
            onValueChange = onFirstNameChange,
            label = "Имя",
            enabled = true,
        )
        AppTextField(
            value = uiState.lastName,
            onValueChange = onLastNameChange,
            label = "Фамилия",
            enabled = true,
        )
        AppTextField(
            value = uiState.nickname,
            onValueChange = onNicknameChange,
            label = "Никнейм",
            enabled = true,
        )
        if (uiState.publicDisplayName.isNotBlank()) {
            Text(
                text = "В ленте: ${uiState.publicDisplayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DatePickerField(
            value = uiState.birthDate,
            onValueChange = onBirthDateChange,
            label = "Дата рождения",
            enabled = true,
        )
        if (uiState.age.isNotBlank()) {
            Text(
                text = "Возраст: ${uiState.age} лет (обновляется автоматически)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "Пол",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = uiState.sex == Constants.Sex.MALE,
                onClick = { onSexChange(Constants.Sex.MALE) },
                label = { Text("Мужской") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipSelectedColor(themedCardBlue()),
                ),
            )
            FilterChip(
                selected = uiState.sex == Constants.Sex.FEMALE,
                onClick = { onSexChange(Constants.Sex.FEMALE) },
                label = { Text("Женский") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipSelectedColor(themedCardMint()),
                ),
            )
        }
        AppTextField(
            value = uiState.height,
            onValueChange = onHeightChange,
            label = "Рост (см)",
            keyboardType = KeyboardType.Number,
            enabled = true,
        )
        AppTextField(
            value = uiState.weight,
            onValueChange = onWeightChange,
            label = "Вес (кг)",
            keyboardType = KeyboardType.Decimal,
            enabled = true,
        )
        ProfileDietFields(
            uiState = uiState,
            onIsVegetarianChange = onIsVegetarianChange,
            onHasAllergiesChange = onHasAllergiesChange,
            onAllergiesTextChange = onAllergiesTextChange,
            onSave = {},
            includeSave = false,
        )
        if (includeSave) {
            AppButton(
                text = when {
                    uiState.isSaving -> "Сохраняем..."
                    uiState.isLoading -> "Загрузка..."
                    else -> "Сохранить данные"
                },
                enabled = !uiState.isSaving && !uiState.isLoading,
                onClick = onSave,
            )
        }
    }
}

@Composable
fun ProfileGoalsHabitsFields(
    uiState: ProfileEditUiState,
    onGoalChange: (String) -> Unit,
    onActivityLevelChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Цель и уровень активности",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Сон, вода и шаги — в блоке «Твои цели» на вкладке профиля. Калории и БЖУ — в разделе «Питание».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GoalSelector(
            selectedGoal = uiState.goal.ifBlank { Constants.Goals.IMPROVE_ENERGY },
            onGoalSelected = onGoalChange,
        )
        ActivityLevelSelector(
            selected = uiState.activityLevel.ifBlank { Constants.ActivityLevel.MEDIUM },
            onSelected = onActivityLevelChange,
        )
        AppButton(
            text = when {
                uiState.isSaving -> "Сохраняем..."
                uiState.isLoading -> "Загрузка..."
                else -> "Сохранить"
            },
            enabled = !uiState.isSaving && !uiState.isLoading && !uiState.isChangingPassword,
            onClick = onSave,
        )
    }
}

@Composable
fun ProfileDietFields(
    uiState: ProfileEditUiState,
    onIsVegetarianChange: (Boolean) -> Unit,
    onHasAllergiesChange: (Boolean) -> Unit,
    onAllergiesTextChange: (String) -> Unit,
    onSave: () -> Unit,
    includeSave: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Вегетарианец?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = !uiState.isVegetarian,
                onClick = { onIsVegetarianChange(false) },
                label = { Text("Нет") },
            )
            FilterChip(
                selected = uiState.isVegetarian,
                onClick = { onIsVegetarianChange(true) },
                label = { Text("Да") },
                leadingIcon = { Icon(Icons.Filled.Eco, contentDescription = null) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipSelectedColor(themedCardMint()),
                ),
            )
        }
        Text(
            text = "Аллергии?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = !uiState.hasAllergies,
                onClick = { onHasAllergiesChange(false) },
                label = { Text("Нет") },
            )
            FilterChip(
                selected = uiState.hasAllergies,
                onClick = { onHasAllergiesChange(true) },
                label = { Text("Да") },
            )
        }
        if (uiState.hasAllergies) {
            AppTextField(
                uiState.allergiesText,
                onAllergiesTextChange,
                label = "На что аллергия",
            )
        }
        if (includeSave) {
            AppButton(
                text = if (uiState.isSaving) "Сохраняем..." else "Сохранить",
                enabled = !uiState.isSaving && !uiState.isLoading,
                onClick = onSave,
            )
        }
    }
}
