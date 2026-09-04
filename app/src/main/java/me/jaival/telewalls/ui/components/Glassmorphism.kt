package me.jaival.telewalls.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassmorphism(
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp,
    shape: Shape? = null
): Modifier = composed {
    val defaultBg = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
    val defaultBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    val finalBg = if (backgroundColor != Color.Unspecified) backgroundColor else defaultBg
    val finalBorder = if (borderColor != Color.Unspecified) borderColor else defaultBorder

    var modifier = this
    if (shape != null) {
        modifier = modifier
            .background(finalBg, shape)
            .border(borderWidth, finalBorder, shape)
    } else {
        modifier = modifier
            .background(finalBg)
            .border(borderWidth, finalBorder)
    }
    modifier
}

