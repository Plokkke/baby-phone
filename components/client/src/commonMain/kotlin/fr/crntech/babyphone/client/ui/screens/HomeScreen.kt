package fr.crntech.babyphone.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.app_version
import fr.crntech.babyphone.client.resources.cancel
import fr.crntech.babyphone.client.resources.close
import fr.crntech.babyphone.client.resources.confirm
import fr.crntech.babyphone.client.resources.home_add_device
import fr.crntech.babyphone.client.resources.home_reset
import fr.crntech.babyphone.client.resources.home_reset_confirm
import fr.crntech.babyphone.client.resources.pairing_scan
import fr.crntech.babyphone.client.resources.role_emitter
import fr.crntech.babyphone.client.resources.role_receiver
import fr.crntech.babyphone.client.resources.role_title
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.client.ui.components.PeerList
import fr.crntech.babyphone.client.ui.components.PairingCode
import fr.crntech.babyphone.client.ui.components.RoleIcon

@Composable
fun HomeScreen(
    link: String,
    peers: List<Peer>,
    onRole: (Role) -> Unit,
    onScan: (() -> Unit)?,
    onReset: () -> Unit,
    appVersion: String?,
) {
    var showQr by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.role_title), style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoleCard(Role.EMITTER, Res.string.role_emitter, onRole, Modifier.weight(1f))
            RoleCard(Role.RECEIVER, Res.string.role_receiver, onRole, Modifier.weight(1f))
        }
        PeerList(peers)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = { showQr = true }) { Text(stringResource(Res.string.home_add_device)) }
            if (onScan != null) TextButton(onClick = onScan) { Text(stringResource(Res.string.pairing_scan)) }
            TextButton(onClick = { confirmReset = true }) { Text(stringResource(Res.string.home_reset)) }
        }
        appVersion?.let { Text(stringResource(Res.string.app_version, it), style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
    }

    if (showQr) {
        AlertDialog(
            onDismissRequest = { showQr = false },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text(stringResource(Res.string.close)) } },
            title = { Text(stringResource(Res.string.home_add_device)) },
            text = { PairingCode(link, Modifier.fillMaxWidth()) },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; onReset() }) { Text(stringResource(Res.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(Res.string.cancel)) } },
            title = { Text(stringResource(Res.string.home_reset)) },
            text = { Text(stringResource(Res.string.home_reset_confirm)) },
        )
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
