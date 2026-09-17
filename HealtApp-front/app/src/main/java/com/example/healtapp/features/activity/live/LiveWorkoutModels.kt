package com.example.healtapp.features.activity.live

data class GpsPoint(
    val lat: Double,
    val lon: Double,
    val timeMs: Long,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speedMps: Float? = null,
)

data class KmSplit(
    val km: Int,
    val paceMinPerKm: Double,
    val fasterThanPrevious: Boolean? = null,
)

data class LiveWorkoutSnapshot(
    val activityTitleRu: String = "Бег",
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val autoPaused: Boolean = false,
    val startedAtEpochMs: Long = 0L,
    val elapsedMs: Long = 0L,
    val movingMs: Long = 0L,
    val distanceMeters: Double = 0.0,
    val points: List<GpsPoint> = emptyList(),
    val lastLat: Double? = null,
    val lastLon: Double? = null,
    val headingDegrees: Float? = null,
    val avgPaceMinPerKm: Double? = null,
    val currentPaceMinPerKm: Double? = null,
    val avgSpeedKmh: Double? = null,
    val steps: Int = 0,
    val cadenceSpm: Int? = null,
    val gpsFix: Boolean = false,
    val statusMessage: String? = null,
    val kmSplits: List<KmSplit> = emptyList(),
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    val durationMinutes: Int get() = ((if (movingMs > 0L) movingMs else elapsedMs) / 60000L).toInt().coerceAtLeast(1)
}
