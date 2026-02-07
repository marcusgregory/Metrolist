package com.metrolist.music.wear.presentation.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Animated equalizer icon with 3 bars that oscillate to indicate music is playing.
 * Standard visual indicator used by Spotify, YouTube Music, Deezer, etc.
 */
@Composable
fun EqualizerIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    animating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")

    // 3 bars with different speeds and heights for natural look
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            tween(300, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(450, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            tween(350, easing = FastOutLinearInEasing),
            RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Canvas(modifier = modifier.size(16.dp)) {
        val barWidth = size.width / 4 // 3 bars with spacing
        val spacing = size.width / 8
        val maxHeight = size.height

        // Use animated values when playing, static values when paused
        val bars = if (animating) listOf(bar1, bar2, bar3) else listOf(0.3f, 0.5f, 0.3f)

        bars.forEachIndexed { index, value ->
            val barHeight = maxHeight * value
            drawRoundRect(
                color = color,
                topLeft = Offset(
                    x = index * (barWidth + spacing),
                    y = maxHeight - barHeight // Draw from bottom up
                ),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}
