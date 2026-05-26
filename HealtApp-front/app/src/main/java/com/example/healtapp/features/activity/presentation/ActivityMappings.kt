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

data class TrainingFormFields(
    val showDistance: Boolean,
    val showNotes: Boolean,
    val showExertion: Boolean,
    val distanceLabel: String = "Дистанция (км)",
    val notesLabel: String = "Заметки",
    val notesPlaceholder: String = "",
)

fun trainingFormFieldsFor(displayRu: String): TrainingFormFields = when (displayRu) {
    "Силовая тренировка" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        notesLabel = "Упражнения и подходы",
        notesPlaceholder = "Например: жим 3×8, присед 4×10",
    )
    "Йога" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        notesLabel = "Практика",
        notesPlaceholder = "Стиль, ключевые асаны, самочувствие",
    )
    "Растяжка" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        notesLabel = "Фокус тренировки",
        notesPlaceholder = "Зоны тела, длительность удержаний",
    )
    "Плавание" -> TrainingFormFields(
        showDistance = true,
        showNotes = true,
        showExertion = false,
        distanceLabel = "Дистанция (м)",
        notesLabel = "Заметки",
    )
    "Велосипед" -> TrainingFormFields(
        showDistance = true,
        showNotes = true,
        showExertion = false,
        distanceLabel = "Дистанция (км)",
    )
    else -> TrainingFormFields(
        showDistance = true,
        showNotes = true,
        showExertion = false,
        distanceLabel = "Дистанция (км)",
        notesLabel = "Заметки",
    )
}