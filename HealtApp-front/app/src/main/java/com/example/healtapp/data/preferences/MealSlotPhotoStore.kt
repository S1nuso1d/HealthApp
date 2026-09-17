package com.example.healtapp.data.preferences

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Обложки приёмов пищи: копируем файл во внутреннее хранилище,
 * иначе content:// из галереи «протухает» и остаётся белый кадр.
 */
object MealSlotPhotoStore {
    private const val PREFS = "meal_slot_photos"

    fun get(context: Context, dateKey: String, mealTypeApi: String): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val k = key(dateKey, mealTypeApi)
        val stored = prefs.getString(k, null) ?: return null
        val file = resolveLocalFile(stored)
        if (file != null) {
            if (file.exists() && file.length() > 0L) return file.absolutePath
            prefs.edit().remove(k).apply()
            return null
        }
        // Старые content:// без копии — больше не открываются.
        if (stored.startsWith("content:", ignoreCase = true)) {
            prefs.edit().remove(k).apply()
            return null
        }
        return stored.takeIf { it.isNotBlank() }
    }

    fun set(context: Context, dateKey: String, mealTypeApi: String, uri: String?) {
        if (uri.isNullOrBlank()) {
            clear(context, dateKey, mealTypeApi)
            return
        }
        val parsed = runCatching { Uri.parse(uri) }.getOrNull()
        if (parsed != null && (parsed.scheme == "content" || parsed.scheme == "file")) {
            persistFromUri(context, dateKey, mealTypeApi, parsed)
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(key(dateKey, mealTypeApi), uri)
                .apply()
        }
    }

    fun persistFromUri(context: Context, dateKey: String, mealTypeApi: String, source: Uri): String? {
        val dest = coverFile(context, dateKey, mealTypeApi)
        val ok = runCatching {
            context.contentResolver.openInputStream(source)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.exists() && dest.length() > 0L
        }.getOrDefault(false)
        if (!ok) {
            dest.delete()
            return null
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(key(dateKey, mealTypeApi), dest.absolutePath)
            .apply()
        return dest.absolutePath
    }

    fun clear(context: Context, dateKey: String, mealTypeApi: String) {
        coverFile(context, dateKey, mealTypeApi).delete()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(key(dateKey, mealTypeApi))
            .apply()
    }

    fun photosForDate(context: Context, dateKey: String): Map<String, String> {
        return listOf("breakfast", "lunch", "snack", "dinner").mapNotNull { type ->
            get(context, dateKey, type)?.let { type to it }
        }.toMap()
    }

    private fun coverFile(context: Context, dateKey: String, mealTypeApi: String): File {
        val dir = File(context.filesDir, "meal_covers").apply { mkdirs() }
        return File(dir, "${dateKey}_${mealTypeApi.lowercase()}.jpg")
    }

    private fun resolveLocalFile(stored: String): File? {
        return when {
            stored.startsWith("file:", ignoreCase = true) ->
                runCatching { File(Uri.parse(stored).path ?: return null) }.getOrNull()
            stored.startsWith("/") -> File(stored)
            else -> null
        }
    }

    private fun key(dateKey: String, mealTypeApi: String) = "${dateKey}_${mealTypeApi.lowercase()}"
}
