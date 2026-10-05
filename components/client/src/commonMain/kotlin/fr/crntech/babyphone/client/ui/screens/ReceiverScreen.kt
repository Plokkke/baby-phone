package fr.crntech.babyphone.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.crntech.babyphone.client.monitor.ReceiverSession
import fr.crntech.babyphone.client.monitor.TalkTarget
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.alarm_silence
import fr.crntech.babyphone.client.resources.alarm_text
import fr.crntech.babyphone.client.resources.alarm_title
import fr.crntech.babyphone.client.resources.receiver_no_baby
import fr.crntech.babyphone.client.resources.receiver_stop
import fr.crntech.babyphone.client.resources.server_connecting
import fr.crntech.babyphone.client.ui.Palette
import fr.crntech.babyphone.client.ui.components.BabyCard
import fr.crntech.babyphone.client.ui.components.PairingActions
import fr.crntech.babyphone.client.ui.components.ParentsCard
import fr.crntech.babyphone.client.ui.components.SoundCheck
import fr.crntech.babyphone.client.ui.components.TalkFeedback
import org.jetbrains.compose.resources.stringResource

/** One card per baby, then the other parents. */
@Composable
fun ReceiverScreen(session: ReceiverSession, link: String, onStop: () -> Unit) {
    val state by session.state.collectAsStateWithLifecycle()
    val sound by session.sound.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PairingActions(link)
        if (state.linkLost) AlarmBanner(state.alarmSilenced, session::silenceAlarm)
        if (!state.connected) Text(stringResource(Res.string.server_connecting))
        SoundCheck(sound, session::makeAudible, session::playTestSound)
        if (state.babies.isEmpty()) Text(stringResource(Res.string.receiver_no_baby), style = MaterialTheme.typography.bodyMedium)
        state.babies.forEach { baby -> Baby(baby, state, session) }
        ParentsCard(state.parents, talking = state.talkingTo == TalkTarget.Parents) { held ->
            session.setTalking(TalkTarget.Parents.takeIf { held })
        }
        TextButton(onClick = onStop) { Text(stringResource(Res.string.receiver_stop)) }
    }
}

@Composable
private fun Baby(baby: ReceiverSession.Baby, state: ReceiverSession.State, session: ReceiverSession) {
    val target = TalkTarget.Baby(baby.deviceId)
    BabyCard(
        baby = baby,
        listening = state.listeningTo == baby.deviceId,
        talk = TalkFeedback(state.talkLevelDb, state.talkMicrophone).takeIf { state.talkingTo == target },
        onThreshold = { session.setThreshold(baby.deviceId, it) },
        onListen = { held -> session.setListening(baby.deviceId.takeIf { held }) },
        onTalk = { held -> session.setTalking(target.takeIf { held }) },
    )
}

@Composable
private fun AlarmBanner(silenced: Boolean, onSilence: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Palette.alarm, contentColor = Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.alarm_title), fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.alarm_text), style = MaterialTheme.typography.bodySmall)
            }
            if (!silenced) TextButton(onClick = onSilence) { Text(stringResource(Res.string.alarm_silence), color = Color.Black) }
        }
    }
}
