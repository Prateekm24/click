package com.remotehost.remote.ui.touchpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.remotehost.remote.ui.theme.OnSurfaceMuted45

/** Phase-2 stub per the build spec — no drag/click implementation in v1. */
@Composable
fun TouchpadScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Text("Touchpad", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Touchpad — coming in a future update",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceMuted45,
            textAlign = TextAlign.Center,
        )
    }
}
