package com.example.healtapp.wear

import android.util.Log
import com.example.healtapp.domain.repository.HydrationRepository
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HealthWearableListenerService : WearableListenerService() {

    @Inject
    lateinit var hydrationRepository: HydrationRepository

    @Inject
    lateinit var widgetSnapshotStore: com.example.healtapp.data.preferences.WidgetSnapshotStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.d("WearableListener", "Received message: ${messageEvent.path}")

        if (messageEvent.path == "/water/add") {
            serviceScope.launch {
                try {
                    val result = hydrationRepository.addHydration(250)
                    result.fold(
                        onSuccess = {
                            Log.d("WearableListener", "Successfully added 250ml of water via Wear OS")
                            widgetSnapshotStore.load()?.let { snap ->
                                val updated = snap.copy(waterMl = snap.waterMl + 250)
                                widgetSnapshotStore.save(updated)
                                WearSnapshotSync.push(applicationContext, updated)
                            }
                        },
                        onFailure = { error ->
                            Log.e("WearableListener", "Failed to add water: ${error.message}", error)
                        }
                    )
                } catch (e: Exception) {
                    Log.e("WearableListener", "Exception while adding water", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
