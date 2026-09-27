package com.uniremote.ai.protocols

import com.uniremote.ai.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Roku External Control Protocol (ECP) — a plain, unauthenticated HTTP API
 * that every Roku device and Roku-TV exposes on port 8060 by default.
 * Official docs: developer.roku.com/docs/developer-program/debugging/external-control-api.md
 */
class RokuController(private val client: OkHttpClient = OkHttpClient()) {

    private fun base(device: TvDevice) = "http://${device.ip}:${if (device.port == 1900) 8060 else device.port}"

    private suspend fun keypress(device: TvDevice, key: String) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("${base(device)}/keypress/$key")
            .post("".toRequestBody(null))
            .build()
        client.newCall(req).execute().close()
    }

    suspend fun volumeUp(device: TvDevice) = keypress(device, "VolumeUp")
    suspend fun volumeDown(device: TvDevice) = keypress(device, "VolumeDown")
    suspend fun mute(device: TvDevice) = keypress(device, "VolumeMute")
    suspend fun home(device: TvDevice) = keypress(device, "Home")
    suspend fun back(device: TvDevice) = keypress(device, "Back")
    suspend fun ok(device: TvDevice) = keypress(device, "Select")
    suspend fun up(device: TvDevice) = keypress(device, "Up")
    suspend fun down(device: TvDevice) = keypress(device, "Down")
    suspend fun left(device: TvDevice) = keypress(device, "Left")
    suspend fun right(device: TvDevice) = keypress(device, "Right")
    suspend fun channelUp(device: TvDevice) = keypress(device, "ChannelUp")
    suspend fun channelDown(device: TvDevice) = keypress(device, "ChannelDown")

    /** Types literal text into whatever text field is focused on the TV. */
    suspend fun typeText(device: TvDevice, text: String) {
        for (ch in text) {
            keypress(device, "Lit_${java.net.URLEncoder.encode(ch.toString(), "UTF-8")}")
        }
    }
}
