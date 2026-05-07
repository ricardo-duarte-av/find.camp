package camp.find.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wave-dot loading indicator matching the Material 3 Expressive style:
 * a row of dots that animate vertically in a staggered sine-wave pattern.
 */
@Composable
fun WavyLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotCount: Int = 5,
    dotSize: Dp = 9.dp,
    amplitude: Dp = 9.dp,
    periodMillis: Int = 1000,
) {
    val transition = rememberInfiniteTransition(label = "wavy")
    // Full cycle = periodMillis up + periodMillis down = 2 × periodMillis
    val stepMs = (2 * periodMillis) / dotCount

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(dotCount) { index ->
            val offsetY by transition.animateFloat(
                initialValue = -amplitude.value,
                targetValue = amplitude.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = periodMillis,
                        easing = FastOutSlowInEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(
                        offsetMillis = index * stepMs,
                        offsetType = StartOffsetType.FastForward,
                    ),
                ),
                label = "dot_$index",
            )

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .offset(y = offsetY.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

@Composable
fun WavyLoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        WavyLoadingIndicator()
    }
}
