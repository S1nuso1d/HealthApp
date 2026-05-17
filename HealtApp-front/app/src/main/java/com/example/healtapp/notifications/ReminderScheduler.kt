package com.example.healtapp.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.healtapp.data.preferences.NotificationPrefs
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

object ReminderScheduler {

    private const val HYDRATION_PERIODIC = "hydration_periodic"
    private const val MEAL_BREAKFAST = "meal_breakfast"
    private const val MEAL_LUNCH = "meal_lunch"
    private const val MEAL_DINNER = "meal_dinner"
    private const val RECOMMENDATION_DAILY = "recommendation_daily"

    fun rescheduleAll(context: Context) {
        val prefs = NotificationPrefs(context)
        val settings = runBlocking { prefs.current() }
        rescheduleHydration(context, settings.hydrationReminders)
        rescheduleMeals(context, settings.mealReminders)
        rescheduleRecommendations(context, settings.recommendationReminders)
    }

    fun rescheduleHydration(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(HYDRATION_PERIODIC)
            return
        }
        val request = PeriodicWorkRequestBuilder<HydrationReminderWorker>(3, TimeUnit.HOURS)
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(
            HYDRATION_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun rescheduleMeals(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(MEAL_BREAKFAST)
            wm.cancelUniqueWork(MEAL_LUNCH)
            wm.cancelUniqueWork(MEAL_DINNER)
            return
        }
        scheduleMealAt(context, MEAL_BREAKFAST, "Завтрак", 8, 0)
        scheduleMealAt(context, MEAL_LUNCH, "Обед", 13, 0)
        scheduleMealAt(context, MEAL_DINNER, "Ужин", 19, 0)
    }

    private fun scheduleMealAt(
        context: Context,
        workName: String,
        mealLabel: String,
        hour: Int,
        minute: Int,
    ) {
        val delayMs = delayUntilNext(hour, minute)
        val request = OneTimeWorkRequestBuilder<MealReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(MealReminderWorker.KEY_MEAL_LABEL to mealLabel))
            .addTag(workName)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun rescheduleRecommendations(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(RECOMMENDATION_DAILY)
            return
        }
        val settings = runBlocking { NotificationPrefs(context).current() }
        scheduleRecommendationsAt(
            context = context,
            hour = settings.recommendationHour,
            minute = settings.recommendationMinute,
        )
    }

    fun scheduleRecommendationsAt(context: Context, hour: Int, minute: Int) {
        val delayMs = delayUntilNext(hour, minute)
        val request = OneTimeWorkRequestBuilder<RecommendationReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            RECOMMENDATION_DAILY,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    internal fun delayUntilNext(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
