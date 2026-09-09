package com.remotehost.remote.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotehost.remote.connection.AppItem
import com.remotehost.remote.connection.RemoteConnection
import com.remotehost.remote.ui.theme.BorderColor
import com.remotehost.remote.ui.theme.BorderStrong
import com.remotehost.remote.ui.theme.MonoFontFamily
import com.remotehost.remote.ui.theme.OnSurface
import com.remotehost.remote.ui.theme.OnSurfaceMuted45
import com.remotehost.remote.ui.theme.OnSurfaceMuted60
import com.remotehost.remote.ui.theme.Surface
import kotlinx.coroutines.launch

@Composable
fun AppsScreen(connection: RemoteConnection, modifier: Modifier = Modifier) {
    val apps by connection.apps.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { connection.sendAppsListRequest() }

    Box(modifier.fillMaxSize()) {
        if (apps.isEmpty()) {
            Text(
                text = "No shortcuts configured on Host yet",
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                color = OnSurfaceMuted45,
                textAlign = TextAlign.Center,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(apps, key = { it.id }) { app ->
                    AppTile(app) {
                        connection.sendAppLaunch(app.id)
                        scope.launch { snackbarHostState.showSnackbar("Launching ${app.name} on host") }
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun AppTile(app: AppItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, BorderStrong, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(app.tag, fontFamily = MonoFontFamily, fontSize = 12.sp, color = OnSurfaceMuted60)
        }
        Text(
            text = app.name,
            fontSize = 11.5.sp,
            textAlign = TextAlign.Center,
            color = OnSurface.copy(alpha = 0.85f),
        )
    }
}
