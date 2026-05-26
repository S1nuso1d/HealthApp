package com.example.healtapp.data.miband

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.miBandDataStore by preferencesDataStore(name = "miband_ble")

data class MiBandSavedDevice(
    val address: String,
    val name: String,
    val authKeyHex: String,
    val lastSteps: Int = 0,
    val lastHeartRate: Int = 0,
    val lastCalories: Int = 0,
    val lastSyncEpochMs: Long = 0L,
)

@Singleton
class MiBandPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ADDRESS = stringPreferencesKey("device_address")
    private val NAME = stringPreferencesKey("device_name")
    private val AUTH_KEY = stringPreferencesKey("auth_key_hex")
    private val LAST_STEPS = intPreferencesKey("last_steps")
    private val LAST_HR = intPreferencesKey("last_hr")
    private val LAST_CAL = intPreferencesKey("last_calories")
    private val LAST_SYNC = stringPreferencesKey("last_sync_ms")

    val savedDevice: Flow<MiBandSavedDevice?> = context.miBandDataStore.data.map { prefs ->
        val address = prefs[ADDRESS] ?: return@map null
        MiBandSavedDevice(
            address = address,
            name = prefs[NAME].orEmpty(),
            authKeyHex = prefs[AUTH_KEY].orEmpty(),
            lastSteps = prefs[LAST_STEPS] ?: 0,
            lastHeartRate = prefs[LAST_HR] ?: 0,
            lastCalories = prefs[LAST_CAL] ?: 0,
            lastSyncEpochMs = prefs[LAST_SYNC]?.toLongOrNull() ?: 0L,
        )
    }

    suspend fun saveDevice(address: String, name: String, authKeyHex: String) {
        context.miBandDataStore.edit {
            it[ADDRESS] = address
            it[NAME] = name
            it[AUTH_KEY] = authKeyHex.trim()
        }
    }

    suspend fun updateLastSync(steps: Int, heartRate: Int, calories: Int) {
        context.miBandDataStore.edit {
            it[LAST_STEPS] = steps
            it[LAST_HR] = heartRate
            it[LAST_CAL] = calories
            it[LAST_SYNC] = System.currentTimeMillis().toString()
        }
    }

    suspend fun clear() {
        context.miBandDataStore.edit { it.clear() }
    }
}
