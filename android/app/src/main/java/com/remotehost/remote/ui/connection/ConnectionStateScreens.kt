package com.remotehost.remote.ui.connection

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import com.remotehost.remote.connection.PairingInfo
import com.remotehost.remote.ui.theme.Accent
import com.remotehost.remote.ui.theme.Bg
import com.remotehost.remote.ui.theme.BorderColor
import com.remotehost.remote.ui.theme.BorderStrong
import com.remotehost.remote.ui.theme.MonoFontFamily
import com.remotehost.remote.ui.theme.NeutralLost
import com.remotehost.remote.ui.theme.OnSurface
import com.remotehost.remote.ui.theme.OnSurfaceMuted38
import com.remotehost.remote.ui.theme.OnSurfaceMuted45
import com.remotehost.remote.ui.theme.Surface
import com.remotehost.remote.ui.theme.Warn

@Composable
private fun ConnectionStateScaffold(
    code: String,
    title: String,
    body: String,
    ctaLabel: String,
    onCta: () -> Unit,
    dotColor: Color,
    pulsing: Boolean = false,
    accentCta: Boolean = false,
    onChangeDetails: (() -> Unit)? = null,
    sessionDetails: PairingInfo? = null,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(24.dp),
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.5.sp,
            color = OnSurfaceMuted38,
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "statePulse")
            val animatedAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
                label = "stateDotAlpha",
            )
            val dotAlpha = if (pulsing) animatedAlpha else 1f

            Box(
                modifier = Modifier
                    .size(66.dp)
                    .border(2.dp, BorderStrong, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .alpha(dotAlpha),
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMuted45,
                textAlign = TextAlign.Center,
            )

            if (sessionDetails != null) {
                SessionDetails(sessionDetails)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (accentCta) SolidColor(Accent) else SolidColor(Color.White.copy(alpha = 0.04f)))
                .border(1.dp, BorderStrong, RoundedCornerShape(14.dp))
                .clickable(onClick = onCta)
                .padding(13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ctaLabel,
                fontWeight = FontWeight.SemiBold,
                color = if (accentCta) Color.White else OnSurface.copy(alpha = 0.7f),
            )
        }

        if (onChangeDetails != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable(onClick = onChangeDetails)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Wrong laptop, or details changed? Enter new pairing details",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceMuted45,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun ConnectingScreen(
    onCancel: () -> Unit,
    onChangeDetails: () -> Unit,
    pairing: PairingInfo? = null,
    modifier: Modifier = Modifier,
) {
    ConnectionStateScaffold(
        code = "STATE / CONNECTING",
        title = "Connecting…",
        body = "Opening a connection to your paired laptop.",
        ctaLabel = "Cancel",
        onCta = onCancel,
        dotColor = Warn,
        pulsing = true,
        onChangeDetails = onChangeDetails,
        sessionDetails = pairing,
    )
}

@Composable
fun ReconnectingScreen(
    onCancel: () -> Unit,
    onChangeDetails: () -> Unit,
    pairing: PairingInfo? = null,
    modifier: Modifier = Modifier,
) {
    ConnectionStateScaffold(
        code = "STATE / SEARCHING",
        title = "Reconnecting…",
        body = "Connection dropped. Retrying automatically — controls are unavailable until reconnected.",
        ctaLabel = "Cancel",
        onCta = onCancel,
        dotColor = Warn,
        pulsing = true,
        onChangeDetails = onChangeDetails,
        sessionDetails = pairing,
    )
}

@Composable
fun HostOfflineScreen(onRetry: () -> Unit, onChangeDetails: () -> Unit, modifier: Modifier = Modifier) {
    ConnectionStateScaffold(
        code = "STATE / NO HOST",
        title = "Host isn't running",
        body = "The laptop answered, but nothing is listening on that port. Start Remote Host there, then retry.",
        ctaLabel = "Retry",
        onCta = onRetry,
        dotColor = NeutralLost,
        onChangeDetails = onChangeDetails,
    )
}

@Composable
fun TokenRejectedScreen(onRescan: () -> Unit, modifier: Modifier = Modifier) {
    ConnectionStateScaffold(
        code = "STATE / REJECTED",
        title = "Pairing token rejected",
        body = "The saved token no longer matches. Show the QR code on the host and scan it again.",
        ctaLabel = "Scan QR",
        onCta = onRescan,
        dotColor = Accent,
        accentCta = true,
    )
}

@Composable
fun FirstRunScreen(onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    ConnectionStateScaffold(
        code = "STATE / FIRST RUN",
        title = "Pair with your laptop",
        body = "Open Remote Host on Windows, then scan the code it shows. One-time setup.",
        ctaLabel = "Scan QR",
        onCta = onScanClick,
        dotColor = Accent,
        accentCta = true,
    )
}

@Composable
fun DisconnectedScreen(onReconnect: () -> Unit, onChangeDetails: () -> Unit, modifier: Modifier = Modifier) {
    ConnectionStateScaffold(
        code = "STATE / DISCONNECTED",
        title = "Disconnected",
        body = "You disconnected from the host. Reconnect when you're ready.",
        ctaLabel = "Reconnect",
        onCta = onReconnect,
        dotColor = NeutralLost,
        onChangeDetails = onChangeDetails,
    )
}

/** Lets you confirm the target while stuck connecting, instead of guessing blind. */
@Composable
private fun SessionDetails(pairing: PairingInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "SESSION DETAILS",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            color = OnSurfaceMuted38,
        )
        SessionDetailRow("host name", pairing.name)
        SessionDetailRow("address", "${pairing.ip}:${pairing.port}")
        SessionDetailRow("token", maskToken(pairing.token))
    }
}

@Composable
private fun SessionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontFamily = MonoFontFamily, fontSize = 12.sp, color = OnSurfaceMuted45)
        Text(value, fontFamily = MonoFontFamily, fontSize = 12.sp, color = OnSurface)
    }
}

private fun maskToken(token: String): String {
    if (token.length <= 4) return "•".repeat(token.length)
    return "•".repeat(token.length - 4) + token.takeLast(4)
}
