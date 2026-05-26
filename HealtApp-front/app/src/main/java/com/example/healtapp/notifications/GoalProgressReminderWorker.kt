package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.healtapp.data.healthconnect.HealthConnectReader
import com.example.healtapp.data.preferences.GoalNotificationPrefs
import com.example.healtapp.di.ReminderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GoalProgressReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        val prefs = entry.notificationPrefs()
        if (!prefs.current().goalAchievementNotifications) return Result.success()

        if (entry.tokenStorage().getToken() == null || entry.tokenStorage().isGuestMode()) {
            return Result.success()
        }
        if (!HealthNotificationHelper.canPost(applicationContext)) return Result.success()

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < 8 || hour > 22) return Result.success()

        val profile = entry.profileRepository().getMyProfile().getOrNull()
        val waterTarget = profile?.target_water_ml?.toInt() ?: 2500
        val stepsGoal = profile?.target_steps?.takeIf { it > 0 } ?: 10_000

        val waterMl = ReminderDataChecker.todayWaterMl(entry.hydrationRepository())
        val hcReader = HealthConnectReader(applicationContext)
        val steps = ReminderDataChecker.todaySteps(
            entry.activityRepository(),
            healthConnectReader = hcReader.takeIf { hcReader.canRequestPermissions() },
        )

        val goalPrefs = GoalNotificationPrefs(applicationContext)

        if (steps >= stepsGoal && !goalPrefs.wasStepsGoalNotifiedToday()) {
            HealthNotificationHelper.stepsGoalReached(applicationContext, steps, stepsGoal)
            goalPrefs.markStepsGoalNotifiedToday()
        }

        if (waterMl >= waterTarget && !goalPrefs.wasWaterGoalNotifiedToday()) {
            HealthNotificationHelper.waterGoalReached(applicationContext, waterMl, waterTarget)
            goalPrefs.markWaterGoalNotifiedToday()
        } else if (
            prefs.current().hydrationReminders &&
            hour in 16..19 &&
            waterTarget > 0 &&
            waterMl < (waterTarget * 0.5f).toInt()
        ) {
            HealthNotificationHelper.waterLowReminder(applicationContext, waterMl, waterTarget)
        }

        return Result.success()
    }

    companion object {
        const val PERIODIC_NAME = "goal_progress_periodic"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<GoalProgressReminderWorker>(4, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
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
