package fr.crntech.babyphone.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SoundGateTest {
    private fun frame(id: Int) = byteArrayOf(id.toByte())
    private fun List<ByteArray>.ids() = map { it[0].toInt() }

    @Test
    fun `silence is never sent`() {
        val gate = SoundGate(preRollFrames = 2, hangoverFrames = 1)
        repeat(10) { assertTrue(gate.process(frame(it), triggered = false).isEmpty()) }
        assertFalse(gate.isOpen)
    }

    @Test
    fun `trigger flushes the pre-roll then the current frame`() {
        val gate = SoundGate(preRollFrames = 2, hangoverFrames = 1)
        (1..3).forEach { gate.process(frame(it), triggered = false) }
        assertEquals(listOf(2, 3, 4), gate.process(frame(4), triggered = true).ids())
    }

    @Test
    fun `gate stays open during hangover then closes`() {
        val gate = SoundGate(preRollFrames = 0, hangoverFrames = 2)
        gate.process(frame(1), triggered = true)
        assertEquals(listOf(2), gate.process(frame(2), triggered = false).ids())
        assertEquals(listOf(3), gate.process(frame(3), triggered = false).ids())
        assertFalse(gate.isOpen)
        assertTrue(gate.process(frame(4), triggered = false).isEmpty())
    }
}
