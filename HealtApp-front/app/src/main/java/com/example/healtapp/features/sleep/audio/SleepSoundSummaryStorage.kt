package com.example.healtapp.features.sleep.audio

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class SleepSoundDaySummary(
    val dateKey: String,
    val summary: String,
    val generatedAtEpochMs: Long,
)

@Singleton
class SleepSoundSummaryStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()
    private val indexFile: File
        get() {
            File(context.filesDir, "sleep_sounds").mkdirs()
            return File(context.filesDir, "sleep_sounds/$INDEX_FILE")
        }

    fun loadAll(): Map<String, SleepSoundDaySummary> {
        if (!indexFile.exists()) return emptyMap()
        return runCatching {
            val type = object : TypeToken<List<SleepSoundDaySummary>>() {}.type
            val list = gson.fromJson<List<SleepSoundDaySummary>>(indexFile.readText(), type).orEmpty()
            list.associateBy { it.dateKey }
        }.getOrElse { emptyMap() }
    }

    fun get(dateKey: String): SleepSoundDaySummary? = loadAll()[dateKey]

    fun save(dateKey: String, summary: String) {
        val trimmed = summary.trim()
        if (trimmed.isEmpty()) return
        val current = loadAll().toMutableMap()
        current[dateKey] = SleepSoundDaySummary(
            dateKey = dateKey,
            summary = trimmed,
            generatedAtEpochMs = System.currentTimeMillis(),
        )
        persist(current.values.toList())
    }

    fun delete(dateKey: String) {
        val current = loadAll().toMutableMap()
        if (current.remove(dateKey) == null) return
        persist(current.values.toList())
    }

    fun pruneToRetention() {
        val zone = ZoneId.systemDefault()
        val cutoff = Instant.now().minusSeconds(SleepSoundStorage.RETENTION_DAYS * 24L * 3600)
            .atZone(zone)
            .toLocalDate()
            .toString()
        val filtered = loadAll().values.filter { it.dateKey >= cutoff }
        if (filtered.size != loadAll().size) {
            persist(filtered)
        }
    }

    fun deleteOrphans(existingClipDateKeys: Set<String>) {
        val summaries = loadAll()
        val toKeep = summaries.values.filter { it.dateKey in existingClipDateKeys }
        if (toKeep.size != summaries.size) {
            persist(toKeep)
        }
    }

    private fun persist(summaries: List<SleepSoundDaySummary>) {
        indexFile.parentFile?.mkdirs()
        if (summaries.isEmpty()) {
            if (indexFile.exists()) indexFile.delete()
            return
        }
        indexFile.writeText(gson.toJson(summaries.sortedByDescending { it.dateKey }))
    }

    private companion object {
        const val INDEX_FILE = "summaries_index.json"
    }
}
