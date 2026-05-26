package com.example.healtapp.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.settings.presentation.DataPrivacyViewModel

@Composable
fun DataPrivacyScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit = {},
) {
    val viewModel: DataPrivacyViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.deleteSuccessEvent) {
        if (uiState.deleteSuccessEvent) {
            onAccountDeleted()
            viewModel.consumeDeleteSuccess()
        }
    }

    AppScreen(
        title = "Конфиденциальность",
        subtitle = "Данные, аккаунт и удаление",
        headerIcon = Icons.Filled.Policy,
        onNavigateBack = onBack,
        scrollable = true,
    ) {
        Text(
            text = "HealthApp хранит введённые вами показатели (сон, вода, питание, активность и др.) на сервере для синхронизации между устройствами и сводок. Пароль хранится в виде хэша и не передаётся в открытом виде.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AppCard {
            Text(
                text = "Что можно сделать сейчас:\n" +
                    "• Не вносить в заметки чувствительные диагнозы, если не хотите хранить их на сервере.\n" +
                    "• Использовать отдельный пароль для приложения.\n" +
                    "• На новом телефоне войти в тот же аккаунт — данные подтянутся с сервера.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        SectionHeader(
            title = "Удаление аккаунта",
            subtitle = "Безвозвратно: профиль, записи и интеграции",
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppMessageBanner(
                    text = UserFacingMessages.IRREVERSIBLE_ACCOUNT_DELETE,
                    type = AppMessageType.Warning,
                    title = "Необратимое действие",
                )
                AppMessageBanner(
                    text = UserFacingMessages.PASSWORD_REQUIRED_TO_DELETE,
                    type = AppMessageType.Info,
                )
                AppTextField(
                    value = uiState.deletePassword,
                    onValueChange = viewModel::updateDeletePassword,
                    label = "Текущий пароль",
                    isPassword = true,
                )
                uiState.deleteError?.let { err ->
                    AppMessageBanner(
                        text = err,
                        type = AppMessageType.Error,
                        title = "Не удалось удалить",
                    )
                }
                AppButton(
                    text = if (uiState.isDeleting) "Удаляем…" else "Удалить аккаунт навсегда",
                    enabled = !uiState.isDeleting,
                    isSecondary = true,
                    onClick = viewModel::deleteAccount,
                )
            }
        }
    }
}
