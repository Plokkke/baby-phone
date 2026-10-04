package fr.crntech.babyphone.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.client.monitor.ReceiverSession
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.battery_level
import fr.crntech.babyphone.client.resources.receiver_emitter_offline
import fr.crntech.babyphone.client.resources.receiver_hold_to_listen
import fr.crntech.babyphone.client.resources.receiver_hold_to_talk
import fr.crntech.babyphone.client.resources.receiver_quiet
import fr.crntech.babyphone.client.resources.receiver_sound_detected
import fr.crntech.babyphone.client.ui.Palette
import fr.crntech.babyphone.shared.Loudness
import fr.crntech.babyphone.shared.MicrophoneHealth
import fr.crntech.babyphone.shared.Role
import org.jetbrains.compose.resources.stringResource

/** What the parent's microphone does while talking to this baby; null when not talking to it. */
data class TalkFeedback(val levelDb: Float, val microphone: MicrophoneHealth.Status)

/** One baby: status, level with its adjustable threshold, and hold-to-listen / push-to-talk at the bottom. */
@Composable
fun BabyCard(
    baby: ReceiverSession.Baby,
    listening: Boolean,
    talk: TalkFeedback?,
    onThreshold: (Float) -> Unit,
    onListen: (Boolean) -> Unit,
    onTalk: (Boolean) -> Unit,
) {
    val status = baby.status
    Card(
        colors = CardDefaults.cardColors(containerColor = if (talk != null) Palette.talk else MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BabyHeader(baby)
            EmitterQuietWarning(status?.quiet)
            if (status != null && baby.online) {
                Text(
                    stringResource(if (status.transmitting) Res.string.receiver_sound_detected else Res.string.receiver_quiet),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (status.transmitting) Palette.sound else Palette.calm,
                )
            }
            LevelMeter(status?.levelDb ?: Loudness.FLOOR_DB, status?.thresholdDb, onThresholdChange = onThreshold.takeIf { status != null })
            if (talk != null) MicrophoneCheck(talk.microphone, talk.levelDb, thresholdDb = null)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HoldButton(
                    Icons.Filled.Hearing, stringResource(Res.string.receiver_hold_to_listen), listening, Palette.sound, onListen,
                    Modifier.weight(1f),
                )
                HoldButton(
                    Icons.Filled.Mic, stringResource(Res.string.receiver_hold_to_talk), talk != null, Color.White.copy(alpha = 0.3f), onTalk,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BabyHeader(baby: ReceiverSession.Baby) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RoleIcon(Role.EMITTER, Modifier.size(32.dp), tint = if (baby.online) Palette.sound else Color.Gray)
        Column(Modifier.weight(1f)) {
            Text(baby.name, fontWeight = FontWeight.Bold)
            if (!baby.online) Text(stringResource(Res.string.receiver_emitter_offline), style = MaterialTheme.typography.bodySmall)
        }
        baby.status?.takeIf { baby.online }?.let { status ->
            Icon(if (status.charging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd, contentDescription = null)
            Text(stringResource(Res.string.battery_level, status.batteryPercent))
        }
    }
}
