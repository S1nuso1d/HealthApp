package com.example.healtapp.features.activity.live

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.healtapp.MainActivity
import com.example.healtapp.R
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

@AndroidEntryPoint
class LiveWorkoutService : Service(), SensorEventListener {

    @Inject lateinit var session: LiveWorkoutSession

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var ticker: Timer? = null
    private var locationCallback: LocationCallback? = null
    private var sensorManager: SensorManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TYPE) ?: "Бег"
                session.start(title)
                startForegroundNotification(title)
                seedLastLocation()
                startLocationUpdates()
                startStepSensor()
                startTicker()
            }
            ACTION_PAUSE -> session.pause()
            ACTION_RESUME -> session.resume()
            ACTION_STOP -> stopWorkout()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopLocationUpdates()
        stopStepSensor()
        ticker?.cancel()
        ticker = null
        super.onDestroy()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = Timer().apply {
            scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        session.tick(System.currentTimeMillis())
                        updateNotification()
                    }
                },
                1000L,
                1000L,
            )
        }
    }

    private fun seedLastLocation() {
        if (!hasLocationPermission()) return
        runCatching {
            fused.lastLocation.addOnSuccessListener { location ->
                if (location == null) return@addOnSuccessListener
                session.addLocation(
                    lat = location.latitude,
                    lon = location.longitude,
                    accuracyM = location.accuracy.coerceAtLeast(1f),
                    timeMs = location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    bearing = if (location.hasBearing()) location.bearing else null,
                    speedMps = if (location.hasSpeed()) location.speed else null,
                    altitude = if (location.hasAltitude()) location.altitude else null,
                    allowWeakFix = true,
                )
            }
        }
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            session.setStatus("Нет доступа к геолокации")
            stopSelf()
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(700L)
            .setMinUpdateDistanceMeters(1.5f)
            .setWaitForAccurateLocation(false)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                session.addLocation(
                    lat = location.latitude,
                    lon = location.longitude,
                    accuracyM = location.accuracy.coerceAtLeast(1f),
                    timeMs = location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    bearing = if (location.hasBearing()) location.bearing else null,
                    speedMps = if (location.hasSpeed()) location.speed else null,
                    altitude = if (location.hasAltitude()) location.altitude else null,
                )
            }
        }
        locationCallback = callback
        runCatching {
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }.onFailure {
            session.setStatus("Не удалось включить GPS: ${it.message}")
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { runCatching { fused.removeLocationUpdates(it) } }
        locationCallback = null
    }

    private fun stopWorkout() {
        stopLocationUpdates()
        stopStepSensor()
        ticker?.cancel()
        ticker = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startStepSensor() {
        val sm = getSystemService(SensorManager::class.java) ?: return
        sensorManager = sm
        val detector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (detector != null) {
            sm.registerListener(this, detector, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopStepSensor() {
        runCatching { sensorManager?.unregisterListener(this) }
        sensorManager = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            session.addStep()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun startForegroundNotification(title: String) {
        ensureChannel()
        val notification = buildNotification(title, "Ищем GPS…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val snap = session.snapshot.value
        if (!snap.isActive) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val dist = "%.2f".format(snap.distanceKm)
        val text = if (snap.isPaused) "Пауза · $dist км" else "$dist км · ${formatElapsed(snap.elapsedMs)}"
        nm.notify(NOTIFICATION_ID, buildNotification(snap.activityTitleRu, text))
    }

    private fun buildNotification(title: String, text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$title · GPS")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Живая тренировка",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Запись GPS-тренировки" },
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 7214
        private const val CHANNEL_ID = "live_workout"
        const val ACTION_START = "com.example.healtapp.live.START"
        const val ACTION_PAUSE = "com.example.healtapp.live.PAUSE"
        const val ACTION_RESUME = "com.example.healtapp.live.RESUME"
        const val ACTION_STOP = "com.example.healtapp.live.STOP"
        const val EXTRA_TYPE = "activity_title"

        fun start(context: Context, activityTitleRu: String) {
            val intent = Intent(context, LiveWorkoutService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TYPE, activityTitleRu)
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) {
            context.startService(Intent(context, LiveWorkoutService::class.java).setAction(ACTION_PAUSE))
        }

        fun resume(context: Context) {
            context.startService(Intent(context, LiveWorkoutService::class.java).setAction(ACTION_RESUME))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, LiveWorkoutService::class.java).setAction(ACTION_STOP))
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
