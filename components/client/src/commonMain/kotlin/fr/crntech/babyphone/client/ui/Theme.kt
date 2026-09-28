package fr.crntech.babyphone.client.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Palette {
    val night = Color(0xFF10131A)
    val surface = Color(0xFF1A1F2B)
    val moon = Color(0xFFF5D76E)
    val calm = Color(0xFF8AB4F8)
    val sound = Color(0xFF7BD88F)
    val talk = Color(0xFFC62828)
    val alarm = Color(0xFFFF8F00)
}

private val colors = darkColorScheme(
    primary = Palette.calm,
    secondary = Palette.moon,
    background = Palette.night,
    surface = Palette.surface,
    error = Palette.talk,
)

@Composable
fun BabyPhoneTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors, content = content)
