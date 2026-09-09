package com.remotehost.remote.connection

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pairing_prefs")

data class PairingInfo(
    val ip: String,
    val port: Int,
    val token: String,
    val name: String,
)

/**
 * Persists pairing details with DataStore Preferences. This is plain (unencrypted)
 * storage, same as SharedPreferences would be — acceptable for a v1 LAN-only tool, but
 * worth being honest about: the token is not protected by Keystore encryption here.
 */
class PairingStore(private val context: Context) {

    private object Keys {
        val IP = stringPreferencesKey("ip")
        val PORT = intPreferencesKey("port")
        val TOKEN = stringPreferencesKey("token")
        val NAME = stringPreferencesKey("name")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    }

    val pairingFlow: Flow<PairingInfo?> = context.dataStore.data.map { prefs ->
        val ip = prefs[Keys.IP]
        val port = prefs[Keys.PORT]
        val token = prefs[Keys.TOKEN]
        val name = prefs[Keys.NAME]
        if (ip != null && port != null && token != null && name != null) {
            PairingInfo(ip, port, token, name)
        } else {
            null
        }
    }

    val autoReconnectFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_RECONNECT] ?: true
    }

    suspend fun savePairing(info: PairingInfo) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IP] = info.ip
            prefs[Keys.PORT] = info.port
            prefs[Keys.TOKEN] = info.token
            prefs[Keys.NAME] = info.name
        }
    }

    suspend fun clearPairing() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.IP)
            prefs.remove(Keys.PORT)
            prefs.remove(Keys.TOKEN)
            prefs.remove(Keys.NAME)
        }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_RECONNECT] = enabled }
    }
}
