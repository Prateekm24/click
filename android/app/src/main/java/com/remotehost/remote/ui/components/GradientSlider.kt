package com.remotehost.remote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.remotehost.remote.ui.theme.AccentStart
import com.remotehost.remote.ui.theme.accentGradientBrush
import kotlin.math.roundToInt

/**
 * Pill-track slider with a gradient (or any brush) fill, matching THEME.md's slider
 * spec (8dp track, 22dp thumb, soft glow). Material3's default Slider doesn't support
 * a gradient fill, hence this small custom composable — reused for both volume
 * (accent gradient) and brightness (neutral gradient).
 */
@Composable
fun GradientSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fillBrush: Brush = accentGradientBrush(),
    enabled: Boolean = true,
) {
    val thumbSize = 22.dp
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(36.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidthPx = constraints.maxWidth.toFloat()
        val fraction = (value.coerceIn(0, 100)) / 100f

        fun updateFromX(x: Float) {
            if (trackWidthPx <= 0f) return
            val pct = ((x / trackWidthPx) * 100f).roundToInt().coerceIn(0, 100)
            onValueChange(pct)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.09f))
                .then(
                    if (enabled) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { offset -> updateFromX(offset.x) }
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    updateFromX(change.position.x)
                                }
                            }
                    } else {
                        Modifier
                    }
                ),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(if (enabled) fillBrush else SolidColor(Color.White.copy(alpha = 0.16f)))
                    .alpha(if (enabled) 1f else 0.5f),
            )
        }

        val thumbOffset = with(density) { (trackWidthPx * fraction).toDp() - thumbSize / 2 }
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(elevation = if (enabled) 8.dp else 0.dp, shape = CircleShape, spotColor = AccentStart)
                .clip(CircleShape)
                .background(Color.White)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), CircleShape),
        )
    }
}
