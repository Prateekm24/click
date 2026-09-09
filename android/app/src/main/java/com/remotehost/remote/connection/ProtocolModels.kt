package com.remotehost.remote.connection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Payload encoded in the Host's pairing QR code (PROTOCOL.md "Pairing / discovery payload").
 */
@Serializable
data class PairingPayload(
    val v: Int,
    val ip: String,
    val port: Int,
    val token: String,
    val name: String,
)

/**
 * One shortcut tile as advertised by the Host's `apps` message.
 */
@Serializable
data class AppItem(
    val id: String,
    val name: String,
    val tag: String,
)

/**
 * Single flexible envelope covering every message type in both directions of
 * PROTOCOL.md. The wire protocol's messages differ enough in shape (and are simple
 * enough) that one class with nullable fields plus a `type` discriminator is easier to
 * keep correct than a sealed polymorphic hierarchy on both send and receive paths.
 */
@Serializable
data class WireMessage(
    val type: String,
    val action: String? = null,
    val value: Int? = null,
    val id: String? = null,
    val token: String? = null,
    val name: String? = null,
    val message: String? = null,
    @SerialName("for") val forType: String? = null,
    val key: String? = null,
    val muted: Boolean? = null,
    val items: List<AppItem>? = null,
)

/**
 * explicitNulls = false keeps unused fields out of the encoded JSON entirely (rather
 * than serializing them as literal `null`), matching the compact message shapes shown
 * in PROTOCOL.md.
 */
val protocolJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
