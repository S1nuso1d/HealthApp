package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.healtapp.data.preferences.LocalPillSchedule
import com.example.healtapp.data.preferences.LocalPillScheduleStore
import com.example.healtapp.di.ReminderEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * Восстанавливает будильники таблеток после перезагрузки / обновления приложения.
 * Сначала ставит из локального кэша, затем синхронизирует список с сервером.
 */
class PillReminderRestoreWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        PillReminderScheduler.restoreFromLocal(applicationContext)

        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        val tokenStorage = entry.tokenStorage()
        if (tokenStorage.getToken() == null) {
            return Result.success()
        }

        val pills = entry.healthRepository().getPillReminders().getOrElse {
            return Result.retry()
        }

        val schedules = pills.mapNotNull { pill ->
            val id = pill.id ?: return@mapNotNull null
            LocalPillSchedule(
                id = id,
                name = pill.name,
                dosage = pill.dosage,
                timeOfDay = pill.timeOfDay,
                active = pill.isActive,
            )
        }
        LocalPillScheduleStore.replaceAll(applicationContext, schedules)
        schedules.forEach { schedule ->
            PillReminderScheduler.schedulePillReminder(
                context = applicationContext,
                pillId = schedule.id,
                name = schedule.name,
                dosage = schedule.dosage,
                timeOfDay = schedule.timeOfDay,
                active = schedule.active,
            )
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "pill_reminder_restore"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<PillReminderRestoreWorker>()
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
