package com.example.healtapp.core.common

import com.example.healtapp.data.network.dto.cycle.CycleEntryDto
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object CycleCalculator {

    enum class Phase(val labelRu: String) {
        MENSTRUAL("Менструация"),
        FOLLICULAR("Фолликулярная"),
        OVULATION("Овуляция"),
        LUTEAL("Лютеиновая"),
        UNKNOWN("Цикл"),
    }

    data class Insight(
        val cycleDay: Int?,
        val averageCycleLength: Int,
        val periodLength: Int,
        val daysUntilNextPeriod: Int?,
        val phase: Phase,
        val nextPeriodDate: LocalDate?,
        val fertileWindowStart: LocalDate?,
        val fertileWindowEnd: LocalDate?,
        val periodDates: Set<LocalDate>,
        val predictedPeriodDates: Set<LocalDate>,
    )

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value, dateFormatter) }.getOrNull()

    fun buildInsight(entries: List<CycleEntryDto>, today: LocalDate = LocalDate.now()): Insight {
        val sorted = entries.mapNotNull { entry ->
            parseDate(entry.start_date)?.let { start ->
                val end = entry.end_date?.let(::parseDate) ?: start.plusDays(4)
                start to end
            }
        }.sortedByDescending { it.first }

        if (sorted.isEmpty()) {
            return Insight(
                cycleDay = null,
                averageCycleLength = 28,
                periodLength = 5,
                daysUntilNextPeriod = null,
                phase = Phase.UNKNOWN,
                nextPeriodDate = null,
                fertileWindowStart = null,
                fertileWindowEnd = null,
                periodDates = emptySet(),
                predictedPeriodDates = emptySet(),
            )
        }

        val lastStart = sorted.first().first
        val lastEnd = sorted.first().second
        val periodLength = sorted.map { (start, end) ->
            ChronoUnit.DAYS.between(start, end).toInt() + 1
        }.average().roundToInt().coerceIn(3, 8)

        val cycleLengths = sorted.zipWithNext { current, previous ->
            ChronoUnit.DAYS.between(previous.first, current.first).toInt()
        }.filter { it in 20..40 }

        val averageCycleLength = if (cycleLengths.isNotEmpty()) {
            cycleLengths.average().roundToInt()
        } else {
            28
        }

        val cycleDay = (ChronoUnit.DAYS.between(lastStart, today).toInt() + 1).coerceAtLeast(1)
        val nextPeriodDate = lastStart.plusDays(averageCycleLength.toLong())
        val daysUntilNextPeriod = ChronoUnit.DAYS.between(today, nextPeriodDate).toInt().coerceAtLeast(0)

        val ovulationDay = lastStart.plusDays((averageCycleLength - 14).toLong())
        val fertileWindowStart = ovulationDay.minusDays(5)
        val fertileWindowEnd = ovulationDay.plusDays(1)

        val phase = when {
            !today.isAfter(lastEnd) -> Phase.MENSTRUAL
            today.isBefore(fertileWindowStart) -> Phase.FOLLICULAR
            !today.isAfter(fertileWindowEnd) -> Phase.OVULATION
            today.isBefore(nextPeriodDate) -> Phase.LUTEAL
            else -> Phase.MENSTRUAL
        }

        val periodDates = buildPeriodDates(sorted)
        val predictedPeriodDates = buildPredictedPeriodDates(nextPeriodDate, periodLength)

        return Insight(
            cycleDay = cycleDay,
            averageCycleLength = averageCycleLength,
            periodLength = periodLength,
            daysUntilNextPeriod = daysUntilNextPeriod,
            phase = phase,
            nextPeriodDate = nextPeriodDate,
            fertileWindowStart = fertileWindowStart,
            fertileWindowEnd = fertileWindowEnd,
            periodDates = periodDates,
            predictedPeriodDates = predictedPeriodDates,
        )
    }

    fun monthDays(month: YearMonth, insight: Insight): List<CalendarDay> {
        val firstDay = month.atDay(1)
        val length = month.lengthOfMonth()
        return (1..length).map { day ->
            val date = month.atDay(day)
            CalendarDay(
                date = date,
                isToday = date == LocalDate.now(),
                isLoggedPeriod = insight.periodDates.contains(date),
                isPredictedPeriod = insight.predictedPeriodDates.contains(date),
                isFertile = isInRange(date, insight.fertileWindowStart, insight.fertileWindowEnd),
            )
        }
    }

    private fun buildPeriodDates(sorted: List<Pair<LocalDate, LocalDate>>): Set<LocalDate> {
        val dates = mutableSetOf<LocalDate>()
        sorted.take(6).forEach { (start, end) ->
            var current = start
            while (!current.isAfter(end)) {
                dates += current
                current = current.plusDays(1)
            }
        }
        return dates
    }

    private fun buildPredictedPeriodDates(nextPeriodDate: LocalDate, periodLength: Int): Set<LocalDate> {
        return (0 until periodLength).map { nextPeriodDate.plusDays(it.toLong()) }.toSet()
    }

    private fun isInRange(date: LocalDate, start: LocalDate?, end: LocalDate?): Boolean {
        if (start == null || end == null) return false
        return !date.isBefore(start) && !date.isAfter(end)
    }

    data class CalendarDay(
        val date: LocalDate,
        val isToday: Boolean,
        val isLoggedPeriod: Boolean,
        val isPredictedPeriod: Boolean,
        val isFertile: Boolean,
    )
}
