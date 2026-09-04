package me.jaival.telewalls.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassmorphism(
    backgroundColor: Color = Color(0xCC0F1117),
    borderColor: Color = Color(0x33FFFFFF),
    borderWidth: Dp = 1.dp,
    shape: Shape? = null
): Modifier = composed {
    var modifier = this
    if (shape != null) {
        modifier = modifier
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
    } else {
        modifier = modifier
            .background(backgroundColor)
            .border(borderWidth, borderColor)
    }
    modifier
}
