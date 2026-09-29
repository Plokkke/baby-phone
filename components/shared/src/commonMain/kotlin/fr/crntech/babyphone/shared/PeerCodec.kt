package fr.crntech.babyphone.shared

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.operations.IvAuthenticatedCipher
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor

/**
 * CBOR + AES-256-GCM. Frame layout: `nonce(12) || ciphertext+tag`, the nonce being random per frame.
 * Suspending because browsers only expose asynchronous crypto (Web Crypto).
 */
@OptIn(ExperimentalSerializationApi::class)
class PeerCodec private constructor(private val cipher: IvAuthenticatedCipher) {

    suspend fun encode(message: PeerMessage): ByteArray =
        cipher.encrypt(CBOR.encodeToByteArray(PeerMessage.serializer(), message))

    /** Returns null for frames that are malformed or not sealed with this pairing's key. */
    suspend fun decode(frame: ByteArray): PeerMessage? = runCatching {
        CBOR.decodeFromByteArray(PeerMessage.serializer(), cipher.decrypt(frame))
    }.getOrNull()

    companion object {
        /** Tolerates fields added by newer app versions. */
        private val CBOR = Cbor { ignoreUnknownKeys = true }

        suspend fun create(secret: PairingSecret): PeerCodec {
            val key = CryptographyProvider.Default.get(AES.GCM)
                .keyDecoder()
                .decodeFromByteArray(AES.Key.Format.RAW, secret.encryptionKey())
            return PeerCodec(key.cipher())
        }
    }
}
