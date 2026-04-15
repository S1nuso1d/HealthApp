package com.example.healtapp.core.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object DateUtils {
    fun todayStart(): LocalDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
    fun todayEnd(): LocalDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
}