package com.remotehost.remote.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.remotehost.remote.MainActivity
import com.remotehost.remote.R
import com.remotehost.remote.RemoteApplication
import com.remotehost.remote.connection.ConnectionPhase
import com.remotehost.remote.connection.PairingInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val CHANNEL_ID = "remote_connection_channel"
private const val NOTIFICATION_ID = 1001

const val EXTRA_IP = "extra_ip"
const val EXTRA_PORT = "extra_port"
const val EXTRA_TOKEN = "extra_token"
const val EXTRA_NAME = "extra_name"

/**
 * Holds the process alive with a low-priority notification while RemoteConnection
 * (an app-scoped singleton) keeps the socket open. Started once pairing exists, from
 * MainActivity; stopped only by an explicit Disconnect.
 */
class RemoteConnectionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var app: RemoteApplication

    override fun onCreate() {
        super.onCreate()
        app = application as RemoteApplication
        startForeground(NOTIFICATION_ID, buildNotification("Connecting…"))
        app.remoteConnection.phase
            .onEach { phase -> updateNotification(phase) }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ip = intent?.getStringExtra(EXTRA_IP)
        val port = intent?.getIntExtra(EXTRA_PORT, 8765) ?: 8765
        val token = intent?.getStringExtra(EXTRA_TOKEN)
        val name = intent?.getStringExtra(EXTRA_NAME)
        if (ip != null && token != null && name != null) {
            app.remoteConnection.start(PairingInfo(ip, port, token, name))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String): Notification {
        ensureChannel()
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_nav_settings)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun updateNotification(phase: ConnectionPhase) {
        val text = when (phase) {
            is ConnectionPhase.Connected -> "Connected to ${phase.hostName}"
            ConnectionPhase.Reconnecting -> "Reconnecting…"
            ConnectionPhase.HostOffline -> "Host offline — retrying…"
            is ConnectionPhase.AuthRejected -> "Pairing rejected — re-pair in app"
            ConnectionPhase.Connecting -> "Connecting…"
            ConnectionPhase.Disconnected -> "Disconnected"
            ConnectionPhase.NoPairing -> "Not paired"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
                manager?.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Remote connection", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }
}

fun startConnectionService(context: Context, pairing: PairingInfo) {
    val intent = Intent(context, RemoteConnectionService::class.java).apply {
        putExtra(EXTRA_IP, pairing.ip)
        putExtra(EXTRA_PORT, pairing.port)
        putExtra(EXTRA_TOKEN, pairing.token)
        putExtra(EXTRA_NAME, pairing.name)
    }
    ContextCompat.startForegroundService(context, intent)
}

fun stopConnectionService(context: Context) {
    context.stopService(Intent(context, RemoteConnectionService::class.java))
}
