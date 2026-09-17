package com.example.healtapp.data.network.offline

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineActionQueue: OfflineActionQueue,
    private val okHttpClient: OkHttpClient,
    private val serverConfig: com.example.healtapp.data.network.ApiServerConfig,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val actions = offlineActionQueue.getActions()
        
        if (actions.isEmpty()) {
            return@withContext Result.success()
        }

        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val base = serverConfig.baseUrl().trimEnd('/')

        for (action in actions) {
            val url = "$base/${action.path.removePrefix("/")}"
            
            val requestBuilder = Request.Builder().url(url)
            
            when (action.method.uppercase()) {
                "POST" -> requestBuilder.post(action.body.toRequestBody(jsonMediaType))
                "PUT" -> requestBuilder.put(action.body.toRequestBody(jsonMediaType))
                "DELETE" -> requestBuilder.delete()
                "PATCH" -> requestBuilder.patch(action.body.toRequestBody(jsonMediaType))
            }
            
            val request = requestBuilder.build()
            
            try {
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful || response.code in 400..499) {
                    // Remove if successful or if it's a client error (to avoid getting stuck)
                    offlineActionQueue.removeAction(action.id)
                } else if (response.code >= 500) {
                    // Server error, might be temporary, but let's stop and retry later
                    return@withContext Result.retry()
                }
            } catch (e: Exception) {
                // Network error, stop processing and retry later
                return@withContext Result.retry()
            }
        }
        
        Result.success()
    }
}