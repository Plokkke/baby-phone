package fr.crntech.babyphone.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.peers_connected
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.Role

@Composable
fun PeerList(peers: List<Peer>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(pluralStringResource(Res.plurals.peers_connected, peers.size, peers.size), style = MaterialTheme.typography.bodyMedium)
        peers.forEach { peer ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (peer.role != Role.IDLE) RoleIcon(peer.role, Modifier.size(24.dp))
                Text(peer.name, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
