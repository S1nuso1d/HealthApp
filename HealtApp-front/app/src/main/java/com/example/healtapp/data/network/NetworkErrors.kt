package com.example.healtapp.data.network

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.toUserMessage(fallback: String): String {
    if (isOfflineLike()) {
        return "Нет связи с сервером. Запись сохранена на телефоне и уйдёт, когда появится сеть."
    }
    when (this) {
        is SocketTimeoutException -> return "Сервер долго отвечает. Проверьте сеть и нажмите «Повторить»."
    }
    val msg = message.orEmpty()
    if (msg.contains("timeout", ignoreCase = true)) {
        return "Превышено время ожидания. Убедитесь, что бэкенд запущен, и повторите попытку."
    }
    if (this is IOException && msg.isBlank()) {
        return "Нет связи с сервером. Проверьте интернет и адрес API в профиле → ещё → сервер."
    }
    return msg.ifBlank { fallback }
}

fun Throwable.isOfflineLike(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is UnknownHostException || current is ConnectException || current is SocketTimeoutException) {
            return true
        }
        val msg = current.message.orEmpty()
        if (
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("Failed to connect", ignoreCase = true) ||
            msg.contains("Network is unreachable", ignoreCase = true) ||
            msg.contains("failed to connect", ignoreCase = true) ||
            msg.contains("No address associated", ignoreCase = true)
        ) {
            return true
        }
        current = current.cause
    }
    return false
}
