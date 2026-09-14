package com.remotehost.remote.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remotehost.remote.connection.AppItem
import com.remotehost.remote.connection.RemoteConnection
import com.remotehost.remote.connection.RttState
import com.remotehost.remote.ui.components.AccentSlider
import com.remotehost.remote.ui.components.LatencyPill
import com.remotehost.remote.ui.components.latencyColor
import com.remotehost.remote.ui.components.latencyLabel
import com.remotehost.remote.ui.theme.Accent
import com.remotehost.remote.ui.theme.BgDeep
import com.remotehost.remote.ui.theme.BorderColor
import com.remotehost.remote.ui.theme.BorderStrong
import com.remotehost.remote.ui.theme.MonoFontFamily
import com.remotehost.remote.ui.theme.NeutralLost
import com.remotehost.remote.ui.theme.OnSurface
import com.remotehost.remote.ui.theme.OnSurfaceMuted28
import com.remotehost.remote.ui.theme.OnSurfaceMuted32
import com.remotehost.remote.ui.theme.OnSurfaceMuted35
import com.remotehost.remote.ui.theme.OnSurfaceMuted38
import com.remotehost.remote.ui.theme.OnSurfaceMuted45
import com.remotehost.remote.ui.theme.OnSurfaceMuted70
import com.remotehost.remote.ui.theme.OnSurfaceMuted80
import com.remotehost.remote.ui.theme.Surface
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * New "Now controlling" home screen (RemotePhonePro.dc.html / THEME.md). The radial
 * dial IS the volume control (drag anywhere on/near it), a NIGHT slider sends
 * brightness to the Host only — it must not affect this phone's own screen — and the
 * header carries a live-measured latency pill (see [RemoteConnection.rtt]) — not the
 * mock's tap-to-cycle demo. Owns its own full-bleed header (host name + "Now
 * controlling" + the latency pill) rather than the shared app top bar; see AppNav.kt.
 */
@Composable
fun DashboardScreen(connection: RemoteConnection, hostName: String, modifier: Modifier = Modifier) {
    val volumeState by connection.volume.collectAsStateWithLifecycle()
    val brightness by connection.brightness.collectAsStateWithLifecycle()
    val apps by connection.apps.collectAsStateWithLifecycle()
    val rtt by connection.rtt.collectAsStateWithLifecycle()

    var sheetVisible by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // The Host only pushes `apps` right after auth_ok / when the list changes, and
    // Dashboard (not Apps) is the start destination — ask explicitly so the
    // quick-launch row isn't empty on a fresh connect if this tab loads first.
    LaunchedEffect(Unit) { connection.sendAppsListRequest() }

    fun toast(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun handleMedia(action: String) {
        if (action == "play_pause") playing = !playing
        connection.sendMedia(action)
    }

    val liveColor = latencyColor(rtt)

    Box(modifier = modifier.fillMaxSize().background(BgDeep)) {
        Column(Modifier.fillMaxSize()) {
            DashboardHeader(hostName = hostName, rtt = rtt)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                VolumeDial(
                    value = volumeState.value,
                    muted = volumeState.muted,
                    progressColor = liveColor,
                    onValueChange = connection::sendVolumeSet,
                    modifier = Modifier.size(252.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "drag the ring · volume ${if (volumeState.muted) "muted" else volumeState.value} · ${latencyLabel(rtt)}",
                    fontFamily = MonoFontFamily,
                    fontSize = 10.5.sp,
                    color = OnSurfaceMuted28,
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DialActionButton(
                        label = if (volumeState.muted) "Unmute" else "Mute",
                        active = volumeState.muted,
                        activeColor = liveColor,
                        modifier = Modifier.weight(1f),
                        onClick = connection::sendVolumeMute,
                    )
                    DialActionButton(
                        label = "Keys",
                        active = false,
                        activeColor = liveColor,
                        modifier = Modifier.weight(1f),
                        onClick = { sheetVisible = true },
                    )
                }
                Spacer(Modifier.height(18.dp))
                MediaRow(playing = playing, liveColor = liveColor, onMedia = ::handleMedia)
            }

            NightRow(
                value = brightness,
                onSet = connection::sendBrightnessSet,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            )

            QuickLaunchRow(
                apps = apps.take(3),
                onLaunch = { app ->
                    connection.sendAppLaunch(app.id)
                    toast("Launching ${app.name} on host")
                },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            )
        }

        AnimatedVisibility(
            visible = sheetVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { sheetVisible = false },
                    ),
            )
        }

        AnimatedVisibility(
            visible = sheetVisible,
            enter = slideInVertically(tween(260)) { it },
            exit = slideOutVertically(tween(260)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            KeysSheet(onKeyTap = { toast("Keyboard input isn't available yet") })
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 78.dp, start = 20.dp, end = 20.dp),
        )
    }
}

