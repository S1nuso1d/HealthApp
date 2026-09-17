package com.example.healtapp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.healtapp.MainActivity
import com.example.healtapp.data.preferences.LocalPillSchedule
import com.example.healtapp.data.preferences.LocalPillScheduleStore
import java.util.Calendar

object PillReminderScheduler {

    fun schedulePillReminder(
        context: Context,
        pillId: Int,
        name: String,
        dosage: String,
        timeOfDay: String,
        active: Boolean = true,
    ) {
        LocalPillScheduleStore.upsert(
            context,
            LocalPillSchedule(
                id = pillId,
                name = name,
                dosage = dosage,
                timeOfDay = timeOfDay,
                active = active,
            ),
        )
        if (!active) {
            cancelPillReminder(context, pillId, removeLocal = false)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PillReminderReceiver::class.java).apply {
            putExtra("PILL_ID", pillId)
            putExtra("PILL_NAME", name)
            putExtra("PILL_DOSAGE", dosage)
            putExtra("PILL_TIME", timeOfDay)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pillId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val parts = timeOfDay.split(":")
        if (parts.size < 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val showIntent = PendingIntent.getActivity(
            context,
            10_000 + pillId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(HealthNotificationHelper.EXTRA_NAV_ROUTE, "pills")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // setAlarmClock — самый надёжный путь для лекарств: не требует
        // SCHEDULE_EXACT_ALARM и меньше режется Doze.
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(calendar.timeInMillis, showIntent),
                pendingIntent,
            )
        } catch (_: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        }
    }

    fun cancelPillReminder(context: Context, pillId: Int, removeLocal: Boolean = true) {
        if (removeLocal) LocalPillScheduleStore.remove(context, pillId)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PillReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pillId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }

    /** Восстановление будильников из локального кэша (без сети). */
    fun restoreFromLocal(context: Context) {
        LocalPillScheduleStore.load(context).forEach { schedule ->
            if (schedule.active) {
                schedulePillReminder(
                    context = context,
                    pillId = schedule.id,
                    name = schedule.name,
                    dosage = schedule.dosage,
                    timeOfDay = schedule.timeOfDay,
                    active = true,
                )
            } else {
                cancelPillReminder(context, schedule.id, removeLocal = false)
            }
        }
    }

    fun needsExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return !alarmManager.canScheduleExactAlarms()
    }
}
