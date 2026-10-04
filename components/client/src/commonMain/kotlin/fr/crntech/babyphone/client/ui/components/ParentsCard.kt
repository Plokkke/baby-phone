package fr.crntech.babyphone.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.client.monitor.ReceiverSession
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.parents_alone
import fr.crntech.babyphone.client.resources.parents_hold_to_talk
import fr.crntech.babyphone.client.resources.parents_title
import fr.crntech.babyphone.client.ui.Palette
import fr.crntech.babyphone.shared.Role
import org.jetbrains.compose.resources.stringResource

/** The other parents listening, who is speaking, and push-to-talk between parents. */
@Composable
fun ParentsCard(parents: List<ReceiverSession.Parent>, talking: Boolean, onTalk: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.parents_title), fontWeight = FontWeight.Bold)
            if (parents.isEmpty()) Text(stringResource(Res.string.parents_alone), style = MaterialTheme.typography.bodySmall)
            parents.forEach { parent ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoleIcon(Role.RECEIVER, Modifier.size(24.dp))
                    Text(parent.peer.name, Modifier.weight(1f))
                    if (parent.talking) Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Palette.sound)
                }
            }
            if (parents.isNotEmpty()) {
                HoldButton(
                    Icons.Filled.RecordVoiceOver, stringResource(Res.string.parents_hold_to_talk), talking, Palette.talk, onTalk,
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
