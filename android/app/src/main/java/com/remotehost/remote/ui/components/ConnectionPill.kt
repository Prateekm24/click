package com.remotehost.remote.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.remotehost.remote.connection.ConnectionPhase
import com.remotehost.remote.ui.theme.AccentStart
import com.remotehost.remote.ui.theme.DangerRed
import com.remotehost.remote.ui.theme.OnSurface
import com.remotehost.remote.ui.theme.SuccessGreen
import com.remotehost.remote.ui.theme.WarningYellow

@Composable
fun ConnectionPill(phase: ConnectionPhase, modifier: Modifier = Modifier) {
    val (dotColor, label, pulsing) = when (phase) {
        is ConnectionPhase.Connected -> Triple(SuccessGreen, "CONNECTED", false)
        ConnectionPhase.Reconnecting -> Triple(WarningYellow, "RECONNECTING", true)
        ConnectionPhase.Connecting -> Triple(WarningYellow, "CONNECTING", true)
        ConnectionPhase.HostOffline -> Triple(DangerRed, "HOST OFFLINE", false)
        is ConnectionPhase.AuthRejected -> Triple(DangerRed, "REJECTED", false)
        ConnectionPhase.Disconnected -> Triple(DangerRed, "DISCONNECTED", false)
        ConnectionPhase.NoPairing -> Triple(AccentStart, "NOT PAIRED", false)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "connDot")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "connDotAlpha",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
            .padding(start = 9.dp, end = 11.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
                .alpha(if (pulsing) pulseAlpha else 1f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurface.copy(alpha = 0.75f),
        )
    }
}
