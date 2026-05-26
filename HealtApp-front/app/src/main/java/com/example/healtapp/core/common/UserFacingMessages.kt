package com.example.healtapp.core.common

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Понятные формулировки для пользователя (сеть, HTTP, типовые сценарии).
 */
object UserFacingMessages {

    const val IRREVERSIBLE_ACCOUNT_DELETE =
        "Удаление необратимо: профиль, дневник и интеграции будут стёрты с сервера."
    const val PASSWORD_REQUIRED_TO_DELETE =
        "Для подтверждения введите текущий пароль от аккаунта."
    const val PASSWORD_REQUIRED_GENERIC =
        "Понадобится текущий пароль для подтверждения."
    const val DELETE_RECORD_WARNING =
        "Запись нельзя будет восстановить после удаления."

    fun fromThrowable(throwable: Throwable, fallback: String): String {
        return when (throwable) {
            is HttpException -> fromHttpCode(throwable.code(), throwable.message())
            is UnknownHostException, is SocketTimeoutException ->
                "Нет связи с сервером. Проверьте интернет и адрес API в настройках."
            is IOException ->
                "Сетевая ошибка. Проверьте подключение и повторите попытку."
            else -> normalize(throwable.message?.trim().orEmpty().ifBlank { fallback })
        }
    }

    fun fromHttpCode(code: Int, raw: String? = null): String = when (code) {
        401 -> "Сессия истекла или недействительна. Выйдите и войдите снова."
        403 -> "Недостаточно прав для этого действия."
        404 -> "Не найдено на сервере. Обновите данные или войдите заново."
        408, 504 -> "Сервер не ответил вовремя. Попробуйте ещё раз."
        429 -> "Слишком много запросов. Подождите немного и повторите."
        in 500..599 -> "Сервер временно недоступен. Повторите позже."
        else -> raw?.let { normalize(it) }?.takeIf { it.isNotBlank() }
            ?: "Ошибка запроса (код $code)."
    }

    fun normalize(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("401") || lower.contains("unauthorized") ||
                lower.contains("недействительный") && lower.contains("токен") ->
                fromHttpCode(401)
            lower.contains("403") || lower.contains("forbidden") -> fromHttpCode(403)
            lower.contains("timeout") || lower.contains("timed out") ->
                "Превышено время ожидания. Проверьте сеть и повторите."
            lower.contains("unable to resolve host") || lower.contains("failed to connect") ->
                "Не удалось подключиться к серверу. Проверьте адрес API и Wi‑Fi."
            lower.contains("llm") && lower.contains("недоступ") ->
                "AI-сервис недоступен. Ответ будет по сохранённой аналитике."
            else -> raw
        }
    }
}
