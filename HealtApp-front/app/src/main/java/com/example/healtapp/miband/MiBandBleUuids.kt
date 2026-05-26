package com.example.healtapp.miband

import java.util.UUID

/**
 * BLE GATT UUIDs for Xiaomi protobuf devices (Mi Band 8, Redmi Band 3, etc.).
 * Service fe95 with encrypted characteristics 51/52/53/55.
 */
object MiBandBleUuids {
    val SERVICE: UUID = UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb")
    val CHAR_COMMAND_READ: UUID = UUID.fromString("00000051-0000-1000-8000-00805f9b34fb")
    val CHAR_COMMAND_WRITE: UUID = UUID.fromString("00000052-0000-1000-8000-00805f9b34fb")
    val CHAR_ACTIVITY_DATA: UUID = UUID.fromString("00000053-0000-1000-8000-00805f9b34fb")
    val CHAR_DATA_UPLOAD: UUID = UUID.fromString("00000055-0000-1000-8000-00805f9b34fb")

    val CLIENT_CHARACTERISTIC_CONFIG: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val PAYLOAD_ACK = byteArrayOf(0, 0, 3, 0)
    val PAYLOAD_CHUNK_START_ACK = byteArrayOf(0x00, 0x00, 0x01, 0x01)
    val PAYLOAD_CHUNK_END_ACK = byteArrayOf(0x00, 0x00, 0x01, 0x00)

    fun isLikelyMiBandName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val n = name.lowercase()
        return n.contains("mi band") ||
            n.contains("mi smart band") ||
            n.contains("xiaomi band") ||
            n.contains("smart band 8") ||
            n.contains("redmi band") ||
            n.contains("m2239") // Mi Band 8 hardware id prefix in some BLE names
    }
}
