package fr.crntech.babyphone.client.platform

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class MicMode {
    /** Raw room sound for the baby monitor: no processing that would skew the level. */
    AMBIENT,

    /** Voice with echo cancellation, for talk-back. */
    VOICE,
}

/** Cold flow of [fr.crntech.babyphone.shared.AudioSpec.FRAME_BYTES] PCM frames; recording lasts as long as the collection. */
fun interface Microphone {
    fun frames(mode: MicMode): Flow<ByteArray>
}

/** Plays PCM frames without ever blocking; overflowing audio is dropped to cap latency. */
interface Speaker : AutoCloseable {
    fun play(pcm: ByteArray)
}

data class BatteryState(val percent: Int, val charging: Boolean)

fun interface Battery {
    fun read(): BatteryState
}

/** Loud, repeating alert raised when the emitter goes silent. */
interface Alarm {
    fun raise()
    fun silence()
    fun clear()
}

/**
 * Do-not-disturb on the emitter, so calls and notifications do not wake the baby.
 * [QuietState.controllable] is false until the user grants the app access to it.
 */
interface QuietMode {
    val state: StateFlow<QuietState>

    /** Toggles do-not-disturb, or opens the system screen granting that access when missing. */
    fun setEnabled(enabled: Boolean)
}

data class QuietState(val supported: Boolean, val enabled: Boolean, val controllable: Boolean) {
    companion object {
        val UNSUPPORTED = QuietState(supported = false, enabled = false, controllable = false)
    }
}

/** What could keep the parents from hearing the baby or the alarm on this device. */
interface SoundOutput {
    val state: StateFlow<SoundOutputState>

    /** Raises the volume (Android) or lets the page play sound (browsers need a user gesture). */
    fun makeAudible()

    fun playTestSound()
}

data class SoundOutputState(
    /** Lowest of the relevant volumes; null when the platform cannot read it (browsers). */
    val volumePercent: Int? = null,
    /** Sound is cut regardless of volume: total-silence do-not-disturb, or a browser blocking audio. */
    val muted: Boolean = false,
    /** System notifications are off, so a lost-link alarm may go unnoticed in the background. */
    val alertsBlocked: Boolean = false,
) {
    val tooQuiet get() = volumePercent != null && volumePercent < MIN_VOLUME_PERCENT

    companion object {
        const val MIN_VOLUME_PERCENT = 60
    }
}

interface KeyValueStorage {
    fun get(key: String): String?
    fun put(values: Map<String, String>)
}

/** Everything the shared client needs from the device it runs on. */
class Platform(
    val publicUrl: String,
    val deviceName: String,
    /** Shown to tell builds apart while testing; null when the platform does not know it. */
    val appVersion: String?,
    val httpClient: HttpClient,
    val storage: KeyValueStorage,
    val microphone: Microphone,
    val speaker: () -> Speaker,
    val battery: Battery,
    val alarm: () -> Alarm,
    val quietMode: QuietMode,
    val soundOutput: SoundOutput,
)
