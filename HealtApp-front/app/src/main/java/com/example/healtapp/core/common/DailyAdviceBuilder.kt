package com.example.healtapp.core.common

/**
 * «Совет дня» из фактических метрик (без демо), если серверный brief недоступен.
 */
object DailyAdviceBuilder {

    data class Input(
        val sleepHours: Float,
        val sleepTargetHours: Float,
        val waterMl: Int,
        val waterTargetMl: Int,
        val caloriesToday: Int,
        val caloriesTarget: Int,
        val stepsToday: Int,
        val stepsGoal: Int,
    )

    fun build(input: Input): Advice? {
        val hasAny = input.sleepHours > 0f || input.waterMl > 0 || input.caloriesToday > 0 || input.stepsToday > 0
        if (!hasAny) {
            return Advice(
                title = "Совет дня",
                summary = "Добавьте сон, воду, еду или шаги — персональный совет появится здесь.",
                keyPoints = listOf(
                    "Запишите хотя бы одну метрику за сегодня",
                    "Цели можно настроить в профиле",
                ),
            )
        }

        val points = mutableListOf<String>()
        var focus = ""

        if (input.sleepHours > 0f && input.sleepTargetHours > 0f) {
            val ratio = input.sleepHours / input.sleepTargetHours
            when {
                ratio < 0.75f -> {
                    focus = "сон"
                    points += "Сон ${formatHours(input.sleepHours)} ч — ниже цели ${formatHours(input.sleepTargetHours)} ч. Старайтесь лечь раньше сегодня."
                }
                ratio >= 1f -> points += "Сон в норме: ${formatHours(input.sleepHours)} ч — хорошая база для энергии."
                else -> points += "Сон ${formatHours(input.sleepHours)} ч — близко к цели, можно добавить 30–60 мин отдыха."
            }
        } else if (input.sleepHours <= 0f) {
            points += "Сна за сегодня нет в сводке — запишите ночь или синхронизируйте Health Connect."
        }

        if (input.waterTargetMl > 0) {
            val pct = (input.waterMl * 100) / input.waterTargetMl
            when {
                input.waterMl == 0 -> points += "Вода: начните с 200–250 мл после пробуждения."
                pct < 50 -> {
                    if (focus.isEmpty()) focus = "вода"
                    points += "Выпито ${input.waterMl} мл из ${input.waterTargetMl} мл — добавьте ещё 1–2 стакана до вечера."
                }
                pct >= 100 -> points += "Водный баланс закрыт: ${input.waterMl} мл."
                else -> points += "Вода: ${input.waterMl} / ${input.waterTargetMl} мл (${pct}%)."
            }
        }

        if (input.caloriesTarget > 0 && input.caloriesToday > 0) {
            val pct = (input.caloriesToday * 100) / input.caloriesTarget
            when {
                pct < 40 -> points += "Калории: ${input.caloriesToday} из ${input.caloriesTarget} ккал — не забудьте полноценный приём пищи."
                pct > 115 -> points += "Калорий ${input.caloriesToday} ккал — выше ориентира, учитывайте перекусы."
                else -> points += "Питание: ${input.caloriesToday} / ${input.caloriesTarget} ккал."
            }
        }

        if (input.stepsGoal > 0) {
            val pct = (input.stepsToday * 100) / input.stepsGoal
            when {
                input.stepsToday < 3000 -> points += "Шаги: ${formatSteps(input.stepsToday)} — короткая прогулка добавит энергии."
                pct >= 100 -> points += "Цель по шагам достигнута: ${formatSteps(input.stepsToday)}."
                else -> points += "Шаги: ${formatSteps(input.stepsToday)} из ${formatSteps(input.stepsGoal)} (${pct}%)."
            }
        }

        val summary = when (focus) {
            "сон" -> "Сегодня главный фокус — восстановление сна."
            "вода" -> "Сегодня важнее всего добрать воду."
            else -> "Краткая сводка по вашим данным за сегодня."
        }

        return Advice(
            title = "Совет дня",
            summary = summary,
            keyPoints = points.take(4),
        )
    }

    data class Advice(
        val title: String,
        val summary: String,
        val keyPoints: List<String>,
    )

    private fun formatHours(h: Float): String = "%.1f".format(h).replace('.', ',')
    private fun formatSteps(n: Int): String = "%,d".format(n).replace(',', '\u00A0')
}
