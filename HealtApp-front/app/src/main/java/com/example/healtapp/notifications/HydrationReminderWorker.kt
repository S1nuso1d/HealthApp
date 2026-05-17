package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healtapp.data.preferences.NotificationPrefs

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
        HealthNotificationHelper.hydrationReminder(applicationContext)
        return Result.success()
    }
}
