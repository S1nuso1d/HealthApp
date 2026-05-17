package com.example.healtapp.core.common

/**
 * Расчёт ИМТ и текстовые категории по ВОЗ (для взрослых).
 */
object BmiHelper {

    enum class Category {
        UNDERWEIGHT,
        NORMAL,
        OVERWEIGHT,
        OBESE,
    }

    data class Result(
        val value: Float,
        val category: Category,
        val labelRu: String,
        val hintRu: String,
    )

    fun calculate(heightCm: Float?, weightKg: Float?): Result? {
        if (heightCm == null || weightKg == null || heightCm <= 0f || weightKg <= 0f) return null
        val heightM = heightCm / 100f
        val bmi = weightKg / (heightM * heightM)
        if (!bmi.isFinite() || bmi <= 0f) return null
        val category = when {
            bmi < 18.5f -> Category.UNDERWEIGHT
            bmi < 25f -> Category.NORMAL
            bmi < 30f -> Category.OVERWEIGHT
            else -> Category.OBESE
        }
        return Result(
            value = bmi,
            category = category,
            labelRu = categoryLabel(category),
            hintRu = categoryHint(category),
        )
    }

    fun formatValue(bmi: Float): String = "%.1f".format(bmi)

    fun categoryLabel(category: Category): String = when (category) {
        Category.UNDERWEIGHT -> "Недостаточный вес"
        Category.NORMAL -> "Норма"
        Category.OVERWEIGHT -> "Избыточный вес"
        Category.OBESE -> "Ожирение"
    }

    private fun categoryHint(category: Category): String = when (category) {
        Category.UNDERWEIGHT -> "Ниже нормы — обсудите питание с врачом при необходимости"
        Category.NORMAL -> "Показатель в рекомендуемом диапазоне"
        Category.OVERWEIGHT -> "Чуть выше нормы — баланс питания и активности помогает"
        Category.OBESE -> "Выше нормы — полезна консультация специалиста"
    }

    /** Диапазон «нормального» веса при заданном росте (кг). */
    fun healthyWeightRangeKg(heightCm: Float): Pair<Float, Float>? {
        if (heightCm <= 0f) return null
        val h = heightCm / 100f
        return (18.5f * h * h) to (24.9f * h * h)
    }
}
