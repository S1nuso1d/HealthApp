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

    data class MoodInsight(
        val label: String,
        val description: String,
    )

    data class Insight(
        val cycleDay: Int?,
        val averageCycleLength: Int,
        val periodLength: Int,
        val daysUntilNextPeriod: Int?,
        val phase: Phase,
        val nextPeriodDate: LocalDate?,
        val ovulationDate: LocalDate?,
        val fertileWindowStart: LocalDate?,
        val fertileWindowEnd: LocalDate?,
        val periodDates: Set<LocalDate>,
        val predictedPeriodDates: Set<LocalDate>,
        val predictedOvulationDates: Set<LocalDate>,
        val fertileDates: Set<LocalDate>,
        val todayMood: MoodInsight,
    )

    data class CycleLengthPoint(
        val lengthDays: Int,
        val cycleStart: LocalDate,
        val periodLengthDays: Int,
    )

    data class CycleStats(
        val previousCycleLength: Int?,
        val previousPeriodLength: Int?,
        val averageCycleLength: Int,
        val averagePeriodLength: Int,
        val cycleLengthMin: Int?,
        val cycleLengthMax: Int?,
        val cycleVariationDays: Int?,
        val recentCycleLengths: List<Int>,
        val chartPoints: List<CycleLengthPoint>,
    )

    data class CalendarDay(
        val date: LocalDate,
        val isToday: Boolean,
        val isLoggedPeriod: Boolean,
        val isPredictedPeriod: Boolean,
        val isOvulation: Boolean,
        val isFertile: Boolean,
        val phase: Phase,
        val cycleDay: Int?,
        val mood: MoodInsight?,
    )

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value, dateFormatter) }.getOrNull()

    fun buildInsight(entries: List<CycleEntryDto>, today: LocalDate = LocalDate.now()): Insight {
        val sorted = parseSortedPeriods(entries)

        if (sorted.isEmpty()) {
            return Insight(
                cycleDay = null,
                averageCycleLength = 28,
                periodLength = 5,
                daysUntilNextPeriod = null,
                phase = Phase.UNKNOWN,
                nextPeriodDate = null,
                ovulationDate = null,
                fertileWindowStart = null,
                fertileWindowEnd = null,
                periodDates = emptySet(),
                predictedPeriodDates = emptySet(),
                predictedOvulationDates = emptySet(),
                fertileDates = emptySet(),
                todayMood = moodForPhase(Phase.UNKNOWN, null, 28, 5),
            )
        }

        val stats = buildCycleStats(entries)
        val lastStart = sorted.first().first
        val periodLength = stats.averagePeriodLength
        val averageCycleLength = stats.averageCycleLength

        val cycleDay = (ChronoUnit.DAYS.between(lastStart, today).toInt() + 1).coerceAtLeast(1)
        val nextPeriodDate = lastStart.plusDays(averageCycleLength.toLong())
        val daysUntilNextPeriod = ChronoUnit.DAYS.between(today, nextPeriodDate).toInt().coerceAtLeast(0)

        val ovulationDate = lastStart.plusDays((averageCycleLength - 14).toLong().coerceAtLeast(periodLength.toLong()))
        val fertileWindowStart = ovulationDate.minusDays(5)
        val fertileWindowEnd = ovulationDate.plusDays(1)

        val phase = phaseForDate(
            date = today,
            lastPeriodStart = lastStart,
            averageCycleLength = averageCycleLength,
            periodLength = periodLength,
        )

        val horizonEnd = today.plusMonths(4)
        val periodDates = buildPeriodDates(sorted)
        val predictedPeriodDates = buildPredictedPeriodDates(
            firstStart = nextPeriodDate,
            periodLength = periodLength,
            cycleLength = averageCycleLength,
            until = horizonEnd,
        )
        val predictedOvulationDates = buildPredictedOvulationDates(
            anchorStart = lastStart,
            cycleLength = averageCycleLength,
            until = horizonEnd,
        )
        val fertileDates = buildFertileDates(predictedOvulationDates)

        return Insight(
            cycleDay = cycleDay,
            averageCycleLength = averageCycleLength,
            periodLength = periodLength,
            daysUntilNextPeriod = daysUntilNextPeriod,
            phase = phase,
            nextPeriodDate = nextPeriodDate,
            ovulationDate = ovulationDate,
            fertileWindowStart = fertileWindowStart,
            fertileWindowEnd = fertileWindowEnd,
            periodDates = periodDates,
            predictedPeriodDates = predictedPeriodDates,
            predictedOvulationDates = predictedOvulationDates,
            fertileDates = fertileDates,
            todayMood = moodForPhase(phase, cycleDay, averageCycleLength, periodLength),
        )
    }

    fun buildCycleStats(entries: List<CycleEntryDto>): CycleStats {
        val sorted = parseSortedPeriods(entries)
        if (sorted.isEmpty()) {
            return CycleStats(
                previousCycleLength = null,
                previousPeriodLength = null,
                averageCycleLength = 28,
                averagePeriodLength = 5,
                cycleLengthMin = null,
                cycleLengthMax = null,
                cycleVariationDays = null,
                recentCycleLengths = emptyList(),
                chartPoints = emptyList(),
            )
        }

        val periodLengths = sorted.map { (start, end) ->
            ChronoUnit.DAYS.between(start, end).toInt() + 1
        }
        val averagePeriodLength = periodLengths.average().roundToInt().coerceIn(3, 8)
        val previousPeriodLength = periodLengths.getOrNull(1) ?: periodLengths.first()

        val sortedAsc = sorted.sortedBy { it.first }
        val chartPoints = buildChartPoints(sortedAsc)
        val cycleLengths = chartPoints.map { it.lengthDays }

        val averageCycleLength = if (cycleLengths.isNotEmpty()) {
            cycleLengths.average().roundToInt()
        } else {
            28
        }
        val previousCycleLength = cycleLengths.lastOrNull()
        val minLen = cycleLengths.minOrNull()
        val maxLen = cycleLengths.maxOrNull()
        val variation = if (minLen != null && maxLen != null) maxLen - minLen else null

        return CycleStats(
            previousCycleLength = previousCycleLength,
            previousPeriodLength = previousPeriodLength,
            averageCycleLength = averageCycleLength,
            averagePeriodLength = averagePeriodLength,
            cycleLengthMin = minLen,
            cycleLengthMax = maxLen,
            cycleVariationDays = variation,
            recentCycleLengths = cycleLengths.takeLast(6),
            chartPoints = chartPoints.takeLast(8),
        )
    }

    fun cycleTrendLabel(points: List<CycleLengthPoint>): String {
        if (points.size < 2) return "Добавьте ещё записи для тренда"
        val spread = points.maxOf { it.lengthDays } - points.minOf { it.lengthDays }
        return when {
            spread <= 2 -> "Стабильный цикл"
            spread <= 5 -> "Небольшие колебания"
            else -> "Заметные колебания"
        }
    }

    private fun buildChartPoints(sortedAsc: List<Pair<LocalDate, LocalDate>>): List<CycleLengthPoint> =
        sortedAsc.zipWithNext { current, next ->
            val length = ChronoUnit.DAYS.between(current.first, next.first).toInt()
            if (length in 20..45) {
                CycleLengthPoint(
                    lengthDays = length,
                    cycleStart = current.first,
                    periodLengthDays = ChronoUnit.DAYS.between(current.first, current.second).toInt() + 1,
                )
            } else {
                null
            }
        }.filterNotNull()

    fun monthDays(
        month: YearMonth,
        entries: List<CycleEntryDto>,
        today: LocalDate = LocalDate.now(),
    ): List<CalendarDay> {
        val insight = buildInsight(entries, today)
        val sorted = parseSortedPeriods(entries)
        val stats = buildCycleStats(entries)
        val lastStart = sorted.firstOrNull()?.first
        val length = month.lengthOfMonth()

        return (1..length).map { day ->
            val date = month.atDay(day)
            val isLogged = insight.periodDates.contains(date)
            val isPredicted = !isLogged && insight.predictedPeriodDates.contains(date)
            val isOvulation = insight.predictedOvulationDates.contains(date)
            val isFertile = insight.fertileDates.contains(date) && !isOvulation

            val cycleDay = lastStart?.let { start ->
                resolveCycleDay(date, start, stats.averageCycleLength)
            }
            val phase = when {
                insight.periodDates.contains(date) -> Phase.MENSTRUAL
                lastStart != null -> phaseForDate(
                    date = date,
                    lastPeriodStart = lastStart,
                    averageCycleLength = stats.averageCycleLength,
                    periodLength = stats.averagePeriodLength,
                )
                else -> Phase.UNKNOWN
            }
            val mood = moodForPhase(phase, cycleDay, stats.averageCycleLength, stats.averagePeriodLength)

            CalendarDay(
                date = date,
                isToday = date == today,
                isLoggedPeriod = isLogged,
                isPredictedPeriod = isPredicted,
                isOvulation = isOvulation,
                isFertile = isFertile,
                phase = phase,
                cycleDay = cycleDay,
                mood = mood,
            )
        }
    }

    fun moodForDate(entries: List<CycleEntryDto>, date: LocalDate): MoodInsight {
        val sorted = parseSortedPeriods(entries)
        if (sorted.isEmpty()) return moodForPhase(Phase.UNKNOWN, null, 28, 5)

        val stats = buildCycleStats(entries)
        val lastStart = sorted.first().first
        val cycleDay = resolveCycleDay(date, lastStart, stats.averageCycleLength)
        val periodDates = buildPeriodDates(sorted)
        val phase = if (periodDates.contains(date)) {
            Phase.MENSTRUAL
        } else {
            phaseForDate(
                date = date,
                lastPeriodStart = lastStart,
                averageCycleLength = stats.averageCycleLength,
                periodLength = stats.averagePeriodLength,
            )
        }
        return moodForPhase(phase, cycleDay, stats.averageCycleLength, stats.averagePeriodLength)
    }

    private fun parseSortedPeriods(entries: List<CycleEntryDto>): List<Pair<LocalDate, LocalDate>> =
        entries.mapNotNull { entry ->
            parseDate(entry.start_date)?.let { start ->
                val end = entry.end_date?.let(::parseDate) ?: start.plusDays(4)
                start to end
            }
        }.sortedByDescending { it.first }

    private fun resolveCycleDay(date: LocalDate, lastPeriodStart: LocalDate, cycleLength: Int): Int {
        if (date.isBefore(lastPeriodStart)) {
            return ((ChronoUnit.DAYS.between(date, lastPeriodStart).toInt() % cycleLength).let {
                cycleLength - it
            }).coerceIn(1, cycleLength)
        }
        val raw = ChronoUnit.DAYS.between(lastPeriodStart, date).toInt() + 1
        return ((raw - 1) % cycleLength) + 1
    }

    private fun phaseForDate(
        date: LocalDate,
        lastPeriodStart: LocalDate,
        averageCycleLength: Int,
        periodLength: Int,
    ): Phase {
        val cycleDay = resolveCycleDay(date, lastPeriodStart, averageCycleLength)
        val ovulationDay = (averageCycleLength - 14).coerceAtLeast(periodLength + 1)
        val fertileStart = ovulationDay - 5
        val fertileEnd = ovulationDay + 1

        return when {
            cycleDay <= periodLength -> Phase.MENSTRUAL
            cycleDay < fertileStart -> Phase.FOLLICULAR
            cycleDay <= fertileEnd -> Phase.OVULATION
            else -> Phase.LUTEAL
        }
    }

    fun moodForPhase(
        phase: Phase,
        cycleDay: Int?,
        cycleLength: Int,
        periodLength: Int,
    ): MoodInsight {
        val day = cycleDay ?: return MoodInsight(
            label = "Нейтральная",
            description = "Добавьте записи цикла — подскажем настроение по фазе и гормонам",
        )

        return when (phase) {
            Phase.MENSTRUAL -> when {
                day <= 2 -> MoodInsight(
                    label = "Чувствительная",
                    description = "Низкий эстроген — возможна усталость и эмоциональная ранимость",
                )
                day <= periodLength -> MoodInsight(
                    label = "Спокойная",
                    description = "Тело восстанавливается, настроение обычно выравнивается к концу менструации",
                )
                else -> MoodInsight(
                    label = "Уставшая",
                    description = "Гормональный спад может снижать тонус и концентрацию",
                )
            }
            Phase.FOLLICULAR -> if (day < cycleLength * 0.35f) {
                MoodInsight(
                    label = "Бодрая",
                    description = "Рост эстрогена — прилив энергии, мотивации и оптимизма",
                )
            } else {
                MoodInsight(
                    label = "Креативная",
                    description = "Фолликулярная фаза — хорошее время для идей и новых начинаний",
                )
            }
            Phase.OVULATION -> MoodInsight(
                label = "Энергичная",
                description = "Пик эстрогена — уверенность, общительность и высокий тонус",
            )
            Phase.LUTEAL -> {
                val daysToEnd = cycleLength - day
                when {
                    daysToEnd <= 3 -> MoodInsight(
                        label = "Плаксивая",
                        description = "Перед месячными прогестерон падает — возможны перепады настроения",
                    )
                    daysToEnd <= 7 -> MoodInsight(
                        label = "Раздражительная",
                        description = "Поздняя лютеиновая фаза — повышенная чувствительность к стрессу",
                    )
                    else -> MoodInsight(
                        label = "Сосредоточенная",
                        description = "Прогестерон доминирует — спокойная продуктивность и внимание к деталям",
                    )
                }
            }
            Phase.UNKNOWN -> MoodInsight(
                label = "Нейтральная",
                description = "Отметьте начало цикла для персонального прогноза настроения",
            )
        }
    }

    private fun buildPeriodDates(sorted: List<Pair<LocalDate, LocalDate>>): Set<LocalDate> {
        val dates = mutableSetOf<LocalDate>()
        sorted.take(12).forEach { (start, end) ->
            var current = start
            while (!current.isAfter(end)) {
                dates += current
                current = current.plusDays(1)
            }
        }
        return dates
    }

    private fun buildPredictedPeriodDates(
        firstStart: LocalDate,
        periodLength: Int,
        cycleLength: Int,
        until: LocalDate,
    ): Set<LocalDate> {
        val dates = mutableSetOf<LocalDate>()
        var cycleStart = firstStart
        while (!cycleStart.isAfter(until)) {
            repeat(periodLength) { offset ->
                dates += cycleStart.plusDays(offset.toLong())
            }
            cycleStart = cycleStart.plusDays(cycleLength.toLong())
        }
        return dates
    }

    private fun buildPredictedOvulationDates(
        anchorStart: LocalDate,
        cycleLength: Int,
        until: LocalDate,
    ): Set<LocalDate> {
        val dates = mutableSetOf<LocalDate>()
        var cycleStart = anchorStart
        while (!cycleStart.isAfter(until)) {
            val ovulation = cycleStart.plusDays((cycleLength - 14).toLong().coerceAtLeast(1))
            if (!ovulation.isAfter(until)) {
                dates += ovulation
            }
            cycleStart = cycleStart.plusDays(cycleLength.toLong())
        }
        return dates
    }

    private fun buildFertileDates(ovulationDates: Set<LocalDate>): Set<LocalDate> {
        val dates = mutableSetOf<LocalDate>()
        ovulationDates.forEach { ovulation ->
            (-5..1).forEach { offset ->
                dates += ovulation.plusDays(offset.toLong())
            }
        }
        return dates
    }
}
