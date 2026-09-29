package fr.crntech.babyphone.platform

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fr.crntech.babyphone.client.platform.QuietMode
import fr.crntech.babyphone.client.platform.QuietState
import fr.crntech.babyphone.client.platform.SoundOutput
import fr.crntech.babyphone.client.platform.SoundOutputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidQuietMode(private val context: Context) : QuietMode {
    private val notifications = requireNotNull(context.getSystemService(NotificationManager::class.java))
    private val _state = MutableStateFlow(read())
    override val state = _state.asStateFlow()

    init {
        context.onBroadcast(
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED,
            NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED,
        ) { _state.value = read() }
    }

    override fun setEnabled(enabled: Boolean) {
        if (!notifications.isNotificationPolicyAccessGranted) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        notifications.setInterruptionFilter(if (enabled) QUIET_FILTER else NotificationManager.INTERRUPTION_FILTER_ALL)
        _state.value = read()
    }

    /** Total silence (`NONE`) would also mute the parents talking back, hence the filter the app sets. */
    val blocksAllSound get() = notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE

    private fun read() = QuietState(
        supported = true,
        enabled = notifications.currentInterruptionFilter !in NOT_QUIET,
        controllable = notifications.isNotificationPolicyAccessGranted,
    )

    private companion object {
        const val QUIET_FILTER = NotificationManager.INTERRUPTION_FILTER_ALARMS
        val NOT_QUIET = setOf(NotificationManager.INTERRUPTION_FILTER_ALL, NotificationManager.INTERRUPTION_FILTER_UNKNOWN)
    }
}

/** Media carries the baby's audio, alarm carries the lost-link alert: both must be loud enough. */
class AndroidSoundOutput(private val context: Context, private val quietMode: AndroidQuietMode) : SoundOutput {
    private val audio = requireNotNull(context.getSystemService(AudioManager::class.java))
    private val main = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(read())
    override val state = _state.asStateFlow()

    init {
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true,
            object : ContentObserver(main) {
                override fun onChange(selfChange: Boolean) = refresh()
            },
        )
        context.onBroadcast(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED, VOLUME_CHANGED) { refresh() }
    }

    override fun makeAudible() {
        if (quietMode.blocksAllSound) quietMode.setEnabled(false)
        STREAMS.forEach { stream ->
            runCatching { audio.setStreamVolume(stream, audio.getStreamMaxVolume(stream), AudioManager.FLAG_SHOW_UI) }
        }
        refresh()
    }

    override fun playTestSound() {
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, TEST_TONE_MS)
        main.postDelayed(tone::release, TEST_TONE_MS * 2L)
    }

    private fun refresh() {
        _state.value = read()
    }

    private fun read() = SoundOutputState(
        volumePercent = STREAMS.minOf { audio.getStreamVolume(it) * 100 / audio.getStreamMaxVolume(it) },
        muted = quietMode.blocksAllSound || STREAMS.any(audio::isStreamMute),
        alertsBlocked = !NotificationManagerCompat.from(context).areNotificationsEnabled(),
    )

    private companion object {
        val STREAMS = listOf(AudioManager.STREAM_MUSIC, AudioManager.STREAM_ALARM)
        const val VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val TEST_TONE_MS = 600
    }
}

private fun Context.onBroadcast(vararg actions: String, onReceive: () -> Unit) {
    val filter = IntentFilter().apply { actions.forEach(::addAction) }
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = onReceive()
    }
    ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
}
