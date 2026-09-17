package com.example.healtapp.features.activity.presentation

import java.time.LocalTime

object CircadianTiming {
    private const val WINDOW_BEFORE_MIN = 180
    private const val WINDOW_AFTER_MIN = 30
    private const val TRAINING_CUTOFF_BEFORE_BED_MIN = 180

    fun clockMinutes(hour: Int, minute: Int): Int {
        var minutes = hour * 60 + minute
        if (hour < 5) minutes += 24 * 60
        return minutes
    }

    fun formatBedtime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

    fun isNearUsualBedtime(
        usualHour: Int?,
        usualMinute: Int?,
        now: LocalTime = LocalTime.now(),
    ): Boolean {
        if (usualHour == null || usualMinute == null) return false
        val usual = clockMinutes(usualHour, usualMinute)
        val start = clockMinutes(now.hour, now.minute)
        val delta = usual - start
        return delta in -WINDOW_AFTER_MIN..WINDOW_BEFORE_MIN
    }

    fun workoutNearBedtimeWarning(
        usualHour: Int?,
        usualMinute: Int?,
        intensity: String?,
        now: LocalTime = LocalTime.now(),
    ): String? {
        if (!isNearUsualBedtime(usualHour, usualMinute, now)) return null
        if (intensity.equals("Низкая", ignoreCase = true) || intensity.equals("low", ignoreCase = true)) {
            return null
        }
        val label = formatBedtime(usualHour!!, usualMinute!!)
        return "Обычно вы засыпаете около $label. Тренировка в этом окне часто мешает " +
            "расслабиться — лучше перенести нагрузку на день, вечером оставить прогулку."
    }

    fun eveningWindowHint(
        usualHour: Int?,
        usualMinute: Int?,
        now: LocalTime = LocalTime.now(),
    ): String? {
        if (!hitsTrainingSleepWindow(usualHour, usualMinute, now)) return null
        val label = if (usualHour != null) {
            formatBedtime(usualHour, usualMinute ?: 0)
        } else {
            "обычного отбоя"
        }
        return "Окно тренировки закрыто: до $label меньше трёх часов."
    }

    fun hitsTrainingSleepWindow(
        usualHour: Int?,
        usualMinute: Int?,
        now: LocalTime = LocalTime.now(),
    ): Boolean {
        if (usualHour == null) return now.hour >= 21
        val usual = clockMinutes(usualHour, usualMinute ?: 0)
        val start = clockMinutes(now.hour, now.minute)
        val delta = usual - start
        return delta in -WINDOW_AFTER_MIN..TRAINING_CUTOFF_BEFORE_BED_MIN
    }

    fun lateGpsConfirmMessage(
        usualHour: Int?,
        usualMinute: Int?,
        now: LocalTime = LocalTime.now(),
    ): String? {
        if (!hitsTrainingSleepWindow(usualHour, usualMinute, now)) return null
        val label = if (usualHour != null) {
            formatBedtime(usualHour, usualMinute ?: 0)
        } else {
            "обычного отбоя"
        }
        return "GPS-тренировка сейчас бьёт по сну: до отбоя ($label) меньше трёх часов. Всё равно начать?"
    }
}
