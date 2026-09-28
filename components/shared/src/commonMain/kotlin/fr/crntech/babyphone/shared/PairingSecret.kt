package fr.crntech.babyphone.shared

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.HMAC
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.io.encoding.Base64

/**
 * Secret carried by the pairing QR code. It never reaches the server:
 * the server only sees [roomId], derived one-way from it.
 */
class PairingSecret(private val bytes: ByteArray) {
    init {
        require(bytes.size == SIZE) { "Pairing secret must be $SIZE bytes" }
    }

    suspend fun roomId(): String = derive("room").toHexString()

    internal suspend fun encryptionKey(): ByteArray = derive("aes-gcm")

    fun encode(): String = BASE64.encode(bytes)

    private suspend fun derive(label: String): ByteArray =
        CryptographyProvider.Default.get(HMAC)
            .keyDecoder(SHA256)
            .decodeFromByteArray(HMAC.Key.Format.RAW, bytes)
            .signatureGenerator()
            .generateSignature("babyphone:$label".encodeToByteArray())

    override fun equals(other: Any?) = other is PairingSecret && bytes.contentEquals(other.bytes)
    override fun hashCode() = bytes.contentHashCode()

    companion object {
        const val SIZE = 32
        private val BASE64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

        fun generate() = PairingSecret(CryptographyRandom.nextBytes(SIZE))

        fun decode(value: String): PairingSecret? = runCatching { PairingSecret(BASE64.decode(value)) }.getOrNull()
    }
}
