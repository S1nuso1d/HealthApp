package com.example.healtapp.miband

import android.bluetooth.BluetoothGattCharacteristic
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList

/**
 * Framing for Xiaomi BLE v1 command characteristic (encrypted Mi Band 8).
 */
class MiBandBleTransport(
    private val authSession: MiBandAuthSession,
    private val onCommandPayload: (ByteArray) -> Unit,
    private val writeGatt: (ByteArray) -> Boolean,
    private val maxWriteSizeProvider: () -> Int,
) {
    private val sendQueue = LinkedList<ByteArray>()
    private var waitingAck = false
    private var sendingChunked = false
    private var currentPayload: ByteArray? = null
    private var encryptedIndex = 1

    private val receivedChunks = mutableMapOf<Int, ByteArray>()
    private var numChunks = 0

    fun reset() {
        sendQueue.clear()
        waitingAck = false
        sendingChunked = false
        currentPayload = null
        encryptedIndex = 1
        receivedChunks.clear()
        numChunks = 0
    }

    fun enqueueCommand(commandBytes: ByteArray) {
        sendQueue.add(commandBytes)
        pumpSendQueue()
    }

    fun onNotify(value: ByteArray) {
        val buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
        val chunkId = buf.short.toInt() and 0xFFFF
        if (chunkId != 0) {
            val chunkBytes = ByteArray(buf.remaining())
            buf.get(chunkBytes)
            receivedChunks[chunkId] = chunkBytes
            if (receivedChunks.size >= numChunks && numChunks > 0) {
                val assembled = reconstructChunks()
                receivedChunks.clear()
                numChunks = 0
                writeGatt(MiBandBleUuids.PAYLOAD_CHUNK_END_ACK)
                onCommandPayload(assembled)
            }
            return
        }

        val type = buf.get().toInt() and 0xFF
        when (type) {
            0 -> handleChunkedStart(buf)
            1 -> { /* chunked ack from watch */ }
            2 -> handleSingleCommand(buf)
            3 -> handleAck(buf.get().toInt() and 0xFF)
        }
    }

    private fun handleChunkedStart(buf: ByteBuffer) {
        val subtype = buf.get().toInt() and 0xFF
        if (subtype == 0) {
            val encrypted = buf.get().toInt() and 0xFF
            numChunks = buf.short.toInt() and 0xFFFF
            receivedChunks.clear()
            writeGatt(MiBandBleUuids.PAYLOAD_CHUNK_START_ACK)
            if (encrypted == 0 && !authSession.encryptionReady) {
                // plaintext chunked — rare on band 8
            }
        }
    }

    private fun handleSingleCommand(buf: ByteBuffer) {
        writeGatt(MiBandBleUuids.PAYLOAD_ACK)
        val encryptedFlag = buf.get().toInt() and 0xFF
        val plain = if (encryptedFlag == 1) {
            if (buf.remaining() >= 2) buf.short // encrypted index
            val enc = ByteArray(buf.remaining())
            buf.get(enc)
            authSession.decryptPayload(enc)
        } else {
            val raw = ByteArray(buf.remaining())
            buf.get(raw)
            raw
        }
        onCommandPayload(plain)
    }

    private fun handleAck(result: Int) {
        waitingAck = false
        sendingChunked = false
        currentPayload = null
        if (result == 0) pumpSendQueue()
    }

    private fun reconstructChunks(): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        for (i in 1..numChunks) {
            val chunk = receivedChunks[i] ?: return ByteArray(0)
            out.write(chunk)
        }
        return out.toByteArray()
    }

    private fun pumpSendQueue() {
        if (waitingAck || sendingChunked) return
        val payload = sendQueue.poll() ?: return
        currentPayload = payload

        val toSend = if (authSession.encryptionReady) {
            authSession.encryptPayload(payload)
        } else {
            payload
        }

        val maxWrite = maxWriteSizeProvider()
        if (toSend.size + 6 > maxWrite) {
            sendChunkedStart(toSend, maxWrite)
        } else {
            sendSingle(toSend)
        }
    }

    private fun sendSingle(encryptedPayload: ByteArray) {
        val buf = ByteBuffer.allocate(6 + encryptedPayload.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(0)
        buf.put(2) // single command
        buf.put(if (authSession.encryptionReady) 1 else 2)
        if (authSession.encryptionReady) {
            buf.putShort((encryptedIndex++).toShort())
        }
        buf.put(encryptedPayload)
        waitingAck = true
        writeGatt(buf.array())
    }

    private fun sendChunkedStart(payload: ByteArray, maxWrite: Int) {
        val chunkPayloadSize = maxWrite - 2
        val chunks = (payload.size + chunkPayloadSize - 1) / chunkPayloadSize
        val header = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(0)
            put(0)
            put(if (authSession.encryptionReady) 1 else 0)
            putShort(chunks.toShort())
        }.array()
        sendingChunked = true
        writeGatt(header)
        for (i in 0 until chunks) {
            val start = i * chunkPayloadSize
            val end = minOf(start + chunkPayloadSize, payload.size)
            val chunk = ByteBuffer.allocate(2 + end - start).order(ByteOrder.LITTLE_ENDIAN).apply {
                putShort((i + 1).toShort())
                put(payload, start, end - start)
            }.array()
            writeGatt(chunk)
        }
    }
}
