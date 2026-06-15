package com.example.healtapp.features.sleep.audio

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
        return runCatching {
            val type = object : TypeToken<List<SleepSoundClip>>() {}.type
            gson.fromJson<List<SleepSoundClip>>(indexFile.readText(), type) ?: emptyList()
        }.getOrElse { emptyList() }
            .sortedByDescending { it.recordedAtEpochMs }
    }

    fun clipFile(clip: SleepSoundClip): File = File(rootDir, clip.fileName)

    fun createClipFile(): File {
        val name = "clip_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.wav"
        return File(rootDir, name)
    }

    fun saveClip(clip: SleepSoundClip) {
        val current = loadClips().toMutableList()
        current.add(0, clip)
        persist(current)
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

    companion object {
        private const val INDEX_FILE = "clips_index.json"
    }
}
