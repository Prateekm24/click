package com.remotehost.remote.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.ConnectException
import java.util.concurrent.TimeUnit

sealed interface ConnectionPhase {
    data object NoPairing : ConnectionPhase
    data object Connecting : ConnectionPhase
    data object Reconnecting : ConnectionPhase
    data object HostOffline : ConnectionPhase
    data class AuthRejected(val message: String) : ConnectionPhase
    data class Connected(val hostName: String) : ConnectionPhase
    data object Disconnected : ConnectionPhase
}

data class VolumeState(val value: Int = 0, val muted: Boolean = false)

/**
 * Live round-trip-latency readout, driven by periodic `ping`/`pong` exchange per
 * PROTOCOL.md. [Measuring] is the initial/transient state before the first `pong` of a
 * connection has arrived; [Lost] means a `ping` went unanswered within the timeout.
 */
sealed interface RttState {
    data object Measuring : RttState
    data class Measured(val ms: Int) : RttState
    data object Lost : RttState
}

/**
 * Owns the single WebSocket link to the Host: connects, re-sends `auth` on every new
 * socket per PROTOCOL.md, retries with backoff, and exposes live state as StateFlows.
 * A process-wide singleton (held by RemoteApplication) rather than something scoped to
 * the foreground service, so the UI can observe it whether or not the service happens
 * to be running.
 */
class RemoteConnection(private val scope: CoroutineScope) {

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var pairing: PairingInfo? = null
    private var reconnectJob: Job? = null
    private var backoffMs = INITIAL_BACKOFF_MS
    private var stoppedByUser = true
    private var autoReconnectEnabled = true

    private val _phase = MutableStateFlow<ConnectionPhase>(ConnectionPhase.NoPairing)
    val phase: StateFlow<ConnectionPhase> = _phase.asStateFlow()

    private val _volume = MutableStateFlow(VolumeState())
    val volume: StateFlow<VolumeState> = _volume.asStateFlow()

    private val _brightness = MutableStateFlow(0)
    val brightness: StateFlow<Int> = _brightness.asStateFlow()

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val _rtt = MutableStateFlow<RttState>(RttState.Measuring)
    val rtt: StateFlow<RttState> = _rtt.asStateFlow()

    private var pingLoopJob: Job? = null
    private var pingTimeoutJob: Job? = null
    private var lastPingSentAtMs: Long = 0L

    fun setAutoReconnect(enabled: Boolean) {
        autoReconnectEnabled = enabled
    }

    /** (Re)starts the connection, resetting backoff. Safe to call repeatedly. */
    fun start(pairingInfo: PairingInfo) {
        pairing = pairingInfo
        stoppedByUser = false
        backoffMs = INITIAL_BACKOFF_MS
        reconnectJob?.cancel()
        webSocket?.cancel()
        connect()
    }

