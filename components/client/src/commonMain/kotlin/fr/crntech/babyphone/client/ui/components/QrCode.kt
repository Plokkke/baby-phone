package fr.crntech.babyphone.client.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

@Composable
fun QrCode(content: String, modifier: Modifier = Modifier) {
    Image(
        painter = rememberQrCodePainter(content),
        contentDescription = null,
        modifier = modifier
            .widthIn(max = 320.dp)
            .aspectRatio(1f)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp),
    )
}
