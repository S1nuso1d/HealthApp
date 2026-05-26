package com.example.healtapp.miband

import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.miband.MiBandPreferences
import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.data.network.dto.dataimport.HealthSampleCreateDto
import com.example.healtapp.data.network.dto.dataimport.ImportBatchRequestDto
import com.example.healtapp.data.network.dto.dataimport.ImportBatchResponseDto
import com.example.healtapp.domain.repository.ImportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class MiBandUiDevice(
    val address: String,
    val name: String,
    val authKeyHex: String,
    val lastSteps: Int,
    val lastHeartRate: Int,
    val lastSyncEpochMs: Long,
    val phase: MiBandConnectionPhase,
    val statusMessage: String?,
    val liveSteps: Int?,
    val liveHeartRate: Int?,
)

@Singleton
class MiBandRepository @Inject constructor(
    private val bleClient: MiBandBleClient,
    private val preferences: MiBandPreferences,
    private val importRepository: ImportRepository,
) {
    private val iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    val deviceState: Flow<MiBandUiDevice?> = combine(
        preferences.savedDevice,
        bleClient.phase,
        bleClient.statusMessage,
        bleClient.liveStats,
    ) { saved, phase, msg, live ->
        saved?.let {
            MiBandUiDevice(
                address = it.address,
                name = it.name,
                authKeyHex = it.authKeyHex,
                lastSteps = it.lastSteps,
                lastHeartRate = it.lastHeartRate,
                lastSyncEpochMs = it.lastSyncEpochMs,
                phase = phase,
                statusMessage = msg,
                liveSteps = live?.steps,
                liveHeartRate = live?.heartRate,
            )
        }
    }

    val scannedDevices = bleClient.scanned
    val liveStats = bleClient.liveStats

    fun isBluetoothReady(): Boolean = bleClient.isBluetoothReady()

    fun startScan() = bleClient.startScan()

    fun stopScan() = bleClient.stopScan()

    suspend fun saveAndConnect(address: String, name: String, authKeyHex: String) {
        preferences.saveDevice(address, name, authKeyHex)
        bleClient.connect(address, authKeyHex)
    }

    fun connectSaved(authKeyHex: String, address: String) {
        bleClient.connect(address, authKeyHex)
    }

    fun disconnect() = bleClient.disconnect()

    fun syncToServer() {
        bleClient.syncToday()
    }

    suspend fun pushLiveStatsToBackend(
        stats: MiBandLiveStats,
        address: String,
    ): Result<ImportBatchResponseDto> {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val start = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val startStr = iso.format(start.atZone(zone))
        val endStr = iso.format(now.atZone(zone))

        val batch = ImportBatchRequestDto(
            activities = listOf(
                ActivityCreateRequestDto(
                    activity_type = "walking",
                    start_time = startStr,
                    end_time = endStr,
                    duration_minutes = 0,
                    steps = stats.steps,
                    calories_burned = stats.calories.toFloat(),
                    source = "miband_ble",
                    notes = "Mi Band BLE ($address)",
                ),
            ),
            health_samples = if (stats.heartRate > 0) {
                listOf(
                    HealthSampleCreateDto(
                        metric = "heart_rate",
                        value1 = stats.heartRate.toDouble(),
                        recorded_at = endStr,
                        source = "miband_ble",
                    ),
                )
            } else {
                emptyList()
            },
        )

        return importRepository.importBatch(batch).also { result ->
            result.onSuccess {
                preferences.updateLastSync(stats.steps, stats.heartRate, stats.calories)
                AppRefreshBus.notifyDataChanged()
            }
        }
    }

    suspend fun clearSaved() {
        disconnect()
        preferences.clear()
    }
}
