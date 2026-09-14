package com.remotehost.remote.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remotehost.remote.RemoteApplication
import com.remotehost.remote.connection.ConnectionPhase
import com.remotehost.remote.connection.PairingInfo
import com.remotehost.remote.service.startConnectionService
import com.remotehost.remote.service.stopConnectionService
import com.remotehost.remote.ui.connection.ConnectingScreen
import com.remotehost.remote.ui.connection.DisconnectedScreen
import com.remotehost.remote.ui.connection.FirstRunScreen
import com.remotehost.remote.ui.connection.HostOfflineScreen
import com.remotehost.remote.ui.connection.ReconnectingScreen
import com.remotehost.remote.ui.connection.TokenRejectedScreen
import com.remotehost.remote.ui.pairing.ManualEntryScreen
import com.remotehost.remote.ui.pairing.QrScanScreen
import kotlinx.coroutines.launch

private enum class PairingFlowStep { None, QrScan, ManualEntry }

@Composable
fun RemoteApp(application: RemoteApplication) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pairing by application.pairingStore.pairingFlow.collectAsStateWithLifecycle(initialValue = null)
    val phase by application.remoteConnection.phase.collectAsStateWithLifecycle()
    val autoReconnect by application.pairingStore.autoReconnectFlow.collectAsStateWithLifecycle(initialValue = true)

    var pairingStep by remember { mutableStateOf(PairingFlowStep.None) }

    LaunchedEffect(pairing) {
        val current = pairing
        if (current != null) {
            startConnectionService(context, current)
        }
    }

    LaunchedEffect(autoReconnect) {
        application.remoteConnection.setAutoReconnect(autoReconnect)
    }

    fun persistAndConnect(info: PairingInfo) {
        scope.launch { application.pairingStore.savePairing(info) }
        pairingStep = PairingFlowStep.None
    }

    when (pairingStep) {
        PairingFlowStep.QrScan -> {
            QrScanScreen(
                onScanned = ::persistAndConnect,
                onManualEntry = { pairingStep = PairingFlowStep.ManualEntry },
                onBack = { pairingStep = PairingFlowStep.None },
            )
            return
        }
        PairingFlowStep.ManualEntry -> {
            ManualEntryScreen(
                onSubmit = ::persistAndConnect,
                onBack = { pairingStep = PairingFlowStep.None },
            )
            return
        }
        PairingFlowStep.None -> Unit
    }

    val currentPairing = pairing
    if (currentPairing == null) {
        FirstRunScreen(onScanClick = { pairingStep = PairingFlowStep.QrScan })
        return
    }

    fun changeDetails() {
        application.remoteConnection.stop()
        stopConnectionService(context)
        pairingStep = PairingFlowStep.QrScan
    }

    when (val currentPhase = phase) {
        is ConnectionPhase.AuthRejected -> TokenRejectedScreen(onRescan = { pairingStep = PairingFlowStep.QrScan })
        ConnectionPhase.HostOffline -> HostOfflineScreen(
            onRetry = { startConnectionService(context, currentPairing) },
            onChangeDetails = ::changeDetails,
        )
        ConnectionPhase.Reconnecting -> ReconnectingScreen(
            onCancel = {
                application.remoteConnection.stop()
                stopConnectionService(context)
            },
            onChangeDetails = ::changeDetails,
        )
        ConnectionPhase.Connecting, ConnectionPhase.NoPairing -> ConnectingScreen(
            onCancel = {
                application.remoteConnection.stop()
                stopConnectionService(context)
            },
            onChangeDetails = ::changeDetails,
        )
        ConnectionPhase.Disconnected -> DisconnectedScreen(
            onReconnect = { startConnectionService(context, currentPairing) },
            onChangeDetails = ::changeDetails,
        )
        is ConnectionPhase.Connected -> MainScaffold(
            application = application,
            hostName = currentPhase.hostName,
            onRescan = { pairingStep = PairingFlowStep.QrScan },
            onDisconnect = {
                application.remoteConnection.stop()
                stopConnectionService(context)
            },
        )
    }
}
