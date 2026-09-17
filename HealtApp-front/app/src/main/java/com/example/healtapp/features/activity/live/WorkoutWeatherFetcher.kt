package com.example.healtapp.features.activity.live

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object WorkoutWeatherFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun fetch(lat: Double, lon: Double): WorkoutWeatherInfo? = withContext(Dispatchers.IO) {
        val url =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code" +
                "&wind_speed_unit=kmh&timezone=auto"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "HealthApp/1.0 (Android)")
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                val parsed = gson.fromJson(body, OpenMeteoCurrentResponse::class.java)
                val cur = parsed.current ?: return@use null
                WorkoutWeatherInfo(
                    temperatureC = cur.temperature2m,
                    humidityPercent = cur.relativeHumidity2m,
                    windKmh = cur.windSpeed10m,
                    description = weatherCodeDescription(cur.weatherCode),
                )
            }
        }.getOrNull()
    }
}