    fun stop() {
        stoppedByUser = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "client disconnect")
        webSocket = null
        stopPingLoop()
        _phase.value = ConnectionPhase.Disconnected
    }

    private fun connect() {
        val p = pairing ?: return
        _phase.value = ConnectionPhase.Connecting
        val request = Request.Builder().url("ws://${p.ip}:${p.port}/").build()
        webSocket = client.newWebSocket(request, Listener(p))
    }

    private inner class Listener(private val pairingInfo: PairingInfo) : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            backoffMs = INITIAL_BACKOFF_MS
            val authMessage = WireMessage(type = "auth", token = pairingInfo.token)
            webSocket.send(protocolJson.encodeToString(WireMessage.serializer(), authMessage))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (stoppedByUser) return
            if (code == AUTH_REJECTED_CLOSE_CODE) {
                _phase.value = ConnectionPhase.AuthRejected(reason.ifBlank { "token rejected" })
                return
            }
            scheduleReconnect(ConnectionPhase.Reconnecting)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (stoppedByUser) return
            // A refused TCP connection means something answered the address but nothing
            // is listening on the port — i.e. the Host app isn't running. Anything else
            // (timeout, unreachable host, DNS failure) is treated as a generic drop and
            // retried the same way, just labeled differently for the user.
            val nextPhase = if (t is ConnectException) ConnectionPhase.HostOffline else ConnectionPhase.Reconnecting
            scheduleReconnect(nextPhase)
        }
    }

    private fun handleMessage(text: String) {
        val msg = try {
            protocolJson.decodeFromString(WireMessage.serializer(), text)
        } catch (e: Exception) {
            return
        }
        when (msg.type) {
            "auth_ok" -> {
                _phase.value = ConnectionPhase.Connected(msg.name ?: pairing?.name ?: "Host")
                // PROTOCOL.md only guarantees a `state` push when volume/brightness
                // *changes*, not an initial snapshot on connect (unlike `apps`, which
                // is explicitly sent right after auth_ok) — so ask for both explicitly
                // to avoid the Dashboard sitting on stale defaults after a fresh pair.
                sendVolumeGet()
                sendBrightnessGet()
                startPingLoop()
            }
            "auth_error" -> _phase.value = ConnectionPhase.AuthRejected(msg.message ?: "invalid token")
            "state" -> when (msg.key) {
                "volume" -> _volume.value = VolumeState(
                    value = msg.value ?: _volume.value.value,
                    muted = msg.muted ?: _volume.value.muted,
                )
                "brightness" -> msg.value?.let { _brightness.value = it }
            }
            "apps" -> _apps.value = msg.items ?: emptyList()
            "pong" -> {
                pingTimeoutJob?.cancel()
                val elapsed = (System.currentTimeMillis() - lastPingSentAtMs).toInt().coerceAtLeast(0)
                _rtt.value = RttState.Measured(elapsed)
            }
            else -> Unit // ack / error: nothing to reflect in UI state today
        }
    }

    /**
     * Sends `ping` on a fixed interval and starts a short timeout on each one; a `pong`
     * (handled above) cancels that timeout and reports the round-trip time. If no
     * `pong` arrives before the next ping would fire, the previous one is considered
     * [RttState.Lost] rather than left showing a stale number.
     */
    private fun startPingLoop() {
        pingLoopJob?.cancel()
        _rtt.value = RttState.Measuring
        pingLoopJob = scope.launch {
            while (true) {
                lastPingSentAtMs = System.currentTimeMillis()
                sendPing()
                pingTimeoutJob?.cancel()
                pingTimeoutJob = launch {
                    delay(PING_TIMEOUT_MS)
                    _rtt.value = RttState.Lost
                }
                delay(PING_INTERVAL_MS)
            }
        }
    }

    private fun stopPingLoop() {
        pingLoopJob?.cancel()
        pingLoopJob = null
        pingTimeoutJob?.cancel()
        pingTimeoutJob = null
        _rtt.value = RttState.Measuring
    }

    private fun scheduleReconnect(interimPhase: ConnectionPhase) {
        stopPingLoop()
        _phase.value = interimPhase
        if (!autoReconnectEnabled) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            if (!stoppedByUser) connect()
        }
    }

    fun send(message: WireMessage) {
        webSocket?.send(protocolJson.encodeToString(WireMessage.serializer(), message))
    }

    fun sendVolumeSet(value: Int) = send(WireMessage(type = "volume", action = "set", value = value.coerceIn(0, 100)))
    fun sendVolumeMute() = send(WireMessage(type = "volume", action = "mute"))
    fun sendVolumeGet() = send(WireMessage(type = "volume", action = "get"))
    fun sendBrightnessSet(value: Int) = send(WireMessage(type = "brightness", action = "set", value = value.coerceIn(0, 100)))
    fun sendBrightnessGet() = send(WireMessage(type = "brightness", action = "get"))
    fun sendMedia(action: String) = send(WireMessage(type = "media", action = action))
    fun sendAppLaunch(id: String) = send(WireMessage(type = "app", action = "launch", id = id))
    fun sendAppsListRequest() = send(WireMessage(type = "apps", action = "list"))
    fun sendPing() = send(WireMessage(type = "ping"))

    companion object {
        private const val INITIAL_BACKOFF_MS = 2000L
        private const val MAX_BACKOFF_MS = 10000L
        private const val AUTH_REJECTED_CLOSE_CODE = 4001
        private const val PING_INTERVAL_MS = 5000L
        private const val PING_TIMEOUT_MS = 3000L
    }
}
