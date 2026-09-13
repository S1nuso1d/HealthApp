package com.example.healtapp.core.common

import java.util.Locale

/**
 * Русская локаль для форматирования дат и чисел в интерфейсе.
 *
 * Приложение русскоязычное, поэтому форматирование не должно зависеть от локали устройства.
 * Конструктор `Locale("ru", "RU")` устарел — используется [Locale.forLanguageTag].
 */
val LocaleRu: Locale = Locale.forLanguageTag("ru-RU")
