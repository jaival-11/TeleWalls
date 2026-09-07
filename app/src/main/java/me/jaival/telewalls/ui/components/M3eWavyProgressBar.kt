package me.jaival.telewalls.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Material 3 Expressive (M3E) Wavy Progress Bar.
 * Renders an animated sine wave track with a smooth filled active progress wave.
 */
@Composable
fun M3eWavyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    strokeWidth: Dp = 6.dp,
    waveAmplitude: Dp = 4.dp,
    waveLength: Dp = 28.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "wavyProgressBarProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wavyWaveTransition")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavyPhaseShift"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val strokeWidthPx = strokeWidth.toPx()
        val amplitudePx = waveAmplitude.toPx()
        val waveLengthPx = waveLength.toPx()

        if (width <= 0f) return@Canvas

        // Generate Wavy Path across total width
        val fullWavePath = Path()
        val stepPx = 4f
        var x = 0f
        var isFirst = true

        while (x <= width) {
            val radians = ((x / waveLengthPx) * 2 * PI).toFloat() + phaseShift
            val y = centerY + (sin(radians) * amplitudePx)

            if (isFirst) {
                fullWavePath.moveTo(x, y)
                isFirst = false
            } else {
                fullWavePath.lineTo(x, y)
            }
            x += stepPx
        }
        // Ensure path reaches the end edge
        val endRadians = ((width / waveLengthPx) * 2 * PI).toFloat() + phaseShift
        fullWavePath.lineTo(width, centerY + (sin(endRadians) * amplitudePx))

        // 1. Draw track wave (Background)
        drawPath(
            path = fullWavePath,
            color = trackColor,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 2. Draw active progress wave (Clipped to animatedProgress * width)
        val activeWidth = width * animatedProgress
        if (activeWidth > 0f) {
            clipRect(
                left = 0f,
                top = 0f,
                right = activeWidth,
                bottom = height
            ) {
                drawPath(
                    path = fullWavePath,
                    color = color,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
