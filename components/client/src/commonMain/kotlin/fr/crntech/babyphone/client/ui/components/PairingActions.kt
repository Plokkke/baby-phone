package fr.crntech.babyphone.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.cancel
import fr.crntech.babyphone.client.resources.close
import fr.crntech.babyphone.client.resources.confirm
import fr.crntech.babyphone.client.resources.home_add_device
import fr.crntech.babyphone.client.resources.home_reset
import fr.crntech.babyphone.client.resources.home_reset_confirm
import org.jetbrains.compose.resources.stringResource

/** Top-right corner: the QR code to add a device, and leaving the pairing when [onReset] is given. */
@Composable
fun PairingActions(link: String, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current, onReset: (() -> Unit)? = null) {
    var showQr by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = { showQr = true }) {
            Icon(Icons.Filled.QrCode2, contentDescription = stringResource(Res.string.home_add_device), tint = tint)
        }
        if (onReset != null) {
            IconButton(onClick = { confirmReset = true }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.home_reset), tint = tint)
            }
        }
    }

    if (showQr) {
        AlertDialog(
            onDismissRequest = { showQr = false },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text(stringResource(Res.string.close)) } },
            title = { Text(stringResource(Res.string.home_add_device)) },
            text = { PairingCode(link, Modifier.fillMaxWidth()) },
        )
    }
    if (confirmReset && onReset != null) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text(stringResource(Res.string.confirm)) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(Res.string.cancel)) } },
            title = { Text(stringResource(Res.string.home_reset)) },
            text = { Text(stringResource(Res.string.home_reset_confirm)) },
        )
    }
}
