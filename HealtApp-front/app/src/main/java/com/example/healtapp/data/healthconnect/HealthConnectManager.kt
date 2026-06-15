package com.example.healtapp.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.domain.repository.ActivityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityRepository: ActivityRepository
) {
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    fun isSupported(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        if (!isSupported()) return false
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncTodaySteps(): Result<Unit> {
        return try {
            if (!hasAllPermissions()) {
                return Result.failure(Exception("Нет разрешений Health Connect"))
            }

            val client = HealthConnectClient.getOrCreate(context)
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val startLdt = today.atStartOfDay()
            val endLdt = LocalDateTime.now()

            val grouped = client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startLdt, endLdt),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )

            var totalSteps = 0L
            for (row in grouped) {
                totalSteps += row.result[StepsRecord.COUNT_TOTAL] ?: 0L
            }

            if (totalSteps > 0) {
                val startInstant = startLdt.atZone(zone).toInstant()
                val endInstant = endLdt.atZone(zone).toInstant()
                
                val durationMinutes = java.time.Duration.between(startInstant, endInstant).toMinutes().toInt().coerceAtLeast(1)

                val request = ActivityCreateRequestDto(
                    activity_type = "walk",
                    start_time = startInstant.toString(),
                    end_time = endInstant.toString(),
                    duration_minutes = durationMinutes,
                    steps = totalSteps.toInt(),
                    distance_km = null,
                    calories_burned = null,
                    intensity = "low",
                    source = "health_connect"
                )
                
                activityRepository.createActivity(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
