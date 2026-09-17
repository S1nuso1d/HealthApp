package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

/**
 * Воскресный разбор недели: локальное уведомление, которое открывает экран итогов.
 *
 * FCM на сервере нет — пуш живёт на устройстве, как остальные напоминания.
 */
class WeeklyReviewReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (HealthNotificationHelper.canPost(applicationContext)) {
            HealthNotificationHelper.weeklyReview(applicationContext)
        }
        ReminderScheduler.rescheduleWeeklyReview(applicationContext, enabled = true)
        return Result.success()
    }
}

internal fun delayUntilNextSundayEvening(): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        set(Calendar.HOUR_OF_DAY, 19)
        set(Calendar.MINUTE, 0)
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }
    if (!target.after(now)) {
        target.add(Calendar.WEEK_OF_YEAR, 1)
    }
    return target.timeInMillis - now.timeInMillis
}
