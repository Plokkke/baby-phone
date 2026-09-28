package fr.crntech.babyphone.shared

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoudnessTest {
    private fun sine(amplitude: Double) = ByteArray(AudioSpec.FRAME_BYTES).also { pcm ->
        (0 until pcm.size / 2).forEach { i ->
            val s = (amplitude * Short.MAX_VALUE * sin(2 * PI * 440 * i / AudioSpec.SAMPLE_RATE)).toInt()
            pcm[2 * i] = s.toByte()
            pcm[2 * i + 1] = (s shr 8).toByte()
        }
    }

    @Test
    fun `silence hits the floor`() = assertEquals(Loudness.FLOOR_DB, Loudness.dbfs(ByteArray(640)))

    @Test
    fun `full scale sine is about minus 3 dB`() = assertTrue(abs(Loudness.dbfs(sine(1.0)) + 3.01f) < 0.2f)

    @Test
    fun `halving amplitude drops 6 dB`() =
        assertTrue(abs(Loudness.dbfs(sine(0.5)) - Loudness.dbfs(sine(0.25)) - 6.02f) < 0.2f)
}
