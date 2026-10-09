package fr.crntech.babyphone.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.compat_peer_outdated
import fr.crntech.babyphone.client.resources.compat_peer_outdated_version
import fr.crntech.babyphone.client.resources.compat_self_outdated
import fr.crntech.babyphone.client.ui.Palette
import fr.crntech.babyphone.shared.Outdated
import fr.crntech.babyphone.shared.Peer
import org.jetbrains.compose.resources.stringResource

/** Devices of the pairing that cannot understand this one, and which side to update; nothing when all can. */
@Composable
fun CompatibilityWarning(peers: List<Peer>, modifier: Modifier = Modifier) {
    val incompatible = peers.mapNotNull { peer -> peer.outdated?.let { peer to it } }
    if (incompatible.isEmpty()) return
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Palette.alarm, contentColor = Color.Black)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                incompatible.forEach { (peer, outdated) -> Text(message(peer, outdated), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun message(peer: Peer, outdated: Outdated) = when {
    outdated == Outdated.SELF -> stringResource(Res.string.compat_self_outdated, peer.name)
    peer.version != null -> stringResource(Res.string.compat_peer_outdated_version, peer.name, peer.version.orEmpty())
    else -> stringResource(Res.string.compat_peer_outdated, peer.name)
}
