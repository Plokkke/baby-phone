package fr.crntech.babyphone.client.platform

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

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

interface KeyValueStorage {
    fun get(key: String): String?
    fun put(values: Map<String, String>)
}

/** Everything the shared client needs from the device it runs on. */
class Platform(
    val publicUrl: String,
    val deviceName: String,
    val httpClient: HttpClient,
    val storage: KeyValueStorage,
    val microphone: Microphone,
    val speaker: () -> Speaker,
    val battery: Battery,
    val alarm: () -> Alarm,
)
