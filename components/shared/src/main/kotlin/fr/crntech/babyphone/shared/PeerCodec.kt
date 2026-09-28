package fr.crntech.babyphone.shared

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** CBOR + AES-256-GCM. Frame layout: `nonce(12) || ciphertext+tag`. */
@OptIn(ExperimentalSerializationApi::class)
class PeerCodec(secret: PairingSecret) {
    private val key = SecretKeySpec(secret.encryptionKey, "AES")
    private val random = SecureRandom()

    fun encode(message: PeerMessage): ByteArray {
        val nonce = ByteArray(NONCE_SIZE).also(random::nextBytes)
        val plain = Cbor.encodeToByteArray(PeerMessage.serializer(), message)
        return nonce + cipher(Cipher.ENCRYPT_MODE, nonce).doFinal(plain)
    }

    /** Returns null for frames that are malformed or not sealed with this pairing's key. */
    fun decode(frame: ByteArray): PeerMessage? = runCatching {
        val nonce = frame.copyOfRange(0, NONCE_SIZE)
        val plain = cipher(Cipher.DECRYPT_MODE, nonce).doFinal(frame, NONCE_SIZE, frame.size - NONCE_SIZE)
        Cbor.decodeFromByteArray(PeerMessage.serializer(), plain)
    }.getOrNull()

    private fun cipher(mode: Int, nonce: ByteArray) =
        Cipher.getInstance("AES/GCM/NoPadding").apply { init(mode, key, GCMParameterSpec(TAG_BITS, nonce)) }

    private companion object {
        const val NONCE_SIZE = 12
        const val TAG_BITS = 128
    }
}
