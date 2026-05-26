package com.example.healtapp.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.settings.presentation.ServerConnectionViewModel

@Composable
fun ServerConnectionScreen(
    onBack: () -> Unit = {},
) {
    val viewModel: ServerConnectionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppScreen(
        title = "Подключение к серверу",
        subtitle = "Дома — Wi‑Fi; вне дома — HTTPS-туннель на ПК",
        headerIcon = Icons.Filled.Cloud,
        onNavigateBack = onBack,
        scrollable = true,
    ) {
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Сейчас используется",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.activeUrl,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "По умолчанию при сборке: ${uiState.buildDefaultUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionHeader(title = "Адрес API")
        AppTextField(
            value = uiState.draftUrl,
            onValueChange = viewModel::onUrlChange,
            label = "BASE URL",
            placeholder = "https://xxxx.trycloudflare.com/",
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "На улице: на ПК запустите backend и туннель (Cloudflare/ngrok), " +
                "вставьте выданный https://… адрес. Этот экран доступен и до входа в аккаунт.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeader(title = "Быстрые пресеты")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    text = "Домашняя Wi‑Fi (из сборки)",
                    onClick = viewModel::resetToBuildDefault,
                    isSecondary = true,
                    enabled = !uiState.isSaving,
                )
                AppButton(
                    text = "Эмулятор Android Studio",
                    onClick = { viewModel.applyPreset("http://10.0.2.2:8001/") },
                    isSecondary = true,
                    enabled = !uiState.isSaving,
                )
            }
        }

        uiState.message?.let { msg ->
            AppMessageBanner(
                text = msg,
                type = if (uiState.messageIsError) AppMessageType.Error else AppMessageType.Success,
            )
        }

        AppButton(
            text = if (uiState.isTesting) "Проверка…" else "Проверить связь",
            onClick = viewModel::testConnection,
            isSecondary = true,
            enabled = !uiState.isTesting && !uiState.isSaving && uiState.draftUrl.isNotBlank(),
        )
        AppButton(
            text = if (uiState.isSaving) "Сохранение…" else "Сохранить и применить",
            onClick = viewModel::save,
            enabled = !uiState.isSaving && !uiState.isTesting,
        )
    }
}
