package fr.crntech.babyphone.shared

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object AudioSpec {
    const val SAMPLE_RATE = 16_000
    const val FRAME_MS = 20
    const val BYTES_PER_SAMPLE = 2
    const val FRAME_BYTES = SAMPLE_RATE * FRAME_MS / 1000 * BYTES_PER_SAMPLE

    fun frames(duration: Duration) = (duration.inWholeMilliseconds / FRAME_MS).toInt()

    fun samples(pcm: ByteArray) = pcm.size / BYTES_PER_SAMPLE

    /** Signed 16-bit little-endian sample. */
    fun sampleAt(pcm: ByteArray, index: Int) = (pcm[2 * index].toInt() and 0xFF) or (pcm[2 * index + 1].toInt() shl 8)
}

object Timing {
    val STATUS_INTERVAL = 250.milliseconds
    val EMITTER_SILENCE_ALARM = 10.seconds
    val FORCE_LISTEN_KEEPALIVE = 500.milliseconds
    val FORCE_LISTEN_TTL = 1500.milliseconds
    val TALKBACK_MAX = 60.seconds
    val PRE_ROLL = 2.seconds
    val HANGOVER = 5.seconds
    val RECONNECT_DELAY = 2.seconds
}

object Threshold {
    const val MIN_DB = -70f
    const val MAX_DB = -10f
    const val DEFAULT_DB = -40f
}
