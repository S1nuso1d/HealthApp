package com.example.healtapp.features.sleep.presentation

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

data class DaySleep(
    val dateKey: String,
    val label: String,
    val hours: Float,
)

object SleepHelper {

    private val dayLabelFormatter = DateTimeFormatter.ofPattern("EE", Locale("ru", "RU"))
    private val chartDayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("ru", "RU"))

    fun sleepDateKey(iso: String): String = wakeDateKey(iso)

    /** Дата пробуждения (конец сна) — как на экране активности по дню. */
    fun wakeDateKey(iso: String): String {
        val trimmed = iso.trim()
        if (trimmed.isEmpty()) return ""
        return runCatching {
            OffsetDateTime.parse(trimmed).toLocalDate().toString()
        }.getOrElse {
            runCatching {
                LocalDateTime.parse(trimmed.take(19)).toLocalDate().toString()
            }.getOrElse {
                trimmed.take(10)
            }
        }
    }

    /** Дата засыпания в локальной зоне устройства. */
    fun bedtimeDateKey(iso: String): String {
        val trimmed = iso.trim()
        if (trimmed.isEmpty()) return ""
        return runCatching {
            OffsetDateTime.parse(trimmed).toLocalDate().toString()
        }.getOrElse {
            runCatching {
                LocalDateTime.parse(trimmed.take(19)).toLocalDate().toString()
            }.getOrElse {
                trimmed.take(10)
            }
        }
    }

    fun effectiveDurationHours(record: SleepRecordUi): Float {
        if (record.durationHours > 0f) return record.durationHours
        return computeDurationHours(record.sleepStartIso, record.sleepEndIso)
    }

    fun computeDurationHours(sleepStartIso: String, sleepEndIso: String): Float {
        val start = parseToLocalDateTime(sleepStartIso) ?: return 0f
        val end = parseToLocalDateTime(sleepEndIso) ?: return 0f
        val minutes = ChronoUnit.MINUTES.between(start, end)
        if (minutes <= 0) return 0f
        return (minutes / 60f * 10f).roundToInt() / 10f
    }

    private fun parseToLocalDateTime(iso: String): LocalDateTime? {
        val trimmed = iso.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { OffsetDateTime.parse(trimmed).toLocalDateTime() }
            .recoverCatching { LocalDateTime.parse(trimmed.take(19)) }
            .getOrNull()
    }

    fun hoursForNight(records: List<SleepRecordUi>, dateKey: String): Float =
        records
            .filter { wakeDateKey(it.sleepEndIso) == dateKey }
            .maxOfOrNull { effectiveDurationHours(it) }
            ?: 0f

    fun weeklySleep(records: List<SleepRecordUi>, days: Int = 7): List<DaySleep> =
        buildCalendarWeek(records, days)

    private fun buildCalendarWeek(records: List<SleepRecordUi>, days: Int): List<DaySleep> {
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val key = date.toString()
            val label = if (offset == 0) {
                "Сегодня"
            } else {
                date.format(dayLabelFormatter).replaceFirstChar { it.uppercase() }
            }
            DaySleep(
                dateKey = key,
                label = label,
                hours = hoursForNight(records, key),
            )
        }
    }

    fun lastNightRecord(records: List<SleepRecordUi>): SleepRecordUi? =
        records.maxByOrNull { it.sleepEndIso }

    fun lastNightHours(records: List<SleepRecordUi>): Float =
        lastNightRecord(records)?.let { effectiveDurationHours(it) } ?: 0f

    /** Сон с датой пробуждения = сегодня (локальная дата устройства). */
    fun todaySleepHours(records: List<SleepRecordUi>): Float =
        hoursForNight(records, LocalDate.now().toString())

    fun averageHoursLast7(records: List<SleepRecordUi>): Float {
        val week = weeklySleep(records)
        val withData = week.filter { it.hours > 0f }
        return if (withData.isEmpty()) 0f else withData.map { it.hours }.average().toFloat()
    }

    fun consistencyPercent(records: List<SleepRecordUi>, targetHours: Float): Int {
        if (targetHours <= 0f) return 0
        val week = weeklySleep(records)
        val tracked = week.count { it.hours > 0f }
        if (tracked == 0) return 0
        val met = week.count { it.hours >= targetHours - 0.25f }
        return ((met * 100f) / tracked).roundToInt().coerceIn(0, 100)
    }

    fun formatHours(hours: Float): String =
        if (hours <= 0f) "—" else "%.1f".format(hours).replace('.', ',')

    fun buildInsight(
        records: List<SleepRecordUi>,
        targetHours: Float,
        averageHours: Float,
        qualityAverage: Int,
        lastNightHours: Float,
    ): String {
        if (records.isEmpty()) {
            return "Добавьте первую ночь вручную или импортируйте из Health Connect — появится недельная динамика и подсказки."
        }
        val debt = (targetHours - averageHours).coerceAtLeast(0f)
        return when {
            lastNightHours > 0f && lastNightHours >= targetHours ->
                "Последняя ночь в норме. Среднее за неделю — ${formatHours(averageHours)} ч при цели ${formatHours(targetHours)} ч."
            debt >= 1.5f ->
                "Средний сон за неделю ниже цели примерно на ${formatHours(debt)} ч. Попробуйте ложиться на 20–30 мин раньше."
            qualityAverage in 1..59 ->
                "Качество сна по записям невысокое ($qualityAverage/100). Проверьте проветривание и время отхода ко сну."
            averageHours >= targetHours * 0.9f ->
                "Ритм стабильный: ${formatHours(averageHours)} ч в среднем за неделю. Продолжайте фиксировать ночи."
            else ->
                "За неделю в среднем ${formatHours(averageHours)} ч. Цель — ${formatHours(targetHours)} ч; регулярные записи улучшают рекомендации."
        }
    }
}
