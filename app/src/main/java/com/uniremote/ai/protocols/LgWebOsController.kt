package com.uniremote.ai.protocols

import com.uniremote.ai.model.TvDevice
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * LG WebOS TVs speak SSAP (WebOS Second-Screen protocol) over a WebSocket on
 * port 3000 (ws) or 3001 (wss). First connection triggers an on-screen
 * pairing prompt on the TV that the TV's owner must accept — this is LG's
 * own security gate, not something this app can or should bypass.
 * Once accepted, the TV returns a client-key we persist and reuse so future
 * connections are silent.
 */
class LgWebOsController(private val client: OkHttpClient = OkHttpClient()) {

    private val manifest = """
        {
          "manifestVersion": 1,
          "permissions": [
            "LAUNCH", "CONTROL_AUDIO", "CONTROL_INPUT_TV", "CONTROL_POWER",
            "READ_INSTALLED_APPS", "CONTROL_INPUT_MEDIA_PLAYBACK",
            "CONTROL_INPUT_TEXT", "CONTROL_MOUSE_AND_KEYBOARD"
          ]
        }
    """.trimIndent()

    private fun registerPayload(existingClientKey: String?): String {
        val payload = JSONObject()
            .put("forcePairing", false)
            .put("pairingType", "PROMPT")
            .put("manifest", JSONObject(manifest))
        existingClientKey?.let { payload.put("client-key", it) }
        return JSONObject()
            .put("type", "register")
            .put("id", "register_0")
            .put("payload", payload)
            .toString()
    }

    /**
     * Connects, completes pairing (blocking on the TV-side prompt the first
     * time), and returns an open, registered WebSocket plus the client-key
     * to persist for next time.
     */
    fun connectAndPair(device: TvDevice, savedClientKey: String?, onPaired: (clientKey: String) -> Unit): WebSocket {
        val latch = CountDownLatch(1)
        val request = Request.Builder().url("ws://${device.ip}:3000").build()
        lateinit var socket: WebSocket
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(registerPayload(savedClientKey))
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("type") == "registered") {
                    val key = json.getJSONObject("payload").optString("client-key")
                    if (key.isNotBlank()) onPaired(key)
                    latch.countDown()
                }
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {}
        })
        latch.await(60, TimeUnit.SECONDS) // TV owner has up to 60s to tap "Accept" on the prompt
        return socket
    }

    private fun sendRequest(socket: WebSocket, uri: String, payload: JSONObject = JSONObject()) {
        val msg = JSONObject()
            .put("type", "request")
            .put("id", "req_${System.currentTimeMillis()}")
            .put("uri", uri)
            .put("payload", payload)
        socket.send(msg.toString())
    }

    fun volumeUp(socket: WebSocket) = sendRequest(socket, "ssap://audio/volumeUp")
    fun volumeDown(socket: WebSocket) = sendRequest(socket, "ssap://audio/volumeDown")
    fun mute(socket: WebSocket, mute: Boolean) =
        sendRequest(socket, "ssap://audio/setMute", JSONObject().put("mute", mute))
    fun channelUp(socket: WebSocket) = sendRequest(socket, "ssap://tv/channelUp")
    fun channelDown(socket: WebSocket) = sendRequest(socket, "ssap://tv/channelDown")
    fun home(socket: WebSocket) =
        sendRequest(socket, "ssap://system.launcher/launch", JSONObject().put("id", "com.webos.app.home"))
    fun launchInputTextField(socket: WebSocket, text: String) =
        sendRequest(socket, "ssap://com.webos.service.ime/insertText", JSONObject().put("text", text).put("replace", false))
}
