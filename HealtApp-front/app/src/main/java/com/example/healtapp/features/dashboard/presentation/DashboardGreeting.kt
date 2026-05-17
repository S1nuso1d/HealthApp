package com.example.healtapp.features.dashboard.presentation

import java.time.LocalTime

object DashboardGreeting {
    fun forNow(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Доброе утро"
            in 12..16 -> "Добрый день"
            in 17..22 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
    }
}
