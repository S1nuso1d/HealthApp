package com.example.healtapp.features.miband.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.miband.MiBandBleForegroundService
import com.example.healtapp.miband.MiBandConnectionPhase
import com.example.healtapp.miband.MiBandRepository
import com.example.healtapp.miband.MiBandScannedDevice
import com.example.healtapp.miband.MiBandUiDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.example.healtapp.miband.MiBandLiveStats
import javax.inject.Inject

data class MiBandConnectionUiState(
    val device: MiBandUiDevice? = null,
    val scanned: List<MiBandScannedDevice> = emptyList(),
    val draftAuthKey: String = "",
    val selectedAddress: String? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val permissionsOk: Boolean = true,
)

@HiltViewModel
class MiBandConnectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MiBandRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MiBandConnectionUiState())
    val uiState: StateFlow<MiBandConnectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.deviceState.collect { dev ->
                _uiState.update { s ->
                    s.copy(
                        device = dev,
                        draftAuthKey = if (s.draftAuthKey.isBlank()) dev?.authKeyHex.orEmpty() else s.draftAuthKey,
                        selectedAddress = s.selectedAddress ?: dev?.address,
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.scannedDevices.collect { list ->
                _uiState.update { it.copy(scanned = list) }
            }
        }
        refreshPermissions()
    }

    fun refreshPermissions() {
        val ok = requiredBlePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        _uiState.update { it.copy(permissionsOk = ok) }
    }

    fun onAuthKeyChange(v: String) {
        _uiState.update { it.copy(draftAuthKey = v, error = null) }
    }

    fun selectDevice(address: String) {
        _uiState.update { it.copy(selectedAddress = address, error = null) }
    }

    fun startScan() {
        if (!repository.isBluetoothReady()) {
            _uiState.update { it.copy(error = "Включите Bluetooth на телефоне") }
            return
        }
        if (!_uiState.value.permissionsOk) {
            _uiState.update { it.copy(error = "Нужны разрешения Bluetooth и (на части устройств) геолокация") }
            return
        }
        MiBandBleForegroundService.start(context)
        repository.startScan()
    }

    fun stopScan() = repository.stopScan()

    fun connectSelected() {
        val s = _uiState.value
        val address = s.selectedAddress
        val name = s.scanned.find { it.address == address }?.name ?: s.device?.name ?: "Mi Band"
        val key = s.draftAuthKey.trim()
        if (address.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Выберите браслет из списка сканирования") }
            return
        }
        if (key.length < 32) {
            _uiState.update {
                it.copy(error = "Вставьте auth key из Mi Fitness (32 hex-символа, можно с префиксом 0x)")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching {
                repository.saveAndConnect(address, name, key)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isBusy = false, error = e.message ?: "Не удалось подключиться")
                }
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun reconnectSaved() {
        val dev = _uiState.value.device ?: return
        val key = _uiState.value.draftAuthKey.trim().ifBlank { dev.authKeyHex }
        if (key.length < 32) {
            _uiState.update { it.copy(error = "Укажите auth key") }
            return
        }
        MiBandBleForegroundService.start(context)
        repository.connectSaved(key, dev.address)
    }

    fun disconnect() {
        MiBandBleForegroundService.stop(context)
        repository.disconnect()
    }

    fun syncNow() {
        val address = _uiState.value.device?.address ?: _uiState.value.selectedAddress
        if (address.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Сначала подключите браслет") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            repository.syncToServer()
            val stats: MiBandLiveStats? = withTimeoutOrNull(12_000) {
                repository.liveStats.filterNotNull().first()
            }
            if (stats != null && stats.steps > 0) {
                repository.pushLiveStatsToBackend(stats, address)
                    .onSuccess {
                        _uiState.update { it.copy(message = "На сервер: ${stats.steps} шагов, пульс ${stats.heartRate}") }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(error = e.message ?: "Ошибка импорта на сервер") }
                    }
            } else {
                _uiState.update {
                    it.copy(
                        message = "Нет ответа от браслета за 12 с. Проверьте auth key, Mi Fitness и близость BLE.",
                    )
                }
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun clearPairing() {
        viewModelScope.launch {
            repository.clearSaved()
            _uiState.update {
                it.copy(message = "Сохранённое устройство удалено", draftAuthKey = "", selectedAddress = null)
            }
        }
    }

    companion object {
        fun requiredBlePermissions(): Array<String> {
            val perms = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms += Manifest.permission.BLUETOOTH_SCAN
                perms += Manifest.permission.BLUETOOTH_CONNECT
            } else {
                perms += Manifest.permission.ACCESS_FINE_LOCATION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // optional for foreground service notification
            }
            return perms.toTypedArray()
        }
    }
}
