package fr.crntech.babyphone.monitor

import android.media.MediaRecorder
import fr.crntech.babyphone.audio.Microphone
import fr.crntech.babyphone.audio.Speaker
import fr.crntech.babyphone.data.SettingsStore
import fr.crntech.babyphone.device.Battery
import fr.crntech.babyphone.net.PeerLink
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class EmitterSession(
    private val link: PeerLink,
    private val settings: SettingsStore,
    private val battery: Battery,
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

    private val clock = TimeSource.Monotonic
    @Volatile private var forcedUntil = clock.markNow()
    @Volatile private var talkbackUntil = clock.markNow()
    @Volatile private var peakDb = Loudness.FLOOR_DB

    override suspend fun run() = coroutineScope {
        launch { link.run() }
        launch { link.connected.collect { c -> _state.update { it.copy(connected = c) } } }
        launch { link.presence.collect { peers -> _state.update { s -> s.copy(receivers = peers.count { it.role == Role.RECEIVER }) } } }
        launch { receive() }
        launch { capture() }
        reportStatus()
    }

    private suspend fun capture() {
        val gate = SoundGate()
        Microphone.frames(MediaRecorder.AudioSource.MIC).collect { frame ->
            val level = Loudness.dbfs(frame)
            peakDb = maxOf(peakDb, level)
            // Half-duplex: never send the parent's own voice back while it plays here.
            if (talkbackUntil.hasNotPassedNow()) return@collect
            val triggered = forcedUntil.hasNotPassedNow() || level >= _state.value.thresholdDb
            gate.process(frame, triggered).forEach { link.send(PeerMessage.Audio(it)) }
            _state.update { it.copy(transmitting = gate.isOpen) }
        }
    }

    private suspend fun receive() = Speaker().use { speaker ->
        link.messages.collect { message ->
            when (message) {
                is PeerMessage.Audio -> {
                    talkbackUntil = clock.markNow() + ECHO_GUARD
                    speaker.play(message.pcm)
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
            link.send(PeerMessage.EmitterStatus(power.percent, power.charging, level, current.thresholdDb, current.transmitting))
        }
    }

    private companion object {
        val ECHO_GUARD = 300.milliseconds
    }
}
