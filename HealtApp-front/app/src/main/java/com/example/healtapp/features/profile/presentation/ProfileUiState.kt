package com.example.healtapp.features.profile.presentation

data class ProfileGoalUi(
    val title: String,
    val value: String
)

data class ProfileSettingUi(
    val title: String,
    val value: String
)

data class ProfileIntegrationUi(
    val title: String,
    val status: String
)

data class ProfileUiState(
    val fullName: String = "Даниил",
    val age: Int = 22,
    val sex: String = "Мужской",
    val heightCm: Int = 180,
    val weightKg: Int = 78,

    val goals: List<ProfileGoalUi> = listOf(
        ProfileGoalUi("Главная цель", "Улучшение сна"),
        ProfileGoalUi("Цель по воде", "2500 мл"),
        ProfileGoalUi("Цель по сну", "8 часов")
    ),

    val settings: List<ProfileSettingUi> = listOf(
        ProfileSettingUi("Уровень активности", "Средний"),
        ProfileSettingUi("Напоминания о воде", "Включены"),
        ProfileSettingUi("Напоминания о сне", "Включены")
    ),

    val integrations: List<ProfileIntegrationUi> = listOf(
        ProfileIntegrationUi("Health Connect", "Не подключено"),
        ProfileIntegrationUi("Умные часы", "Не подключено"),
        ProfileIntegrationUi("Локальная синхронизация", "Активна")
    )
)