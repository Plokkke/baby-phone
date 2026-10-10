package fr.crntech.babyphone.shared

import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.seconds

/**
 * Background level of the room, so that a threshold set "X dB above it" means the same on every device:
 * microphones differ by 25 dB or more between a phone and a computer. Follows the median of each second,
 * falls at once, and rises only [riseDbPerSecond] so that a long cry never becomes the background.
 */
class NoiseFloor(private val riseDbPerSecond: Float = 1f / 60) {
    private val second = ArrayList<Float>(FRAMES_PER_SECOND)

    @Volatile
    var db: Float? = null
        private set

    fun onFrame(levelDb: Float) {
        if (db == null) db = levelDb
        second += levelDb
        if (second.size < FRAMES_PER_SECOND) return
        val median = second.sorted()[second.size / 2]
        second.clear()
        db = minOf(median, (db ?: median) + riseDbPerSecond)
    }

    private companion object {
        val FRAMES_PER_SECOND = AudioSpec.frames(1.seconds)
    }
}
