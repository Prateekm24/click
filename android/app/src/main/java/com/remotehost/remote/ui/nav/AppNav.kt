package com.remotehost.remote.ui.nav

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.remotehost.remote.R
import com.remotehost.remote.RemoteApplication
import com.remotehost.remote.ui.apps.AppsScreen
import com.remotehost.remote.ui.components.ConnectionPill
import com.remotehost.remote.ui.dashboard.DashboardScreen
import com.remotehost.remote.ui.settings.SettingsScreen
import com.remotehost.remote.ui.theme.AccentSoft
import com.remotehost.remote.ui.theme.AccentText
import com.remotehost.remote.ui.theme.Bg
import com.remotehost.remote.ui.theme.BorderColor
import com.remotehost.remote.ui.theme.MonoFontFamily
import com.remotehost.remote.ui.theme.OnSurfaceMuted38
import com.remotehost.remote.ui.theme.SurfaceSunken
import com.remotehost.remote.ui.touchpad.TouchpadScreen
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Arrangement

enum class BottomTab(val route: String, val label: String, @DrawableRes val icon: Int) {
    Dashboard("dashboard", "Dashboard", R.drawable.ic_nav_dashboard),
    Apps("apps", "Apps", R.drawable.ic_nav_apps),
    Touchpad("touchpad", "Touchpad", R.drawable.ic_nav_touchpad),
    Settings("settings", "Settings", R.drawable.ic_nav_settings),
}

@Composable
fun MainScaffold(
    application: RemoteApplication,
    hostName: String,
    onRescan: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val connection = application.remoteConnection
    val phase by connection.phase.collectAsStateWithLifecycle()
    val pairing by application.pairingStore.pairingFlow.collectAsStateWithLifecycle(initialValue = null)
    val autoReconnect by application.pairingStore.autoReconnectFlow.collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        containerColor = Bg,
        topBar = {
            Column(Modifier.background(Bg)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Remote", style = MaterialTheme.typography.titleLarge)
                        Text(hostName, fontFamily = MonoFontFamily, fontSize = 11.sp, color = OnSurfaceMuted38)
                    }
                    ConnectionPill(phase = phase)
                }
                HorizontalDivider(color = BorderColor)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceSunken, tonalElevation = 0.dp) {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                BottomTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(painterResource(tab.icon), contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentText,
                            selectedTextColor = AccentText,
                            unselectedIconColor = OnSurfaceMuted38,
                            unselectedTextColor = OnSurfaceMuted38,
                            indicatorColor = AccentSoft,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Dashboard.route,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            composable(BottomTab.Dashboard.route) { DashboardScreen(connection) }
            composable(BottomTab.Apps.route) { AppsScreen(connection) }
            composable(BottomTab.Touchpad.route) { TouchpadScreen() }
            composable(BottomTab.Settings.route) {
                pairing?.let { p ->
                    SettingsScreen(
                        pairing = p,
                        autoReconnect = autoReconnect,
                        onAutoReconnectChange = { enabled ->
                            scope.launch { application.pairingStore.setAutoReconnect(enabled) }
                            connection.setAutoReconnect(enabled)
                        },
                        onRescan = onRescan,
                        onDisconnect = onDisconnect,
                    )
                }
            }
        }
    }
}
