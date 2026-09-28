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
        val status = PeerMessage.EmitterStatus(80, true, -42.5f, -40f, transmitting = false)
        assertEquals(status, codec.decode(codec.encode(status)))
        assertEquals(PeerMessage.ForceListen, codec.decode(codec.encode(PeerMessage.ForceListen)))

        val pcm = ByteArray(AudioSpec.FRAME_BYTES) { it.toByte() }
        val audio = assertIs<PeerMessage.Audio>(PeerCodec.create(secret).decode(codec.encode(PeerMessage.Audio(pcm))))
        assertContentEquals(pcm, audio.pcm)
    }

    @Test
    fun `other pairings cannot read frames`() = runTest {
        val codec = PeerCodec.create(secret)
        val frame = codec.encode(PeerMessage.SetThreshold(-30f))
        assertNull(PeerCodec.create(PairingSecret.generate()).decode(frame))
    }

    @Test
    fun `tampered or garbage frames are dropped`() = runTest {
        val codec = PeerCodec.create(secret)
        val frame = codec.encode(PeerMessage.SetThreshold(-30f))
        frame[frame.lastIndex] = (frame.last() + 1).toByte()
        assertNull(codec.decode(frame))
        assertNull(codec.decode(byteArrayOf(1, 2, 3)))
    }
}
