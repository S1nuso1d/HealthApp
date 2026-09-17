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
        rescheduleMissedMealChecks(context, settings.missedMealChecks)
        rescheduleGoalChecks(context, settings.goalAchievementNotifications)
        rescheduleSleepEvening(context, settings.hydrationReminders || settings.recommendationReminders)
        rescheduleWeightReminder(context, settings.goalAchievementNotifications)
        rescheduleSmartContext(context, settings.goalAchievementNotifications || settings.hydrationReminders)
        rescheduleHourlyWaterReminder(context, settings.hydrationReminders)
        rescheduleAiCoach(context, true) // Always enabled or bind to a specific setting
        rescheduleWeeklyReview(context, enabled = true)
    }

    private const val AI_COACH_PERIODIC = "ai_coach_periodic"

    fun rescheduleAiCoach(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(AI_COACH_PERIODIC)
            return
        }
        val request = PeriodicWorkRequestBuilder<AiCoachWorker>(3, TimeUnit.HOURS)
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(
            AI_COACH_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private const val WEEKLY_REVIEW = "weekly_review"

    fun rescheduleWeeklyReview(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WEEKLY_REVIEW)
            return
        }
        val request = OneTimeWorkRequestBuilder<WeeklyReviewReminderWorker>()
            .setInitialDelay(delayUntilNextSundayEvening(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork(WEEKLY_REVIEW, ExistingWorkPolicy.REPLACE, request)
    }

    private fun rescheduleHourlyWaterReminder(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        val workName = "hourly_water_reminder"
        if (!enabled) {
            wm.cancelUniqueWork(workName)
            return
        }
        val request = PeriodicWorkRequestBuilder<HydrationReminderWorker>(1, TimeUnit.HOURS)
            .build()
        wm.enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun rescheduleSmartContext(context: Context, enabled: Boolean) {
        if (!enabled) {
            SmartContextReminderWorker.cancelPeriodic(context)
            return
        }
        SmartContextReminderWorker.schedulePeriodic(context)
    }

    fun rescheduleMissedMealChecks(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(MissedMealReminderWorker.WORK_BREAKFAST)
            wm.cancelUniqueWork(MissedMealReminderWorker.WORK_LUNCH)
            wm.cancelUniqueWork(MissedMealReminderWorker.WORK_DINNER)
            return
        }
        scheduleMissedMeal(context, MissedMealReminderWorker.WORK_BREAKFAST, "breakfast", "Завтрак", 10, 30)
        scheduleMissedMeal(context, MissedMealReminderWorker.WORK_LUNCH, "lunch", "Обед", 15, 0)
        scheduleMissedMeal(context, MissedMealReminderWorker.WORK_DINNER, "dinner", "Ужин", 21, 0)
    }

    private fun scheduleMissedMeal(
        context: Context,
        workName: String,
        apiType: String,
        label: String,
        hour: Int,
        minute: Int,
    ) {
        val delayMs = delayUntilNext(hour, minute)
        val request = OneTimeWorkRequestBuilder<MissedMealReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    MissedMealReminderWorker.KEY_MEAL_API_TYPE to apiType,
                    MissedMealReminderWorker.KEY_MEAL_LABEL to label,
                ),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun rescheduleGoalChecks(context: Context, enabled: Boolean) {
        if (!enabled) {
            GoalProgressReminderWorker.cancelPeriodic(context)
            return
        }
        GoalProgressReminderWorker.schedulePeriodic(context)
    }

    fun rescheduleHydration(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(HYDRATION_PERIODIC)
            return
        }
        val request = PeriodicWorkRequestBuilder<HydrationReminderWorker>(2, TimeUnit.HOURS)
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

    private const val SLEEP_EVENING = "sleep_evening"

    fun rescheduleSleepEvening(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(SLEEP_EVENING)
            return
        }
        val settings = runBlocking { NotificationPrefs(context).current() }
        val usualHour = settings.usualBedtimeHour
        val usualMinute = settings.usualBedtimeMinute
        val (hour, minute) = if (usualHour != null && usualMinute != null) {
            val wrapped = ((usualHour * 60 + usualMinute - 30) % (24 * 60) + 24 * 60) % (24 * 60)
            wrapped / 60 to wrapped % 60
        } else {
            NotificationPrefs.DEFAULT_BEDTIME_NUDGE_HOUR to NotificationPrefs.DEFAULT_BEDTIME_NUDGE_MINUTE
        }
        val request = OneTimeWorkRequestBuilder<SleepEveningReminderWorker>()
            .setInitialDelay(delayUntilNext(hour, minute), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork(SLEEP_EVENING, ExistingWorkPolicy.REPLACE, request)
    }

    private const val WEIGHT_WEEKLY = "weight_weekly"

    fun rescheduleWeightReminder(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WEIGHT_WEEKLY)
            return
        }
        val request = PeriodicWorkRequestBuilder<WeightReminderWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(12, TimeUnit.HOURS)
            .build()
        wm.enqueueUniquePeriodicWork(
            WEIGHT_WEEKLY,
            ExistingPeriodicWorkPolicy.UPDATE,
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
