package com.example.healtapp.features.auth.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGuestDemo: () -> Unit = {},
    onRegisterClick: () -> Unit,
    onForgotPassword: () -> Unit = {},
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isAuthorized) {
        if (uiState.isAuthorized) {
            viewModel.consumeAuthorization()
            onLoginSuccess()
        }
    }

    AuthScaffold {
        AuthHeroBanner(
            title = "Добро пожаловать",
            subtitle = "Войдите в аккаунт и продолжайте следить за сном, питанием и активностью",
        )

        AuthFormCard(
            sectionTitle = "Вход",
            sectionSubtitle = "Email и пароль от вашего аккаунта",
        ) {
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

            uiState.error?.let { err ->
                AuthMessageBanner(text = err, type = AuthMessageType.Error)
            }

            AppButton(
                text = if (uiState.isLoading) "Входим…" else "Войти",
                onClick = { viewModel.onEvent(AuthEvent.SubmitLogin) },
                enabled = !uiState.isLoading,
            )

            AppButton(
                text = "Создать аккаунт",
                onClick = onRegisterClick,
                enabled = !uiState.isLoading,
                isSecondary = true,
            )

            AppButton(
                text = "Попробовать демо",
                onClick = { viewModel.enterGuestMode(onGuestDemo) },
                enabled = !uiState.isLoading,
                isSecondary = true,
            )

            TextButton(
                onClick = onForgotPassword,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Забыли пароль?",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = "Данные синхронизируются с сервером после входа",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
