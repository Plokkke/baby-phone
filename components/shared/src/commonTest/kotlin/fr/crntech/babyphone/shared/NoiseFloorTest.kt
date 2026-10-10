package fr.crntech.babyphone.shared

import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoiseFloorTest {
    private val framesPerSecond = AudioSpec.frames(1.seconds)

    private fun NoiseFloor.hear(levelDb: Float, seconds: Int) = repeat(seconds * framesPerSecond) { onFrame(levelDb) }

    @Test
    fun `follows a quieter room at once`() {
        val floor = NoiseFloor()
        floor.hear(-50f, seconds = 5)
        floor.hear(-80f, seconds = 1)
        assertEquals(-80f, floor.db)
    }

    @Test
    fun `a long cry does not become the background`() {
        val floor = NoiseFloor()
        floor.hear(-80f, seconds = 10)
        floor.hear(-30f, seconds = 5 * 60)
        val risen = floor.db!! + 80f
        assertTrue(risen in 4f..6f, "rose by $risen dB in five minutes")
    }

    @Test
    fun `short gaps in a sound do not drag the floor down`() {
        val floor = NoiseFloor()
        floor.hear(-60f, seconds = 2)
        repeat(framesPerSecond) { floor.onFrame(if (it % 5 == 0) -90f else -60f) }
        assertEquals(-60f, floor.db)
    }
}
