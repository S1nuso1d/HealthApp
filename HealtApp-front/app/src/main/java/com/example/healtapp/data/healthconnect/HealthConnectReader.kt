package com.example.healtapp.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.data.network.dto.dataimport.HealthSampleCreateDto
import com.example.healtapp.data.network.dto.dataimport.ImportBatchRequestDto
import com.example.healtapp.data.network.dto.meal.MealCreateRequestDto
import com.example.healtapp.data.network.dto.sleep.CreateSleepRequestDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.reflect.KClass

@Singleton
class HealthConnectReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        /** Минимум для импорта сна и шагов. */
        fun coreReadPermissions(): Set<String> = setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
        )

        /** Все типы, которые мы пытаемся прочитать при полной выдаче прав. */
        fun extendedReadPermissions(): Set<String> = buildSet {
            add(HealthPermission.getReadPermission(ExerciseSessionRecord::class))
            add(HealthPermission.getReadPermission(HeartRateRecord::class))
            add(HealthPermission.getReadPermission(BloodPressureRecord::class))
            add(HealthPermission.getReadPermission(OxygenSaturationRecord::class))
            add(HealthPermission.getReadPermission(BloodGlucoseRecord::class))
            add(HealthPermission.getReadPermission(WeightRecord::class))
            add(HealthPermission.getReadPermission(HeightRecord::class))
            add(HealthPermission.getReadPermission(BodyFatRecord::class))
            add(HealthPermission.getReadPermission(BasalMetabolicRateRecord::class))
            add(HealthPermission.getReadPermission(Vo2MaxRecord::class))
            add(HealthPermission.getReadPermission(PowerRecord::class))
            add(HealthPermission.getReadPermission(SpeedRecord::class))
            add(HealthPermission.getReadPermission(DistanceRecord::class))
            add(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))
            add(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))
            add(HealthPermission.getReadPermission(NutritionRecord::class))
        }

        fun importReadPermissions(): Set<String> = coreReadPermissions() + extendedReadPermissions()

        fun requiredReadPermissions(): Set<String> = importReadPermissions()
    }

    suspend fun areReadPermissionsGranted(): Boolean {
        val client = getClient() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return coreReadPermissions().all { it in granted }
    }

    fun sdkStatus(): Int = try {
        HealthConnectClient.getSdkStatus(context)
    } catch (_: Throwable) {
        HealthConnectClient.SDK_UNAVAILABLE
    }

    fun isHealthConnectUsable(): Boolean {
        return when (sdkStatus()) {
            HealthConnectClient.SDK_AVAILABLE,
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
            -> true
            else -> false
        }
    }

    fun needsProviderUpdate(): Boolean =
        sdkStatus() == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    fun canRequestPermissions(): Boolean =
        sdkStatus() == HealthConnectClient.SDK_AVAILABLE

    fun isSupported(): Boolean = isHealthConnectUsable()

    fun getClient(): HealthConnectClient? {
        if (!canRequestPermissions()) return null
        return try {
            HealthConnectClient.getOrCreate(context)
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun <T : Record> readRecordsIfPermitted(
        client: HealthConnectClient,
        clazz: KClass<T>,
        start: Instant,
        end: Instant,
        granted: Set<String>,
    ): List<T> {
        val perm = runCatching { HealthPermission.getReadPermission(clazz) }.getOrNull() ?: return emptyList()
        if (perm !in granted) return emptyList()
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = clazz,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                ),
            ).records
        }.getOrElse { emptyList() }
    }

    private fun overlaps(aStart: Instant, aEnd: Instant, bStart: Instant, bEnd: Instant): Boolean =
        aStart.isBefore(bEnd) && bStart.isBefore(aEnd)

    private fun exerciseSlug(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "walk"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
        -> "run"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
        -> "cycling"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
        -> "swim"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
        -> "gym"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
        ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
        -> "yoga"
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "workout"
        else -> "workout"
    }

    suspend fun buildImportPayload(days: Int): Result<ImportBatchRequestDto> {
        val client = getClient() ?: return Result.failure(IllegalStateException("Health Connect недоступен"))
        val end = Instant.now()
        val start = end.minusSeconds(days * 24L * 3600L)
        val zone = ZoneId.systemDefault()
        val granted = client.permissionController.getGrantedPermissions()

        val heartRates = readRecordsIfPermitted(client, HeartRateRecord::class, start, end, granted)
        val totalCalList = readRecordsIfPermitted(client, TotalCaloriesBurnedRecord::class, start, end, granted)
        val distanceList = readRecordsIfPermitted(client, DistanceRecord::class, start, end, granted)
        val powerAll = readRecordsIfPermitted(client, PowerRecord::class, start, end, granted)
        val speedAll = readRecordsIfPermitted(client, SpeedRecord::class, start, end, granted)

        val sleeps = mutableListOf<CreateSleepRequestDto>()
        val activities = mutableListOf<ActivityCreateRequestDto>()
        val meals = mutableListOf<MealCreateRequestDto>()
        val samples = mutableListOf<HealthSampleCreateDto>()

        val sleepResp = readRecordsIfPermitted(client, SleepSessionRecord::class, start, end, granted)
        for (session in sleepResp) {
            if (!session.endTime.isAfter(session.startTime)) continue
            sleeps.add(
                CreateSleepRequestDto(
                    sleep_start = session.startTime.toString(),
                    sleep_end = session.endTime.toString(),
                    quality_score = null,
                    notes = "Health Connect",
                    source = "health_connect",
                ),
            )
        }

        val stepsByDay = readStepsAggregatedByDay(client, start, end, zone)

        for ((day, total) in stepsByDay) {
            if (total <= 0) continue
            val dayStart = java.time.LocalDate.parse(day).atStartOfDay(zone).toInstant()
            val dayEnd = dayStart.plusSeconds(3600L)
            activities.add(
                ActivityCreateRequestDto(
                    activity_type = "walk",
                    start_time = dayStart.toString(),
                    end_time = dayEnd.toString(),
                    duration_minutes = 60,
                    steps = total.toInt().coerceAtMost(Int.MAX_VALUE),
                    distance_km = null,
                    calories_burned = null,
                    intensity = "low",
                    source = "health_connect",
                ),
            )
        }

        for (rec in heartRates) {
            val bpms = rec.samples.map { it.beatsPerMinute.toDouble() }
            val avg = bpms.average().takeIf { !it.isNaN() } ?: continue
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.startTime.toString(),
                    period_end = rec.endTime.toString(),
                    metric = "heart_rate_bpm",
                    value1 = avg,
                    source = "health_connect",
                ),
            )
        }

        for (rec in readRecordsIfPermitted(client, BloodPressureRecord::class, start, end, granted)) {
            val sys = rec.systolic.inMillimetersOfMercury
            val dia = rec.diastolic.inMillimetersOfMercury
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.time.toString(),
                    metric = "blood_pressure_mmhg",
                    value1 = sys,
                    value2 = dia,
                    source = "health_connect",
                ),
            )
        }

        for (rec in readRecordsIfPermitted(client, OxygenSaturationRecord::class, start, end, granted)) {
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.time.toString(),
                    metric = "spo2_percent",
                    value1 = rec.percentage.value,
                    source = "health_connect",
                ),
            )
        }

        for (rec in readRecordsIfPermitted(client, BloodGlucoseRecord::class, start, end, granted)) {
            val mmol = runCatching { rec.level.inMillimolesPerLiter }.getOrNull() ?: continue
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.time.toString(),
                    metric = "blood_glucose_mmol_l",
                    value1 = mmol,
                    source = "health_connect",
                ),
            )
        }

        for (rec in readRecordsIfPermitted(client, WeightRecord::class, start, end, granted)) {
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.time.toString(),
                    metric = "weight_kg",
                    value1 = rec.weight.inKilograms,
                    source = "health_connect",
                ),
            )
        }

        for (rec in readRecordsIfPermitted(client, HeightRecord::class, start, end, granted)) {
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.time.toString(),
                    metric = "height_cm",
                    value1 = rec.height.inMeters * 100.0,
                    source = "health_connect",
                ),
            )
        }

        for (rec in readRecordsIfPermitted(client, BodyFatRecord::class, start, end, granted)) {
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.time.toString(),
                    metric = "body_fat_percent",
                    value1 = rec.percentage.value,
                    source = "health_connect",
                ),
            )
        }

        for (rec in readRecordsIfPermitted(client, BasalMetabolicRateRecord::class, start, end, granted)) {
            val kcal = runCatching { rec.basalMetabolicRate.inKilocaloriesPerDay }.getOrNull() ?: continue
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.time.toString(),
                    metric = "bmr_kcal",
                    value1 = kcal,
                    source = "health_connect",
                ),
            )
        }

        for (rec in readRecordsIfPermitted(client, Vo2MaxRecord::class, start, end, granted)) {
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.time.toString(),
                    metric = "vo2_max",
                    value1 = rec.vo2MillilitersPerMinuteKilogram,
                    source = "health_connect",
                ),
            )
        }

        for (rec in powerAll) {
            val watts = rec.samples.map { it.power.inWatts }
            val avg = watts.average().takeIf { !it.isNaN() } ?: continue
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.startTime.toString(),
                    period_end = rec.endTime.toString(),
                    metric = "power_w",
                    value1 = avg,
                    source = "health_connect",
                ),
            )
        }

        for (rec in speedAll) {
            val speeds = rec.samples.map { it.speed.inMetersPerSecond }
            val avg = speeds.average().takeIf { !it.isNaN() } ?: continue
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.startTime.toString(),
                    period_end = rec.endTime.toString(),
                    metric = "speed_m_s",
                    value1 = avg,
                    source = "health_connect",
                ),
            )
        }

        for (rec in distanceList) {
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.startTime.toString(),
                    period_end = rec.endTime.toString(),
                    metric = "distance_m",
                    value1 = rec.distance.inMeters,
                    source = "health_connect",
                ),
            )
        }

        for (rec in readRecordsIfPermitted(client, ActiveCaloriesBurnedRecord::class, start, end, granted)) {
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.startTime.toString(),
                    period_end = rec.endTime.toString(),
                    metric = "active_calories_kcal",
                    value1 = rec.energy.inKilocalories,
                    source = "health_connect",
                ),
            )
        }

        for (rec in totalCalList) {
            samples.add(
                HealthSampleCreateDto(
                    recorded_at = rec.startTime.toString(),
                    period_end = rec.endTime.toString(),
                    metric = "total_calories_kcal",
                    value1 = rec.energy.inKilocalories,
                    source = "health_connect",
                ),
            )
        }

        for (rec in readRecordsIfPermitted(client, NutritionRecord::class, start, end, granted)) {
            val name = rec.name?.takeIf { it.isNotBlank() } ?: "Питание Health Connect"
            val kcal = rec.energy?.inKilocalories
            meals.add(
                MealCreateRequestDto(
                    meal_type = "snack",
                    name = name,
                    calories = kcal?.toFloat(),
                    protein_g = rec.protein?.inGrams?.toFloat(),
                    fat_g = rec.totalFat?.inGrams?.toFloat(),
                    carbs_g = rec.totalCarbohydrate?.inGrams?.toFloat(),
                    meal_time = rec.startTime.toString(),
                    notes = "Health Connect",
                    source = "health_connect",
                ),
            )
        }

        val exercises = readRecordsIfPermitted(client, ExerciseSessionRecord::class, start, end, granted)

        for (ex in exercises) {
            if (!ex.endTime.isAfter(ex.startTime)) continue
            val durMin = ChronoUnit.MINUTES.between(ex.startTime, ex.endTime).toInt().coerceAtLeast(1)

            val hrInSession = heartRates.flatMap { hr ->
                hr.samples.filter { s ->
                    !s.time.isBefore(ex.startTime) && !s.time.isAfter(ex.endTime)
                }.map { it.beatsPerMinute }
            }
            val avgHr = hrInSession.average().takeIf { !it.isNaN() }?.roundToInt()

            var kcalSum = 0.0
            for (tc in totalCalList) {
                if (overlaps(ex.startTime, ex.endTime, tc.startTime, tc.endTime)) {
                    kcalSum += tc.energy.inKilocalories
                }
            }
            var distM = 0.0
            for (d in distanceList) {
                if (overlaps(ex.startTime, ex.endTime, d.startTime, d.endTime)) {
                    distM += d.distance.inMeters
                }
            }

            val pAvg = powerAll
                .filter { overlaps(ex.startTime, ex.endTime, it.startTime, it.endTime) }
                .flatMap { it.samples.map { s -> s.power.inWatts } }
                .average()
                .takeIf { !it.isNaN() }

            val sAvg = speedAll
                .filter { overlaps(ex.startTime, ex.endTime, it.startTime, it.endTime) }
                .flatMap { it.samples.map { s -> s.speed.inMetersPerSecond } }
                .average()
                .takeIf { !it.isNaN() }

            val title = ex.title?.takeIf { it.isNotBlank() }
            val notes = buildString {
                append("Health Connect")
                if (title != null) append(": ").append(title)
            }

            activities.add(
                ActivityCreateRequestDto(
                    activity_type = exerciseSlug(ex.exerciseType),
                    start_time = ex.startTime.toString(),
                    end_time = ex.endTime.toString(),
                    duration_minutes = durMin,
                    steps = null,
                    distance_km = if (distM > 0) (distM / 1000.0).toFloat() else null,
                    calories_burned = if (kcalSum > 0) kcalSum.toFloat() else null,
                    avg_heart_rate = avgHr,
                    avg_power_w = pAvg?.toFloat(),
                    avg_speed_m_s = sAvg?.toFloat(),
                    intensity = "medium",
                    activity_category = "cardio",
                    notes = notes,
                    source = "health_connect",
                ),
            )
        }

        return Result.success(
            ImportBatchRequestDto(
                sleeps = sleeps,
                activities = activities,
                meals = meals,
                health_samples = samples,
            ),
        )
    }

    /** Шаги за сегодня из Health Connect (агрегат COUNT_TOTAL, без суммирования сырых записей). */
    suspend fun readTodaySteps(): Int? {
        val client = getClient() ?: return null
        if (!areReadPermissionsGranted()) return null
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant()
        val end = Instant.now()
        val byDay = readStepsAggregatedByDay(client, start, end, zone)
        return byDay[today.toString()]?.toInt()?.coerceAtMost(Int.MAX_VALUE)
    }

    /**
     * Health Connect хранит много пересекающихся [StepsRecord] (телефон, часы, Mi Band).
     * Складывать их нельзя — только дневной агрегат [StepsRecord.COUNT_TOTAL].
     */
    private suspend fun readStepsAggregatedByDay(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        zone: ZoneId,
    ): Map<String, Long> {
        val startLdt = LocalDateTime.ofInstant(start, zone)
        val endLdt = LocalDateTime.ofInstant(end, zone)
        val out = mutableMapOf<String, Long>()
        runCatching {
            val grouped = client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startLdt, endLdt),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            )
            for (row in grouped) {
                val total = row.result[StepsRecord.COUNT_TOTAL] ?: 0L
                if (total <= 0L) continue
                val day = row.startTime.atZone(zone).toLocalDate().toString()
                out[day] = total
            }
        }
        return out
    }
}
