package com.example.healtapp.features.auth.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.features.auth.presentation.ForgotPasswordViewModel
import com.example.healtapp.features.auth.ui.components.AuthFormCard
import com.example.healtapp.features.auth.ui.components.AuthHeroBanner
import com.example.healtapp.features.auth.ui.components.AuthMessageBanner
import com.example.healtapp.features.auth.ui.components.AuthMessageType
import com.example.healtapp.features.auth.ui.components.AuthScaffold

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
) {
    val viewModel: ForgotPasswordViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthScaffold(onBack = onBack) {
        AuthHeroBanner(
            title = "Восстановление доступа",
            subtitle = "Отправим временный пароль на почту. После входа смените его в профиле.",
        )

        AuthFormCard(
            sectionTitle = "Сброс пароля",
            sectionSubtitle = "Укажите email, с которым регистрировались",
        ) {
            AppTextField(
                value = uiState.email,
                onValueChange = viewModel::updateEmail,
                label = "Email",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
            )

            uiState.infoMessage?.let { msg ->
                AuthMessageBanner(text = msg, type = AuthMessageType.Success)
            }
            uiState.error?.let { err ->
                AuthMessageBanner(text = err, type = AuthMessageType.Error)
            }

            AppButton(
                text = if (uiState.isLoading) "Отправляем…" else "Отправить пароль на почту",
                onClick = viewModel::submit,
                enabled = !uiState.isLoading,
            )

            AppButton(
                text = "Назад ко входу",
                onClick = onBack,
                enabled = !uiState.isLoading,
                isSecondary = true,
            )
        }

        Text(
            text = "Временный пароль — 8 цифр. Используйте его для входа, затем задайте новый в разделе «Профиль → Пароль».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
