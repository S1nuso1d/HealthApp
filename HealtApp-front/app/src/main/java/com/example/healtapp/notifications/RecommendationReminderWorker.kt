package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.healtapp.data.preferences.NotificationPrefs
import java.util.concurrent.TimeUnit

class RecommendationReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = NotificationPrefs(applicationContext)
        if (!prefs.current().recommendationReminders) return Result.success()
        if (HealthNotificationHelper.canPost(applicationContext)) {
            HealthNotificationHelper.recommendationReminder(applicationContext)
        }
        val settings = prefs.current()
        ReminderScheduler.scheduleRecommendationsAt(
            context = applicationContext,
            hour = settings.recommendationHour,
            minute = settings.recommendationMinute,
        )
        return Result.success()
    }
}
