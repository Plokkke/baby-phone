package fr.crntech.babyphone.client.monitor

import fr.crntech.babyphone.client.net.PeerLink
import fr.crntech.babyphone.client.platform.Alarm
import fr.crntech.babyphone.client.platform.MicMode
import fr.crntech.babyphone.client.platform.Microphone
import fr.crntech.babyphone.client.platform.SoundOutput
import fr.crntech.babyphone.client.platform.Speaker
import fr.crntech.babyphone.shared.Loudness
import fr.crntech.babyphone.shared.MicrophoneHealth
import fr.crntech.babyphone.shared.PeerMessage
import fr.crntech.babyphone.shared.Threshold
import fr.crntech.babyphone.shared.Timing
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class ReceiverSession(
    private val link: PeerLink,
    private val microphone: Microphone,
    private val speaker: () -> Speaker,
    private val alarm: Alarm,
    private val soundOutput: SoundOutput,
) : MonitorSession {

    data class State(
        val connected: Boolean = false,
        val emitterOnline: Boolean = false,
        val batteryPercent: Int? = null,
        val charging: Boolean = false,
        val levelDb: Float = Loudness.FLOOR_DB,
        val thresholdDb: Float = Threshold.DEFAULT_DB,
        val transmitting: Boolean = false,
        val emitterQuiet: Boolean? = null,
        val listening: Boolean = false,
        val talkingSince: TimeMark? = null,
        val talkLevelDb: Float = Loudness.FLOOR_DB,
        val talkMicrophone: MicrophoneHealth.Status = MicrophoneHealth.Status.STARTING,
        val linkLost: Boolean = false,
        val alarmSilenced: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    val sound = soundOutput.state

    fun makeAudible() = soundOutput.makeAudible()

    fun playTestSound() = soundOutput.playTestSound()

    private val listening = MutableStateFlow(false)
    private val talking = MutableStateFlow(false)
    private val outgoing = Channel<PeerMessage>(Channel.UNLIMITED)

    @Volatile private var talkHealth: MicrophoneHealth? = null

    /** Armed once the emitter has been heard, so starting the receiver first does not ring. */
    @Volatile private var lastStatus: TimeMark? = null

    fun setListening(on: Boolean) {
        listening.value = on
    }

    fun setTalking(on: Boolean) {
        talking.value = on
    }

    fun setThreshold(db: Float) {
        _state.update { it.copy(thresholdDb = db) }
        outgoing.trySend(PeerMessage.SetThreshold(db))
    }

    fun silenceAlarm() {
        alarm.silence()
        _state.update { it.copy(alarmSilenced = true) }
    }

    override suspend fun run() = coroutineScope {
        if (soundOutput.state.value.tooQuiet) soundOutput.makeAudible()
        launch { link.run() }
        launch { link.connected.collect { c -> _state.update { it.copy(connected = c) } } }
        launch { for (message in outgoing) link.send(message) }
        launch { receive() }
        launch { keepListening() }
        launch { talk() }
        try {
            watchLink()
        } finally {
            alarm.clear()
        }
    }

    private suspend fun receive() = speaker().use { output ->
        link.messages.collect { message ->
            when (message) {
                is PeerMessage.EmitterStatus -> {
                    lastStatus = TimeSource.Monotonic.markNow()
                    _state.update {
                        it.copy(
                            batteryPercent = message.batteryPercent,
                            charging = message.charging,
                            levelDb = message.levelDb,
                            thresholdDb = message.thresholdDb,
                            transmitting = message.transmitting,
                            emitterQuiet = message.quiet,
                        )
                    }
                }
                is PeerMessage.Audio -> output.play(message.pcm)
                else -> Unit
            }
        }
    }

    private suspend fun keepListening(): Nothing = listening.collectLatest { on ->
        _state.update { it.copy(listening = on) }
        while (on) {
            link.send(PeerMessage.ForceListen)
            delay(Timing.FORCE_LISTEN_KEEPALIVE)
        }
    }

    private suspend fun talk(): Nothing = talking.collectLatest { on ->
        _state.update {
            it.copy(
                talkingSince = if (on) TimeSource.Monotonic.markNow() else null,
                talkLevelDb = Loudness.FLOOR_DB,
                talkMicrophone = MicrophoneHealth.Status.STARTING,
            )
        }
        talkHealth = if (on) MicrophoneHealth() else null
        if (!on) return@collectLatest
        withTimeoutOrNull(Timing.TALKBACK_MAX) {
            microphone.frames(MicMode.VOICE)
                .catch { println("$TAG: talk-back microphone failed: ${it.message}") }
                .collect { frame ->
                    val level = Loudness.dbfs(frame)
                    talkHealth?.onFrame(level)
                    _state.update { it.copy(talkLevelDb = level) }
                    link.send(PeerMessage.Audio(frame))
                }
        }
        talking.value = false
    }

    private suspend fun watchLink(): Nothing {
        while (true) {
            delay(WATCHDOG_TICK)
            val lost = lastStatus?.let { it.elapsedNow() > Timing.EMITTER_SILENCE_ALARM } ?: false
            val silenced = lost && _state.value.alarmSilenced
            val talkMicrophone = talkHealth?.status() ?: MicrophoneHealth.Status.STARTING
            _state.update {
                it.copy(emitterOnline = lastStatus != null && !lost, linkLost = lost, alarmSilenced = silenced, talkMicrophone = talkMicrophone)
            }
            when {
                !lost -> alarm.clear()
                !silenced -> alarm.raise()
            }
        }
    }

    private companion object {
        const val TAG = "ReceiverSession"
        val WATCHDOG_TICK = 1.seconds
    }
}
