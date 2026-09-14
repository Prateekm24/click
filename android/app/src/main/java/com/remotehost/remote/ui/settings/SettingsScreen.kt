package com.remotehost.remote.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotehost.remote.connection.PairingInfo
import com.remotehost.remote.ui.components.ToggleRow
import com.remotehost.remote.ui.theme.Accent
import com.remotehost.remote.ui.theme.AccentLight
import com.remotehost.remote.ui.theme.AccentSoft
import com.remotehost.remote.ui.theme.BorderColor
import com.remotehost.remote.ui.theme.MonoFontFamily
import com.remotehost.remote.ui.theme.OnSurface
import com.remotehost.remote.ui.theme.OnSurfaceMuted45
import com.remotehost.remote.ui.theme.Surface
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsScreen(
    pairing: PairingInfo,
    autoReconnect: Boolean,
    onAutoReconnectChange: (Boolean) -> Unit,
    onRescan: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = "PAIRED HOST",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMuted45,
                letterSpacing = 1.sp,
            )
            InfoRow("name", pairing.name)
            InfoRow("address", "${pairing.ip}:${pairing.port}")
            InfoRow("token", maskToken(pairing.token))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AccentSoft)
                .border(1.dp, Accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clickable(onClick = onRescan)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            )
            Column {
                Text("Scan QR to re-pair", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                Text(
                    text = "Opens the camera scanner",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMuted45,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp),
        ) {
            ToggleRow(
                label = "Auto-reconnect",
                checked = autoReconnect,
                onCheckedChange = onAutoReconnectChange,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .clickable(onClick = onDisconnect)
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Disconnect", color = AccentLight, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
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
