package com.example.healtapp.features.miband.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.AppCard
import com.example.healtapp.core.ui.components.AppMessageBanner
import com.example.healtapp.core.ui.components.AppMessageType
import com.example.healtapp.core.ui.components.AppScreen
import com.example.healtapp.core.ui.components.AppTextField
import com.example.healtapp.core.ui.components.SectionHeader
import com.example.healtapp.features.miband.presentation.MiBandConnectionViewModel
import com.example.healtapp.miband.MiBandConnectionPhase

@Composable
fun MiBandConnectionScreen(
    onBack: () -> Unit = {},
) {
    val viewModel: MiBandConnectionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshPermissions() }

    AppScreen(
        title = "Mi Band 8 (BLE)",
        subtitle = "Прямое подключение по Bluetooth Low Energy",
        headerIcon = Icons.Filled.Watch,
        onNavigateBack = onBack,
        scrollable = true,
    ) {
        uiState.error?.let {
            AppMessageBanner(text = it, type = AppMessageType.Error)
        }
        uiState.message?.let {
            AppMessageBanner(text = it, type = AppMessageType.Info)
        }

        if (!uiState.permissionsOk) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Для сканирования BLE нужны разрешения Bluetooth" +
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) " и геолокации" else "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AppButton(
                        text = "Выдать разрешения",
                        onClick = {
                            permissionLauncher.launch(MiBandConnectionViewModel.requiredBlePermissions())
                        },
                    )
                }
            }
        }

        SectionHeader(
            title = "Auth key из Mi Fitness",
            subtitle = "Сначала привяжите браслет в Mi Fitness, затем извлеките ключ (см. подсказку ниже)",
        )
        AppTextField(
            value = uiState.draftAuthKey,
            onValueChange = viewModel::onAuthKeyChange,
            label = "auth_key (32 hex)",
            isPassword = true,
            modifier = Modifier.fillMaxWidth(),
        )

        AppCard {
            Text(
                text = "На телефоне с root/adb: в базе com.xiaomi.wearable поле auth_key в JSON device. " +
                    "Без ключа Mi Band 8 не примет стороннее приложение — это ограничение Xiaomi, не HealthApp.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeader(title = "Поиск и подключение")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Статус: ${phaseLabel(uiState.device?.phase ?: MiBandConnectionPhase.Idle)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                uiState.device?.statusMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppButton(
                    text = if (uiState.device?.phase == MiBandConnectionPhase.Scanning) {
                        "Сканирование…"
                    } else {
                        "Сканировать BLE"
                    },
                    enabled = uiState.permissionsOk && !uiState.isBusy,
                    onClick = viewModel::startScan,
                )
                AppButton(
                    text = "Остановить скан",
                    isSecondary = true,
                    enabled = !uiState.isBusy,
                    onClick = viewModel::stopScan,
                )
            }
        }

        if (uiState.scanned.isNotEmpty()) {
            SectionHeader(title = "Найденные устройства")
            AppCard {
                Column {
                    uiState.scanned.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectDevice(dev.address) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = uiState.selectedAddress == dev.address,
                                onClick = { viewModel.selectDevice(dev.address) },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = dev.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${dev.address} · ${dev.rssi} dBm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            AppButton(
                text = if (uiState.isBusy) "Подключение…" else "Подключить выбранный",
                enabled = !uiState.isBusy && uiState.permissionsOk,
                onClick = viewModel::connectSelected,
            )
        }

        uiState.device?.let { saved ->
            SectionHeader(title = "Сохранённое устройство")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = saved.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = saved.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (saved.lastSteps > 0) {
                        Text(
                            text = "Последняя синхронизация: ${saved.lastSteps} шагов, пульс ${saved.lastHeartRate}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    saved.liveSteps?.let {
                        Text(
                            text = "Сейчас с браслета: $it шагов, пульс ${saved.liveHeartRate}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            AppButton(
                text = "Переподключить",
                enabled = !uiState.isBusy,
                isSecondary = true,
                onClick = viewModel::reconnectSaved,
            )
            AppButton(
                text = if (uiState.isBusy) "Синхронизация…" else "Синхронизировать → сервер",
                enabled = !uiState.isBusy && saved.phase == MiBandConnectionPhase.Ready,
                onClick = viewModel::syncNow,
            )
            AppButton(
                text = "Отключить BLE",
                isSecondary = true,
                enabled = !uiState.isBusy,
                onClick = viewModel::disconnect,
            )
            AppButton(
                text = "Забыть устройство",
                isSecondary = true,
                enabled = !uiState.isBusy,
                onClick = viewModel::clearPairing,
            )
        }
    }
}

private fun phaseLabel(phase: MiBandConnectionPhase): String = when (phase) {
    MiBandConnectionPhase.Idle -> "Не подключено"
    MiBandConnectionPhase.Scanning -> "Сканирование"
    MiBandConnectionPhase.Connecting -> "Подключение"
    MiBandConnectionPhase.Discovering -> "GATT"
    MiBandConnectionPhase.Authenticating -> "Аутентификация"
    MiBandConnectionPhase.Ready -> "Готово"
    MiBandConnectionPhase.Syncing -> "Синхронизация"
    MiBandConnectionPhase.Error -> "Ошибка"
}
