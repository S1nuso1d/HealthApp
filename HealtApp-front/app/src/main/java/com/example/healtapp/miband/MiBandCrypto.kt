package com.example.healtapp.miband

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.CCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object MiBandCrypto {

    private val MIVEAR_AUTH = "miwear-auth".toByteArray(Charsets.UTF_8)

    fun parseAuthKeyHex(input: String): ByteArray {
        val trimmed = input.trim()
        val hex = when {
            trimmed.startsWith("0x", ignoreCase = true) && trimmed.length >= 4 ->
                trimmed.substring(2).replace(" ", "")
            else -> trimmed.replace(" ", "").replace(":", "")
        }
        require(hex.length >= 32) { "Ключ должен быть 32 hex-символа (16 байт)" }
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray().copyOf(16)
    }

    fun computeAuthStep3Hmac(secretKey: ByteArray, phoneNonce: ByteArray, watchNonce: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(phoneNonce + watchNonce, "HmacSHA256"))
        val hmacKeyBytes = mac.doFinal(secretKey)
        mac.init(SecretKeySpec(hmacKeyBytes, "HmacSHA256"))

        val output = ByteArray(64)
        var tmp = ByteArray(0)
        var b: Byte = 1
        var i = 0
        while (i < output.size) {
            mac.update(tmp)
            mac.update(MIVEAR_AUTH)
            mac.update(b)
            tmp = mac.doFinal()
            val copyLen = minOf(tmp.size, output.size - i)
            System.arraycopy(tmp, 0, output, i, copyLen)
            i += copyLen
            b = (b + 1).toByte()
        }
        return output
    }

    fun hmacSha256(key: ByteArray, input: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(input)
    }

    fun encrypt(key: ByteArray, nonce: ByteArray, payload: ByteArray, macSizeBits: Int = 32): ByteArray {
        val cipher = createCcmCipher(forEncrypt = true, key, macSizeBits, nonce)
        val out = ByteArray(cipher.getOutputSize(payload.size))
        var len = cipher.processBytes(payload, 0, payload.size, out, 0)
        len += cipher.doFinal(out, len)
        return out.copyOf(len)
    }

    fun decrypt(
        key: ByteArray,
        nonce: ByteArray,
        encryptedPayload: ByteArray,
        checkMac: Boolean = true,
        macSizeBits: Int = 32,
    ): ByteArray {
        val actualLen = if (checkMac) encryptedPayload.size else encryptedPayload.size - 4
        val cipher = createCcmCipher(forEncrypt = false, key, macSizeBits, nonce)
        val out = ByteArray(cipher.getOutputSize(actualLen))
        var len = cipher.processBytes(encryptedPayload, 0, actualLen, out, 0)
        len += cipher.doFinal(out, len)
        return out.copyOf(len)
    }

    fun packetNonce(baseNonce: ByteArray, index: Int): ByteArray =
        ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(baseNonce)
            putInt(index)
        }.array()

    private fun createCcmCipher(
        forEncrypt: Boolean,
        secretKey: ByteArray,
        macSizeBits: Int,
        nonce: ByteArray,
    ): CCMBlockCipher {
        val aes = AESEngine()
        aes.init(forEncrypt, KeyParameter(secretKey))
        val blockCipher = CCMBlockCipher(aes)
        blockCipher.init(
            forEncrypt,
            AEADParameters(KeyParameter(secretKey), macSizeBits, nonce, null),
        )
        return blockCipher
    }
}
