package com.example.healtapp.features.activity.live

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class WorkoutWeatherInfo(
    val temperatureC: Double? = null,
    val humidityPercent: Int? = null,
    val windKmh: Double? = null,
    val description: String? = null,
)

data class WorkoutSessionMeta(
    val weather: WorkoutWeatherInfo? = null,
    val photoUri: String? = null,
    /** 0 = верх кадра (по умолчанию), 0.5 = центр, 1 = низ. */
    val photoFocusY: Float = 0f,
    val avgPaceMinPerKm: Double? = null,
    val cadenceSpm: Int? = null,
    val mapPoints: Int = 0,
)

data class WorkoutNotesPayload(
    val meta: WorkoutSessionMeta? = null,
    val note: String = "",
)

object WorkoutNotesCodec {
    private val gson = Gson()
    private const val PREFIX = "[workout_meta]"

    fun encode(meta: WorkoutSessionMeta?, note: String): String {
        val cleanNote = note.trim()
        if (meta == null) return cleanNote
        return buildString {
            append(PREFIX)
            append(gson.toJson(meta))
            if (cleanNote.isNotEmpty()) {
                append('\n')
                append(cleanNote)
            }
        }
    }

    fun decode(raw: String?): WorkoutNotesPayload {
        if (raw.isNullOrBlank()) return WorkoutNotesPayload()
        if (!raw.startsWith(PREFIX)) return WorkoutNotesPayload(note = raw.trim())
        val rest = raw.removePrefix(PREFIX)
        val nl = rest.indexOf('\n')
        val json = if (nl >= 0) rest.substring(0, nl) else rest
        val note = if (nl >= 0) rest.substring(nl + 1).trim() else ""
        val meta = runCatching { gson.fromJson(json, WorkoutSessionMeta::class.java) }.getOrNull()
        return WorkoutNotesPayload(meta = meta, note = note)
    }
}

data class OpenMeteoCurrentResponse(
    val current: OpenMeteoCurrent? = null,
)

data class OpenMeteoCurrent(
    @SerializedName("temperature_2m") val temperature2m: Double? = null,
    @SerializedName("relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @SerializedName("wind_speed_10m") val windSpeed10m: Double? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
)

fun weatherCodeDescription(code: Int?): String = when (code) {
    0 -> "Ясно"
    1, 2 -> "Переменная облачность"
    3 -> "Пасмурно"
    45, 48 -> "Туман"
    51, 53, 55 -> "Морось"
    61, 63, 65 -> "Дождь"
    71, 73, 75 -> "Снег"
    80, 81, 82 -> "Ливень"
    95, 96, 99 -> "Гроза"
    else -> "Погода"
}
