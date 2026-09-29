package fr.crntech.babyphone.shared

import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Tells whether the local microphone really delivers sound. Failures are silent by nature:
 * a blocked permission leaves the stream pending, a system-muted input yields exact zeros.
 */
class MicrophoneHealth(private val clock: TimeSource = TimeSource.Monotonic) {
    enum class Status {
        STARTING,

        /** No frame at all: permission pending or blocked, or the device is unavailable. */
        NO_SIGNAL,

        /** Frames of digital silence: input muted by the system or a dead device. */
        SILENT,
        OK,
    }

    private val startedAt = clock.markNow()
    private var lastFrame: TimeMark? = null
    private var lastSound: TimeMark? = null

    fun onFrame(levelDb: Float) {
        val now = clock.markNow()
        lastFrame = now
        if (levelDb > SILENCE_DB) lastSound = now
    }

    fun status(): Status {
        val frame = lastFrame
        return when {
            frame == null || frame.elapsedNow() > PATIENCE ->
                if (startedAt.elapsedNow() > PATIENCE) Status.NO_SIGNAL else Status.STARTING
            lastSound.let { it == null || it.elapsedNow() > PATIENCE } ->
                if (startedAt.elapsedNow() > PATIENCE) Status.SILENT else Status.STARTING
            else -> Status.OK
        }
    }

    private companion object {
        /** Just above the floor: real microphones always pick up some noise, muted ones give zeros. */
        const val SILENCE_DB = Loudness.FLOOR_DB + 2
        val PATIENCE = 3.seconds
    }
}
