package com.example.healtapp.features.sleep.audio

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepSoundStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()
    private val rootDir: File by lazy {
        File(context.filesDir, "sleep_sounds").apply { mkdirs() }
    }

    private val indexFile: File
        get() = File(rootDir, INDEX_FILE)

    fun clipsDir(): File = rootDir

    fun loadClips(): List<SleepSoundClip> {
        if (!indexFile.exists()) return emptyList()
        val raw = runCatching {
            val type = object : TypeToken<List<SleepSoundClip>>() {}.type
            gson.fromJson<List<SleepSoundClip>>(indexFile.readText(), type) ?: emptyList()
        }.getOrElse { emptyList() }
        val pruned = pruneClips(raw, deleteRemovedFiles = true)
        if (pruned.size != raw.size) {
            persist(pruned)
        }
        return pruned.sortedByDescending { it.recordedAtEpochMs }
    }

    fun clipFile(clip: SleepSoundClip): File = File(rootDir, clip.fileName)

    fun createClipFile(): File {
        val name = "clip_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.wav"
        return File(rootDir, name)
    }

    fun saveClip(clip: SleepSoundClip) {
        val current = loadClips().toMutableList()
        current.add(0, clip)
        persist(pruneClips(current, deleteRemovedFiles = true))
    }

    fun deleteClip(id: String): Boolean {
        val current = loadClips()
        val target = current.find { it.id == id } ?: return false
        clipFile(target).delete()
        persist(current.filterNot { it.id == id })
        return true
    }

    fun deleteAllClips() {
        loadClips().forEach { clipFile(it).delete() }
        if (indexFile.exists()) indexFile.delete()
    }

    private fun persist(clips: List<SleepSoundClip>) {
        indexFile.writeText(gson.toJson(clips))
    }

    private fun pruneClips(clips: List<SleepSoundClip>, deleteRemovedFiles: Boolean = false): List<SleepSoundClip> {
        val zone = ZoneId.systemDefault()
        val cutoff = Instant.now().minusSeconds(RETENTION_DAYS * 24L * 3600).toEpochMilli()
        val pruned = clips
            .filter { it.recordedAtEpochMs >= cutoff }
            .groupBy { clipDayKey(it.recordedAtEpochMs, zone) }
            .flatMap { (_, dayClips) ->
                dayClips.sortedByDescending { it.recordedAtEpochMs }.take(MAX_CLIPS_PER_DAY)
            }
            .sortedByDescending { it.recordedAtEpochMs }
        if (deleteRemovedFiles) {
            val removed = clips.filter { existing -> pruned.none { it.id == existing.id } }
            removed.forEach { clipFile(it).delete() }
        }
        return pruned
    }

    companion object {
        private const val INDEX_FILE = "clips_index.json"
        const val RETENTION_DAYS = 14
        const val MAX_CLIPS_PER_DAY = 40

        fun clipDayKey(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
            Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().toString()
    }
}
