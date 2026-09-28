package fr.crntech.babyphone.shared

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PeerCodecTest {
    private val secret = PairingSecret.generate()
    private val codec = PeerCodec(secret)

    @Test
    fun `messages round-trip`() {
        val status = PeerMessage.EmitterStatus(80, true, -42.5f, -40f, transmitting = false)
        assertEquals(status, codec.decode(codec.encode(status)))
        assertEquals(PeerMessage.ForceListen, codec.decode(codec.encode(PeerMessage.ForceListen)))

        val pcm = ByteArray(AudioSpec.FRAME_BYTES) { it.toByte() }
        val audio = assertIs<PeerMessage.Audio>(PeerCodec(secret).decode(codec.encode(PeerMessage.Audio(pcm))))
        assertContentEquals(pcm, audio.pcm)
    }

    @Test
    fun `other pairings cannot read frames`() {
        val frame = codec.encode(PeerMessage.SetThreshold(-30f))
        assertNull(PeerCodec(PairingSecret.generate()).decode(frame))
    }

    @Test
    fun `tampered or garbage frames are dropped`() {
        val frame = codec.encode(PeerMessage.SetThreshold(-30f))
        frame[frame.lastIndex] = (frame.last() + 1).toByte()
        assertNull(codec.decode(frame))
        assertNull(codec.decode(byteArrayOf(1, 2, 3)))
    }
}
