package com.uniremote.ai.protocols

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.uniremote.ai.model.TvDevice

class AdbController {

    private val adb = AndroidDebugBridgeClientFactory().build()

    private fun serial(device: TvDevice) = "${device.ip}:${device.port}"

    suspend fun sendKeyEvent(device: TvDevice, keycode: Int) {
        adb.execute(ShellCommandRequest("input keyevent $keycode"), serial = serial(device))
    }

    suspend fun typeText(device: TvDevice, text: String) {
        val escaped = text.replace(" ", "%s")
        adb.execute(ShellCommandRequest("input text \"$escaped\""), serial = serial(device))
    }

    suspend fun tap(device: TvDevice, x: Int, y: Int) {
        adb.execute(ShellCommandRequest("input tap $x $y"), serial = serial(device))
    }

    suspend fun swipe(device: TvDevice, x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 150) {
        adb.execute(ShellCommandRequest("input swipe $x1 $y1 $x2 $y2 $durationMs"), serial = serial(device))
    }

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
