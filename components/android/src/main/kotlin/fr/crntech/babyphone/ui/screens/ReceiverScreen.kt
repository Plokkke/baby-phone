package fr.crntech.babyphone.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.crntech.babyphone.R
import fr.crntech.babyphone.monitor.ReceiverSession
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.shared.Threshold
import fr.crntech.babyphone.shared.Timing
import fr.crntech.babyphone.ui.Palette
import fr.crntech.babyphone.ui.components.LevelMeter
import fr.crntech.babyphone.ui.components.RoleIcon
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark

@Composable
fun ReceiverScreen(session: ReceiverSession, onStop: () -> Unit) {
    val state by session.state.collectAsStateWithLifecycle()
    val talkingSince = state.talkingSince
    val background by animateColorAsState(
        if (talkingSince != null) Palette.talk else MaterialTheme.colorScheme.background, label = "background",
    )
    Box(Modifier.fillMaxSize().background(background).safeDrawingPadding().padding(24.dp)) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            if (state.linkLost) AlarmBanner(state.alarmSilenced, session::silenceAlarm)
            if (talkingSince != null) {
                TalkingPanel(talkingSince) { session.setTalking(false) }
            } else {
                MonitorPanel(state, session, onStop)
            }
        }
    }
}

@Composable
private fun MonitorPanel(state: ReceiverSession.State, session: ReceiverSession, onStop: () -> Unit) {
    StatusRow(state)
    Text(
        stringResource(if (state.transmitting) R.string.receiver_sound_detected else R.string.receiver_quiet),
        style = MaterialTheme.typography.headlineMedium,
        color = if (state.transmitting) Palette.sound else Palette.calm,
    )
    LevelMeter(state.levelDb, state.thresholdDb)
    ThresholdSlider(state.thresholdDb, session::setThreshold)
    HoldToListen(state.listening, session::setListening)
    OutlinedButton(onClick = { session.setTalking(true) }) {
        Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(stringResource(R.string.receiver_talk))
    }
    TextButton(onClick = onStop) { Text(stringResource(R.string.receiver_stop)) }
}

@Composable
private fun StatusRow(state: ReceiverSession.State) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val online = state.connected && state.emitterOnline
        RoleIcon(Role.EMITTER, Modifier.size(32.dp), tint = if (online) Palette.sound else Color.Gray)
        Text(
            stringResource(
                when {
                    !state.connected -> R.string.server_connecting
                    online -> R.string.receiver_emitter_online
                    else -> R.string.receiver_emitter_offline
                },
            ),
        )
        state.batteryPercent?.let { percent ->
            Icon(if (state.charging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd, contentDescription = null)
            Text(stringResource(R.string.battery_level, percent))
        }
    }
}

@Composable
private fun ThresholdSlider(thresholdDb: Float, onChange: (Float) -> Unit) {
    var draft by remember(thresholdDb) { mutableFloatStateOf(thresholdDb) }
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.receiver_threshold, draft.roundToInt()), style = MaterialTheme.typography.bodySmall)
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onChange(draft) },
            valueRange = Threshold.MIN_DB..Threshold.MAX_DB,
        )
    }
}

/** Live audio only while the finger stays down; the emitter times out on its own if we vanish. */
@Composable
private fun HoldToListen(listening: Boolean, onListening: (Boolean) -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (listening) Palette.sound else MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(180.dp).pointerInput(Unit) {
            detectTapGestures(onPress = {
                onListening(true)
                try {
                    awaitRelease()
                } finally {
                    onListening(false)
                }
            })
        },
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Hearing, contentDescription = null, modifier = Modifier.size(48.dp))
            Text(
                stringResource(if (listening) R.string.receiver_listening else R.string.receiver_hold_to_listen),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TalkingPanel(since: TimeMark, onStop: () -> Unit) {
    val remaining by produceState((Timing.TALKBACK_MAX - since.elapsedNow()).inWholeSeconds, since) {
        while (true) {
            value = (Timing.TALKBACK_MAX - since.elapsedNow()).inWholeSeconds.coerceAtLeast(0)
            delay(250.milliseconds)
        }
    }
    Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(120.dp))
    Text(
        stringResource(R.string.receiver_talking_banner),
        style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color.White,
    )
    Text(stringResource(R.string.receiver_talking_hint), color = Color.White)
    Text(stringResource(R.string.receiver_talk_remaining, remaining.toInt()), color = Color.White)
    Button(
        onClick = onStop,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Palette.talk),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    ) {
        Text(stringResource(R.string.receiver_stop_talking), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun AlarmBanner(silenced: Boolean, onSilence: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Palette.alarm, contentColor = Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.alarm_title), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.alarm_text), style = MaterialTheme.typography.bodySmall)
            }
            if (!silenced) TextButton(onClick = onSilence) { Text(stringResource(R.string.alarm_silence), color = Color.Black) }
        }
    }
}
