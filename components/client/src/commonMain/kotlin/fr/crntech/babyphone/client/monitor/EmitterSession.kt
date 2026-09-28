package fr.crntech.babyphone.client.monitor

import fr.crntech.babyphone.client.data.SettingsStore
import fr.crntech.babyphone.client.net.PeerLink
import fr.crntech.babyphone.client.platform.Battery
import fr.crntech.babyphone.client.platform.MicMode
import fr.crntech.babyphone.client.platform.Microphone
import fr.crntech.babyphone.client.platform.QuietMode
import fr.crntech.babyphone.client.platform.Speaker
import fr.crntech.babyphone.shared.Loudness
import fr.crntech.babyphone.shared.PeerMessage
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.shared.SoundGate
import fr.crntech.babyphone.shared.Threshold
import fr.crntech.babyphone.shared.Timing
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class EmitterSession(
    private val link: PeerLink,
    private val settings: SettingsStore,
    private val microphone: Microphone,
    private val speaker: () -> Speaker,
    private val battery: Battery,
    private val quietMode: QuietMode,
    thresholdDb: Float,
) : MonitorSession {

    data class State(
        val connected: Boolean = false,
        val receivers: Int = 0,
        val levelDb: Float = Loudness.FLOOR_DB,
        val thresholdDb: Float = Threshold.DEFAULT_DB,
        val transmitting: Boolean = false,
        val parentTalking: Boolean = false,
    )

    private val _state = MutableStateFlow(State(thresholdDb = thresholdDb))
    val state = _state.asStateFlow()
    val quiet = quietMode.state

    fun setQuiet(enabled: Boolean) = quietMode.setEnabled(enabled)

    private val clock = TimeSource.Monotonic
    @Volatile private var forcedUntil = clock.markNow()
    @Volatile private var talkbackUntil = clock.markNow()
    @Volatile private var peakDb = Loudness.FLOOR_DB

    /** Silences the phone for the night, and gives it back as it was found. */
    override suspend fun run() {
        val silencedHere = quietMode.state.value.let { it.controllable && !it.enabled }
        if (silencedHere) quietMode.setEnabled(true)
        try {
            monitor()
        } finally {
            if (silencedHere) quietMode.setEnabled(false)
        }
    }

    private suspend fun monitor(): Unit = coroutineScope {
        launch { link.run() }
        launch { link.connected.collect { c -> _state.update { it.copy(connected = c) } } }
        launch { link.presence.collect { peers -> _state.update { s -> s.copy(receivers = peers.count { it.role == Role.RECEIVER }) } } }
        launch { receive() }
        launch { capture() }
        reportStatus()
    }

    private suspend fun capture() {
        val gate = SoundGate()
        microphone.frames(MicMode.AMBIENT).collect { frame ->
            val level = Loudness.dbfs(frame)
            peakDb = maxOf(peakDb, level)
            // Half-duplex: never send the parent's own voice back while it plays here.
            if (talkbackUntil.hasNotPassedNow()) return@collect
            val triggered = forcedUntil.hasNotPassedNow() || level >= _state.value.thresholdDb
            gate.process(frame, triggered).forEach { link.send(PeerMessage.Audio(it)) }
            _state.update { it.copy(transmitting = gate.isOpen) }
        }
    }

    private suspend fun receive() = speaker().use { output ->
        link.messages.collect { message ->
            when (message) {
                is PeerMessage.Audio -> {
                    talkbackUntil = clock.markNow() + ECHO_GUARD
                    output.play(message.pcm)
                }
                is PeerMessage.SetThreshold -> {
                    val db = message.db.coerceIn(Threshold.MIN_DB, Threshold.MAX_DB)
                    _state.update { it.copy(thresholdDb = db) }
                    settings.setThreshold(db)
                }
                PeerMessage.ForceListen -> forcedUntil = clock.markNow() + Timing.FORCE_LISTEN_TTL
                is PeerMessage.EmitterStatus -> Unit
            }
        }
    }

    private suspend fun reportStatus(): Nothing {
        while (true) {
            delay(Timing.STATUS_INTERVAL)
            val level = peakDb.also { peakDb = Loudness.FLOOR_DB }
            val current = _state.updateAndGet { it.copy(levelDb = level, parentTalking = talkbackUntil.hasNotPassedNow()) }
            val power = battery.read()
            val quiet = quietMode.state.value.takeIf { it.supported }?.enabled
            link.send(PeerMessage.EmitterStatus(power.percent, power.charging, level, current.thresholdDb, current.transmitting, quiet))
        }
    }

    private companion object {
        val ECHO_GUARD = 300.milliseconds
    }
}
