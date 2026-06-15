package com.example.healtapp.features.sleep.audio

data class SleepSoundClip(
    val id: String,
    val fileName: String,
    val recordedAtEpochMs: Long,
    val durationMs: Int,
    val peakRms: Int,
    val label: String,
)
