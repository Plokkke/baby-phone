package fr.crntech.babyphone.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.pairing_hint
import fr.crntech.babyphone.client.resources.pairing_scan
import fr.crntech.babyphone.client.resources.pairing_title
import fr.crntech.babyphone.client.resources.pairing_waiting
import fr.crntech.babyphone.client.ui.components.QrCode

@Composable
fun PairingScreen(link: String, onScan: (() -> Unit)?) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.pairing_title), style = MaterialTheme.typography.headlineSmall)
        QrCode(link, Modifier.fillMaxWidth(0.8f))
        Text(stringResource(Res.string.pairing_hint), textAlign = TextAlign.Center)
        Text(stringResource(Res.string.pairing_waiting), style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(Modifier.fillMaxWidth(0.5f))
        if (onScan != null) {
            OutlinedButton(onClick = onScan) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(Res.string.pairing_scan))
            }
        }
    }
}
