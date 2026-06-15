package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.SleepApi
import com.example.healtapp.data.network.dto.sleep.CreateSleepRequestDto
import com.example.healtapp.data.network.dto.sleep.SleepDto
import com.example.healtapp.data.network.offline.OfflineActionQueue
import com.example.healtapp.domain.repository.SleepRepository
import com.google.gson.Gson
import java.net.ConnectException
import java.net.UnknownHostException
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SleepRepositoryImpl @Inject constructor(
    private val sleepApi: SleepApi,
    private val offlineActionQueue: OfflineActionQueue,
    private val gson: Gson
) : SleepRepository {

    override suspend fun getSleepHistory(): Result<List<SleepDto>> {
        return runCatching {
            sleepApi.getSleepHistory()
        }
    }

    override suspend fun addSleep(request: CreateSleepRequestDto): Result<SleepDto> {
        return runCatching {
            sleepApi.addSleep(request)
        }.recoverCatching { e ->
            if (e is UnknownHostException || e is ConnectException) {
                offlineActionQueue.enqueue(
                    path = "sleep/",
                    method = "POST",
                    body = gson.toJson(request)
                )
                
                // Calculate duration locally for dummy object
                val start = LocalDateTime.parse(request.sleep_start, DateTimeFormatter.ISO_DATE_TIME)
                val end = LocalDateTime.parse(request.sleep_end, DateTimeFormatter.ISO_DATE_TIME)
                val hours = Duration.between(start, end).toMinutes() / 60f
                
                SleepDto(
                    id = -1,
                    user_id = 0,
                    sleep_start = request.sleep_start,
                    sleep_end = request.sleep_end,
                    duration_hours = hours,
                    quality_score = request.quality_score,
                    notes = request.notes
                )
            } else throw e
        }
    }

    override suspend fun updateSleep(id: Int, request: CreateSleepRequestDto): Result<SleepDto> {
        return runCatching {
            sleepApi.updateSleep(id, request)
        }.recoverCatching { e ->
             if (e is UnknownHostException || e is ConnectException) {
                offlineActionQueue.enqueue(
                    path = "sleep/$id",
                    method = "PUT",
                    body = gson.toJson(request)
                )
                val start = LocalDateTime.parse(request.sleep_start, DateTimeFormatter.ISO_DATE_TIME)
                val end = LocalDateTime.parse(request.sleep_end, DateTimeFormatter.ISO_DATE_TIME)
                val hours = Duration.between(start, end).toMinutes() / 60f
                
                SleepDto(
                    id = id,
                    user_id = 0,
                    sleep_start = request.sleep_start,
                    sleep_end = request.sleep_end,
                    duration_hours = hours,
                    quality_score = request.quality_score,
                    notes = request.notes
                )
            } else throw e
        }
    }

    override suspend fun deleteSleep(id: Int): Result<Unit> {
        return runCatching {
            sleepApi.deleteSleep(id)
            Unit
        }.recoverCatching { e ->
            if (e is UnknownHostException || e is ConnectException) {
                offlineActionQueue.enqueue(
                    path = "sleep/$id",
                    method = "DELETE",
                    body = "{}"
                )
            } else throw e
        }
    }
}