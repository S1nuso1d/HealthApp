package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.ActivityApi
import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.data.network.isOfflineLike
import com.example.healtapp.data.network.offline.OfflineActionQueue
import com.example.healtapp.domain.repository.ActivityRepository
import com.google.gson.Gson
import javax.inject.Inject

class ActivityRepositoryImpl @Inject constructor(
    private val api: ActivityApi,
    private val offlineActionQueue: OfflineActionQueue,
    private val gson: Gson,
) : ActivityRepository {

    override suspend fun getTodayActivity(): Result<ActivityDto?> {
        return runCatching {
            api.getTodayActivity()
        }
    }

    override suspend fun getActivityHistory(): Result<List<ActivityDto>> {
        return runCatching {
            api.getActivityHistory()
        }
    }

    override suspend fun createActivity(
        request: ActivityCreateRequestDto
    ): Result<ActivityDto> {
        return runCatching {
            api.createActivity(request)
        }.recoverCatching { e ->
            if (e.isOfflineLike()) {
                offlineActionQueue.enqueue(
                    path = "activity/",
                    method = "POST",
                    body = gson.toJson(request),
                )
                ActivityDto(
                    id = -1,
                    user_id = 0,
                    activity_type = request.activity_type,
                    start_time = request.start_time,
                    end_time = request.end_time,
                    duration_minutes = request.duration_minutes,
                    steps = request.steps,
                    distance_km = request.distance_km,
                    calories_burned = request.calories_burned,
                    intensity = request.intensity,
                    notes = request.notes,
                    source = request.source,
                )
            } else {
                throw e
            }
        }
    }

    override suspend fun updateActivity(
        id: Int,
        request: ActivityCreateRequestDto
    ): Result<ActivityDto> {
        return runCatching {
            api.updateActivity(id, request)
        }
    }

    override suspend fun deleteActivity(id: Int): Result<Unit> {
        return runCatching {
            api.deleteActivity(id)
            Unit
        }
    }
}
