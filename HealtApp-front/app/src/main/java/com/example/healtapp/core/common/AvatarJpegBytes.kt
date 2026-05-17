package com.example.healtapp.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Декодирует любой поддерживаемый системой формат (в т.ч. HEIC из галереи)
 * и сжимает в JPEG под лимит [Constants.AVATAR_MAX_BYTES], как ожидает бэкенд.
 */
object AvatarJpegBytes {

    private const val MAX_DIMENSION = 1600

    fun fromUri(context: Context, uri: Uri): ByteArray {
        val bitmap = decode(context, uri)
        return try {
            compressToJpegUnderCap(bitmap, Constants.AVATAR_MAX_BYTES)
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun decode(context: Context, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeApi28Plus(context, uri)
        } else {
            decodeLegacy(context, uri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeApi28Plus(context: Context, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val longEdge = max(w, h)
            if (longEdge > MAX_DIMENSION) {
                val scale = longEdge.toFloat() / MAX_DIMENSION
                decoder.setTargetSize(
                    (w / scale).roundToInt().coerceAtLeast(1),
                    (h / scale).roundToInt().coerceAtLeast(1),
                )
            }
        }
    }

    private fun decodeLegacy(context: Context, uri: Uri): Bitmap {
        val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: error("Не удалось прочитать изображение")
        return scaleDownIfNeeded(raw, MAX_DIMENSION)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longEdge = max(w, h)
        if (longEdge <= maxDim) return bitmap
        val scale = maxDim / longEdge.toFloat()
        val nw = (w * scale).roundToInt().coerceAtLeast(1)
        val nh = (h * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, nw, nh, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    private fun compressToJpegUnderCap(bitmap: Bitmap, maxBytes: Int): ByteArray {
        val os = ByteArrayOutputStream()
        var quality = 92
        while (quality >= 45) {
            os.reset()
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, os)) {
                error("Не удалось сжать изображение")
            }
            if (os.size() <= maxBytes) return os.toByteArray()
            quality -= 10
        }
        error("Файл всё ещё больше ${maxBytes / (1024 * 1024)} МБ — выбери другое фото")
    }
}
