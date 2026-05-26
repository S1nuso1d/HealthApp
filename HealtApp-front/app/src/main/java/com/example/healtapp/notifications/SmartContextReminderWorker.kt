package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.healtapp.di.ReminderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Контекстные напоминания: отставание по шагам, мало воды, пропущенный приём пищи.
 */
class SmartContextReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        val prefs = entry.notificationPrefs()
        if (!prefs.current().goalAchievementNotifications && !prefs.current().hydrationReminders) {
            return Result.success()
        }
        if (entry.tokenStorage().getToken() == null || entry.tokenStorage().isGuestMode()) {
            return Result.success()
        }
        if (!HealthNotificationHelper.canPost(applicationContext)) return Result.success()

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < 9 || hour > 21) return Result.success()

        val profile = entry.profileRepository().getMyProfile().getOrNull()
        val waterTarget = profile?.target_water_ml?.toInt() ?: 2500
        val stepsGoal = profile?.target_steps?.takeIf { it > 0 } ?: 10_000

        val waterMl = ReminderDataChecker.todayWaterMl(entry.hydrationRepository())
        val steps = ReminderDataChecker.todaySteps(
            entry.activityRepository(),
            healthConnectReader = null,
        )

        if (prefs.current().hydrationReminders && waterTarget > 0 && waterMl < (waterTarget * 0.45f).toInt() && hour in 12..18) {
            HealthNotificationHelper.waterLowReminder(applicationContext, waterMl, waterTarget)
        }

        if (hour in 14..20 && stepsGoal > 0) {
            val expectedByNow = (stepsGoal * (hour - 8).coerceAtLeast(1) / 14f).toInt()
            if (steps < expectedByNow * 0.55f) {
                val left = (stepsGoal - steps).coerceAtLeast(0)
                HealthNotificationHelper.stepsBehindPace(
                    applicationContext,
                    steps,
                    stepsGoal,
                    left,
                )
            }
        }

        if (prefs.current().missedMealChecks && hour in 14..16) {
            val hasLunch = ReminderDataChecker.hasMealTypeToday(entry.mealRepository(), "lunch")
            if (!hasLunch) {
                HealthNotificationHelper.missedMealReminder(applicationContext, "Обед")
            }
        }

        return Result.success()
    }

    companion object {
        const val PERIODIC_NAME = "smart_context_periodic"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SmartContextReminderWorker>(3, TimeUnit.HOURS)
                .setInitialDelay(45, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NAME)
        }
    }
}
