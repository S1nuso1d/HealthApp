package com.example.healtapp.features.activity.presentation

data class TrainingTypeDef(
    val slug: String,
    val titleRu: String,
    val category: String,
)

val allTrainingTypes: List<TrainingTypeDef> = listOf(
    TrainingTypeDef("run", "Бег", "Кардио"),
    TrainingTypeDef("bike", "Велосипед", "Кардио"),
    TrainingTypeDef("swim", "Плавание", "Кардио"),
    TrainingTypeDef("hiit", "HIIT", "Кардио"),
    TrainingTypeDef("elliptical", "Эллипс", "Кардио"),
    TrainingTypeDef("rowing", "Гребля", "Кардио"),
    TrainingTypeDef("dance", "Танцы", "Кардио"),
    TrainingTypeDef("hiking", "Поход", "Кардио"),
    TrainingTypeDef("strength", "Силовая тренировка", "Сила"),
    TrainingTypeDef("crossfit", "Кроссфит", "Сила"),
    TrainingTypeDef("boxing", "Бокс", "Сила"),
    TrainingTypeDef("yoga", "Йога", "Гибкость"),
    TrainingTypeDef("pilates", "Пилатес", "Гибкость"),
    TrainingTypeDef("stretch", "Растяжка", "Гибкость"),
    TrainingTypeDef("football", "Футбол", "Игры"),
    TrainingTypeDef("tennis", "Теннис", "Игры"),
)

/** @deprecated используйте allTrainingTypes */
val trainingActivityTypes: List<String> = allTrainingTypes.map { it.titleRu }

private val defaultQuickPickSlugs = listOf("run", "strength", "yoga")

fun trainingTypeBySlug(slug: String): TrainingTypeDef? =
    allTrainingTypes.firstOrNull { it.slug.equals(slug, ignoreCase = true) }

fun trainingTypeByTitle(titleRu: String): TrainingTypeDef? =
    allTrainingTypes.firstOrNull { it.titleRu == titleRu }

fun computeQuickPickSlugs(
    favorites: Set<String>,
    usageCounts: Map<String, Int>,
    historySlugs: List<String>,
): List<String> {
    val picked = linkedSetOf<String>()

    favorites.forEach { slug ->
        if (trainingTypeBySlug(slug) != null && picked.size < 3) picked.add(slug)
    }

    if (picked.size < 3) {
        val fromUsage = usageCounts.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .filter { trainingTypeBySlug(it) != null && it !in picked }
        fromUsage.forEach { if (picked.size < 3) picked.add(it) }
    }

    if (picked.size < 3) {
        val fromHistory = historySlugs
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .filter { trainingTypeBySlug(it) != null && it !in picked }
        fromHistory.forEach { if (picked.size < 3) picked.add(it) }
    }

    if (picked.size < 3) {
        defaultQuickPickSlugs
            .filter { it !in picked }
            .forEach { if (picked.size < 3) picked.add(it) }
    }

    return picked.take(3).toList()
}
