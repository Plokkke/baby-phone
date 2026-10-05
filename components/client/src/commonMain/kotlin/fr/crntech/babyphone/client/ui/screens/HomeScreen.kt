package fr.crntech.babyphone.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.app_version
import fr.crntech.babyphone.client.resources.role_emitter
import fr.crntech.babyphone.client.resources.role_receiver
import fr.crntech.babyphone.client.resources.role_title
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.client.ui.components.PairingActions
import fr.crntech.babyphone.client.ui.components.PeerList
import fr.crntech.babyphone.client.ui.components.RoleIcon

@Composable
fun HomeScreen(
    link: String,
    peers: List<Peer>,
    onRole: (Role) -> Unit,
    onReset: () -> Unit,
    appVersion: String?,
) {
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(Res.string.role_title), style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RoleCard(Role.EMITTER, Res.string.role_emitter, onRole, Modifier.weight(1f))
                RoleCard(Role.RECEIVER, Res.string.role_receiver, onRole, Modifier.weight(1f))
            }
            PeerList(peers)
            appVersion?.let { Text(stringResource(Res.string.app_version, it), style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
        }
        PairingActions(link, Modifier.align(Alignment.TopEnd).padding(8.dp), onReset = onReset)
    }
}

@Composable
private fun RoleCard(role: Role, label: StringResource, onRole: (Role) -> Unit, modifier: Modifier) {
    ElevatedCard(onClick = { onRole(role) }, modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RoleIcon(role, Modifier.fillMaxWidth().aspectRatio(1f))
            Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
        }
    }
}
