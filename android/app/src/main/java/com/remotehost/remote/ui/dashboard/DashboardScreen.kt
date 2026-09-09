package com.remotehost.remote.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotehost.remote.connection.RemoteConnection
import com.remotehost.remote.connection.VolumeState
import com.remotehost.remote.ui.components.GradientSlider
import com.remotehost.remote.ui.theme.AccentSoft
import com.remotehost.remote.ui.theme.BorderColor
import com.remotehost.remote.ui.theme.BorderStrong
import com.remotehost.remote.ui.theme.MonoFontFamily
import com.remotehost.remote.ui.theme.OnSurfaceMuted38
import com.remotehost.remote.ui.theme.OnSurfaceMuted45
import com.remotehost.remote.ui.theme.OnSurfaceMuted60
import com.remotehost.remote.ui.theme.Surface
import com.remotehost.remote.ui.theme.accentGradientBrush
import com.remotehost.remote.ui.theme.neutralGradientBrush

@Composable
fun DashboardScreen(connection: RemoteConnection, modifier: Modifier = Modifier) {
    val volumeState by connection.volume.collectAsStateWithLifecycle()
    val brightness by connection.brightness.collectAsStateWithLifecycle()
    var playing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VolumeCard(
            state = volumeState,
            onSet = connection::sendVolumeSet,
            onMute = connection::sendVolumeMute,
        )
        BrightnessCard(
            value = brightness,
            onSet = connection::sendBrightnessSet,
        )
        MediaCard(
            playing = playing,
            onPlayPause = {
                playing = !playing
                connection.sendMedia("play_pause")
            },
            onNext = { connection.sendMedia("next") },
            onPrev = { connection.sendMedia("previous") },
            onSeekForward = { connection.sendMedia("seek_forward") },
            onSeekBack = { connection.sendMedia("seek_back") },
        )
    }
}

@Composable
private fun CardLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        letterSpacing = 1.sp,
        color = OnSurfaceMuted45,
    )
}

@Composable
private fun VolumeCard(state: VolumeState, onSet: (Int) -> Unit, onMute: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(AccentSoft, Color.Transparent)))
            .border(1.dp, BorderStrong, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardLabel("VOLUME")
            Text(
                text = if (state.muted) "MUTED" else "${state.value}",
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = if (state.muted) OnSurfaceMuted38 else Color.White,
            )
        }
        Spacer(Modifier.height(16.dp))
        GradientSlider(
            value = state.value,
            onValueChange = onSet,
            fillBrush = accentGradientBrush(),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state.muted) Color(0x38D946EF) else Color.White.copy(alpha = 0.04f))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .clickable(onClick = onMute)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (state.muted) "Unmute" else "Mute", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
            }
            NudgeButton("−") { onSet((state.value - 5).coerceAtLeast(0)) }
            NudgeButton("+") { onSet((state.value + 5).coerceAtMost(100)) }
        }
    }
}

@Composable
private fun NudgeButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun BrightnessCard(value: Int, onSet: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardLabel("BRIGHTNESS")
            Text(text = "$value", fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        }
        Spacer(Modifier.height(16.dp))
        GradientSlider(
            value = value,
            onValueChange = onSet,
            fillBrush = neutralGradientBrush(),
        )
    }
}

@Composable
private fun MediaCard(
    playing: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        CardLabel("MEDIA")
        Text(
            text = "Commands sent to Host — v1 protocol has no track info to display",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceMuted45,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MediaTile(label = "−10s", modifier = Modifier.weight(1f), onClick = onSeekBack)
            MediaTile(label = "⏮", modifier = Modifier.weight(1f), onClick = onPrev)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentGradientBrush())
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (playing) "❙❙" else "▶", color = Color.White, fontSize = 17.sp)
            }
            MediaTile(label = "⏭", modifier = Modifier.weight(1f), onClick = onNext)
            MediaTile(label = "+10s", modifier = Modifier.weight(1f), onClick = onSeekForward)
        }
    }
}

@Composable
private fun MediaTile(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = MonoFontFamily, fontSize = 12.sp, color = OnSurfaceMuted60)
    }
}
