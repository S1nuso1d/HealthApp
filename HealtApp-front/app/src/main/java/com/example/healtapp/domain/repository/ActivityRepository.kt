package com.example.healtapp.domain.repository

import com.example.healtapp.data.network.dto.activity.ActivityCreateRequestDto
import com.example.healtapp.data.network.dto.activity.ActivityDto

interface ActivityRepository {
    suspend fun getTodayActivity(): Result<ActivityDto?>
    suspend fun getActivityHistory(): Result<List<ActivityDto>>
    suspend fun createActivity(request: ActivityCreateRequestDto): Result<ActivityDto>
    suspend fun updateActivity(id: Int, request: ActivityCreateRequestDto): Result<ActivityDto>
    suspend fun deleteActivity(id: Int): Result<Unit>
}
