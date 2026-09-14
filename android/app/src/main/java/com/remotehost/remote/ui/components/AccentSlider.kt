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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.remotehost.remote.ui.theme.Accent
import kotlin.math.roundToInt

/**
 * Pill-track slider with a flat, solid-color fill — THEME.md v2 has no gradients
 * anywhere, so this replaces the old GradientSlider (which filled with a two-color
 * brush). Used for the dashboard's NIGHT/brightness row; the volume dial has its own
 * Canvas-based control and doesn't use this.
 */
@Composable
fun AccentSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fillColor: Color = Accent,
    enabled: Boolean = true,
) {
    val thumbSize = 14.dp
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(26.dp),
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
                .height(4.dp)
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
                    .background(if (enabled) fillColor else Color.White.copy(alpha = 0.16f))
                    .alpha(if (enabled) 1f else 0.5f),
            )
        }

        val thumbOffset = with(density) { (trackWidthPx * fraction).toDp() - thumbSize / 2 }
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(elevation = if (enabled) 6.dp else 0.dp, shape = CircleShape, spotColor = fillColor)
                .clip(CircleShape)
                .background(Color.White)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), CircleShape),
        )
    }
}
