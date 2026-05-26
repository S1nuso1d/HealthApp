package com.example.healtapp.core.common

import android.content.Context
import android.content.Intent

object ShareProgressHelper {

    fun shareWeeklySummary(
        context: Context,
        periodLabel: String,
        stepsAvg: String?,
        waterAvg: String?,
        sleepAvg: String?,
        healthScore: Int?,
    ) {
        val lines = buildList {
            add("Моя неделя в HealthApp ($periodLabel)")
            stepsAvg?.let { add("Шаги в среднем: $it") }
            waterAvg?.let { add("Вода в среднем: $it") }
            sleepAvg?.let { add("Сон в среднем: $it") }
            healthScore?.takeIf { it > 0 }?.let { add("Индекс здоровья: $it/100") }
            add("")
            add("Веду дневник здоровья в HealthApp")
        }
        val text = lines.joinToString("\n")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(intent, "Поделиться прогрессом").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
