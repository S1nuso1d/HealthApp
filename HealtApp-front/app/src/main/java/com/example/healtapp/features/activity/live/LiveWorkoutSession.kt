package com.example.healtapp.features.activity.live

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveWorkoutSession @Inject constructor() {
    private val _snapshot = MutableStateFlow(LiveWorkoutSnapshot())
    val snapshot: StateFlow<LiveWorkoutSnapshot> = _snapshot.asStateFlow()

    private var stillSinceMs: Long = 0L
    private var lastSplitMeters: Double = 0.0
    private var lastSplitMovingMs: Long = 0L
    private var pauseStartedAtMs: Long = 0L
    private var pausedAccumulatedMs: Long = 0L

    @Synchronized
    fun start(activityTitleRu: String) {
        val now = System.currentTimeMillis()
        stillSinceMs = 0L
        lastSplitMeters = 0.0
        lastSplitMovingMs = 0L
        pauseStartedAtMs = 0L
        pausedAccumulatedMs = 0L
        _snapshot.value = LiveWorkoutSnapshot(
            activityTitleRu = activityTitleRu,
            isActive = true,
            isPaused = false,
            autoPaused = false,
            startedAtEpochMs = now,
            elapsedMs = 0L,
            movingMs = 0L,
            statusMessage = "Ищем спутник…",
        )
    }

    @Synchronized
    fun setStatus(message: String?) {
        _snapshot.update { it.copy(statusMessage = message) }
    }

    @Synchronized
    fun pause() {
        val now = System.currentTimeMillis()
        pauseStartedAtMs = now
        stillSinceMs = 0L
        _snapshot.update { it.copy(isPaused = true, autoPaused = false, statusMessage = "Пауза") }
    }

    @Synchronized
    fun resume() {
        val now = System.currentTimeMillis()
        if (pauseStartedAtMs > 0L) {
            pausedAccumulatedMs += (now - pauseStartedAtMs).coerceAtLeast(0L)
            pauseStartedAtMs = 0L
        }
        stillSinceMs = 0L
        _snapshot.update { it.copy(isPaused = false, autoPaused = false, statusMessage = null) }
    }

    @Synchronized
    fun tick(nowMs: Long) {
        val state = _snapshot.value
        if (!state.isActive || state.startedAtEpochMs <= 0L) return
        val elapsed = (nowMs - state.startedAtEpochMs).coerceAtLeast(0L)
        val pausedNow = if (state.isPaused && pauseStartedAtMs > 0L) nowMs - pauseStartedAtMs else 0L
        val moving = (elapsed - pausedAccumulatedMs - pausedNow).coerceAtLeast(0L)
        val cadence = if (moving >= 30_000L && state.steps > 0) {
            ((state.steps * 60_000.0) / moving).toInt().coerceIn(0, 240)
        } else {
            state.cadenceSpm
        }
        _snapshot.update {
            it.copy(
                elapsedMs = elapsed,
                movingMs = moving,
                avgPaceMinPerKm = paceMinPerKm(it.distanceMeters, moving),
                avgSpeedKmh = speedKmh(it.distanceMeters, moving),
                cadenceSpm = cadence,
            )
        }
    }

    @Synchronized
    fun addStep() {
        val state = _snapshot.value
        if (!state.isActive || state.isPaused) return
        _snapshot.update { it.copy(steps = it.steps + 1) }
    }

    @Synchronized
    fun addLocation(
        lat: Double,
        lon: Double,
        accuracyM: Float,
        timeMs: Long,
        bearing: Float?,
        speedMps: Float?,
        altitude: Double?,
        allowWeakFix: Boolean = false,
    ) {
        val state = _snapshot.value
        if (!state.isActive) return
        if (state.isPaused && !state.autoPaused) return

        val accuracyLimit = if (state.points.isEmpty() || allowWeakFix) 85f else 55f
        if (accuracyM > accuracyLimit) {
            _snapshot.update {
                it.copy(
                    lastLat = lat,
                    lastLon = lon,
                    headingDegrees = bearing?.takeIf { deg -> deg >= 0f } ?: it.headingDegrees,
                    gpsFix = it.gpsFix,
                    statusMessage = "Слабый GPS · точность ${accuracyM.toInt()} м",
                )
            }
            return
        }

        val last = state.points.lastOrNull()
        val added = if (last == null) {
            0.0
        } else {
            val delta = haversineMeters(last.lat, last.lon, lat, lon)
            val dt = (timeMs - last.timeMs).coerceAtLeast(1L)
            val maxSpeedMps = 12.0
            when {
                delta < 1.2 -> 0.0
                delta / (dt / 1000.0) > maxSpeedMps -> 0.0
                else -> delta
            }
        }
        val speed = speedMps ?: if (last != null && added > 0.0) {
            val dt = (timeMs - last.timeMs).coerceAtLeast(1L) / 1000.0
            (added / dt).toFloat()
        } else {
            0f
        }
        val movingNow = added >= 1.5 || speed > 0.45f

        if (state.autoPaused && state.isPaused) {
            if (movingNow) {
                resume()
            } else {
                _snapshot.update {
                    it.copy(
                        lastLat = lat,
                        lastLon = lon,
                        headingDegrees = bearing?.takeIf { deg -> deg >= 0f } ?: it.headingDegrees,
                    )
                }
                return
            }
        } else if (!movingNow && state.points.isNotEmpty()) {
            if (stillSinceMs == 0L) stillSinceMs = timeMs
            if (timeMs - stillSinceMs >= 25_000L) {
                pauseStartedAtMs = timeMs
                stillSinceMs = 0L
                _snapshot.update {
                    it.copy(
                        isPaused = true,
                        autoPaused = true,
                        lastLat = lat,
                        lastLon = lon,
                        headingDegrees = bearing?.takeIf { deg -> deg >= 0f } ?: it.headingDegrees,
                        statusMessage = "Автопауза — стоите на месте",
                    )
                }
                return
            }
        } else {
            stillSinceMs = 0L
        }

        val live = _snapshot.value
        val points = (live.points + GpsPoint(lat, lon, timeMs, altitude, bearing, speedMps)).takeLast(5000)
        val distance = live.distanceMeters + added
        val splits = maybeAddSplit(live.kmSplits, distance, live.movingMs)
        val currentPace = currentPace(points)
        _snapshot.update {
            it.copy(
                points = points,
                lastLat = lat,
                lastLon = lon,
                headingDegrees = bearing?.takeIf { deg -> deg >= 0f } ?: it.headingDegrees,
                distanceMeters = distance,
                gpsFix = true,
                currentPaceMinPerKm = currentPace,
                avgPaceMinPerKm = paceMinPerKm(distance, it.movingMs.coerceAtLeast(1L)),
                avgSpeedKmh = speedKmh(distance, it.movingMs.coerceAtLeast(1L)),
                kmSplits = splits,
                statusMessage = if (it.autoPaused) it.statusMessage else null,
            )
        }
    }

    @Synchronized
    fun stop(): LiveWorkoutSnapshot {
        val done = _snapshot.value.copy(isActive = false, isPaused = false, autoPaused = false)
        _snapshot.value = LiveWorkoutSnapshot()
        stillSinceMs = 0L
        lastSplitMeters = 0.0
        lastSplitMovingMs = 0L
        pauseStartedAtMs = 0L
        pausedAccumulatedMs = 0L
        return done
    }

    @Synchronized
    fun discard() {
        _snapshot.value = LiveWorkoutSnapshot()
        stillSinceMs = 0L
        lastSplitMeters = 0.0
        lastSplitMovingMs = 0L
        pauseStartedAtMs = 0L
        pausedAccumulatedMs = 0L
    }

    private fun maybeAddSplit(current: List<KmSplit>, distance: Double, movingMs: Long): List<KmSplit> {
        if (distance - lastSplitMeters < 1000.0) return current
        val km = (distance / 1000.0).toInt().coerceAtLeast(1)
        val splitMoving = (movingMs - lastSplitMovingMs).coerceAtLeast(1L)
        val pace = (splitMoving / 60000.0)
        val previous = current.lastOrNull()
        val faster = previous?.let { pace < it.paceMinPerKm - 0.03 }
        lastSplitMeters = km * 1000.0
        lastSplitMovingMs = movingMs
        return current + KmSplit(km = km, paceMinPerKm = pace, fasterThanPrevious = faster)
    }

    private fun paceMinPerKm(distanceMeters: Double, movingMs: Long): Double? {
        if (distanceMeters < 30.0 || movingMs < 5_000L) return null
        val km = distanceMeters / 1000.0
        return (movingMs / 60000.0) / km
    }

    private fun speedKmh(distanceMeters: Double, movingMs: Long): Double? {
        if (distanceMeters < 10.0 || movingMs < 3_000L) return null
        return (distanceMeters / (movingMs / 1000.0)) * 3.6
    }

    private fun currentPace(points: List<GpsPoint>): Double? {
        if (points.size < 3) return null
        val window = points.takeLast(min(8, points.size))
        val first = window.first()
        val last = window.last()
        val dist = haversineMeters(first.lat, first.lon, last.lat, last.lon)
        val dt = last.timeMs - first.timeMs
        if (dist < 12.0 || dt < 4_000L) return null
        return (dt / 60000.0) / (dist / 1000.0)
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earth = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return 2 * earth * asin(sqrt(a))
    }
}
