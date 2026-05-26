package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healtapp.data.preferences.NotificationPrefs
import com.example.healtapp.di.ReminderEntryPoint
import dagger.hilt.android.EntryPointAccessors

class HydrationReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = NotificationPrefs(applicationContext)
        if (!prefs.current().hydrationReminders) return Result.success()
        if (!HealthNotificationHelper.canPost(applicationContext)) return Result.success()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour < 8 || hour > 22) return Result.success()

        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        if (entry.tokenStorage().getToken() != null && !entry.tokenStorage().isGuestMode()) {
            val profile = entry.profileRepository().getMyProfile().getOrNull()
            val target = profile?.target_water_ml?.toInt() ?: 2500
            val current = ReminderDataChecker.todayWaterMl(entry.hydrationRepository())
            if (current >= target) return Result.success()
            if (current < (target * 0.85f).toInt()) {
                HealthNotificationHelper.hydrationReminder(applicationContext)
            }
        } else {
            HealthNotificationHelper.hydrationReminder(applicationContext)
        }
        return Result.success()
    }
}
