package com.example.healtapp.features.settings.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.FeatureCollapsibleCard
import com.example.healtapp.core.ui.components.FeatureGlassCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.GradientOutlinedField
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.data.healthconnect.HealthConnectReader
import com.example.healtapp.features.settings.presentation.IntegrationsViewModel
import com.example.healtapp.features.settings.ui.components.SettingsCapabilityPanel
import com.example.healtapp.features.settings.ui.components.SettingsInfoText

@Composable
fun IntegrationsScreen(
    onBack: () -> Unit = {},
    onOpenMiBandBle: () -> Unit = {},
    registrationMode: Boolean = false,
    onContinueToApp: (() -> Unit)? = null,
) {
    val viewModel: IntegrationsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showFatSecretOAuth by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHealthConnectAvailability()
                viewModel.refreshHealthConnectPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hcPermissionContract = remember { PermissionController.createRequestPermissionResultContract() }
    val hcPermissionLauncher = rememberLauncherForActivityResult(hcPermissionContract) {
        viewModel.refreshHealthConnectPermissions()
    }

    val connectedCount = listOf(
        uiState.healthConnectPermissionsGranted,
        uiState.fatSecretToken.isNotBlank(),
    ).count { it }

    FeatureScreenShell(
        title = if (registrationMode) "Подключи сервисы" else "Интеграции",
        subtitle = if (registrationMode) {
            "Health Connect и FatSecret — можно настроить сейчас или позже"
        } else {
            "Health Connect, FatSecret и носимые устройства"
        },
        icon = Icons.Filled.Sync,
        onBack = onBack,
        heroFooter = {
            Spacer(Modifier.height(10.dp))
            FeatureHeroChip(
                label = if (connectedCount > 0) "Подключено: $connectedCount" else "Настройте сервисы",
                modifier = Modifier.padding(start = 12.dp),
            )
        },
    ) {
        uiState.error?.let { FeatureInlineNotice(text = it, isError = true) }
        uiState.message?.let { FeatureInlineNotice(text = it) }

        SettingsCapabilityPanel(
            title = if (registrationMode) "С чего начать" else "Синхронизация данных",
            subtitle = "Подключите внешние сервисы — шаги, сон и питание подтянутся в HealthApp.",
            items = listOf(
                Triple(Icons.Filled.Sync, "Health Connect", "Сон и шаги"),
                Triple(Icons.Filled.Watch, "Mi Band", "Прямой BLE"),
                Triple(Icons.Filled.Restaurant, "FatSecret", "Поиск еды"),
            ),
        )

        FeatureSectionTitle(
            title = "Подключённые сервисы",
            subtitle = "Разверните блок и настройте доступ",
        )

        FeatureCollapsibleCard(
            title = "Health Connect",
            subtitle = if (registrationMode) {
                "Сон, шаги, тренировки"
            } else {
                "Сон, шаги, пульс, SpO₂ и др."
            },
            initiallyExpanded = registrationMode,
        ) {
            if (!uiState.healthConnectSupported) {
                SettingsInfoText(
                    text = "Health Connect сейчас недоступен: для Android 13 и ниже установи приложение Health Connect из Play Маркет; на Android 14+ модуль встроен в систему.",
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (uiState.healthConnectNeedsProviderUpdate) {
                        SettingsInfoText(
                            text = "Нужно обновить модуль Health Connect (или приложение из маркета на старых версиях Android). После обновления вернись сюда.",
                        )
                        AppButton(
                            text = "Открыть страницу в Play Маркете",
                            enabled = !uiState.isBusy,
                            onClick = {
                                val url =
                                    "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                )
                            },
                        )
                    }

                    if (uiState.healthConnectCanRequestPermissions) {
                        SettingsInfoText(
                            text = if (uiState.healthConnectPermissionsGranted) {
                                "Доступ к сну и шагам выдан — можно синхронизировать."
                            } else {
                                "Нужны разрешения на чтение сна и шагов. Нажми кнопку — откроется экран Health Connect."
                            },
                        )
                        if (uiState.healthConnectPermissionsGranted) {
                            Text(
                                text = "Для Mi Band 8 без Health Connect используйте раздел «Mi Band 8 — прямой BLE» ниже.",
                                style = MaterialTheme.typography.bodySmall,
                                color = contentSecondaryColor(),
                            )
                        }
                        if (!uiState.healthConnectPermissionsGranted) {
                            AppButton(
                                text = "Запросить разрешения",
                                enabled = !uiState.isBusy,
                                onClick = {
                                    runCatching {
                                        hcPermissionLauncher.launch(HealthConnectReader.requiredReadPermissions())
                                    }.onFailure { e ->
                                        viewModel.reportHealthConnectPermissionError(
                                            e.message ?: "Не удалось открыть запрос разрешений",
                                        )
                                    }
                                },
                            )
                        } else {
                            AppButton(
                                text = if (uiState.isBusy) "Синхронизация…" else "Импортировать в HealthApp",
                                enabled = !uiState.isBusy,
                                onClick = { viewModel.syncFromHealthConnect(14) },
                            )
                        }
                    }
                }
            }
        }

        FeatureCollapsibleCard(
            title = "Mi Band 8 — прямой BLE",
            subtitle = "Шаги и пульс без Health Connect",
            initiallyExpanded = false,
        ) {
            SettingsInfoText(
                text = "Mi Band 8 использует закрытый протокол fe95. HealthApp подключается напрямую по BLE, как Gadgetbridge — с ключом, полученным после привязки в Mi Fitness.",
            )
            AppButton(
                text = "Настроить Mi Band BLE",
                enabled = !uiState.isBusy,
                onClick = onOpenMiBandBle,
            )
        }

        FeatureCollapsibleCard(
            title = "FatSecret",
            subtitle = if (registrationMode) {
                "Поиск через сервер (.env)"
            } else {
                "Поиск, штрихкод, OAuth"
            },
            initiallyExpanded = registrationMode,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientFormPanel {
                    Text(
                        text = "Поиск продуктов",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    GradientOutlinedField(
                        value = uiState.fatSecretSearchQuery,
                        onValueChange = viewModel::updateFatSecretSearchQuery,
                        label = "Например, овсянка",
                    )
                }
                AppButton(
                    text = if (uiState.isBusy) "Запрос…" else "Поиск через API",
                    enabled = !uiState.isBusy,
                    onClick = viewModel::searchFatSecretFoods,
                )
                if (!registrationMode) {
                    AppButton(
                        text = "Отвязать FatSecret",
                        enabled = !uiState.isBusy,
                        isSecondary = true,
                        onClick = viewModel::unlinkFatSecret,
                    )
                }
                AppButton(
                    text = if (showFatSecretOAuth) {
                        "Скрыть привязку OAuth к дневнику"
                    } else {
                        "Дополнительно: OAuth токены дневника FatSecret"
                    },
                    enabled = !uiState.isBusy,
                    isSecondary = true,
                    onClick = { showFatSecretOAuth = !showFatSecretOAuth },
                )
                if (showFatSecretOAuth) {
                    GradientFormPanel {
                        Text(
                            text = "OAuth токены",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Resource owner token и secret из кабинета разработчика FatSecret.",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentSecondaryColor(),
                        )
                        GradientOutlinedField(
                            value = uiState.fatSecretToken,
                            onValueChange = viewModel::updateFatSecretToken,
                            label = "Access token (OAuth key)",
                            isPassword = true,
                        )
                        GradientOutlinedField(
                            value = uiState.fatSecretSecret,
                            onValueChange = viewModel::updateFatSecretSecret,
                            label = "Access secret (OAuth secret)",
                            isPassword = true,
                        )
                    }
                    AppButton(
                        text = if (uiState.isBusy) "Подождите…" else "Привязать FatSecret",
                        enabled = !uiState.isBusy,
                        onClick = viewModel::linkFatSecret,
                    )
                }
            }
        }

        uiState.fatSecretPreview?.let { raw ->
            FeatureGlassCard {
                Text(
                    text = "Ответ API",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = raw,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentSecondaryColor(),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
        }

        if (registrationMode && onContinueToApp != null) {
            AppButton(
                text = "Продолжить в приложение",
                enabled = !uiState.isBusy,
                onClick = onContinueToApp,
            )
            AppButton(
                text = "Пропустить этот шаг",
                enabled = !uiState.isBusy,
                isSecondary = true,
                onClick = onContinueToApp,
            )
        }
    }
}
