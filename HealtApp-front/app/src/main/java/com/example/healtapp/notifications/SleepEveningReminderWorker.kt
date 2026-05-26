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
        if (!entry.notificationPrefs().current().hydrationReminders) return Result.success()
        if (entry.tokenStorage().getToken() == null || entry.tokenStorage().isGuestMode()) {
            return Result.success()
        }
        if (!HealthNotificationHelper.canPost(applicationContext)) return Result.success()

        HealthNotificationHelper.sleepEveningReminder(applicationContext)
        ReminderScheduler.rescheduleSleepEvening(applicationContext, enabled = true)
        return Result.success()
    }
}
