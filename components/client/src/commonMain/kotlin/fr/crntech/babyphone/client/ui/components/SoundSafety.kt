package fr.crntech.babyphone.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotDisturbOff
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.client.platform.QuietState
import fr.crntech.babyphone.client.platform.SoundOutputState
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.mic_no_signal
import fr.crntech.babyphone.client.resources.mic_silent
import fr.crntech.babyphone.client.resources.quiet_grant
import fr.crntech.babyphone.client.resources.quiet_off
import fr.crntech.babyphone.client.resources.quiet_on
import fr.crntech.babyphone.client.resources.receiver_emitter_not_quiet
import fr.crntech.babyphone.client.resources.sound_alerts_blocked
import fr.crntech.babyphone.client.resources.sound_muted
import fr.crntech.babyphone.client.resources.sound_raise
import fr.crntech.babyphone.client.resources.sound_test
import fr.crntech.babyphone.client.resources.sound_too_quiet
import fr.crntech.babyphone.client.resources.sound_unknown_volume
import fr.crntech.babyphone.client.resources.sound_unmute
import fr.crntech.babyphone.client.ui.Palette
import fr.crntech.babyphone.shared.MicrophoneHealth
import org.jetbrains.compose.resources.stringResource

/** Local microphone level, and why nothing is captured when that happens. */
@Composable
fun MicrophoneCheck(status: MicrophoneHealth.Status, levelDb: Float, thresholdDb: Float?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LevelMeter(levelDb, thresholdDb)
        val warning = when (status) {
            MicrophoneHealth.Status.NO_SIGNAL -> Res.string.mic_no_signal
            MicrophoneHealth.Status.SILENT -> Res.string.mic_silent
            else -> null
        }
        if (warning != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.MicOff, contentDescription = null, tint = Palette.alarm, modifier = Modifier.size(18.dp))
                Text(stringResource(warning), color = Palette.alarm, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Emitter-side do-not-disturb switch; asks for the system access first when the app lacks it. */
@Composable
fun QuietToggle(state: QuietState, onToggle: (Boolean) -> Unit, tint: Color) {
    if (!state.supported) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(if (state.enabled) Icons.Filled.DoNotDisturbOn else Icons.Filled.DoNotDisturbOff, contentDescription = null, tint = tint)
        Text(
            stringResource(
                when {
                    !state.controllable && !state.enabled -> Res.string.quiet_grant
                    state.enabled -> Res.string.quiet_on
                    else -> Res.string.quiet_off
                },
            ),
            color = tint,
            style = MaterialTheme.typography.bodySmall,
        )
        Switch(
            checked = state.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = tint.copy(alpha = 0.4f), checkedThumbColor = tint),
        )
    }
}

/** Receiver-side hint about the baby's phone; nothing when it is fine or unknown. */
@Composable
fun EmitterQuietWarning(quiet: Boolean?) {
    if (quiet != false) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.DoNotDisturbOff, contentDescription = null, tint = Palette.alarm, modifier = Modifier.size(18.dp))
        Text(stringResource(Res.string.receiver_emitter_not_quiet), color = Palette.alarm, style = MaterialTheme.typography.bodySmall)
    }
}

/** Everything that could keep the parents from hearing: shown only when something is wrong. */
@Composable
fun SoundCheck(state: SoundOutputState, onMakeAudible: () -> Unit, onTest: () -> Unit) {
    val problems = listOfNotNull(
        stringResource(Res.string.sound_muted).takeIf { state.muted },
        state.volumePercent?.let { stringResource(Res.string.sound_too_quiet, it) }?.takeIf { state.tooQuiet },
        stringResource(Res.string.sound_alerts_blocked).takeIf { state.alertsBlocked },
    )
    if (problems.isNotEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = Palette.alarm, contentColor = Color.Black)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VolumeOff, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    problems.forEach { Text(it, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) }
                }
                if (state.muted || state.tooQuiet) {
                    TextButton(onClick = onMakeAudible) {
                        Text(stringResource(if (state.muted) Res.string.sound_unmute else Res.string.sound_raise), color = Color.Black)
                    }
                }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (state.volumePercent == null) {
            Text(stringResource(Res.string.sound_unknown_volume), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onTest) { Text(stringResource(Res.string.sound_test)) }
    }
}
