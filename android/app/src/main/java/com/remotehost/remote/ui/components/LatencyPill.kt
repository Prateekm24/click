package com.remotehost.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotehost.remote.connection.RttState
import com.remotehost.remote.ui.theme.Accent
import com.remotehost.remote.ui.theme.MonoFontFamily
import com.remotehost.remote.ui.theme.NeutralLost
import com.remotehost.remote.ui.theme.Warn

/**
 * Color band for a given [RttState], per THEME.md: fast is the accent red (meaning
 * "live/good" in this palette, not error), medium is warn amber, and no response
 * within the timeout is neutral gray — never a second "error" red.
 */
fun latencyColor(state: RttState): Color = when (state) {
    is RttState.Measured -> if (state.ms < FAST_THRESHOLD_MS) Accent else Warn
    RttState.Measuring -> NeutralLost
    RttState.Lost -> NeutralLost
}

fun latencyLabel(state: RttState): String = when (state) {
    is RttState.Measured -> "${state.ms} ms"
    RttState.Measuring -> "…"
    RttState.Lost -> "LOST"
}

private const val FAST_THRESHOLD_MS = 50

/**
 * Live ping readout, top-right of the dashboard header — a real measurement (see
 * [com.remotehost.remote.connection.RemoteConnection.rtt]), not the mock's tap-to-cycle
 * demo, so this is intentionally not clickable.
 */
@Composable
fun LatencyPill(state: RttState, modifier: Modifier = Modifier) {
    val color = latencyColor(state)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = latencyLabel(state),
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp,
            color = color,
        )
    }
}
