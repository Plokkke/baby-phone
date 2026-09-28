package fr.crntech.babyphone.shared

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Secret carried by the pairing QR code. It never reaches the server:
 * the server only sees [roomId], derived one-way from it.
 */
class PairingSecret(private val bytes: ByteArray) {
    init {
        require(bytes.size == SIZE) { "Pairing secret must be $SIZE bytes" }
    }

    val roomId: String by lazy { derive("room").toHexString() }
    internal val encryptionKey: ByteArray by lazy { derive("aes-gcm") }

    fun encode(): String = ENCODER.encodeToString(bytes)

    private fun derive(label: String): ByteArray = Mac.getInstance(MAC).run {
        init(SecretKeySpec(bytes, MAC))
        doFinal("babyphone:$label".toByteArray())
    }

    override fun equals(other: Any?) = other is PairingSecret && bytes.contentEquals(other.bytes)
    override fun hashCode() = bytes.contentHashCode()

    companion object {
        const val SIZE = 32
        private const val MAC = "HmacSHA256"
        private val ENCODER = Base64.getUrlEncoder().withoutPadding()

        fun generate() = PairingSecret(ByteArray(SIZE).also(SecureRandom()::nextBytes))

        fun decode(value: String): PairingSecret? =
            runCatching { PairingSecret(Base64.getUrlDecoder().decode(value)) }.getOrNull()
    }
}
