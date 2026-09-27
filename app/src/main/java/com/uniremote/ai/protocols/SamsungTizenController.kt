package com.uniremote.ai.protocols

import android.util.Base64
import com.uniremote.ai.model.TvDevice
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

/**
 * Samsung Tizen TVs expose a WebSocket remote-control endpoint on port 8001
 * (or 8002 for TLS). Like LG, the first connection shows a pairing popup on
 * the TV that must be accepted by whoever is holding the physical remote —
 * this app cannot and does not bypass that.
 *
 * Endpoint: ws://<ip>:8001/api/v2/channels/samsung.remote.control?name=<base64 app name>
 * Key list: https://github.com/Bntdumas/SamsungIPRemote (community-documented key names,
 * mirrors Samsung's own multiremote SDK naming).
 */
class SamsungTizenController(private val client: OkHttpClient = OkHttpClient()) {

    fun connect(device: TvDevice, onToken: (String) -> Unit, onReady: () -> Unit): WebSocket {
        val appName = Base64.encodeToString("UniRemoteAI".toByteArray(), Base64.NO_WRAP)
        val url = "ws://${device.ip}:8001/api/v2/channels/samsung.remote.control?name=$appName"
        return client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { /* wait for ms.channel.connect */ }
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("event") == "ms.channel.connect") {
                    json.optJSONObject("data")?.optString("token")?.takeIf { it.isNotBlank() }?.let(onToken)
                    onReady()
                }
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {}
        })
    }

    private fun sendKey(socket: WebSocket, key: String) {
        val payload = JSONObject()
            .put("method", "ms.remote.control")
            .put(
                "params", JSONObject()
                    .put("Cmd", "Click")
                    .put("DataOfCmd", key)
                    .put("Option", "false")
                    .put("TypeOfRemote", "SendRemoteKey")
            )
        socket.send(payload.toString())
    }

    fun volumeUp(socket: WebSocket) = sendKey(socket, "KEY_VOLUP")
    fun volumeDown(socket: WebSocket) = sendKey(socket, "KEY_VOLDOWN")
    fun mute(socket: WebSocket) = sendKey(socket, "KEY_MUTE")
    fun channelUp(socket: WebSocket) = sendKey(socket, "KEY_CHUP")
    fun channelDown(socket: WebSocket) = sendKey(socket, "KEY_CHDOWN")
    fun up(socket: WebSocket) = sendKey(socket, "KEY_UP")
    fun down(socket: WebSocket) = sendKey(socket, "KEY_DOWN")
    fun left(socket: WebSocket) = sendKey(socket, "KEY_LEFT")
    fun right(socket: WebSocket) = sendKey(socket, "KEY_RIGHT")
    fun enter(socket: WebSocket) = sendKey(socket, "KEY_ENTER")
    fun home(socket: WebSocket) = sendKey(socket, "KEY_HOME")
    fun back(socket: WebSocket) = sendKey(socket, "KEY_RETURN")
}
