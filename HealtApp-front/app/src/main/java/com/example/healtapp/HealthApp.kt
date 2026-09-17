package com.example.healtapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.healtapp.core.telemetry.CrashReporting
import com.example.healtapp.data.healthconnect.HealthConnectSyncWorker
import com.example.healtapp.notifications.HealthNotificationChannels
import com.example.healtapp.notifications.PillReminderRestoreWorker
import com.example.healtapp.notifications.PillReminderScheduler
import com.example.healtapp.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class HealthApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var connectivitySyncCoordinator: com.example.healtapp.data.network.offline.ConnectivitySyncCoordinator
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
            
    override fun onCreate() {
        super.onCreate()
        CrashReporting.init(this)
        HealthNotificationChannels.createAll(this)
        ReminderScheduler.rescheduleAll(this)
        // Будильники таблеток: сразу из локального кэша, затем sync с API.
        PillReminderScheduler.restoreFromLocal(this)
        PillReminderRestoreWorker.enqueue(this)
        scheduleHealthConnectSync()
        scheduleSocialSync()
        scheduleOfflineSync()
        connectivitySyncCoordinator.start()
    }

    private fun scheduleOfflineSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val offlineSyncRequest = PeriodicWorkRequestBuilder<com.example.healtapp.data.network.offline.OfflineSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OfflineSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            offlineSyncRequest
        )
    }

    private fun scheduleSocialSync() {
        val constraints = Constraints.Builder()
        // Require network to fetch pending friends
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<com.example.healtapp.notifications.SocialSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SocialSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }

    private fun scheduleHealthConnectSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(
            15, TimeUnit.MINUTES // Minimum periodic interval is 15 minutes
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "HealthConnectSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}