package com.example.healtapp.features.activity.presentation

/** Типы для ручного добавления тренировок (без ходьбы — шаги отдельно). */
// Список перенесён в TrainingCatalog.kt — allTrainingTypes / trainingActivityTypes

fun activityApiSlug(displayRu: String): String = when (displayRu) {
    "Бег" -> "run"
    "Прогулка", "Ходьба" -> "walk"
    "Велосипед" -> "bike"
    "Силовая тренировка" -> "strength"
    "Йога" -> "yoga"
    "Растяжка" -> "stretch"
    "Плавание" -> "swim"
    "HIIT" -> "hiit"
    "Пилатес" -> "pilates"
    "Эллипс" -> "elliptical"
    "Гребля" -> "rowing"
    "Танцы" -> "dance"
    "Бокс" -> "boxing"
    "Футбол" -> "football"
    "Теннис" -> "tennis"
    "Поход" -> "hiking"
    "Кроссфит" -> "crossfit"
    else -> trainingTypeByTitle(displayRu)?.slug
        ?: displayRu.lowercase().replace(" ", "_").ifBlank { "workout" }
}

fun activityTitleFromApi(apiType: String): String = when (apiType.lowercase()) {
    "run" -> "Бег"
    "walk" -> "Ходьба"
    "bike" -> "Велосипед"
    "strength" -> "Силовая тренировка"
    "yoga" -> "Йога"
    "stretch" -> "Растяжка"
    "swim" -> "Плавание"
    "hiit" -> "HIIT"
    "pilates" -> "Пилатес"
    "elliptical" -> "Эллипс"
    "rowing" -> "Гребля"
    "dance" -> "Танцы"
    "boxing" -> "Бокс"
    "football" -> "Футбол"
    "tennis" -> "Теннис"
    "hiking" -> "Поход"
    "crossfit" -> "Кроссфит"
    else -> trainingTypeBySlug(apiType)?.titleRu
        ?: apiType.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun isWalkLikeApi(apiType: String): Boolean =
    apiType.equals("walk", ignoreCase = true)

fun isWalkLikeDisplay(displayRu: String): Boolean =
    displayRu == "Ходьба" || displayRu == "Прогулка"

data class TrainingFormFields(
    val showDistance: Boolean,
    val showNotes: Boolean,
    val showExertion: Boolean,
    val showCalories: Boolean = false,
    val showIntensity: Boolean = false,
    val supportsLiveGps: Boolean = false,
    val distanceLabel: String = "Дистанция (км)",
    val notesLabel: String = "Заметки",
    val notesPlaceholder: String = "",
    val hint: String = "Заполните детали и сохраните",
)

fun trainingFormFieldsFor(displayRu: String): TrainingFormFields = when (displayRu) {
    "Бег" -> TrainingFormFields(
        showDistance = true,
        showNotes = true,
        showExertion = false,
        supportsLiveGps = true,
        distanceLabel = "Дистанция (км)",
        hint = "Живая запись на карте или ручной ввод",
    )
    "Ходьба", "Прогулка" -> TrainingFormFields(
        showDistance = true,
        showNotes = true,
        showExertion = false,
        supportsLiveGps = true,
        distanceLabel = "Дистанция (км)",
        hint = "Шаги идут отдельно — здесь прогулка или ходьба",
    )
    "Велосипед" -> TrainingFormFields(
        showDistance = true,
        showNotes = true,
        showExertion = false,
        supportsLiveGps = true,
        distanceLabel = "Дистанция (км)",
        hint = "Километраж и время на карте или вручную",
    )
    "Поход" -> TrainingFormFields(
        showDistance = true,
        showNotes = true,
        showExertion = false,
        supportsLiveGps = true,
        distanceLabel = "Дистанция (км)",
        notesLabel = "Маршрут",
        notesPlaceholder = "Тропа, набор высоты, покрытие",
        hint = "Маршрут на карте или ручная запись",
    )
    "Силовая тренировка" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        showIntensity = true,
        notesLabel = "Упражнения и подходы",
        notesPlaceholder = "Например: жим 3×8, присед 4×10",
        hint = "Подходы, повторения и ощущаемая нагрузка",
    )
    "Йога" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        notesLabel = "Практика",
        notesPlaceholder = "Стиль, ключевые асаны, самочувствие",
        hint = "Длительность и характер практики",
    )
    "Растяжка" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        notesLabel = "Фокус тренировки",
        notesPlaceholder = "Зоны тела, длительность удержаний",
        hint = "Какие мышцы растягивали",
    )
    "Пилатес" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        notesLabel = "Блок",
        notesPlaceholder = "Коврик, реформер, акцент на корпус",
        hint = "Длительность и ощущаемая нагрузка",
    )
    "Плавание" -> TrainingFormFields(
        showDistance = true,
        showNotes = true,
        showExertion = false,
        distanceLabel = "Дистанция (м)",
        notesLabel = "Стиль",
        notesPlaceholder = "Кроль, брасс, интервалы",
        hint = "Метры и стиль в бассейне",
    )
    "HIIT" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        showIntensity = true,
        notesLabel = "Протокол",
        notesPlaceholder = "20/10, 8 раундов, упражнения",
        hint = "Интервалы и оценка нагрузки",
    )
    "Кроссфит" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        showIntensity = true,
        notesLabel = "WOD",
        notesPlaceholder = "Комплекс, раунды, вес",
        hint = "Комплекс и ощущаемая нагрузка",
    )
    "Бокс" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        showIntensity = true,
        notesLabel = "Раунды",
        notesPlaceholder = "Мешок, спарринг, работа на лапах",
        hint = "Раунды и интенсивность",
    )
    "Танцы" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        notesLabel = "Стиль",
        notesPlaceholder = "Зумба, хип-хоп, бальные",
        hint = "Длительность и стиль",
    )
    "Футбол", "Теннис" -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        showIntensity = true,
        notesLabel = "Игра",
        notesPlaceholder = "Тренировка или матч, счёт",
        hint = "Длительность и характер игры",
    )
    "Эллипс" -> TrainingFormFields(
        showDistance = true,
        showNotes = false,
        showExertion = true,
        distanceLabel = "Дистанция (км)",
        hint = "Время и дистанция с тренажёра",
    )
    "Гребля" -> TrainingFormFields(
        showDistance = true,
        showNotes = false,
        showExertion = true,
        distanceLabel = "Дистанция (км)",
        hint = "Дистанция и время на эргометре",
    )
    else -> TrainingFormFields(
        showDistance = false,
        showNotes = true,
        showExertion = true,
        showIntensity = true,
        hint = "Длительность и заметки по тренировке",
    )
}

fun estimateTrainingCalories(displayRu: String, durationMinutes: Int, distanceKm: Float?): Float {
    val hours = durationMinutes.coerceAtLeast(1) / 60f
    val perMinute = when (displayRu) {
        "Бег" -> 10f
        "Велосипед" -> 8f
        "Поход" -> 7f
        "Плавание" -> 9f
        "HIIT", "Кроссфит", "Бокс" -> 11f
        "Силовая тренировка" -> 6f
        "Эллипс", "Гребля" -> 8f
        "Танцы", "Футбол", "Теннис" -> 7f
        "Йога", "Пилатес", "Растяжка" -> 3.5f
        "Ходьба", "Прогулка" -> 4f
        else -> 6f
    }
    val fromTime = perMinute * durationMinutes
    val fromDistance = distanceKm?.let { it * 60f }
    return (fromDistance?.coerceAtLeast(fromTime) ?: fromTime).coerceAtLeast(hours * 50f)
}