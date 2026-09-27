package com.uniremote.ai.protocols

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.uniremote.ai.model.TvDevice

/**
 * Fallback for the truly unbranded / no-name Android-based boxes that don't
 * answer to DLNA cleanly: raw ADB over the network (port 5555, or the
 * Android 11+ "wireless debugging" pairing port).
 *
 * IMPORTANT / by design, not a limitation to work around:
 * This only works if the box's owner has turned on network debugging
 * (Settings > Developer options > Wireless debugging / Network ADB) AND,
 * for Android 11+, entered the on-screen pairing code themselves. There is
 * no way to control a device over ADB without that explicit, physical
 * cooperation from whoever holds the box — which is the correct behavior:
 * this is a remote-control convenience for your own devices, not a way to
 * reach devices that haven't opted in.
 */
class AdbController {

    private val adb = AndroidDebugBridgeClientFactory().build()

    suspend fun sendKeyEvent(device: TvDevice, keycode: Int) {
        val client = adb.startSession(device.ip, device.port)
        client.execute(ShellCommandRequest("input keyevent $keycode"))
    }

    suspend fun typeText(device: TvDevice, text: String) {
        val escaped = text.replace(" ", "%s")
        val client = adb.startSession(device.ip, device.port)
        client.execute(ShellCommandRequest("input text \"$escaped\""))
    }

    suspend fun tap(device: TvDevice, x: Int, y: Int) {
        val client = adb.startSession(device.ip, device.port)
        client.execute(ShellCommandRequest("input tap $x $y"))
    }

    suspend fun swipe(device: TvDevice, x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 150) {
        val client = adb.startSession(device.ip, device.port)
        client.execute(ShellCommandRequest("input swipe $x1 $y1 $x2 $y2 $durationMs"))
    }

    // Common Android keycodes for convenience.
    object Keys {
        const val VOLUME_UP = 24
        const val VOLUME_DOWN = 25
        const val HOME = 3
        const val BACK = 4
        const val DPAD_UP = 19
        const val DPAD_DOWN = 20
        const val DPAD_LEFT = 21
        const val DPAD_RIGHT = 22
        const val DPAD_CENTER = 23
        const val CHANNEL_UP = 166
        const val CHANNEL_DOWN = 167
    }
}
