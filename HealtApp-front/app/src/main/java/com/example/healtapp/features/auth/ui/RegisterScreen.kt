package com.example.healtapp.features.auth.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.features.auth.presentation.AuthEvent
import com.example.healtapp.features.auth.presentation.AuthViewModel
import com.example.healtapp.features.auth.ui.components.AuthFormCard
import com.example.healtapp.features.auth.ui.components.AuthHeroBanner
import com.example.healtapp.features.auth.ui.components.AuthMessageBanner
import com.example.healtapp.features.auth.ui.components.AuthMessageType
import com.example.healtapp.features.auth.ui.components.AuthScaffold

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val passwordsMatch = uiState.password == uiState.repeatPassword || uiState.repeatPassword.isBlank()

    LaunchedEffect(uiState.isAuthorized) {
        if (uiState.isAuthorized) {
            viewModel.consumeAuthorization()
            onRegisterSuccess()
        }
    }

    val awaiting = uiState.awaitingEmailVerification

    AuthScaffold(onBack = onBackClick) {
        AuthHeroBanner(
            title = if (awaiting) "Подтверждение почты" else "Регистрация",
            subtitle = if (awaiting) {
                "Введите 6-значный код из письма"
            } else {
                "Создайте аккаунт и получайте персональные рекомендации"
            },
        )

        AuthFormCard(
            sectionTitle = if (awaiting) "Код подтверждения" else "Данные аккаунта",
            sectionSubtitle = if (awaiting) {
                "Письмо отправлено на ${uiState.email}"
            } else {
                "Минимум 6 символов в пароле"
            },
        ) {
            if (!awaiting) {
                AppTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEvent(AuthEvent.EmailChanged(it)) },
                    label = "Email",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                )
                AppTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onEvent(AuthEvent.PasswordChanged(it)) },
                    label = "Пароль",
                    isPassword = true,
                    leadingIcon = Icons.Outlined.Lock,
                )
                AppTextField(
                    value = uiState.repeatPassword,
                    onValueChange = { viewModel.onEvent(AuthEvent.RepeatPasswordChanged(it)) },
                    label = "Повторите пароль",
                    isPassword = true,
                    leadingIcon = Icons.Outlined.PersonAddAlt1,
                )
                if (!passwordsMatch) {
                    AuthMessageBanner(
                        text = "Пароли не совпадают",
                        type = AuthMessageType.Error,
                    )
                }
            } else {
                AppTextField(
                    value = uiState.email,
                    onValueChange = {},
                    label = "Email",
                    leadingIcon = Icons.Outlined.Email,
                    readOnly = true,
                    enabled = false,
                )
                uiState.infoMessage?.let { msg ->
                    AuthMessageBanner(text = msg, type = AuthMessageType.Info)
                }
                AppTextField(
                    value = uiState.verificationCode,
                    onValueChange = { viewModel.onEvent(AuthEvent.VerificationCodeChanged(it)) },
                    label = "Код из письма",
                    leadingIcon = Icons.Outlined.MarkEmailRead,
                    keyboardType = KeyboardType.Number,
                    placeholder = "000000",
                )
                Text(
                    text = "Нет письма? Проверьте папку «Спам» или запросите код повторно.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            uiState.error?.let { err ->
                AuthMessageBanner(text = err, type = AuthMessageType.Error)
            }

            if (!awaiting) {
                AppButton(
                    text = if (uiState.isLoading) "Отправляем код…" else "Получить код на почту",
                    onClick = { viewModel.onEvent(AuthEvent.SubmitRegisterSendCode) },
                    enabled = passwordsMatch && !uiState.isLoading,
                )
            } else {
                AppButton(
                    text = if (uiState.isLoading) "Регистрируем…" else "Подтвердить и войти",
                    onClick = { viewModel.onEvent(AuthEvent.SubmitRegisterConfirm) },
                    enabled = uiState.verificationCode.length == 6 && !uiState.isLoading,
                )
                AppButton(
                    text = "Отправить код ещё раз",
                    onClick = { viewModel.onEvent(AuthEvent.SubmitRegisterSendCode) },
                    enabled = !uiState.isLoading,
                    isSecondary = true,
                )
                AppButton(
                    text = "Изменить email или пароль",
                    onClick = { viewModel.onEvent(AuthEvent.RegisterEditCredentials) },
                    enabled = !uiState.isLoading,
                    isSecondary = true,
                )
            }
        }
    }
}
