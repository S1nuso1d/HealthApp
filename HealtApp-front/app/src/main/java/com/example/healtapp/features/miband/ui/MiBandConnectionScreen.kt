package com.example.healtapp.features.miband.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healtapp.core.ui.components.AppButton
import com.example.healtapp.core.ui.components.FeatureCardDivider
import com.example.healtapp.core.ui.components.FeatureGlassCard
import com.example.healtapp.core.ui.components.FeatureHeroChip
import com.example.healtapp.core.ui.components.FeatureInlineNotice
import com.example.healtapp.core.ui.components.FeatureScreenShell
import com.example.healtapp.core.ui.components.FeatureSectionTitle
import com.example.healtapp.core.ui.components.GradientFormPanel
import com.example.healtapp.core.ui.components.GradientOutlinedField
import com.example.healtapp.core.ui.theme.contentSecondaryColor
import com.example.healtapp.features.miband.presentation.MiBandConnectionViewModel
import com.example.healtapp.features.settings.ui.components.SettingsCapabilityPanel
import com.example.healtapp.features.settings.ui.components.SettingsInfoText
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

    val phase = uiState.device?.phase ?: MiBandConnectionPhase.Idle
    val statusLabel = phaseLabel(phase)

    FeatureScreenShell(
        title = "Умные часы и трекеры",
        subtitle = "Прямое подключение по Bluetooth Low Energy",
        icon = Icons.Filled.Watch,
        onBack = onBack,
        heroFooter = {
            Spacer(Modifier.height(10.dp))
            FeatureHeroChip(
                label = statusLabel,
                modifier = Modifier.padding(start = 12.dp),
            )
        },
    ) {
        uiState.error?.let { FeatureInlineNotice(text = it, isError = true) }
        uiState.message?.let { FeatureInlineNotice(text = it) }

        SettingsCapabilityPanel(
            title = "Mi Band 8 по BLE",
            subtitle = "Прямое подключение без Health Connect — шаги и пульс с браслета Xiaomi.",
            items = listOf(
                Triple(Icons.Filled.VpnKey, "Auth key", "Из Mi Fitness"),
                Triple(Icons.Filled.Bluetooth, "BLE", "Сканирование"),
                Triple(Icons.Filled.Sync, "Синхронизация", "На сервер"),
            ),
        )

        if (!uiState.permissionsOk) {
            FeatureGlassCard {
                SettingsInfoText(
                    text = "Для сканирования BLE нужны разрешения Bluetooth" +
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) " и геолокации" else "",
                )
                AppButton(
                    text = "Выдать разрешения",
                    onClick = {
                        permissionLauncher.launch(MiBandConnectionViewModel.requiredBlePermissions())
                    },
                )
            }
        }

        FeatureSectionTitle(
            title = "Auth key из Mi Fitness",
            subtitle = "Сначала привяжите браслет в Mi Fitness",
        )

        GradientFormPanel {
            GradientOutlinedField(
                value = uiState.draftAuthKey,
                onValueChange = viewModel::onAuthKeyChange,
                label = "auth_key (32 hex)",
                isPassword = true,
            )
        }

        FeatureGlassCard {
            SettingsInfoText(
                text = "На телефоне с root/adb: в базе com.xiaomi.wearable поле auth_key в JSON device. " +
                    "Без ключа Mi Band 8 не примет стороннее приложение — это ограничение Xiaomi, не HealthApp.",
            )
        }

        FeatureSectionTitle(title = "Поиск и подключение")

        FeatureGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Статус: $statusLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                uiState.device?.statusMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentSecondaryColor(),
                    )
                }
                AppButton(
                    text = if (phase == MiBandConnectionPhase.Scanning) {
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
            FeatureSectionTitle(title = "Найденные устройства")
            FeatureGlassCard {
                Column {
                    uiState.scanned.forEachIndexed { index, dev ->
                        if (index > 0) FeatureCardDivider()
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
                                    color = contentSecondaryColor(),
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
            FeatureSectionTitle(title = "Сохранённое устройство")
            FeatureGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = saved.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = saved.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentSecondaryColor(),
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
                            fontWeight = FontWeight.SemiBold,
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

        FeatureSectionTitle(title = "Другие устройства", subtitle = "Через Health Connect")

        FeatureGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Garmin, Apple Watch, Samsung, Huawei",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                SettingsInfoText(
                    text = "Эти часы синхронизируются автоматически через Google Health Connect (Android) или Apple Health (iOS). Дополнительное сопряжение по Bluetooth в HealthApp не требуется.",
                )
            }
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
