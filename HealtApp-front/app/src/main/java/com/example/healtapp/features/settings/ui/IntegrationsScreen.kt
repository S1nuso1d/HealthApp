package com.example.healtapp.features.settings.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.CollapsibleAppCard
import com.example.healtapp.data.healthconnect.HealthConnectReader
import com.example.healtapp.features.settings.presentation.IntegrationsViewModel

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

    AppScreen(
        title = if (registrationMode) "Подключи сервисы" else "Интеграции",
        subtitle = if (registrationMode) {
            "Health Connect (сон и шаги) и FatSecret — можно настроить сейчас или позже в профиле"
        } else {
            "Health Connect и FatSecret"
        },
        headerIcon = Icons.Filled.Sync,
        onNavigateBack = onBack,
        scrollable = true,
    ) {
        uiState.error?.let {
            AppMessageBanner(text = it, type = AppMessageType.Error)
        }
        uiState.message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        CollapsibleAppCard(
            title = "Health Connect",
            subtitle = if (registrationMode) {
                "Сон, шаги, тренировки"
            } else {
                "Сон, шаги, пульс, SpO₂ и др."
            },
            initiallyExpanded = registrationMode,
        ) {
        if (!uiState.healthConnectSupported) {
            Text(
                text = "Health Connect сейчас недоступен: для Android 13 и ниже установи приложение Health Connect из Play Маркет; на Android 14+ модуль встроен в систему.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (uiState.healthConnectNeedsProviderUpdate) {
                        Text(
                            text = "Нужно обновить модуль Health Connect (или приложение из маркета на старых версиях Android). После обновления вернись сюда.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
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
                        Text(
                            text = if (uiState.healthConnectPermissionsGranted) {
                                "Доступ к сну и шагам выдан — можно синхронизировать."
                            } else {
                                "Нужны разрешения на чтение сна и шагов. Нажми кнопку — откроется экран Health Connect."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (uiState.healthConnectPermissionsGranted) {
                            Text(
                                text = "Для Mi Band 8 без Health Connect используйте раздел «Mi Band 8 — прямой BLE» ниже.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        CollapsibleAppCard(
            title = "Mi Band 8 — прямой BLE",
            subtitle = "Шаги и пульс без Health Connect",
            initiallyExpanded = false,
        ) {
            Text(
                text = "Mi Band 8 использует закрытый протокол fe95. HealthApp подключается напрямую по BLE, как Gadgetbridge — с ключом, полученным после привязки в Mi Fitness.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AppButton(
                text = "Настроить Mi Band BLE",
                enabled = !uiState.isBusy,
                onClick = onOpenMiBandBle,
            )
        }

        CollapsibleAppCard(
            title = "FatSecret",
            subtitle = if (registrationMode) {
                "Поиск через сервер (.env)"
            } else {
                "Поиск, штрихкод, OAuth"
            },
            initiallyExpanded = registrationMode,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = uiState.fatSecretSearchQuery,
                    onValueChange = viewModel::updateFatSecretSearchQuery,
                    label = "Поиск продукта (например, овсянка)",
                )
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
                    Text(
                        text = "Resource owner token и secret из кабинета разработчика FatSecret — сервер сохранит их для запросов к API дневника.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AppTextField(
                        value = uiState.fatSecretToken,
                        onValueChange = viewModel::updateFatSecretToken,
                        label = "Access token (OAuth key)",
                        isPassword = true,
                    )
                    AppTextField(
                        value = uiState.fatSecretSecret,
                        onValueChange = viewModel::updateFatSecretSecret,
                        label = "Access secret (OAuth secret)",
                        isPassword = true,
                    )
                    AppButton(
                        text = if (uiState.isBusy) "Подождите…" else "Привязать FatSecret",
                        enabled = !uiState.isBusy,
                        onClick = viewModel::linkFatSecret,
                    )
                }
            }
        }

        uiState.fatSecretPreview?.let { raw ->
            AppCard {
                Text(
                    text = "Ответ API",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = raw,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
        }

        if (registrationMode && onContinueToApp != null) {
            Spacer(modifier = Modifier.height(8.dp))
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