@Composable
private fun DashboardHeader(hostName: String, rtt: RttState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = hostName.uppercase(),
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = OnSurfaceMuted35,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "Now controlling",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = (-0.2).sp,
                color = OnSurface,
            )
        }
        LatencyPill(state = rtt)
    }
}

/**
 * The radial volume dial. A 270° arc (with a 90° gap centered at the bottom) is the
 * whole control — dragging anywhere on it computes an angle from center exactly like
 * RemotePhonePro.dc.html's `angleVal()` (`atan2(dx, -dy)`, normalized to 0-100 with the
 * same clamp behavior at the gap edges) and calls [onValueChange] directly. The
 * progress arc and handle both track the *same* canonical angle so the handle always
 * sits at the tip of the filled arc, matching the mock's `dialDash`/`handleRotate`.
 */
@Composable
private fun VolumeDial(
    value: Int,
    muted: Boolean,
    progressColor: Color,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.dialGestures(onValueChange),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background track: the full 270° sweep, gap centered at the bottom.
            drawArc(
                color = Color.White.copy(alpha = 0.07f),
                startAngle = DIAL_START_ANGLE,
                sweepAngle = DIAL_SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Progress arc — hidden while muted, exactly like the mock's `dialDash`.
            if (!muted && value > 0) {
                drawArc(
                    color = progressColor,
                    startAngle = DIAL_START_ANGLE,
                    sweepAngle = DIAL_SWEEP_ANGLE * (value / 100f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            // Handle dot — always tracks the real volume, even while muted (the mock's
            // `handleRotate` isn't gated on mute, only the arc fill is).
            val handleAngleRad = Math.toRadians((DIAL_START_ANGLE + DIAL_SWEEP_ANGLE * (value / 100f)).toDouble())
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val handleCenter = Offset(
                x = center.x + radius * cos(handleAngleRad).toFloat(),
                y = center.y + radius * sin(handleAngleRad).toFloat(),
            )
            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 10.dp.toPx(), center = handleCenter)
            drawCircle(color = Color.White, radius = 8.dp.toPx(), center = handleCenter)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (muted) "—" else "$value",
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 54.sp,
                letterSpacing = (-2).sp,
                color = if (muted) OnSurfaceMuted38 else Color.White,
            )
            Text(
                text = "SYSTEM VOLUME",
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.5.sp,
                letterSpacing = 1.5.sp,
                color = OnSurfaceMuted32,
            )
        }
    }
}

// 270° sweep, gap centered at the bottom — see the module doc comment on the angle
// math (matches RemotePhonePro.dc.html's `angleVal`/`dialDash`/`handleRotate` exactly,
// just re-derived in Compose's drawArc angle convention: 0° = 3 o'clock, clockwise).
private const val DIAL_START_ANGLE = 135f
private const val DIAL_SWEEP_ANGLE = 270f

private fun Modifier.dialGestures(onValueChange: (Int) -> Unit): Modifier {
    // Keyed on Unit (not `onValueChange`) so an in-progress drag survives recomposition
    // — `onValueChange` here is always a bound reference to the same long-lived
    // RemoteConnection singleton, so the closure never goes stale.
    return this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            onValueChange(angleToPercent(down.position, size))
            drag(down.id) { change ->
                onValueChange(angleToPercent(change.position, size))
                change.consume()
            }
        }
    }
}

