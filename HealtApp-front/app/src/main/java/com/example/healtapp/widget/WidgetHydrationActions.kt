package com.example.healtapp.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.healtapp.data.preferences.PendingSyncStore
import com.example.healtapp.data.preferences.WidgetSnapshot
import com.example.healtapp.data.preferences.WidgetSnapshotStore
import com.example.healtapp.features.hydration.widget.SyncWaterWorker

object WidgetHydrationActions {

    suspend fun addWater(context: Context, amountMl: Int) {
        if (amountMl <= 0) return

        val snapshotStore = WidgetSnapshotStore(context)
        val current = snapshotStore.load() ?: WidgetSnapshot()
        snapshotStore.save(current.copy(waterMl = current.waterMl + amountMl))

        PendingSyncStore(context).enqueueHydration(amountMl)

        val workRequest = OneTimeWorkRequestBuilder<SyncWaterWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncWaterWork",
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )

        refreshAllWidgets(context)
    }
}
