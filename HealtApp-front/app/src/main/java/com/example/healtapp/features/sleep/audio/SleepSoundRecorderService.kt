package com.example.healtapp.features.sleep.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.healtapp.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SleepSoundRecorderService : Service() {

    @Inject lateinit var tracker: SleepSoundTracker
    @Inject lateinit var storage: SleepSoundStorage

    private var engine: SleepSoundRecorderEngine? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        engine?.stop()
        engine = null
        super.onDestroy()
    }

    private fun startMonitoring() {
        startForegroundWithNotification(clipsCount = 0, recording = false)
        engine?.stop()
        engine = SleepSoundRecorderEngine(
            storage = storage,
            onClipSaved = { clip -> tracker.onClipSaved(clip) },
            onTick = { count, recording ->
                tracker.onMonitoringTick(count, recording)
                updateNotification(count, recording)
            },
        ).also { it.start() }
    }

    private fun stopMonitoring() {
        engine?.stop()
        engine = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundWithNotification(clipsCount: Int, recording: Boolean) {
        ensureChannel()
        val notification = buildNotification(clipsCount, recording)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(clipsCount: Int, recording: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(clipsCount, recording))
    }

    private fun buildNotification(clipsCount: Int, recording: Boolean): Notification {
        val status = when {
            recording -> "Запись фрагмента…"
            clipsCount > 0 -> "Зафиксировано моментов: $clipsCount"
            else -> "Ожидание активности"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Запись звуков сна")
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Запись звуков сна",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    companion object {
        private const val CHANNEL_ID = "sleep_sound_recorder"
        private const val NOTIFICATION_ID = 7201
        const val ACTION_START = "com.example.healtapp.sleep.SOUND_START"
        const val ACTION_STOP = "com.example.healtapp.sleep.SOUND_STOP"

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, SleepSoundRecorderService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, SleepSoundRecorderService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
