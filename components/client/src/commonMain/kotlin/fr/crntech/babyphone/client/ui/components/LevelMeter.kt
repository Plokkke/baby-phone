package fr.crntech.babyphone.client.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.client.resources.Res
import fr.crntech.babyphone.client.resources.receiver_threshold
import fr.crntech.babyphone.client.ui.Palette
import fr.crntech.babyphone.shared.Threshold
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Live sound level with the trigger threshold on the same bar.
 * With [onThresholdChange] the threshold is a handle to drag (or tap); without it the bar is read-only.
 */
@Composable
fun LevelMeter(levelDb: Float, thresholdDb: Float?, modifier: Modifier = Modifier, onThresholdChange: ((Float) -> Unit)? = null) {
    var draft by remember { mutableStateOf<Float?>(null) }
    val shownThreshold = draft ?: thresholdDb
    Column(modifier.fillMaxWidth()) {
        if (onThresholdChange != null && shownThreshold != null) {
            Text(stringResource(Res.string.receiver_threshold, shownThreshold.roundToInt()), style = MaterialTheme.typography.bodySmall)
        }
        val level by animateFloatAsState(fraction(levelDb), label = "level")
        val editable = onThresholdChange != null
        Canvas(
            Modifier.fillMaxWidth().height(if (editable) EDITABLE_HEIGHT else BAR_HEIGHT)
                .thresholdInput(onThresholdChange, onDraft = { draft = it }),
        ) {
            drawBar(level, above = shownThreshold != null && levelDb >= shownThreshold)
            shownThreshold?.let { drawThreshold(fraction(it), handle = editable) }
        }
    }
}

/** Tap to set, or drag the handle: the value is only sent on release, not on every move. */
@Composable
private fun Modifier.thresholdInput(onChange: ((Float) -> Unit)?, onDraft: (Float?) -> Unit): Modifier {
    if (onChange == null) return this
    val commit by rememberUpdatedState(onChange)
    var current by remember { mutableFloatStateOf(0f) }
    return this
        .pointerInput(Unit) {
            detectTapGestures { offset -> commit(dbAt(offset.x / size.width)) }
        }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { offset -> current = dbAt(offset.x / size.width).also(onDraft) },
                onDragEnd = { commit(current); onDraft(null) },
                onDragCancel = { onDraft(null) },
            ) { change, _ -> current = dbAt(change.position.x / size.width).also(onDraft) }
        }
}

private fun DrawScope.drawBar(level: Float, above: Boolean) {
    val top = (size.height - BAR_HEIGHT.toPx()) / 2
    val barSize = Size(size.width, BAR_HEIGHT.toPx())
    val radius = CornerRadius(barSize.height / 2)
    drawRoundRect(TRACK, Offset(0f, top), barSize, radius)
    drawRoundRect(if (above) Palette.sound else Palette.calm, Offset(0f, top), barSize.copy(width = size.width * level), radius)
}

private fun DrawScope.drawThreshold(position: Float, handle: Boolean) {
    val x = size.width * position
    drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3.dp.toPx())
    if (handle) {
        drawCircle(Palette.night, HANDLE_RADIUS.toPx() + 2.dp.toPx(), Offset(x, size.height / 2))
        drawCircle(Color.White, HANDLE_RADIUS.toPx(), Offset(x, size.height / 2))
    }
}

private fun fraction(db: Float) =
    ((db - Threshold.MIN_DB) / (Threshold.MAX_DB - Threshold.MIN_DB)).coerceIn(0f, 1f)

private fun dbAt(fraction: Float) =
    (Threshold.MIN_DB + fraction.coerceIn(0f, 1f) * (Threshold.MAX_DB - Threshold.MIN_DB)).roundToInt().toFloat()

private val BAR_HEIGHT = 28.dp
private val EDITABLE_HEIGHT = 44.dp
private val HANDLE_RADIUS = 12.dp

/** Translucent, so the empty bar shows on the black night screen as well as on cards. */
private val TRACK = Color.White.copy(alpha = 0.1f)
