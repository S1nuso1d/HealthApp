package com.example.healtapp.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.common.UserFacingMessages
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.FeatureGlassCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.GradientOutlinedField
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.features.settings.presentation.DataPrivacyViewModel
import com.example.healtapp.features.settings.ui.components.SettingsCapabilityPanel
import com.example.healtapp.features.settings.ui.components.SettingsInfoText

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

    FeatureScreenShell(
        title = "Конфиденциальность",
        subtitle = "Данные, аккаунт и удаление",
        icon = Icons.Filled.Policy,
        onBack = onBack,
        heroFooter = {
            Spacer(Modifier.height(10.dp))
            FeatureHeroChip(
                label = "Защита аккаунта",
                modifier = Modifier.padding(start = 12.dp),
            )
        },
    ) {
        SettingsCapabilityPanel(
            title = "Ваши данные под контролем",
            subtitle = "HealthApp хранит записи на сервере только для синхронизации между устройствами.",
            items = listOf(
                Triple(Icons.Filled.Shield, "Приватность", "Без лишних полей"),
                Triple(Icons.Filled.Lock, "Пароль", "Отдельный доступ"),
                Triple(Icons.Filled.Policy, "Аккаунт", "Удаление навсегда"),
            ),
        )

        FeatureSectionTitle(
            title = "О данных",
            subtitle = "Как хранится информация в HealthApp",
        )

        FeatureGlassCard {
            SettingsInfoText(
                text = "Что можно сделать сейчас:\n" +
                    "• Не вносить в заметки чувствительные диагнозы, если не хотите хранить их на сервере.\n" +
                    "• Использовать отдельный пароль для приложения.\n" +
                    "• На новом телефоне войти в тот же аккаунт — данные подтянутся с сервера.",
            )
        }

        FeatureSectionTitle(
            title = "Удаление аккаунта",
            subtitle = "Безвозвратно: профиль, записи и интеграции",
        )

        FeatureGlassCard {
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
                GradientFormPanel {
                    Text(
                        text = "Подтверждение паролем",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    GradientOutlinedField(
                        value = uiState.deletePassword,
                        onValueChange = viewModel::updateDeletePassword,
                        label = "Текущий пароль",
                        isPassword = true,
                    )
                }
                uiState.deleteError?.let { err ->
                    FeatureInlineNotice(text = err, isError = true)
                }
                AppButton(
                    text = if (uiState.isDeleting) "Удаляем…" else "Удалить аккаунт навсегда",
                    enabled = !uiState.isDeleting,
                    isSecondary = true,
                    onClick = viewModel::deleteAccount,
                )
            }
        }

        Text(
            text = "Удаление аккаунта отменить нельзя — все записи и интеграции будут стёрты с сервера.",
            style = MaterialTheme.typography.bodySmall,
            color = contentSecondaryColor(),
        )
    }
}
