package fr.crntech.babyphone.client.monitor

import fr.crntech.babyphone.client.data.SettingsStore
import fr.crntech.babyphone.client.net.PeerLink
import fr.crntech.babyphone.client.platform.Battery
import fr.crntech.babyphone.client.platform.MicMode
import fr.crntech.babyphone.client.platform.Microphone
import fr.crntech.babyphone.client.platform.QuietMode
import fr.crntech.babyphone.client.platform.Speaker
import fr.crntech.babyphone.shared.AutoGain
import fr.crntech.babyphone.shared.Loudness
import fr.crntech.babyphone.shared.MicrophoneHealth
import fr.crntech.babyphone.shared.PeerMessage
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.shared.SoundGate
import fr.crntech.babyphone.shared.Threshold
import fr.crntech.babyphone.shared.Timing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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
        val microphone: MicrophoneHealth.Status = MicrophoneHealth.Status.STARTING,
    )

    private val _state = MutableStateFlow(State(thresholdDb = thresholdDb))
    val state = _state.asStateFlow()
    val quiet = quietMode.state
    override val presence = link.presence

    fun setQuiet(enabled: Boolean) = quietMode.setEnabled(enabled)

    private val clock = TimeSource.Monotonic
    @Volatile private var forcedUntil = clock.markNow()
    @Volatile private var peakDb = Loudness.FLOOR_DB
    private val microphoneHealth = MicrophoneHealth()
    private val parentTalking = MutableStateFlow(false)
    private val parentVoice = Channel<Unit>(Channel.CONFLATED)

    /** Silences the phone for the night, and gives it back as it was found. */
    override suspend fun run() {
        val silencedHere = quietMode.state.value.let { it.controllable && !it.enabled }
        if (silencedHere) quietMode.setEnabled(true)
        try {
            connected { monitor() }
        } finally {
            if (silencedHere) quietMode.setEnabled(false)
        }
    }

    /**
     * Keeps the connection out of the session's cancellation, so that being stopped on purpose can still
     * tell the parents before closing. A failure, a crash or a lost network says nothing: their alarm rings.
     */
    private suspend fun connected(block: suspend () -> Unit) {
        val connection = CoroutineScope(currentCoroutineContext().minusKey(Job)).launch { link.run() }
        try {
            block()
        } catch (e: CancellationException) {
            withContext(NonCancellable) { withTimeoutOrNull(FAREWELL_TIMEOUT) { link.sendLast(PeerMessage.Leaving) } }
            throw e
        } finally {
            connection.cancel()
        }
    }

    private suspend fun monitor(): Unit = coroutineScope {
        launch { link.connected.collect { c -> _state.update { it.copy(connected = c) } } }
        launch { link.presence.collect { peers -> _state.update { s -> s.copy(receivers = peers.count { it.role == Role.RECEIVER }) } } }
        launch { receive() }
        launch { trackParentVoice() }
        launch { capture() }
        reportStatus()
    }

    /**
     * Full duplex: while a parent talks, the echo-cancelled microphone keeps streaming,
     * so the parent hears the baby without hearing their own voice back.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun capture() {
        val gate = SoundGate()
        val gain = AutoGain()
        parentTalking.flatMapLatest { talking ->
            microphone.frames(if (talking) MicMode.VOICE else MicMode.AMBIENT).map { it to talking }
        }.collect { (frame, talking) ->
            val level = Loudness.dbfs(frame)
            peakDb = maxOf(peakDb, level)
            if (!talking) microphoneHealth.onFrame(level)
            val triggered = talking || forcedUntil.hasNotPassedNow() || level >= _state.value.thresholdDb
            // Detection reads the raw level; the parents get it amplified, or they would barely hear a voice.
            gate.process(gain.process(frame), triggered).forEach { link.send(PeerMessage.Audio(it)) }
            _state.update { it.copy(transmitting = gate.isOpen) }
        }
    }

    private suspend fun receive() = SpeakerMixer(speaker).use { speakers ->
        link.messages.collect { (from, _, message) ->
            when (message) {
                is PeerMessage.Audio -> {
                    parentVoice.trySend(Unit)
                    speakers.play(from, message.pcm)
                }
                is PeerMessage.SetThreshold -> {
                    val db = message.db.coerceIn(Threshold.MIN_DB, Threshold.MAX_DB)
                    _state.update { it.copy(thresholdDb = db) }
                    settings.setThreshold(db)
                }
                PeerMessage.ForceListen -> forcedUntil = clock.markNow() + Timing.FORCE_LISTEN_TTL
                is PeerMessage.EmitterStatus, is PeerMessage.Intercom, PeerMessage.Leaving -> Unit
            }
        }
    }

    private suspend fun trackParentVoice(): Nothing {
        while (true) {
            parentVoice.receive()
            parentTalking.value = true
            while (withTimeoutOrNull(VOICE_GRACE) { parentVoice.receive() } != null) Unit
            parentTalking.value = false
        }
    }

    private suspend fun reportStatus(): Nothing {
        while (true) {
            delay(Timing.STATUS_INTERVAL)
            val level = peakDb.also { peakDb = Loudness.FLOOR_DB }
            val current = _state.updateAndGet {
                it.copy(levelDb = level, parentTalking = parentTalking.value, microphone = microphoneHealth.status())
            }
            val power = battery.read()
            val quiet = quietMode.state.value.takeIf { it.supported }?.enabled
            link.send(PeerMessage.EmitterStatus(power.percent, power.charging, level, current.thresholdDb, current.transmitting, quiet))
        }
    }

    private companion object {
        /** Bridges network jitter between voice frames, so the microphone is not switched back and forth. */
        val VOICE_GRACE = 500.milliseconds
        val FAREWELL_TIMEOUT = 1.seconds
    }
}
