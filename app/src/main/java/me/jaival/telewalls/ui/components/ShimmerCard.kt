package me.jaival.telewalls.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import me.jaival.telewalls.ui.theme.LocalReduceAnimations

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    aspectRatio: Float = 0.65f
) {
    val reduceAnimations = LocalReduceAnimations.current
    val baseColor = MaterialTheme.colorScheme.surfaceContainer
    val highlightColor = MaterialTheme.colorScheme.surfaceContainerHigh

    if (reduceAnimations) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(24.dp))
                .background(baseColor)
        )
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnim = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )

        val shimmerColors = listOf(
            baseColor,
            highlightColor,
            baseColor
        )

        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim.value - 300f, translateAnim.value - 300f),
            end = Offset(translateAnim.value, translateAnim.value)
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(24.dp))
                .background(brush)
        )
    }
}