/** Ports RemotePhonePro.dc.html's `angleVal()` verbatim: `atan2(dx, -dy)` from center, 0 = top. */
private fun angleToPercent(offset: Offset, containerSize: IntSize): Int {
    val dx = offset.x - containerSize.width / 2f
    val dy = offset.y - containerSize.height / 2f
    val angleDeg = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble()))
    return (((angleDeg + 135.0) / 270.0) * 100.0).roundToInt().coerceIn(0, 100)
}

@Composable
private fun DialActionButton(
    label: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) activeColor else Color.Transparent)
            .border(1.dp, BorderStrong, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = if (active) Color.White else OnSurfaceMuted80,
        )
    }
}

@Composable
private fun NightRow(value: Int, onSet: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "NIGHT",
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.5.sp,
            letterSpacing = 2.sp,
            color = OnSurfaceMuted32,
        )
        AccentSlider(value = value, onValueChange = onSet, modifier = Modifier.weight(1f))
        Text(
            text = "$value%",
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            color = OnSurfaceMuted45,
        )
    }
}

@Composable
private fun QuickLaunchRow(apps: List<AppItem>, onLaunch: (AppItem) -> Unit, modifier: Modifier = Modifier) {
    // Real Host shortcuts only — never padded with placeholder tiles when there are
    // fewer than 3 configured (unlike the mock's hardcoded Browser/Macro 1/Sleep PC).
    if (apps.isEmpty()) return
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        apps.forEach { app ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .clickable { onLaunch(app) }
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(app.tag, fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = OnSurfaceMuted45)
                Text(app.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = OnSurfaceMuted80)
            }
        }
    }
}

/** Inline playback row living in the dashboard body (not the Keys sheet) — always visible. */
@Composable
private fun MediaRow(playing: Boolean, liveColor: Color, onMedia: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MediaTile("−10s", mono = true, modifier = Modifier.weight(1f)) { onMedia("seek_back") }
        MediaTile("⏮", modifier = Modifier.weight(1f)) { onMedia("previous") }
        MediaTile(
            label = if (playing) "❚❚" else "▶",
            primary = true,
            primaryColor = liveColor,
            modifier = Modifier.weight(1f),
        ) { onMedia("play_pause") }
        MediaTile("⏭", modifier = Modifier.weight(1f)) { onMedia("next") }
        MediaTile("+10s", mono = true, modifier = Modifier.weight(1f)) { onMedia("seek_forward") }
    }
}

@Composable
private fun KeysSheet(onKeyTap: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Color(0xFF121214))
            .border(1.dp, BorderStrong, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(44.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.2f)),
        )
        Spacer(Modifier.height(18.dp))
        Text("Keys", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = OnSurface)
        Spacer(Modifier.height(14.dp))
        val keys = listOf("esc", "tab", "↑", "space", "←", "↓", "→", "enter")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.chunked(4).forEach { rowKeys ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowKeys.forEach { key -> KeyTile(key, Modifier.weight(1f), onKeyTap) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // Honest about scope: PROTOCOL.md has no key-press message type (Phase 2), so
        // taps above only surface a toast — nothing is ever sent for them.
        Text(
            text = "keys aren't wired up yet — tapping shows a note, nothing is sent",
            fontFamily = MonoFontFamily,
            fontSize = 10.sp,
            color = NeutralLost,
        )
    }
}

@Composable
private fun MediaTile(
    label: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    primary: Boolean = false,
    primaryColor: Color = Accent,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) primaryColor else Color.Transparent)
            .border(1.dp, BorderStrong, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = if (mono) MonoFontFamily else FontFamily.Default,
            fontSize = if (mono) 11.sp else 16.sp,
            color = if (primary) Color.White else OnSurfaceMuted80,
        )
    }
}

@Composable
private fun KeyTile(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderStrong, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = OnSurfaceMuted70)
    }
}
