package com.example.healtapp.features.activity.presentation

import com.example.healtapp.data.network.dto.activity.ActivityDto
import java.time.LocalDate
import java.time.OffsetDateTime

data class DaySteps(
    val dateKey: String,
    val label: String,
    val steps: Int,
)

object ActivityStepsHelper {

    private val dayLabelFormatter = java.time.format.DateTimeFormatter.ofPattern("EE", java.util.Locale("ru", "RU"))

    fun activityDateKey(raw: String): String = raw.take(10)

    /**
     * Шаги за день из сохранённых записей: для «ходьбы» берём максимум (не сумму —
     * иначе дубли после каждого импорта Health Connect раздувают число).
     */
    fun sumStepsForDate(activities: List<ActivityDto>, dateKey: String): Int {
        val dayActivities = activities.filter { activityDateKey(it.start_time) == dateKey }
        val walkSteps = dayActivities
            .filter { isWalkLikeApi(it.activity_type) }
            .maxOfOrNull { it.steps ?: 0 }
        if (walkSteps != null && walkSteps > 0) return walkSteps
        return dayActivities.maxOfOrNull { it.steps ?: 0 } ?: 0
    }

    fun stepsToday(activities: List<ActivityDto>): Int =
        sumStepsForDate(activities, LocalDate.now().toString())

    fun weeklySteps(activities: List<ActivityDto>, days: Int = 7): List<DaySteps> {
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val key = date.toString()
            val label = if (offset == 0) {
                "Сегодня"
            } else {
                date.format(dayLabelFormatter).replaceFirstChar { it.uppercase() }
            }
            DaySteps(
                dateKey = key,
                label = label,
                steps = sumStepsForDate(activities, key),
            )
        }
    }

    fun trainingHistory(activities: List<ActivityDto>): List<ActivityDto> =
        activities.filter { !isWalkLikeApi(it.activity_type) }

    /** Все сессии из Health Connect (включая бег, силовую и т.д.), кроме агрегата «ходьба за день». */
    fun syncedFromHealthConnect(activities: List<ActivityDto>): List<ActivityDto> =
        activities
            .filter { isHealthConnectSource(it) }
            .filter { act ->
                val stepsOnlyWalk = isWalkLikeApi(act.activity_type) &&
                    (act.steps ?: 0) > 0 &&
                    (act.calories_burned ?: 0f) <= 0f &&
                    act.duration_minutes >= 60
                !stepsOnlyWalk
            }
            .sortedByDescending { it.start_time }

    fun isHealthConnectSource(activity: ActivityDto): Boolean {
        val src = activity.source.trim()
        if (src.equals("health_connect", ignoreCase = true)) return true
        if (src.contains("health", ignoreCase = true) && src.contains("connect", ignoreCase = true)) {
            return true
        }
        return activity.notes.orEmpty().contains("Health Connect", ignoreCase = true)
    }

    fun findTodayWalkRecord(activities: List<ActivityDto>): ActivityDto? {
        val today = LocalDate.now().toString()
        return activities
            .filter { isWalkLikeApi(it.activity_type) && activityDateKey(it.start_time) == today }
            .maxByOrNull { it.steps ?: 0 }
    }

    /**
     * Дубликаты тренировок HC: тот же тип в один день, близкое время (±2 ч) и длительность (±20%).
     */
    fun trainingDuplicateIdsToRemove(activities: List<ActivityDto>): List<Int> {
        val toRemove = mutableListOf<Int>()
        val trainings = activities.filter {
            isHealthConnectSource(it) && !isWalkLikeApi(it.activity_type)
        }
        val grouped = trainings.groupBy { activityDateKey(it.start_time) + "|" + it.activity_type.lowercase() }
        grouped.values.forEach { dayType ->
            if (dayType.size <= 1) return@forEach
            val sorted = dayType.sortedByDescending { it.calories_burned ?: 0f }
            val keep = sorted.first()
            sorted.drop(1).forEach { candidate ->
                val keepStart = parseStartTime(keep.start_time)
                val candStart = parseStartTime(candidate.start_time)
                val minutesApart = kotlin.math.abs(
                    java.time.Duration.between(keepStart, candStart).toMinutes(),
                )
                val durKeep = keep.duration_minutes.coerceAtLeast(1)
                val durCand = candidate.duration_minutes.coerceAtLeast(1)
                val durRatio = durCand.toFloat() / durKeep.toFloat()
                if (minutesApart <= 120 && durRatio in 0.8f..1.25f) {
                    toRemove.add(candidate.id)
                }
            }
        }
        return toRemove.distinct()
    }

    /** Оставляет одну запись «ходьба» на день с макс. шагами, остальные — id на удаление. */
    fun walkDuplicateIdsToRemove(activities: List<ActivityDto>): List<Int> {
        val toRemove = mutableListOf<Int>()
        activities
            .filter { isWalkLikeApi(it.activity_type) }
            .groupBy { activityDateKey(it.start_time) }
            .forEach { (_, dayWalks) ->
                if (dayWalks.size <= 1) return@forEach
                val keep = dayWalks.maxByOrNull { it.steps ?: 0 } ?: return@forEach
                dayWalks.filter { it.id != keep.id }.forEach { toRemove.add(it.id) }
            }
        return toRemove
    }

    fun parseStartTime(raw: String): java.time.LocalDateTime {
        return try {
            OffsetDateTime.parse(raw).toLocalDateTime()
        } catch (_: Exception) {
            try {
                java.time.LocalDateTime.parse(raw.take(19))
            } catch (_: Exception) {
                java.time.LocalDateTime.now()
            }
        }
    }
}
