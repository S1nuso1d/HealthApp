package com.example.healtapp.core.common

import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.data.network.dto.meal.MealDto
import com.example.healtapp.data.network.dto.sleep.SleepDto
import com.example.healtapp.features.activity.presentation.ActivityStepsHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Итоги календарной недели (пн–вс). В среднее попадают только дни с введёнными данными (> 0).
 */
object WeeklySummaryCalculator {

    data class Metric(
        val key: String,
        val label: String,
        val averageDisplay: String,
        val daysLogged: Int,
        val daysInPeriod: Int,
        val hint: String,
    )

    data class Result(
        val weekStart: LocalDate,
        val weekEnd: LocalDate,
        val periodLabel: String,
        val metrics: List<Metric>,
    ) {
        val hasAnyData: Boolean get() = metrics.any { it.daysLogged > 0 }
    }

    private val labelFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))

    fun currentWeekMonday(today: LocalDate = LocalDate.now()): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun compute(
        today: LocalDate = LocalDate.now(),
        sleeps: List<SleepDto>,
        hydrationHistory: List<HydrationDto>,
        activityHistory: List<ActivityDto>,
        mealHistory: List<MealDto>,
    ): Result {
        val weekStart = currentWeekMonday(today)
        val weekEnd = weekStart.plusDays(6)
        val periodEnd = if (today.isBefore(weekEnd)) today else weekEnd
        val daysInPeriod = (periodEnd.toEpochDay() - weekStart.toEpochDay() + 1).toInt().coerceAtLeast(1)

        val periodLabel = buildString {
            append(weekStart.format(labelFormatter))
            append(" — ")
            append(weekEnd.format(labelFormatter))
        }

        val sleepByDay = mutableMapOf<LocalDate, Float>()
        for (s in sleeps) {
            val hours = s.duration_hours ?: continue
            if (hours <= 0f) continue
            val day = parseDate(s.sleep_end) ?: continue
            if (day !in weekStart..periodEnd) continue
            sleepByDay[day] = hours
        }

        val waterByDay = mutableMapOf<LocalDate, Float>()
        for (h in hydrationHistory) {
            val day = parseDate(h.record_time) ?: continue
            if (day !in weekStart..periodEnd) continue
            val ml = h.amount_ml
            if (ml <= 0) continue
            waterByDay[day] = (waterByDay[day] ?: 0f) + ml
        }

        val stepsByDay = mutableMapOf<LocalDate, Int>()
        for (day in weekStart.datesUntil(periodEnd.plusDays(1))) {
            val key = day.toString()
            val steps = ActivityStepsHelper.sumStepsForDate(activityHistory, key)
            if (steps > 0) stepsByDay[day] = steps
        }

        val caloriesByDay = mutableMapOf<LocalDate, Int>()
        for (m in mealHistory) {
            val day = parseDate(m.meal_time) ?: continue
            if (day !in weekStart..periodEnd) continue
            val kcal = (m.calories ?: 0f).toInt()
            if (kcal <= 0) continue
            caloriesByDay[day] = (caloriesByDay[day] ?: 0) + kcal
        }

        val metrics = listOf(
            buildMetric("sleep", "Сон", sleepByDay.values, daysInPeriod) { v ->
                "${"%.1f".format(v).replace('.', ',')} ч / ночь"
            },
            buildMetric("water", "Вода", waterByDay.values, daysInPeriod) { v ->
                "${"%,d".format(v.roundToInt()).replace(',', ' ')} мл / день"
            },
            buildMetric("steps", "Шаги", stepsByDay.values.map { it.toFloat() }, daysInPeriod) { v ->
                "${"%,d".format(v.roundToInt()).replace(',', ' ')} / день"
            },
            buildMetric("calories", "Калории", caloriesByDay.values.map { it.toFloat() }, daysInPeriod) { v ->
                "${"%,d".format(v.roundToInt()).replace(',', ' ')} ккал / день"
            },
        )

        return Result(
            weekStart = weekStart,
            weekEnd = weekEnd,
            periodLabel = periodLabel,
            metrics = metrics,
        )
    }

    private fun buildMetric(
        key: String,
        label: String,
        values: Collection<Float>,
        daysInPeriod: Int,
        formatAvg: (Float) -> String,
    ): Metric {
        val filtered = values.filter { it > 0f }
        val daysLogged = filtered.size
        val avg = if (filtered.isEmpty()) null else filtered.sum() / filtered.size
        return Metric(
            key = key,
            label = label,
            averageDisplay = avg?.let(formatAvg) ?: "—",
            daysLogged = daysLogged,
            daysInPeriod = daysInPeriod,
            hint = if (daysLogged == 0) {
                "Нет записей на этой неделе"
            } else {
                "Среднее за $daysLogged ${daysWord(daysLogged)} с данными"
            },
        )
    }

    private fun parseDate(iso: String): LocalDate? =
        runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()

    private fun daysWord(n: Int): String = when {
        n % 100 in 11..14 -> "дней"
        n % 10 == 1 -> "день"
        n % 10 in 2..4 -> "дня"
        else -> "дней"
    }
}
