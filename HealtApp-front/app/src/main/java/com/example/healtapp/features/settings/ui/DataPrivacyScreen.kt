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
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
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
        title = "Данные и конфиденциальность",
        subtitle = "Как устроено хранение информации",
        headerIcon = Icons.Filled.Policy,
        onNavigateBack = onBack,
        scrollable = true,
    ) {
        Text(
            text = "HealthApp хранит введённые тобой показатели (сон, вода, питание, активность и др.) на сервере, чтобы синхронизировать их между устройствами и строить сводки. Учётная запись привязана к e-mail; пароль хранится в виде хэша и не передаётся в открытом виде.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AppCard {
            Text(
                text = "Что можно сделать сейчас:\n" +
                    "• Не вводить в заметки и названия чувствительные медицинские диагнозы, если не хочешь, чтобы они хранились на сервере.\n" +
                    "• Использовать отдельный пароль для приложения, не совпадающий с банковским или почтой.\n" +
                    "• При смене телефона войти в тот же аккаунт — данные подтянутся с бэкенда.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        SectionHeader(
            title = "Удаление аккаунта",
            subtitle = "Безвозвратно: профиль, записи и привязки интеграций",
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Это действие нельзя отменить. Понадобится текущий пароль.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                AppTextField(
                    value = uiState.deletePassword,
                    onValueChange = viewModel::updateDeletePassword,
                    label = "Пароль для подтверждения",
                    isPassword = true,
                )
                uiState.deleteError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
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
