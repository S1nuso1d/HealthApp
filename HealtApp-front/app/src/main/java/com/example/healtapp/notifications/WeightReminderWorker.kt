package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healtapp.data.preferences.WeightHistoryStore
import com.example.healtapp.di.ReminderEntryPoint
import dagger.hilt.android.EntryPointAccessors

class WeightReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        if (!entry.notificationPrefs().current().goalAchievementNotifications) return Result.success()
        if (entry.tokenStorage().getToken() == null || entry.tokenStorage().isGuestMode()) {
            return Result.success()
        }
        if (!HealthNotificationHelper.canPost(applicationContext)) return Result.success()

        val store = WeightHistoryStore(applicationContext)
        if (store.shouldShowWeeklyReminder()) {
            HealthNotificationHelper.weightUpdateReminder(applicationContext)
            store.markWeeklyPromptShown()
        }
        return Result.success()
    }
}
