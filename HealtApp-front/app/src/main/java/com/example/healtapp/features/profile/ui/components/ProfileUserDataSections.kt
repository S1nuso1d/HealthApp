package com.example.healtapp.features.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.example.healtapp.features.profile.ui.ProfileExpandableCard

@Composable
fun ProfilePersonalDataSection(
    uiState: ProfileEditUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSexChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    ProfileExpandableCard(
        title = "Основные данные",
        icon = Icons.Filled.Tune,
        initiallyExpanded = false,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        Text(
            text = "Имя и фамилия обязательны — по ним вас найдут друзья в поиске.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppTextField(
            value = uiState.firstName,
            onValueChange = onFirstNameChange,
            label = "Имя",
            enabled = !uiState.guestMode,
        )
        AppTextField(
            value = uiState.lastName,
            onValueChange = onLastNameChange,
            label = "Фамилия",
            enabled = !uiState.guestMode,
        )
        AppTextField(
            value = uiState.nickname,
            onValueChange = onNicknameChange,
            label = "Никнейм",
            enabled = !uiState.guestMode,
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
            enabled = !uiState.guestMode,
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
            enabled = !uiState.guestMode,
        )
        AppTextField(
            value = uiState.weight,
            onValueChange = onWeightChange,
            label = "Вес (кг)",
            keyboardType = KeyboardType.Decimal,
            enabled = !uiState.guestMode,
        )
        AppButton(
            text = when {
                uiState.isSaving -> "Сохраняем..."
                uiState.isLoading -> "Загрузка..."
                else -> "Сохранить данные"
            },
            enabled = !uiState.isSaving && !uiState.isLoading && !uiState.guestMode,
            onClick = onSave,
        )
    }
}

@Composable
fun ProfileGoalsHabitsSection(
    uiState: ProfileEditUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGoalChange: (String) -> Unit,
    onActivityLevelChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    ProfileExpandableCard(
        title = "Цель и активность",
        icon = Icons.Filled.Flag,
        initiallyExpanded = false,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        Text(
            text = "Сон, вода и шаги — в блоке «Твои цели» выше. Калории и БЖУ — в разделе «Питание».",
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
            enabled = !uiState.isSaving && !uiState.isLoading && !uiState.isChangingPassword && !uiState.guestMode,
            onClick = onSave,
        )
    }
}

@Composable
fun ProfileDietSection(
    uiState: ProfileEditUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onIsVegetarianChange: (Boolean) -> Unit,
    onHasAllergiesChange: (Boolean) -> Unit,
    onAllergiesTextChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    ProfileExpandableCard(
        title = "Питание и ограничения",
        icon = Icons.Filled.Restaurant,
        initiallyExpanded = false,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
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
        AppButton(
            text = if (uiState.isSaving) "Сохраняем..." else "Сохранить",
            enabled = !uiState.isSaving && !uiState.isLoading && !uiState.guestMode,
            onClick = onSave,
        )
    }
}
