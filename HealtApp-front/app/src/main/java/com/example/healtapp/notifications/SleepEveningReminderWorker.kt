package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healtapp.di.ReminderEntryPoint
import dagger.hilt.android.EntryPointAccessors

class SleepEveningReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        val settings = entry.notificationPrefs().current()
        if (!settings.hydrationReminders && !settings.recommendationReminders) return Result.success()
        if (entry.tokenStorage().getToken() == null) {
            return Result.success()
        }
        if (!HealthNotificationHelper.canPost(applicationContext)) return Result.success()

        val bedtime = entry.notificationPrefs().current().let { settings ->
            if (settings.usualBedtimeHour != null && settings.usualBedtimeMinute != null) {
                "%02d:%02d".format(settings.usualBedtimeHour, settings.usualBedtimeMinute)
            } else {
                null
            }
        }
        HealthNotificationHelper.sleepEveningReminder(applicationContext, bedtime)
        ReminderScheduler.rescheduleSleepEvening(applicationContext, enabled = true)
        return Result.success()
    }
}
