package com.example.healtapp.core.common

import java.time.LocalDate

object DateRules {

    fun today(): LocalDate = LocalDate.now()

    fun isFuture(date: LocalDate): Boolean = date.isAfter(today())

    fun isFutureDateString(value: String): Boolean =
        runCatching { LocalDate.parse(value.trim()) }
            .getOrNull()
            ?.let(::isFuture)
            ?: false

    fun clampToTodayOrPast(date: LocalDate): LocalDate =
        if (date.isAfter(today())) today() else date
}
