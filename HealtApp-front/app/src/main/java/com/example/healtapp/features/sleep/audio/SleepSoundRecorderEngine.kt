package com.example.healtapp.features.sleep.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import kotlin.math.sqrt

/**
 * Monitors ambient audio and saves short WAV clips when activity is detected.
 */
class SleepSoundRecorderEngine(
    private val storage: SleepSoundStorage,
    private val onClipSaved: (SleepSoundClip) -> Unit,
    private val onTick: (clipsCount: Int, isRecordingClip: Boolean) -> Unit,
) {
    @Volatile
    private var running = false

    private var workerThread: Thread? = null
    private var clipsThisSession = 0

    fun start() {
        if (running) return
        running = true
        clipsThisSession = 0
        workerThread = Thread(::monitorLoop, "SleepSoundMonitor").also { it.start() }
    }

    fun stop() {
        running = false
        workerThread?.interrupt()
        workerThread = null
    }

    @Suppress("DEPRECATION")
    private fun monitorLoop() {
        val sampleRate = SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuffer <= 0) return

        val bufferSize = minBuffer * 2
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return
        }

        val readBuffer = ShortArray(bufferSize / 2)
        val preBuffer = ArrayDeque<ByteArray>()
        val preBufferMaxChunks = (PRE_BUFFER_MS / CHUNK_MS).coerceAtLeast(1)

        var loudStreak = 0
        var quietStreak = 0
        var isRecordingClip = false
        var clipChunks = mutableListOf<ByteArray>()
        var clipPeakRms = 0.0
        var clipStartedAt = 0L

        recorder.startRecording()
        try {
            while (running) {
                val read = recorder.read(readBuffer, 0, readBuffer.size)
                if (read <= 0) continue

                val rms = computeRms(readBuffer, read)
                val chunk = shortsToBytes(readBuffer, read)

                if (!isRecordingClip) {
                    preBuffer.addLast(chunk)
                    while (preBuffer.size > preBufferMaxChunks) preBuffer.removeFirst()

                    if (rms >= TRIGGER_RMS) {
                        loudStreak++
                    } else {
                        loudStreak = 0
                    }

                    if (loudStreak >= LOUD_STREAK_REQUIRED) {
                        isRecordingClip = true
                        clipChunks = mutableListOf()
                        preBuffer.forEach { clipChunks.add(it) }
                        preBuffer.clear()
                        clipPeakRms = rms
                        clipStartedAt = System.currentTimeMillis()
                        quietStreak = 0
                        loudStreak = 0
                    }
                } else {
                    clipChunks.add(chunk)
                    clipPeakRms = maxOf(clipPeakRms, rms)

                    if (rms < TRIGGER_RMS * QUIET_RATIO) {
                        quietStreak++
                    } else {
                        quietStreak = 0
                    }

                    val clipDurationMs = System.currentTimeMillis() - clipStartedAt
                    val shouldFinish = quietStreak >= QUIET_STREAK_REQUIRED ||
                        clipDurationMs >= MAX_CLIP_MS

                    if (shouldFinish) {
                        finalizeClip(clipChunks, clipPeakRms, clipStartedAt)
                        clipsThisSession++
                        isRecordingClip = false
                        clipChunks = mutableListOf()
                        quietStreak = 0
                        loudStreak = 0
                    }
                }

                onTick(clipsThisSession, isRecordingClip)
            }

            if (isRecordingClip && clipChunks.isNotEmpty()) {
                finalizeClip(clipChunks, clipPeakRms, clipStartedAt)
            }
        } finally {
            runCatching {
                recorder.stop()
            }
            recorder.release()
        }
    }

    private fun finalizeClip(chunks: List<ByteArray>, peakRms: Double, startedAt: Long) {
        if (chunks.isEmpty()) return
        val pcm = chunks.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
        if (pcm.size < MIN_PCM_BYTES) return

        val outFile = storage.createClipFile()
        writeWav(outFile, pcm, SAMPLE_RATE)

        val durationMs = ((pcm.size / 2.0) / SAMPLE_RATE * 1000).toInt()
        val clip = SleepSoundClip(
            id = outFile.nameWithoutExtension,
            fileName = outFile.name,
            recordedAtEpochMs = startedAt,
            durationMs = durationMs.coerceAtLeast(1),
            peakRms = peakRms.toInt(),
            label = classifySound(peakRms),
        )
        storage.saveClip(clip)
        onClipSaved(clip)
    }

    private fun computeRms(buffer: ShortArray, read: Int): Double {
        if (read == 0) return 0.0
        var sum = 0.0
        for (i in 0 until read) {
            val v = buffer[i].toDouble()
            sum += v * v
        }
        return sqrt(sum / read)
    }

    private fun shortsToBytes(buffer: ShortArray, read: Int): ByteArray {
        val bytes = ByteArray(read * 2)
        var i = 0
        while (i < read) {
            bytes[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (buffer[i].toInt() shr 8 and 0xFF).toByte()
            i++
        }
        return bytes
    }

    private fun writeWav(file: File, pcm: ByteArray, sampleRate: Int) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = pcm.size + 36

        RandomAccessFile(file, "rw").use { out ->
            out.setLength(0)
            out.write("RIFF".toByteArray())
            out.write(intToLittleEndian(totalDataLen))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToLittleEndian(16))
            out.write(shortToLittleEndian(1))
            out.write(shortToLittleEndian(channels.toShort()))
            out.write(intToLittleEndian(sampleRate))
            out.write(intToLittleEndian(byteRate))
            out.write(shortToLittleEndian((channels * bitsPerSample / 8).toShort()))
            out.write(shortToLittleEndian(bitsPerSample.toShort()))
            out.write("data".toByteArray())
            out.write(intToLittleEndian(pcm.size))
            out.write(pcm)
        }
    }

    private fun intToLittleEndian(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun shortToLittleEndian(value: Short): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHUNK_MS = 100
        private const val PRE_BUFFER_MS = 600
        private const val TRIGGER_RMS = 500.0
        private const val QUIET_RATIO = 0.5
        private const val LOUD_STREAK_REQUIRED = 2
        private const val QUIET_STREAK_REQUIRED = 8
        private const val MAX_CLIP_MS = 30_000
        private const val MIN_PCM_BYTES = SAMPLE_RATE * 2 / 10

        fun classifySound(peakRms: Double): String = when {
            peakRms >= 1800 -> "Громкий звук"
            peakRms >= 800 -> "Храп"
            else -> "Шум"
        }
    }
}
