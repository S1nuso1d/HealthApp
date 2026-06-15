package com.example.healtapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hydrationPrefsStore by preferencesDataStore(name = "hydration_prefs")

@Singleton
class HydrationPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val customQuickKey = stringPreferencesKey("custom_quick_ml")

    val customQuickAmountsFlow: Flow<List<Int>> = context.hydrationPrefsStore.data.map { prefs ->
        decodeAmounts(prefs[customQuickKey].orEmpty())
    }

    suspend fun addCustomQuickAmount(ml: Int): Boolean {
        if (ml !in MIN_ML..MAX_ML) return false
        var added = false
        context.hydrationPrefsStore.edit { prefs ->
            val current = decodeAmounts(prefs[customQuickKey].orEmpty()).toMutableList()
            if (DEFAULT_QUICK_AMOUNTS.contains(ml) || current.contains(ml)) return@edit
            if (current.size >= MAX_CUSTOM_BUTTONS) return@edit
            current.add(ml)
            prefs[customQuickKey] = encodeAmounts(current)
            added = true
        }
        return added
    }

    suspend fun removeCustomQuickAmount(ml: Int) {
        context.hydrationPrefsStore.edit { prefs ->
            val current = decodeAmounts(prefs[customQuickKey].orEmpty()).filterNot { it == ml }
            prefs[customQuickKey] = encodeAmounts(current)
        }
    }

    private fun encodeAmounts(amounts: List<Int>): String =
        amounts.joinToString(",")

    private fun decodeAmounts(raw: String): List<Int> =
        raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in MIN_ML..MAX_ML }

    companion object {
        val DEFAULT_QUICK_AMOUNTS = listOf(200, 250, 500)
        const val MAX_CUSTOM_BUTTONS = 6
        const val MIN_ML = 10
        const val MAX_ML = 5000
    }
}
