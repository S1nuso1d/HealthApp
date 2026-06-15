package com.example.healtapp.features.meal.ui

import android.content.Context
import android.net.Uri
import java.io.File

fun getFileFromUri(context: Context, uri: Uri): File? = runCatching {
    val mime = context.contentResolver.getType(uri)
    val ext = when (mime) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
    val file = File(context.cacheDir, "food_photo_${System.currentTimeMillis()}.$ext")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    file.takeIf { it.exists() && it.length() > 0L }
}.getOrNull()
