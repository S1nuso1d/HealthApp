package com.example.healtapp.data.repository

import com.example.healtapp.data.network.api.ActivityApi
import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.data.network.dto.activity.ActivityDto
import com.example.healtapp.domain.repository.ActivityRepository
import javax.inject.Inject

class ActivityRepositoryImpl @Inject constructor(
    private val api: ActivityApi,
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
        }
    }
}