package com.uniremote.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.uniremote.ai.discovery.SsdpDiscovery
import com.uniremote.ai.model.Protocol
import com.uniremote.ai.model.TvDevice
import com.uniremote.ai.protocols.DlnaController
import com.uniremote.ai.protocols.RokuController
import com.uniremote.ai.ui.RemoteScreen
import com.uniremote.ai.ui.ScanScreen
import kotlinx.coroutines.launch

/**
 * Navigation: ScanScreen -> RemoteScreen.
 *
 * This wiring shows the two protocols with no pairing step (DLNA, Roku) end
 * to end. LG WebOS / Samsung Tizen / ADB controllers exist as full classes
 * in protocols/ — hooking them into RemoteScreen's callbacks is the same
 * pattern, just gated behind their pairing step (see each controller's
 * connect()/connectAndPair() for how to surface that prompt in the UI).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    val nav = rememberNavController()
                    AppNav(nav)
                }
            }
        }
    }
}

@Composable
fun AppNav(nav: NavHostController) {
    val context = LocalContext.current
    val discovery = remember { SsdpDiscovery(context) }
    val dlna = remember { DlnaController() }
    val roku = remember { RokuController() }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<TvDevice?>(null) }

    NavHost(navController = nav, startDestination = "scan") {
        composable("scan") {
            ScanScreen(discovery) { device ->
                selected = device
                nav.navigate("remote")
            }
        }
        composable("remote") {
            val device = selected ?: return@composable
            RemoteScreen(
                device = device,
                onVolumeUp = { scope.launch { when (device.protocol) {
                    Protocol.ROKU -> roku.volumeUp(device)
                    else -> dlna.setVolume(device, 60) // DLNA has no relative "up"; nudge to a fixed step or track state
                } } },
                onVolumeDown = { scope.launch { when (device.protocol) {
                    Protocol.ROKU -> roku.volumeDown(device)
                    else -> dlna.setVolume(device, 40)
                } } },
                onMute = { scope.launch { when (device.protocol) {
                    Protocol.ROKU -> roku.mute(device)
                    else -> dlna.setMute(device, true)
                } } },
                onDpad = { dx, dy -> scope.launch {
                    // Touchpad -> discrete direction for protocols without a real pointer API (DLNA has none standard).
                    when (device.protocol) {
                        Protocol.ROKU -> when {
                            kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx > 15 -> roku.right(device)
                            kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx < -15 -> roku.left(device)
                            dy > 15 -> roku.down(device)
                            dy < -15 -> roku.up(device)
                            else -> {}
                        }
                        else -> {} // LG/Samsung: route to their pointer-socket / KEY_UP-DOWN-LEFT-RIGHT equivalents
                    }
                } },
                onSelect = { scope.launch { if (device.protocol == Protocol.ROKU) roku.ok(device) } },
                onHome = { scope.launch { if (device.protocol == Protocol.ROKU) roku.home(device) } },
                onBack = { scope.launch { if (device.protocol == Protocol.ROKU) roku.back(device) } },
                onChannelUp = { scope.launch { if (device.protocol == Protocol.ROKU) roku.channelUp(device) } },
                onChannelDown = { scope.launch { if (device.protocol == Protocol.ROKU) roku.channelDown(device) } },
                onSendText = { text -> scope.launch { if (device.protocol == Protocol.ROKU) roku.typeText(device, text) } }
            )
        }
    }
}
