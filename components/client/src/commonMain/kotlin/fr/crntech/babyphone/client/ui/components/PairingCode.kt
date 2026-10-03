package fr.crntech.babyphone.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.client.platform.LocalPlatformUi
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.pairing_copied
import fr.crntech.babyphone.client.resources.pairing_copy
import fr.crntech.babyphone.client.resources.pairing_share
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val COPIED_FEEDBACK_MS = 2_000L

/** The pairing QR code, with the same link to copy or share for devices that cannot scan it. */
@Composable
fun PairingCode(link: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QrCode(link, modifier)
        LinkActions(link)
    }
}

@Composable
private fun LinkActions(link: String) {
    val platformUi = LocalPlatformUi.current
    val copy = platformUi.rememberCopier()
    val share = platformUi.rememberSharer()
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) delay(COPIED_FEEDBACK_MS).also { copied = false }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (copied) ActionButton(Icons.Filled.Check, stringResource(Res.string.pairing_copied)) {}
        else ActionButton(Icons.Filled.ContentCopy, stringResource(Res.string.pairing_copy)) { copy(link); copied = true }
        if (share != null) ActionButton(Icons.Filled.Share, stringResource(Res.string.pairing_share)) { share(link) }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(label)
    }
}
