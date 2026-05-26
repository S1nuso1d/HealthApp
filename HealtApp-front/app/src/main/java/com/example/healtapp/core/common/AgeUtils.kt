package com.example.healtapp.core.common

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

object AgeUtils {
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    fun ageFromBirthDate(birthDateIso: String): Int? {
        return runCatching {
            val birth = LocalDate.parse(birthDateIso.take(10), iso)
            Period.between(birth, LocalDate.now()).years.coerceIn(1, 120)
        }.getOrNull()
    }

    /** Оценка даты рождения 1 июля года (N - age), если в профиле только возраст. */
    fun estimatedBirthDateFromAge(age: Int): String {
        val year = LocalDate.now().year - age.coerceIn(1, 120)
        return LocalDate.of(year, 7, 1).format(iso)
    }
}
