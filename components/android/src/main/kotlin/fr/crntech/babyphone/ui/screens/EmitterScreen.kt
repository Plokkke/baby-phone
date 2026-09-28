package fr.crntech.babyphone.ui.screens

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.crntech.babyphone.R
import fr.crntech.babyphone.monitor.EmitterSession
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.ui.Palette
import fr.crntech.babyphone.ui.components.RoleIcon

private val dim = Color(0xFF3A3F4B)

/** Nearly black, dimmed screen: it sits in the baby's room all night. */
@Composable
fun EmitterScreen(session: EmitterSession, onStop: () -> Unit) {
    val state by session.state.collectAsStateWithLifecycle()
    DimScreen()
    Column(
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        RoleIcon(Role.EMITTER, Modifier.size(72.dp), tint = dim)
        Text(stringResource(R.string.emitter_active), color = dim)
        Text(
            if (state.connected) pluralStringResource(R.plurals.emitter_receivers, state.receivers, state.receivers)
            else stringResource(R.string.server_connecting),
            color = dim, style = MaterialTheme.typography.bodySmall,
        )
        if (state.transmitting) Text(stringResource(R.string.emitter_transmitting), color = Palette.sound.copy(alpha = 0.5f))
        if (state.parentTalking) Text(stringResource(R.string.emitter_parent_talking), color = Palette.moon.copy(alpha = 0.6f))
        Box(Modifier.weight(1f))
        HoldToStop(onStop)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HoldToStop(onStop: () -> Unit) {
    Text(
        stringResource(R.string.emitter_hold_to_stop),
        color = dim,
        modifier = Modifier
            .background(Color(0xFF0C0D10), RoundedCornerShape(24.dp))
            .combinedClickable(onClick = {}, onLongClick = onStop)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
private fun DimScreen() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        val previous = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = 0.01f }
        onDispose {
            window.attributes = window.attributes.apply { screenBrightness = previous }
        }
    }
}

