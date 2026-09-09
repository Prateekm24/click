package com.remotehost.remote

import android.app.Application
import com.remotehost.remote.connection.PairingStore
import com.remotehost.remote.connection.RemoteConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RemoteApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var pairingStore: PairingStore
        private set

    lateinit var remoteConnection: RemoteConnection
        private set

    override fun onCreate() {
        super.onCreate()
        pairingStore = PairingStore(this)
        remoteConnection = RemoteConnection(appScope)
    }
}
