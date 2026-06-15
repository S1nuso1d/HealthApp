package com.example.healtapp.data.healthconnect

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log
import com.example.healtapp.domain.repository.ImportRepository

@HiltWorker
class HealthConnectSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthConnectReader: HealthConnectReader,
    private val importRepository: ImportRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i("HealthConnectSyncWorker", "Starting background sync")
        try {
            if (!healthConnectReader.isHealthConnectUsable()) {
                return Result.success()
            }
            val permitted = runCatching { healthConnectReader.areReadPermissionsGranted() }.getOrDefault(false)
            if (!permitted) {
                return Result.success()
            }

            val batch = healthConnectReader.buildImportPayload(14).getOrElse {
                return Result.retry()
            }
            val result = importRepository.importBatch(batch)
            if (result.isSuccess) {
                Log.i("HealthConnectSyncWorker", "Background sync completed successfully")
                return Result.success()
            } else {
                Log.e("HealthConnectSyncWorker", "Background sync failed: ${result.exceptionOrNull()?.message}")
                return Result.retry()
            }
        } catch (e: Exception) {
            Log.e("HealthConnectSyncWorker", "Error in background sync", e)
            return Result.retry()
        }
    }
}
