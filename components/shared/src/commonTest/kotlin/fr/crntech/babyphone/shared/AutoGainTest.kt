package fr.crntech.babyphone.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class AutoGainTest {
    private fun frame(amplitude: Int) = ByteArray(AudioSpec.FRAME_BYTES).also { pcm ->
        repeat(AudioSpec.samples(pcm)) { AudioSpec.writeSample(pcm, it, if (it % 2 == 0) amplitude else -amplitude) }
    }

    private fun AutoGain.settle(amplitude: Int) = (1..500).map { process(frame(amplitude)) }.last()

    @Test
    fun `a quiet voice is brought to a listenable level`() {
        val quiet = frame(60) // about -55 dBFS
        val out = AutoGain().settle(60)
        assertTrue(Loudness.dbfs(out) > Loudness.dbfs(quiet) + 35, "boosted to ${Loudness.dbfs(out)} dBFS")
    }

    @Test
    fun `a sudden loud sound is never clipped`() {
        val gain = AutoGain()
        gain.settle(60)
        val loud = gain.process(frame(20_000))
        assertTrue(Loudness.dbfs(loud) < -5f, "loud frame at ${Loudness.dbfs(loud)} dBFS")
    }

    @Test
    fun `gain stays bounded in silence`() {
        val out = AutoGain().settle(1)
        assertTrue(Loudness.dbfs(out) < -40f, "silence at ${Loudness.dbfs(out)} dBFS")
    }
}
