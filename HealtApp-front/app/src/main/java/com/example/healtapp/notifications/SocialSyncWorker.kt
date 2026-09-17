package com.example.healtapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healtapp.data.preferences.SocialPrefs
import com.example.healtapp.di.ReminderEntryPoint
import dagger.hilt.android.EntryPointAccessors

class SocialSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!HealthNotificationHelper.canPost(applicationContext)) {
            return Result.success()
        }

        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )

        if (entry.tokenStorage().getToken() == null) {
            return Result.success()
        }

        val repository = entry.socialRepository()
        val result = repository.pendingFriends()

        if (result.isSuccess) {
            val pendingFriends = result.getOrNull()?.incoming ?: emptyList()
            val currentPendingIds = pendingFriends.map { it.user_id.toString() }.toSet()

            val prefs = SocialPrefs(applicationContext)
            val knownPendingIds = prefs.getKnownPendingFriends()

            val newPendingIds = currentPendingIds - knownPendingIds

            if (newPendingIds.isNotEmpty()) {
                val newFriends = pendingFriends.filter { it.user_id.toString() in newPendingIds }
                for (newFriend in newFriends) {
                    HealthNotificationHelper.friendRequestReminder(
                        applicationContext,
                        newFriend.display_name,
                        newFriend.user_id
                    )
                }
            }

            prefs.saveKnownPendingFriends(currentPendingIds)
        }

        return Result.success()
    }
}
