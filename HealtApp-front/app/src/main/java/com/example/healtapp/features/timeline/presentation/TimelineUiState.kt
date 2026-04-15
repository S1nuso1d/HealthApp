package com.example.healtapp.features.timeline.presentation

data class TimelineEventUi(
    val id: Int,
    val time: String,
    val title: String,
    val description: String,
    val type: String
)

data class TimelineUiState(
    val selectedDate: String = "Сегодня, 1 апреля",
    val summaryText: String = "За день у тебя были записи сна, воды, питания, активности и персональные рекомендации.",
    val insightText: String = "День выглядит более сбалансированным, когда вода и активность распределены равномерно, а кофеин не смещается на вечер.",
    val events: List<TimelineEventUi> = listOf(
        TimelineEventUi(
            id = 1,
            time = "07:30",
            title = "Подъем",
            description = "Пробуждение после сна 7.7 ч",
            type = "sleep"
        ),
        TimelineEventUi(
            id = 2,
            time = "08:10",
            title = "Вода",
            description = "300 мл воды после пробуждения",
            type = "hydration"
        ),
        TimelineEventUi(
            id = 3,
            time = "08:20",
            title = "Завтрак",
            description = "Омлет и тосты • 420 ккал",
            type = "nutrition"
        ),
        TimelineEventUi(
            id = 4,
            time = "13:20",
            title = "Напиток",
            description = "Зеленый чай • 250 мл",
            type = "hydration"
        ),
        TimelineEventUi(
            id = 5,
            time = "18:40",
            title = "Капучино",
            description = "120 ккал • 85 мг кофеина",
            type = "nutrition"
        ),
        TimelineEventUi(
            id = 6,
            time = "19:30",
            title = "Прогулка",
            description = "40 минут • 3500 шагов",
            type = "activity"
        ),
        TimelineEventUi(
            id = 7,
            time = "21:00",
            title = "Рекомендация",
            description = "Старайся не пить кофе после 16:00",
            type = "recommendation"
        ),
        TimelineEventUi(
            id = 8,
            time = "23:40",
            title = "Сон",
            description = "Отход ко сну",
            type = "sleep"
        )
    )
)