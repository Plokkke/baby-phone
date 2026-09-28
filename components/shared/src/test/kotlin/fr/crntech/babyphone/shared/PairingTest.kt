package fr.crntech.babyphone.shared

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
    fun `secret stays out of the part sent to the server`() {
        val secret = PairingSecret.generate()
        val link = PairingLink.build(url, secret)
        assertFalse(secret.encode() in link.substringBefore('#'))
        assertFalse(secret.encode() in secret.roomId)
    }

    @Test
    fun `room id is stable and unique per secret`() {
        val secret = PairingSecret.generate()
        assertEquals(secret.roomId, PairingSecret.decode(secret.encode())?.roomId)
        assertNotEquals(secret.roomId, PairingSecret.generate().roomId)
        assertEquals(64, secret.roomId.length)
    }

    @Test
    fun `foreign or broken links are rejected`() {
        assertNull(PairingLink.parse("https://example.com/other#s=abc"))
        assertNull(PairingLink.parse("$url/pair"))
        assertNull(PairingLink.parse("$url/pair#s=tooshort"))
    }
}
