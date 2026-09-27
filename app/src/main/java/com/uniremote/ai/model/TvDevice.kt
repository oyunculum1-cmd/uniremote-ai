package com.uniremote.ai.model

/**
 * A TV (or set-top box) discovered on the LAN, plus the protocol we intend
 * to control it with. `extra` carries protocol-specific bits we learn
 * during discovery/pairing (e.g. DLNA control URLs, a WebOS client-key,
 * a Tizen pairing token).
 */
enum class Protocol { DLNA, ROKU, LG_WEBOS, SAMSUNG_TIZEN, ADB, UNKNOWN }

data class TvDevice(
    val name: String,
    val ip: String,
    val port: Int,
    val protocol: Protocol,
    val friendlyServer: String? = null,   // raw SSDP "SERVER" header, for debugging
    val extra: MutableMap<String, String> = mutableMapOf()
)
