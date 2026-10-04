package fr.crntech.babyphone.client.monitor

import fr.crntech.babyphone.client.net.PeerLink
import fr.crntech.babyphone.client.platform.Alarm
import fr.crntech.babyphone.client.platform.MicMode
import fr.crntech.babyphone.client.platform.Microphone
import fr.crntech.babyphone.client.platform.SoundOutput
import fr.crntech.babyphone.client.platform.Speaker
import fr.crntech.babyphone.shared.Loudness
import fr.crntech.babyphone.shared.MicrophoneHealth
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.PeerMessage
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.shared.Timing
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Who a parent's push-to-talk reaches. */
sealed interface TalkTarget {
    data class Baby(val deviceId: String) : TalkTarget
    data object Parents : TalkTarget
}

class ReceiverSession(
    private val link: PeerLink,
    private val microphone: Microphone,
    private val speaker: () -> Speaker,
    private val alarm: Alarm,
    private val soundOutput: SoundOutput,
) : MonitorSession {

    /** A baby's phone, kept once heard so that losing it raises the alarm. */
    data class Baby(
        val deviceId: String,
        val name: String,
        val status: PeerMessage.EmitterStatus? = null,
        val lastHeard: TimeMark? = null,
        val online: Boolean = false,
        val lost: Boolean = false,
    )

    data class Parent(val peer: Peer, val lastHeard: TimeMark? = null, val talking: Boolean = false)

    data class State(
        val connected: Boolean = false,
        val babies: List<Baby> = emptyList(),
        val parents: List<Parent> = emptyList(),
        val listeningTo: String? = null,
        val talkingTo: TalkTarget? = null,
        val talkLevelDb: Float = Loudness.FLOOR_DB,
        val talkMicrophone: MicrophoneHealth.Status = MicrophoneHealth.Status.STARTING,
        val alarmSilenced: Boolean = false,
    ) {
        val linkLost get() = babies.any { it.lost }
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    val sound = soundOutput.state

    fun makeAudible() = soundOutput.makeAudible()

    fun playTestSound() = soundOutput.playTestSound()

    private val listening = MutableStateFlow<String?>(null)
    private val talking = MutableStateFlow<TalkTarget?>(null)
    private val outgoing = Channel<Pair<String, PeerMessage>>(Channel.UNLIMITED)

    @Volatile private var talkHealth: MicrophoneHealth? = null

    /** Live audio from [babyId] while held; null releases it. */
    fun setListening(babyId: String?) {
        listening.value = babyId
    }

    /** Push-to-talk: the microphone streams to [target] while held; null releases it. */
    fun setTalking(target: TalkTarget?) {
        talking.value = target
    }

    fun setThreshold(babyId: String, db: Float) {
        updateBaby(babyId) { baby -> baby.copy(status = baby.status?.copy(thresholdDb = db)) }
        outgoing.trySend(babyId to PeerMessage.SetThreshold(db))
    }

    fun silenceAlarm() {
        alarm.silence()
        _state.update { it.copy(alarmSilenced = true) }
    }

    override suspend fun run() = coroutineScope {
        if (soundOutput.state.value.tooQuiet) soundOutput.makeAudible()
        launch { link.run() }
        launch { link.connected.collect { c -> _state.update { it.copy(connected = c) } } }
        launch { link.presence.collect(::onPresence) }
        launch { for ((to, message) in outgoing) link.send(message, to) }
        launch { receive() }
        launch { keepListening() }
        launch { talk() }
        try {
            watch()
        } finally {
            alarm.clear()
        }
    }

    private fun onPresence(peers: List<Peer>) = _state.update { state ->
        val present = peers.filter { it.deviceId != link.self }
        val known = state.babies.associateBy { it.deviceId }
        val babies = present.filter { it.role == Role.EMITTER }
            .map { known[it.deviceId]?.copy(name = it.name) ?: Baby(it.deviceId, it.name) }
        val heardAndGone = state.babies.filter { it.lastHeard != null && babies.none { b -> b.deviceId == it.deviceId } }
        val talking = state.parents.associateBy { it.peer.deviceId }
        val parents = present.filter { it.role == Role.RECEIVER }.map { talking[it.deviceId]?.copy(peer = it) ?: Parent(it) }
        state.copy(babies = (babies + heardAndGone).sortedBy { it.name }, parents = parents.sortedBy { it.peer.name })
    }

    private suspend fun receive() = SpeakerMixer(speaker).use { speakers ->
        link.messages.collect { (from, _, message) ->
            val now = TimeSource.Monotonic.markNow()
            when (message) {
                is PeerMessage.EmitterStatus -> updateBaby(from) { it.copy(status = message, lastHeard = now) }
                PeerMessage.Leaving -> _state.update { s -> s.copy(babies = s.babies.filterNot { it.deviceId == from }) }
                is PeerMessage.Audio -> speakers.play(from, message.pcm)
                is PeerMessage.Intercom -> {
                    speakers.play(from, message.pcm)
                    _state.update { s -> s.copy(parents = s.parents.map { if (it.peer.deviceId == from) it.copy(lastHeard = now) else it }) }
                }
                else -> Unit
            }
        }
    }

    private fun updateBaby(deviceId: String, change: (Baby) -> Baby) = _state.update { state ->
        val babies = if (state.babies.any { it.deviceId == deviceId }) state.babies else state.babies + Baby(deviceId, "")
        state.copy(babies = babies.map { if (it.deviceId == deviceId) change(it) else it })
    }

    private suspend fun keepListening(): Nothing = listening.collectLatest { babyId ->
        _state.update { it.copy(listeningTo = babyId) }
        while (babyId != null) {
            link.send(PeerMessage.ForceListen, babyId)
            delay(Timing.FORCE_LISTEN_KEEPALIVE)
        }
    }

    private suspend fun talk(): Nothing = talking.collectLatest { target ->
        _state.update { it.copy(talkingTo = target, talkLevelDb = Loudness.FLOOR_DB, talkMicrophone = MicrophoneHealth.Status.STARTING) }
        talkHealth = target?.let { MicrophoneHealth() }
        if (target == null) return@collectLatest
        // A press that is never released (lost touch event) must not leave the microphone open.
        withTimeoutOrNull(Timing.TALKBACK_MAX) {
            microphone.frames(MicMode.VOICE)
                .catch { println("$TAG: talk microphone failed: ${it.message}") }
                .collect { frame ->
                    val level = Loudness.dbfs(frame)
                    talkHealth?.onFrame(level)
                    _state.update { it.copy(talkLevelDb = level) }
                    when (target) {
                        is TalkTarget.Baby -> link.send(PeerMessage.Audio(frame), target.deviceId)
                        TalkTarget.Parents -> link.send(PeerMessage.Intercom(frame))
                    }
                }
        }
        talking.value = null
    }

    /** Raises the alarm when a baby that was heard goes silent, and fades the "talking" marks. */
    private suspend fun watch(): Nothing {
        while (true) {
            delay(WATCH_TICK)
            val state = _state.updateAndGet(::watched)
            when {
                !state.linkLost -> alarm.clear()
                !state.alarmSilenced -> alarm.raise()
            }
        }
    }

    private fun watched(state: State): State {
        val babies = state.babies.map { baby ->
            val lost = baby.lastHeard?.let { it.elapsedNow() > Timing.EMITTER_SILENCE_ALARM } ?: false
            baby.copy(online = baby.lastHeard != null && !lost, lost = lost)
        }
        val parents = state.parents.map { it.copy(talking = it.lastHeard?.let { mark -> mark.elapsedNow() < TALKING_FADE } ?: false) }
        val silenced = state.alarmSilenced && babies.any { it.lost }
        return state.copy(babies = babies, parents = parents, alarmSilenced = silenced, talkMicrophone = talkHealth?.status() ?: state.talkMicrophone)
    }

    private companion object {
        const val TAG = "ReceiverSession"
        val WATCH_TICK = 250.milliseconds
        val TALKING_FADE = 600.milliseconds
    }
}
