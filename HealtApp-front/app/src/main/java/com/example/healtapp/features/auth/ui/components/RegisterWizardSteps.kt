package com.example.healtapp.features.auth.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healtapp.core.common.Constants
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppPasswordField
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.DatePickerField
import com.example.healtapp.core.ui.theme.chipSelectedColor
import com.example.healtapp.core.ui.theme.themedCardBlue
import com.example.healtapp.core.ui.theme.themedCardMint
import com.example.healtapp.features.auth.presentation.AuthEvent
import com.example.healtapp.features.auth.presentation.AuthUiState
import com.example.healtapp.features.auth.presentation.DietaryExclusionCatalog
import com.example.healtapp.features.auth.presentation.RegisterStep
import com.example.healtapp.features.onboarding.ui.components.GoalSelector

@Composable
fun RegisterProfileStep(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
) {
    AuthFormCard(
        sectionTitle = "О вас",
        sectionSubtitle = "Имя, фамилия, рост, вес, пол и цель для персональных рекомендаций",
    ) {
        AppTextField(
            value = uiState.firstName,
            onValueChange = { onEvent(AuthEvent.FirstNameChanged(it)) },
            label = "Имя",
            leadingIcon = Icons.Outlined.Person,
        )
        AppTextField(
            value = uiState.lastName,
            onValueChange = { onEvent(AuthEvent.LastNameChanged(it)) },
            label = "Фамилия",
            leadingIcon = Icons.Outlined.Badge,
        )
        AppTextField(
            value = uiState.nickname,
            onValueChange = { onEvent(AuthEvent.NicknameChanged(it)) },
            label = "Никнейм (необязательно)",
            leadingIcon = Icons.Outlined.Tag,
            placeholder = "Как вас найти в поиске",
        )
        when {
            uiState.nicknameChecking -> {
                Text(
                    text = "Проверяем никнейм…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            uiState.nicknameError != null -> {
                AuthMessageBanner(text = uiState.nicknameError, type = AuthMessageType.Error)
            }
        }
        DatePickerField(
            value = uiState.birthDate,
            onValueChange = { onEvent(AuthEvent.BirthDateChanged(it)) },
            label = "Дата рождения (необязательно)",
        )
        Text(
            text = "Пол",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = uiState.sex == Constants.Sex.MALE,
                onClick = { onEvent(AuthEvent.SexChanged(Constants.Sex.MALE)) },
                label = { Text("Мужской") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipSelectedColor(themedCardBlue()),
                ),
            )
            FilterChip(
                selected = uiState.sex == Constants.Sex.FEMALE,
                onClick = { onEvent(AuthEvent.SexChanged(Constants.Sex.FEMALE)) },
                label = { Text("Женский") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipSelectedColor(themedCardMint()),
                ),
            )
        }
        AppTextField(
            value = uiState.height,
            onValueChange = { onEvent(AuthEvent.HeightChanged(it)) },
            label = "Рост (см)",
            leadingIcon = Icons.Outlined.Straighten,
            keyboardType = KeyboardType.Number,
        )
        AppTextField(
            value = uiState.weight,
            onValueChange = { onEvent(AuthEvent.WeightChanged(it)) },
            label = "Вес (кг)",
            leadingIcon = Icons.Outlined.MonitorWeight,
            keyboardType = KeyboardType.Decimal,
        )
        GoalSelector(
            selectedGoal = uiState.goal,
            onGoalSelected = { onEvent(AuthEvent.GoalChanged(it)) },
        )
    }
}

@Composable
fun RegisterDietaryStep(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
) {
    AuthFormCard(
        sectionTitle = "Питание и ограничения",
        sectionSubtitle = "Учтём в рекомендациях и не будем предлагать неподходящие блюда",
    ) {
        AuthStepHint(
            "Отметьте религиозные, этические ограничения и аллергии — приложение будет их избегать",
        )
        AuthDietaryExclusionGroup(
            title = "Религиозные и традиции",
            options = DietaryExclusionCatalog.religious,
            selected = uiState.dietaryExclusions,
            onToggle = { onEvent(AuthEvent.DietaryExclusionToggled(it)) },
        )
        AuthDietaryExclusionGroup(
            title = "Этические предпочтения",
            options = DietaryExclusionCatalog.ethical,
            selected = uiState.dietaryExclusions,
            onToggle = { onEvent(AuthEvent.DietaryExclusionToggled(it)) },
        )
        AuthDietaryExclusionGroup(
            title = "Непереносимость и аллергии",
            options = DietaryExclusionCatalog.other,
            selected = uiState.dietaryExclusions,
            onToggle = { onEvent(AuthEvent.DietaryExclusionToggled(it)) },
        )
        AppTextField(
            value = uiState.allergiesText,
            onValueChange = { onEvent(AuthEvent.AllergiesTextChanged(it)) },
            label = "Аллергии и реакции (если есть)",
            placeholder = "Орехи, морепродукты, лактоза…",
        )
        AppTextField(
            value = uiState.dietaryNotes,
            onValueChange = { onEvent(AuthEvent.DietaryNotesChanged(it)) },
            label = "Дополнительно",
            placeholder = "Пост по пятницам, без острого и т.д.",
        )
    }
}

