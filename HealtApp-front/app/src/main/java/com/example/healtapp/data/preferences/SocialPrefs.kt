package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.socialDataStore by preferencesDataStore(name = "social_prefs")

class SocialPrefs(private val context: Context) {

    companion object {
        private val KNOWN_PENDING_FRIENDS = stringSetPreferencesKey("known_pending_friends")
    }

    suspend fun getKnownPendingFriends(): Set<String> {
        return context.socialDataStore.data.map { prefs ->
            prefs[KNOWN_PENDING_FRIENDS] ?: emptySet()
        }.first()
    }

    suspend fun saveKnownPendingFriends(userIds: Set<String>) {
        context.socialDataStore.edit { prefs ->
            prefs[KNOWN_PENDING_FRIENDS] = userIds
        }
    }
}
