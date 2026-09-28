package fr.crntech.babyphone.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.crntech.babyphone.shared.Threshold
import fr.crntech.babyphone.ui.Palette

/** Horizontal sound meter with the trigger threshold drawn as a vertical mark. */
@Composable
fun LevelMeter(levelDb: Float, thresholdDb: Float, modifier: Modifier = Modifier) {
    val level by animateFloatAsState(fraction(levelDb), label = "level")
    val above = levelDb >= thresholdDb
    Canvas(modifier.fillMaxWidth().height(28.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(Palette.surface, cornerRadius = radius)
        drawRoundRect(if (above) Palette.sound else Palette.calm, size = Size(size.width * level, size.height), cornerRadius = radius)
        val x = size.width * fraction(thresholdDb)
        drawLine(Color.White, Offset(x, -4f), Offset(x, size.height + 4f), strokeWidth = 3.dp.toPx())
    }
}

private fun fraction(db: Float) =
    ((db - Threshold.MIN_DB) / (Threshold.MAX_DB - Threshold.MIN_DB)).coerceIn(0f, 1f)
