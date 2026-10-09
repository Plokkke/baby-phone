package fr.crntech.babyphone.shared

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PairingTest {
    private val url = "https://babyphone.example.com"

    @Test
    fun `link round-trips the secret`() {
        val secret = PairingSecret.generate()
        assertEquals(secret, PairingLink.parse(PairingLink.build(url, secret)))
    }

    @Test
    fun `secret stays out of the part sent to the server`() = runTest {
        val secret = PairingSecret.generate()
        val link = PairingLink.build(url, secret)
        assertFalse(secret.encode() in link.substringBefore('#'))
        assertFalse(secret.encode() in secret.roomId())
    }

    @Test
    fun `room id is stable and unique per secret`() = runTest {
        val secret = PairingSecret.generate()
        assertEquals(secret.roomId(), PairingSecret.decode(secret.encode())?.roomId())
        assertNotEquals(secret.roomId(), PairingSecret.generate().roomId())
        assertEquals(64, secret.roomId().length)
    }

    /** HMAC-SHA256(0x00..0x1f, "babyphone:room"), computed outside Kotlin: devices of every version must meet. */
    @Test
    fun `room id derivation matches the reference vector`() = runTest {
        assertEquals("c50366259f86b8c66233e620bb50f4547132c0ce3e2b8267e741d7fd4a7cb450", PairingSecret(ByteArray(PairingSecret.SIZE) { it.toByte() }).roomId())
    }

    @Test
    fun `socket url escapes peer names`() {
        val url = Endpoints.roomSocket("https://h", "r", Peer("id", "Pixel 7 é&", Role.EMITTER, 3, "1.2.3"))
        assertEquals("wss://h/ws/r?device=id&name=Pixel%207%20%C3%A9%26&role=EMITTER&protocol=3&version=1.2.3", url)
    }

    @Test
    fun `android intent query form is accepted`() {
        val secret = PairingSecret.generate()
        assertEquals(secret, PairingLink.parse("$url/pair?s=${secret.encode()}"))
    }

    @Test
    fun `foreign or broken links are rejected`() {
        assertNull(PairingLink.parse("https://example.com/other#s=abc"))
        assertNull(PairingLink.parse("$url/pair"))
        assertNull(PairingLink.parse("$url/pair#s=tooshort"))
    }
}
