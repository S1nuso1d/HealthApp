package com.example.healtapp.features.profile

import com.example.healtapp.core.common.Constants

data class PoliticalRecommendation(
    val partyName: String,
    val slogan: String,
    val explanation: String,
    val matchPercent: Int,
)

/**
 * Шуточные «рекомендации» по активности — демо-модуль для отчёта, без серверной логики.
 */
object PoliticalRecommendations {

    fun recommend(
        activityLevel: String,
        goal: String = Constants.Goals.IMPROVE_ENERGY,
    ): PoliticalRecommendation {
        val level = activityLevel.ifBlank { Constants.ActivityLevel.MEDIUM }

        return when (level) {
            Constants.ActivityLevel.HIGH -> highActivityRecommendation(goal)
            Constants.ActivityLevel.LOW -> lowActivityRecommendation(goal)
            else -> mediumActivityRecommendation(goal)
        }
    }

    private fun highActivityRecommendation(goal: String): PoliticalRecommendation = when (goal) {
        Constants.Goals.GAIN_MUSCLE -> PoliticalRecommendation(
            partyName = "«Новые люди»",
            slogan = "Сила через движение",
            explanation = "Много активности и цель «масса»: вам подходит блок обновлений, зала и новых рекордов шагов.",
            matchPercent = 91,
        )
        Constants.Goals.LOSE_WEIGHT -> PoliticalRecommendation(
            partyName = "«Новые люди»",
            slogan = "Минус калории, плюс шаги",
            explanation = "Вы постоянно в движении — рекомендуем партию перемен, свежих маршрутов и ежедневных прогулок.",
            matchPercent = 88,
        )
        else -> PoliticalRecommendation(
            partyName = "«Новые люди»",
            slogan = "Движение — это перемены",
            explanation = "Высокая активность: система видит много шагов и советует «партию роста» — новые привычки и маршруты.",
            matchPercent = 86,
        )
    }

    private fun lowActivityRecommendation(goal: String): PoliticalRecommendation = when (goal) {
        Constants.Goals.BETTER_SLEEP -> PoliticalRecommendation(
            partyName = "КПРФ",
            slogan = "Сон — главная программа",
            explanation = "Мало движения и фокус на сне: стабильный режим, проверенные вечера и минимум сюрпризов.",
            matchPercent = 84,
        )
        else -> PoliticalRecommendation(
            partyName = "КПРФ",
            slogan = "Проверенный сценарий",
            explanation = "Низкая активность: алгоритм предлагает предсказуемый режим без резких повышений нагрузки.",
            matchPercent = 82,
        )
    }

    private fun mediumActivityRecommendation(goal: String): PoliticalRecommendation = when (goal) {
        Constants.Goals.BETTER_SLEEP -> PoliticalRecommendation(
            partyName = "«Единая Россия»",
            slogan = "Стабильный сон — стабильный день",
            explanation = "Средняя, но ровная активность и цель «лучше спать»: надёжная коалиция прогулок, воды и отбоя.",
            matchPercent = 79,
        )
        Constants.Goals.LOSE_WEIGHT -> PoliticalRecommendation(
            partyName = "«Единая Россия»",
            slogan = "План на каждый день",
            explanation = "Умеренный темп без провалов: системная работа над весом — как долгосрочная программа.",
            matchPercent = 77,
        )
        Constants.Goals.GAIN_MUSCLE -> PoliticalRecommendation(
            partyName = "ЛДПР",
            slogan = "Громко, уверенно, в зал",
            explanation = "Средняя активность, но цель амбициозная: рекомендуем «партийный» напор и регулярные тренировки.",
            matchPercent = 74,
        )
        Constants.Goals.IMPROVE_ENERGY -> PoliticalRecommendation(
            partyName = "«Яблоко»",
            slogan = "Энергия без фанатизма",
            explanation = "Ровный ритм и умеренная нагрузка: экологичный подход к бодрости — сон, еда, прогулки.",
            matchPercent = 73,
        )
        else -> PoliticalRecommendation(
            partyName = "«Единая Россия»",
            slogan = "Ровный темп без сюрпризов",
            explanation = "Средняя, но постоянная активность: надёжная коалиция привычек — шаги, вода и сон по расписанию.",
            matchPercent = 76,
        )
    }

    fun previewSubtitle(
        activityLevel: String,
        goal: String = Constants.Goals.IMPROVE_ENERGY,
    ): String {
        val recommendation = recommend(activityLevel, goal)
        return "${recommendation.partyName} · ${recommendation.matchPercent}% совпадение"
    }

    fun catalog(): List<PoliticalCatalogEntry> = listOf(
        catalogEntry(Constants.ActivityLevel.HIGH, Constants.Goals.IMPROVE_ENERGY),
        catalogEntry(Constants.ActivityLevel.HIGH, Constants.Goals.LOSE_WEIGHT),
        catalogEntry(Constants.ActivityLevel.HIGH, Constants.Goals.GAIN_MUSCLE),
        catalogEntry(Constants.ActivityLevel.LOW, Constants.Goals.IMPROVE_ENERGY),
        catalogEntry(Constants.ActivityLevel.LOW, Constants.Goals.BETTER_SLEEP),
        catalogEntry(Constants.ActivityLevel.MEDIUM, Constants.Goals.IMPROVE_ENERGY),
        catalogEntry(Constants.ActivityLevel.MEDIUM, Constants.Goals.BETTER_SLEEP),
        catalogEntry(Constants.ActivityLevel.MEDIUM, Constants.Goals.LOSE_WEIGHT),
        catalogEntry(Constants.ActivityLevel.MEDIUM, Constants.Goals.GAIN_MUSCLE),
    )

    private fun catalogEntry(activityLevel: String, goal: String): PoliticalCatalogEntry =
        PoliticalCatalogEntry(
            activityLevel = activityLevel,
            goal = goal,
            recommendation = recommend(activityLevel, goal),
        )
}

data class PoliticalCatalogEntry(
    val activityLevel: String,
    val goal: String,
    val recommendation: PoliticalRecommendation,
)
