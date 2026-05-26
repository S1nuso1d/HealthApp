package com.example.healtapp.miband

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class MiBandScannedDevice(
    val address: String,
    val name: String,
    val rssi: Int,
)

enum class MiBandConnectionPhase {
    Idle,
    Scanning,
    Connecting,
    Discovering,
    Authenticating,
    Ready,
    Syncing,
    Error,
}

@Singleton
class MiBandBleClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager.adapter

    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var authSession: MiBandAuthSession? = null
    private var transport: MiBandBleTransport? = null
    private var commandHandler: MiBandCommandHandler? = null
    private var mtu: Int = 247

    private val _phase = MutableStateFlow(MiBandConnectionPhase.Idle)
    val phase: StateFlow<MiBandConnectionPhase> = _phase.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _scanned = MutableStateFlow<List<MiBandScannedDevice>>(emptyList())
    val scanned: StateFlow<List<MiBandScannedDevice>> = _scanned.asStateFlow()

    private val _liveStats = MutableStateFlow<MiBandLiveStats?>(null)
    val liveStats: StateFlow<MiBandLiveStats?> = _liveStats.asStateFlow()

    private var scanCallback: ScanCallback? = null

    fun isBluetoothReady(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan() {
        val bt = adapter
        if (bt == null || !bt.isEnabled) {
            _phase.value = MiBandConnectionPhase.Error
            _statusMessage.value = "Включите Bluetooth"
            return
        }
        _scanned.value = emptyList()
        _phase.value = MiBandConnectionPhase.Scanning
        _statusMessage.value = "Поиск Mi Band / Xiaomi…"

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(android.os.ParcelUuid(MiBandBleUuids.SERVICE))
                .build(),
        )

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device.name
                if (!MiBandBleUuids.isLikelyMiBandName(name) && name != null) return
                val item = MiBandScannedDevice(
                    address = result.device.address,
                    name = name ?: "Xiaomi Band",
                    rssi = result.rssi,
                )
                _scanned.value = (_scanned.value + item)
                    .distinctBy { it.address }
                    .sortedByDescending { it.rssi }
            }

            override fun onScanFailed(errorCode: Int) {
                _phase.value = MiBandConnectionPhase.Error
                _statusMessage.value = "Сканирование не удалось (код $errorCode)"
            }
        }
        bt.bluetoothLeScanner.startScan(filters, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanCallback?.let { adapter?.bluetoothLeScanner?.stopScan(it) }
        scanCallback = null
        if (_phase.value == MiBandConnectionPhase.Scanning) {
            _phase.value = MiBandConnectionPhase.Idle
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String, authKeyHex: String) {
        stopScan()
        val device = adapter?.getRemoteDevice(address) ?: run {
            _phase.value = MiBandConnectionPhase.Error
            _statusMessage.value = "Устройство не найдено"
            return
        }
        disconnect()
        authSession = MiBandAuthSession(authKeyHex)
        _phase.value = MiBandConnectionPhase.Connecting
        _statusMessage.value = "Подключение к $address…"
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            @Suppress("DEPRECATION")
            device.connectGatt(context, false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        transport?.reset()
        transport = null
        commandHandler = null
        authSession = null
        gatt?.close()
        gatt = null
        writeChar = null
        _phase.value = MiBandConnectionPhase.Idle
        _statusMessage.value = "Отключено"
        _liveStats.value = null
    }

    @SuppressLint("MissingPermission")
    fun syncToday() {
        val handler = commandHandler ?: return
        if (_phase.value != MiBandConnectionPhase.Ready) return
        _phase.value = MiBandConnectionPhase.Syncing
        transport?.enqueueCommand(handler.buildFetchTodayCommand().toByteArray())
        transport?.enqueueCommand(handler.buildRealtimeStartCommand().toByteArray())
        _statusMessage.value = "Синхронизация шагов и пульса…"
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _phase.value = MiBandConnectionPhase.Discovering
                _statusMessage.value = "Сервисы GATT…"
                gatt.requestMtu(512)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _phase.value = MiBandConnectionPhase.Idle
                _statusMessage.value = "Соединение разорвано"
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@MiBandBleClient.mtu = mtu
            }
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _phase.value = MiBandConnectionPhase.Error
                _statusMessage.value = "Ошибка GATT $status"
                return
            }
            val service = gatt.getService(MiBandBleUuids.SERVICE) ?: run {
                _phase.value = MiBandConnectionPhase.Error
                _statusMessage.value = "Сервис Xiaomi fe95 не найден (не Band 8?)"
                return
            }
            val read = service.getCharacteristic(MiBandBleUuids.CHAR_COMMAND_READ)
            val write = service.getCharacteristic(MiBandBleUuids.CHAR_COMMAND_WRITE)
            val activity = service.getCharacteristic(MiBandBleUuids.CHAR_ACTIVITY_DATA)
            if (read == null || write == null) {
                _phase.value = MiBandConnectionPhase.Error
                _statusMessage.value = "Характеристики 51/52 недоступны"
                return
            }
            writeChar = write
            setupPipeline(gatt, read, write, activity)
            gatt.setCharacteristicNotification(read, true)
            gatt.setCharacteristicNotification(write, true)
            activity?.let { gatt.setCharacteristicNotification(it, true) }
            enableDescriptor(gatt, read)
            enableDescriptor(gatt, write)
            activity?.let { enableDescriptor(gatt, it) }

            _phase.value = MiBandConnectionPhase.Authenticating
            _statusMessage.value = "Аутентификация (нужен ключ Mi Fitness)…"
            val nonceCmd = authSession?.buildPhoneNonceCommand() ?: return
            transport?.enqueueCommand(nonceCmd.toByteArray())
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == MiBandBleUuids.CHAR_COMMAND_READ ||
                characteristic.uuid == MiBandBleUuids.CHAR_COMMAND_WRITE
            ) {
                transport?.onNotify(value)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            if (characteristic.uuid == MiBandBleUuids.CHAR_COMMAND_READ ||
                characteristic.uuid == MiBandBleUuids.CHAR_COMMAND_WRITE
            ) {
                transport?.onNotify(value)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupPipeline(
        gatt: BluetoothGatt,
        read: BluetoothGattCharacteristic,
        write: BluetoothGattCharacteristic,
        @Suppress("UNUSED_PARAMETER") activity: BluetoothGattCharacteristic?,
    ) {
        val session = authSession ?: return
        val writeFn: (ByteArray) -> Boolean = { bytes ->
            writeChar?.let { ch ->
                ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(ch, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
                        BluetoothGatt.GATT_SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    ch.value = bytes
                    gatt.writeCharacteristic(ch)
                }
            } ?: false
        }

        commandHandler = MiBandCommandHandler(
            authSession = session,
            onState = { _statusMessage.value = it },
            onAuthenticated = {
                _phase.value = MiBandConnectionPhase.Ready
                _statusMessage.value = "Готово — можно синхронизировать"
            },
            onStats = { stats ->
                _liveStats.value = stats
                if (_phase.value == MiBandConnectionPhase.Syncing) {
                    _phase.value = MiBandConnectionPhase.Ready
                    _statusMessage.value = "Получено: ${stats.steps} шагов, пульс ${stats.heartRate}"
                }
            },
            sendCommand = { cmd -> transport?.enqueueCommand(cmd.toByteArray()) },
        )

        transport = MiBandBleTransport(
            authSession = session,
            onCommandPayload = { payload -> commandHandler?.handlePayload(payload) },
            writeGatt = writeFn,
            maxWriteSizeProvider = { (mtu - 3).coerceAtLeast(20) },
        )
    }

    @SuppressLint("MissingPermission")
    private fun enableDescriptor(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val descriptor = characteristic.getDescriptor(MiBandBleUuids.CLIENT_CHARACTERISTIC_CONFIG)
            ?: return
        val value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            gatt.writeDescriptor(descriptor)
        }
    }

    companion object {
        private const val TAG = "MiBandBleClient"
    }
}
