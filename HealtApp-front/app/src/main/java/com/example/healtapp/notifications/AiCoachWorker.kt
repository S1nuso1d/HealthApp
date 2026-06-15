package com.example.healtapp.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healtapp.domain.repository.AiRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AiCoachWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val aiRepository: AiRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = aiRepository.getProactiveTip()
            if (result.isSuccess) {
                val tip = result.getOrNull()?.tip
                if (!tip.isNullOrBlank()) {
                    HealthNotificationHelper.aiCoachReminder(context, tip)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
