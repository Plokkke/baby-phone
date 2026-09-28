package fr.crntech.babyphone.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.R
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.ui.components.PeerList
import fr.crntech.babyphone.ui.components.QrCode
import fr.crntech.babyphone.ui.components.RoleIcon

@Composable
fun HomeScreen(
    link: String,
    peers: List<Peer>,
    onRole: (Role) -> Unit,
    onScan: () -> Unit,
    onReset: () -> Unit,
) {
    var showQr by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.role_title), style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoleCard(Role.EMITTER, R.string.role_emitter, onRole, Modifier.weight(1f))
            RoleCard(Role.RECEIVER, R.string.role_receiver, onRole, Modifier.weight(1f))
        }
        PeerList(peers)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = { showQr = true }) { Text(stringResource(R.string.home_add_device)) }
            TextButton(onClick = onScan) { Text(stringResource(R.string.pairing_scan)) }
            TextButton(onClick = { confirmReset = true }) { Text(stringResource(R.string.home_reset)) }
        }
    }

    if (showQr) {
        AlertDialog(
            onDismissRequest = { showQr = false },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text(stringResource(R.string.close)) } },
            title = { Text(stringResource(R.string.home_add_device)) },
            text = { QrCode(link, Modifier.fillMaxWidth()) },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; onReset() }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.home_reset)) },
            text = { Text(stringResource(R.string.home_reset_confirm)) },
        )
    }
}

@Composable
private fun RoleCard(role: Role, label: Int, onRole: (Role) -> Unit, modifier: Modifier) {
    ElevatedCard(onClick = { onRole(role) }, modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RoleIcon(role, Modifier.fillMaxWidth().aspectRatio(1f))
            Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
        }
    }
}
