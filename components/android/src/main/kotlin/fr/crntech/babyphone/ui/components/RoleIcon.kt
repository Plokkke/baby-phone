package fr.crntech.babyphone.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import fr.crntech.babyphone.shared.Role
import fr.crntech.babyphone.ui.Palette

/** Sleeping baby vs. talking group: roles are told apart by pictures, not labels. */
@Composable
fun RoleIcon(role: Role, modifier: Modifier = Modifier, tint: Color = Color.White) {
    val (main, badge) = role.icons()
    Box(modifier) {
        Icon(main, contentDescription = null, tint = tint, modifier = Modifier.fillMaxSize())
        Icon(
            badge, contentDescription = null, tint = Palette.moon,
            modifier = Modifier.fillMaxSize(BADGE_RATIO).align(Alignment.TopEnd),
        )
    }
}

private fun Role.icons(): Pair<ImageVector, ImageVector> = when (this) {
    Role.EMITTER -> Icons.Filled.ChildCare to Icons.Filled.Bedtime
    else -> Icons.Filled.Groups to Icons.AutoMirrored.Filled.Chat
}

private const val BADGE_RATIO = 0.4f
