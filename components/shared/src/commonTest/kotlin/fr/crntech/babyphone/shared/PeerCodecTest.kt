package fr.crntech.babyphone.shared

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PeerCodecTest {
    private val secret = PairingSecret.generate()

    @Test
    fun `messages round-trip`() = runTest {
        val codec = PeerCodec.create(secret)
        val status = Envelope("baby", message = PeerMessage.EmitterStatus(80, true, -42.5f, -40f, transmitting = false))
        assertEquals(status, codec.decode(codec.encode(status)))
        val force = Envelope("parent", to = "baby", message = PeerMessage.ForceListen)
        assertEquals(force, codec.decode(codec.encode(force)))

        val pcm = ByteArray(AudioSpec.FRAME_BYTES) { it.toByte() }
        val decoded = PeerCodec.create(secret).decode(codec.encode(Envelope("parent", message = PeerMessage.Intercom(pcm))))
        assertContentEquals(pcm, assertIs<PeerMessage.Intercom>(decoded?.message).pcm)
    }

    @Test
    fun `other pairings cannot read frames`() = runTest {
        val codec = PeerCodec.create(secret)
        val frame = codec.encode(Envelope("parent", "baby", PeerMessage.SetThreshold(-30f)))
        assertNull(PeerCodec.create(PairingSecret.generate()).decode(frame))
    }

    @Test
    fun `tampered or garbage frames are dropped`() = runTest {
        val codec = PeerCodec.create(secret)
        val frame = codec.encode(Envelope("parent", "baby", PeerMessage.SetThreshold(-30f)))
        frame[frame.lastIndex] = (frame.last() + 1).toByte()
        assertNull(codec.decode(frame))
        assertNull(codec.decode(byteArrayOf(1, 2, 3)))
    }
}
