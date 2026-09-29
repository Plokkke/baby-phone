package fr.crntech.babyphone.client.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.emitter_active
import fr.crntech.babyphone.client.resources.emitter_hold_to_stop
import fr.crntech.babyphone.client.resources.emitter_parent_talking
import fr.crntech.babyphone.client.resources.emitter_receivers
import fr.crntech.babyphone.client.resources.emitter_transmitting
import fr.crntech.babyphone.client.resources.server_connecting
import fr.crntech.babyphone.client.monitor.EmitterSession
import fr.crntech.babyphone.client.platform.LocalPlatformUi
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.client.ui.Palette
import fr.crntech.babyphone.client.ui.components.MicrophoneCheck
import fr.crntech.babyphone.client.ui.components.QuietToggle
import fr.crntech.babyphone.client.ui.components.RoleIcon

private val dim = Color(0xFF3A3F4B)

/** Nearly black, dimmed screen: it sits in the baby's room all night. */
@Composable
fun EmitterScreen(session: EmitterSession, onStop: () -> Unit) {
    val state by session.state.collectAsStateWithLifecycle()
    val quiet by session.quiet.collectAsStateWithLifecycle()
    LocalPlatformUi.current.NightScreen()
    Column(
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        RoleIcon(Role.EMITTER, Modifier.size(72.dp), tint = dim)
        Text(stringResource(Res.string.emitter_active), color = dim)
        Text(
            if (state.connected) pluralStringResource(Res.plurals.emitter_receivers, state.receivers, state.receivers)
            else stringResource(Res.string.server_connecting),
            color = dim, style = MaterialTheme.typography.bodySmall,
        )
        if (state.transmitting) Text(stringResource(Res.string.emitter_transmitting), color = Palette.sound.copy(alpha = 0.5f))
        if (state.parentTalking) Text(stringResource(Res.string.emitter_parent_talking), color = Palette.moon.copy(alpha = 0.6f))
        QuietToggle(quiet, session::setQuiet, dim)
        MicrophoneCheck(state.microphone, state.levelDb, state.thresholdDb, Modifier.fillMaxWidth(0.7f).alpha(0.5f))
        Box(Modifier.weight(1f))
        HoldToStop(onStop)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HoldToStop(onStop: () -> Unit) {
    Text(
        stringResource(Res.string.emitter_hold_to_stop),
        color = dim,
        modifier = Modifier
            .background(Color(0xFF0C0D10), RoundedCornerShape(24.dp))
            .combinedClickable(onClick = {}, onLongClick = onStop)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    )
}
