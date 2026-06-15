package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.HydrationApi
import com.example.healtapp.data.network.dto.hydration.CreateHydrationRequestDto
import com.example.healtapp.data.network.dto.hydration.HydrationDto
import com.example.healtapp.data.network.dto.hydration.HydrationSummaryDto
import com.example.healtapp.data.network.offline.OfflineActionQueue
import com.example.healtapp.domain.repository.HydrationRepository
import com.google.gson.Gson
import java.net.ConnectException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class HydrationRepositoryImpl @Inject constructor(
    private val hydrationApi: HydrationApi,
    private val offlineActionQueue: OfflineActionQueue,
    private val gson: Gson
) : HydrationRepository {

    override suspend fun getHydrationHistory(): Result<List<HydrationDto>> {
        return runCatching {
            hydrationApi.getHydrationHistory()
        }
    }

    override suspend fun getTodayHydrationSummary(): Result<HydrationSummaryDto> {
        return runCatching {
            hydrationApi.getTodayHydrationSummary()
        }
    }

    override suspend fun addHydration(amountMl: Int): Result<HydrationDto> {
        val requestDto = CreateHydrationRequestDto(amount_ml = amountMl)
        return runCatching {
            hydrationApi.addHydration(requestDto)
        }.recoverCatching { e ->
            if (e is UnknownHostException || e is ConnectException) {
                offlineActionQueue.enqueue(
                    path = "hydration/",
                    method = "POST",
                    body = gson.toJson(requestDto)
                )
                // Return a dummy object for offline mode
                HydrationDto(
                    id = -1,
                    user_id = 0,
                    amount_ml = amountMl,
                    record_time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    source = "offline"
                )
            } else {
                throw e
            }
        }
    }

    override suspend fun updateHydration(
        id: Int,
        amountMl: Int,
        recordTimeIso: String?,
    ): Result<HydrationDto> {
        val requestDto = CreateHydrationRequestDto(
            amount_ml = amountMl,
            record_time = recordTimeIso,
            source = "manual",
        )
        return runCatching {
            hydrationApi.updateHydration(id, requestDto)
        }.recoverCatching { e ->
            if (e is UnknownHostException || e is ConnectException) {
                offlineActionQueue.enqueue(
                    path = "hydration/$id",
                    method = "PUT",
                    body = gson.toJson(requestDto)
                )
                HydrationDto(
                    id = id,
                    user_id = 0,
                    amount_ml = amountMl,
                    record_time = recordTimeIso ?: LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    source = "offline"
                )
            } else {
                throw e
            }
        }
    }

    override suspend fun deleteHydration(id: Int): Result<Unit> {
        return runCatching {
            hydrationApi.deleteHydration(id)
            Unit
        }.recoverCatching { e ->
            if (e is UnknownHostException || e is ConnectException) {
                offlineActionQueue.enqueue(
                    path = "hydration/$id",
                    method = "DELETE",
                    body = "{}"
                )
            } else {
                throw e
            }
        }
    }
}