package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.healtapp.data.preferences.NotificationPrefs
import com.example.healtapp.di.ReminderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit

class MealReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = NotificationPrefs(applicationContext)
        if (!prefs.current().mealReminders) return Result.success()
        if (!HealthNotificationHelper.canPost(applicationContext)) {
            rescheduleSelf()
            return Result.success()
        }
        val label = inputData.getString(KEY_MEAL_LABEL) ?: "Обед"
        val apiType = when (label) {
            "Завтрак" -> "breakfast"
            "Обед" -> "lunch"
            else -> "dinner"
        }
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        if (entry.tokenStorage().getToken() != null && !entry.tokenStorage().isGuestMode()) {
            if (!ReminderDataChecker.hasMealTypeToday(entry.mealRepository(), apiType)) {
                HealthNotificationHelper.mealReminder(applicationContext, label)
            }
        } else {
            HealthNotificationHelper.mealReminder(applicationContext, label)
        }
        rescheduleSelf()
        return Result.success()
    }

    private fun rescheduleSelf() {
        val label = inputData.getString(KEY_MEAL_LABEL) ?: return
        val (hour, minute) = when (label) {
            "Завтрак" -> 8 to 0
            "Обед" -> 13 to 0
            else -> 19 to 0
        }
        val workName = when (label) {
            "Завтрак" -> "meal_breakfast"
            "Обед" -> "meal_lunch"
            else -> "meal_dinner"
        }
        val delayMs = ReminderScheduler.delayUntilNext(hour, minute)
        val request = OneTimeWorkRequestBuilder<MealReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_MEAL_LABEL to label))
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(workName, androidx.work.ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val KEY_MEAL_LABEL = "meal_label"
    }
}
