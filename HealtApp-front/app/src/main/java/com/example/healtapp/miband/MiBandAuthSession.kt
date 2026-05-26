package com.example.healtapp.miband

import android.os.Build
import com.example.healtapp.miband.proto.XiaomiProto
import com.google.protobuf.ByteString
import java.security.SecureRandom
import java.util.Locale
import kotlin.random.Random

/**
 * Xiaomi Mi Band 8 encrypted pairing (auth key from Mi Fitness).
 */
class MiBandAuthSession(private val authKeyHex: String) {

    private val secretKey: ByteArray = MiBandCrypto.parseAuthKeyHex(authKeyHex)
    private val phoneNonce = ByteArray(16).also { SecureRandom().nextBytes(it) }

    private val encryptionKey = ByteArray(16)
    private val decryptionKey = ByteArray(16)
    private val encryptionNonce = ByteArray(4)
    private val decryptionNonce = ByteArray(4)

    var encryptionReady: Boolean = false
        private set

    private var encryptedIndex = 1

    fun buildPhoneNonceCommand(): XiaomiProto.Command {
        val auth = XiaomiProto.Auth.newBuilder()
            .setPhoneNonce(
                XiaomiProto.PhoneNonce.newBuilder()
                    .setNonce(ByteString.copyFrom(phoneNonce))
                    .build(),
            )
            .build()
        return XiaomiProto.Command.newBuilder()
            .setType(TYPE_AUTH)
            .setSubtype(SUBTYPE_NONCE)
            .setAuth(auth)
            .build()
    }

    fun handleWatchNonce(watchNonce: XiaomiProto.WatchNonce): XiaomiProto.Command? {
        val step2 = MiBandCrypto.computeAuthStep3Hmac(
            secretKey,
            phoneNonce,
            watchNonce.nonce.toByteArray(),
        )
        System.arraycopy(step2, 0, decryptionKey, 0, 16)
        System.arraycopy(step2, 16, encryptionKey, 0, 16)
        System.arraycopy(step2, 32, decryptionNonce, 0, 4)
        System.arraycopy(step2, 36, encryptionNonce, 0, 4)

        val confirm = MiBandCrypto.hmacSha256(
            decryptionKey,
            watchNonce.nonce.toByteArray() + phoneNonce,
        )
        if (!confirm.contentEquals(watchNonce.hmac.toByteArray())) {
            return null
        }

        val region = Locale.getDefault().language.take(2).uppercase(Locale.ROOT)
        val deviceInfo = XiaomiProto.AuthDeviceInfo.newBuilder()
            .setUnknown1(0)
            .setPhoneApiLevel(Build.VERSION.SDK_INT.toFloat())
            .setPhoneName(Build.MODEL ?: "Android")
            .setUnknown3(224)
            .setRegion(region)
            .build()

        val encryptedNonces = MiBandCrypto.hmacSha256(
            encryptionKey,
            phoneNonce + watchNonce.nonce.toByteArray(),
        )
        val encryptedDeviceInfo = MiBandCrypto.encrypt(
            encryptionKey,
            MiBandCrypto.packetNonce(encryptionNonce, 0),
            deviceInfo.toByteArray(),
        )

        val auth = XiaomiProto.Auth.newBuilder()
            .setAuthStep3(
                XiaomiProto.AuthStep3.newBuilder()
                    .setEncryptedNonces(ByteString.copyFrom(encryptedNonces))
                    .setEncryptedDeviceInfo(ByteString.copyFrom(encryptedDeviceInfo))
                    .build(),
            )
            .build()

        return XiaomiProto.Command.newBuilder()
            .setType(TYPE_AUTH)
            .setSubtype(SUBTYPE_AUTH)
            .setAuth(auth)
            .build()
    }

    fun onAuthSuccess(subtype: Int) {
        encryptionReady = subtype == SUBTYPE_AUTH
        encryptedIndex = 1
    }

    fun encryptPayload(payload: ByteArray, incrementNonce: Boolean = true): ByteArray {
        val idx = if (incrementNonce) encryptedIndex++ else 0
        return MiBandCrypto.encrypt(
            encryptionKey,
            MiBandCrypto.packetNonce(encryptionNonce, idx),
            payload,
        )
    }

    fun decryptPayload(encrypted: ByteArray): ByteArray =
        MiBandCrypto.decrypt(
            decryptionKey,
            MiBandCrypto.packetNonce(decryptionNonce, 0),
            encrypted,
        )

    companion object {
        const val TYPE_AUTH = 1
        const val SUBTYPE_NONCE = 26
        const val SUBTYPE_AUTH = 27
    }
}
