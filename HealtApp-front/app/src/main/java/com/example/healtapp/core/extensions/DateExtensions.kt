package com.example.healtapp.core.extensions

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun LocalDateTime.toReadable(): String {
    return this.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
}