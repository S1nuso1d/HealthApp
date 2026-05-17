package com.example.healtapp.core.dataimport

/** Подмножество правил серверного CSV-импорта (разделитель «;»). */
object ImportCsvFormat {
    fun rowKind(line: String): String? {
        val t = line.trim()
        if (t.isEmpty() || t.startsWith("#")) return null
        return t.substringBefore(';').lowercase()
    }
}