@Composable
fun RegisterHealthConnectStep(
    uiState: AuthUiState,
    healthConnectAvailable: Boolean,
    onRequestPermissions: () -> Unit,
    onSkip: () -> Unit,
) {
    AuthFormCard(
        sectionTitle = "Health Connect",
        sectionSubtitle = "Синхронизация шагов и сна с вашего устройства",
    ) {
        if (!healthConnectAvailable) {
            AuthMessageBanner(
                text = "Health Connect недоступен на этом устройстве. Продолжайте регистрацию — подключите позже в профиле.",
                type = AuthMessageType.Info,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Разрешите чтение шагов и сна — данные попадут в сводку и рекомендации. Можно отказаться и настроить позже.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (uiState.healthConnectGranted) {
                    AuthMessageBanner(
                        text = "Доступ выдан — после регистрации данные начнут синхронизироваться.",
                        type = AuthMessageType.Success,
                    )
                }
                AppButton(
                    text = if (uiState.healthConnectGranted) "Доступ уже выдан" else "Разрешить Health Connect",
                    onClick = onRequestPermissions,
                    enabled = !uiState.healthConnectGranted && healthConnectAvailable,
                )
                AppButton(
                    text = "Пропустить",
                    onClick = onSkip,
                    isSecondary = true,
                )
            }
        }
    }
}

@Composable
fun RegisterCredentialsStep(
    uiState: AuthUiState,
    passwordsMatch: Boolean,
    onEvent: (AuthEvent) -> Unit,
) {
    AuthFormCard(
        sectionTitle = "Аккаунт",
        sectionSubtitle = "Email и пароль для входа",
    ) {
        AppTextField(
            value = uiState.email,
            onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
            label = "Email",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
        )
        AppPasswordField(
            value = uiState.password,
            onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
            label = "Пароль",
            leadingIcon = Icons.Outlined.Lock,
        )
        AppPasswordField(
            value = uiState.repeatPassword,
            onValueChange = { onEvent(AuthEvent.RepeatPasswordChanged(it)) },
            label = "Повторите пароль",
            leadingIcon = Icons.Outlined.PersonAddAlt1,
        )
        if (!passwordsMatch) {
            AuthMessageBanner(text = "Пароли не совпадают", type = AuthMessageType.Error)
        }
        Text(
            text = "Минимум 6 символов. На следующем шаге отправим код подтверждения на почту.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun RegisterVerifyStep(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
) {
    AuthFormCard(
        sectionTitle = "Подтверждение почты",
        sectionSubtitle = "Введите 6-значный код из письма",
    ) {
        AppTextField(
            value = uiState.email,
            onValueChange = {},
            label = "Email",
            leadingIcon = Icons.Outlined.Email,
            readOnly = true,
            enabled = false,
        )
        AppTextField(
            value = uiState.verificationCode,
            onValueChange = { onEvent(AuthEvent.VerificationCodeChanged(it)) },
            label = "Код из письма",
            leadingIcon = Icons.Outlined.MarkEmailRead,
            keyboardType = KeyboardType.Number,
            placeholder = "000000",
        )
    }
}

@Composable
fun RegisterWizardNav(
    uiState: AuthUiState,
    passwordsMatch: Boolean,
    onEvent: (AuthEvent) -> Unit,
    showBack: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        uiState.error?.let { err ->
            AuthMessageBanner(text = err, type = AuthMessageType.Error)
        }

        when (uiState.registerStep) {
            RegisterStep.Verify -> {
                AppButton(
                    text = "Далее",
                    onClick = { onEvent(AuthEvent.SubmitRegisterNext) },
                    enabled = uiState.verificationCode.length == 6 && !uiState.isLoading,
                )
                AppButton(
                    text = "Отправить код ещё раз",
                    onClick = { onEvent(AuthEvent.SubmitRegisterSendCode) },
                    enabled = !uiState.isLoading,
                    isSecondary = true,
                )
                AppButton(
                    text = "Изменить email или пароль",
                    onClick = { onEvent(AuthEvent.RegisterEditCredentials) },
                    enabled = !uiState.isLoading,
                    isSecondary = true,
                )
            }
            RegisterStep.Credentials -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (showBack) {
                        AppButton(
                            text = "Назад",
                            onClick = { onEvent(AuthEvent.SubmitRegisterBack) },
                            enabled = !uiState.isLoading,
                            isSecondary = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AppButton(
                        text = if (uiState.isLoading) "Отправляем код…" else "Получить код на почту",
                        onClick = { onEvent(AuthEvent.SubmitRegisterSendCode) },
                        enabled = passwordsMatch && !uiState.isLoading,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (showBack) {
                        AppButton(
                            text = "Назад",
                            onClick = { onEvent(AuthEvent.SubmitRegisterBack) },
                            enabled = !uiState.isLoading,
                            isSecondary = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    val isFinal = uiState.registerStep == RegisterStep.HealthConnect
                    AppButton(
                        text = when {
                            uiState.isLoading && isFinal -> "Регистрируем…"
                            isFinal -> "Завершить регистрацию"
                            else -> "Далее"
                        },
                        onClick = {
                            if (isFinal) {
                                onEvent(AuthEvent.SubmitRegisterConfirm)
                            } else {
                                onEvent(AuthEvent.SubmitRegisterNext)
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
