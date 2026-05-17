package com.example.healtapp.features.activity.presentation

/** Типы для ручного добавления тренировок (без ходьбы — шаги отдельно). */
val trainingActivityTypes = listOf(
    "Бег",
    "Велосипед",
    "Силовая тренировка",
    "Йога",
    "Растяжка",
    "Плавание",
)

fun activityApiSlug(displayRu: String): String = when (displayRu) {
    "Бег" -> "run"
    "Прогулка", "Ходьба" -> "walk"
    "Велосипед" -> "bike"
    "Силовая тренировка" -> "strength"
    "Йога" -> "yoga"
    "Растяжка" -> "stretch"
    "Плавание" -> "swim"
    else -> displayRu.lowercase().replace(" ", "_").ifBlank { "workout" }
}

fun activityTitleFromApi(apiType: String): String = when (apiType.lowercase()) {
    "run" -> "Бег"
    "walk" -> "Ходьба"
    "bike" -> "Велосипед"
    "strength" -> "Силовая тренировка"
    "yoga" -> "Йога"
    "stretch" -> "Растяжка"
    "swim" -> "Плавание"
    else -> apiType.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun isWalkLikeApi(apiType: String): Boolean =
    apiType.equals("walk", ignoreCase = true)

fun isWalkLikeDisplay(displayRu: String): Boolean =
    displayRu == "Ходьба" || displayRu == "Прогулка"