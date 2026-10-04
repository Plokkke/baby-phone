package fr.crntech.babyphone.client.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Active only while the finger stays down: listening and talking can never be left on by mistake. */
@Composable
fun HoldButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    onHold: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hold by rememberUpdatedState(onHold)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (active) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onPress = {
                hold(true)
                try {
                    awaitRelease()
                } finally {
                    hold(false)
                }
            })
        },
    ) {
        Row(
            Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
