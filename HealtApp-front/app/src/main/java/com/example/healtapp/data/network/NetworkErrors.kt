package com.example.healtapp.data.network

import java.io.IOException
import java.net.SocketTimeoutException

fun Throwable.toUserMessage(fallback: String): String {
    when (this) {
        is SocketTimeoutException -> return "Сервер долго отвечает. Проверьте сеть и нажмите «Повторить»."
    }
    val msg = message.orEmpty()
    if (msg.contains("timeout", ignoreCase = true)) {
        return "Превышено время ожидания. Убедитесь, что бэкенд запущен, и повторите попытку."
    }
    if (this is IOException && msg.isBlank()) {
        return "Нет связи с сервером. Проверьте интернет и адрес API в настройках сборки."
    }
    return msg.ifBlank { fallback }
}
