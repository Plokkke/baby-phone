package fr.crntech.babyphone.shared

import fr.crntech.babyphone.shared.MicrophoneHealth.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class MicrophoneHealthTest {
    private val clock = TestTimeSource()
    private val health = MicrophoneHealth(clock)

    @Test
    fun `gives the microphone time to start`() {
        clock += 1.seconds
        assertEquals(Status.STARTING, health.status())
    }

    @Test
    fun `no frames means no signal`() {
        clock += 4.seconds
        assertEquals(Status.NO_SIGNAL, health.status())
    }

    @Test
    fun `room noise is healthy`() {
        clock += 4.seconds
        health.onFrame(-62f)
        assertEquals(Status.OK, health.status())
    }

    @Test
    fun `digital silence is reported`() {
        repeat(8) {
            clock += 0.5.seconds
            health.onFrame(Loudness.FLOOR_DB)
        }
        assertEquals(Status.SILENT, health.status())
    }

    @Test
    fun `frames that stop are reported`() {
        health.onFrame(-50f)
        clock += 4.seconds
        assertEquals(Status.NO_SIGNAL, health.status())
    }
}
