package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.healtapp.data.preferences.NotificationPrefs
import com.example.healtapp.di.ReminderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit

/** Напоминание, если приём пищи за день не записан к контрольному времени. */
class MissedMealReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        val prefs = entry.notificationPrefs()
        if (!prefs.current().missedMealChecks) return rescheduleAndSuccess()
        if (entry.tokenStorage().getToken() == null || entry.tokenStorage().isGuestMode()) {
            return rescheduleAndSuccess()
        }
        if (!HealthNotificationHelper.canPost(applicationContext)) return rescheduleAndSuccess()

        val apiType = inputData.getString(KEY_MEAL_API_TYPE) ?: "lunch"
        val label = inputData.getString(KEY_MEAL_LABEL) ?: "Обед"
        if (!ReminderDataChecker.hasMealTypeToday(entry.mealRepository(), apiType)) {
            HealthNotificationHelper.missedMealReminder(applicationContext, label)
        }
        return rescheduleAndSuccess()
    }

    private fun rescheduleAndSuccess(): Result {
        val apiType = inputData.getString(KEY_MEAL_API_TYPE) ?: return Result.success()
        val label = inputData.getString(KEY_MEAL_LABEL) ?: return Result.success()
        val (hour, minute, workName) = when (apiType) {
            "breakfast" -> Triple(10, 30, WORK_BREAKFAST)
            "lunch" -> Triple(15, 0, WORK_LUNCH)
            else -> Triple(21, 0, WORK_DINNER)
        }
        val delayMs = ReminderScheduler.delayUntilNext(hour, minute)
        val request = OneTimeWorkRequestBuilder<MissedMealReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    KEY_MEAL_API_TYPE to apiType,
                    KEY_MEAL_LABEL to label,
                ),
            )
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return Result.success()
    }

    companion object {
        const val KEY_MEAL_API_TYPE = "meal_api_type"
        const val KEY_MEAL_LABEL = "meal_label"
        const val WORK_BREAKFAST = "missed_meal_breakfast"
        const val WORK_LUNCH = "missed_meal_lunch"
        const val WORK_DINNER = "missed_meal_dinner"
    }
}
