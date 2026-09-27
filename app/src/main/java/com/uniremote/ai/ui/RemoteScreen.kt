@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.uniremote.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.uniremote.ai.model.Protocol
import com.uniremote.ai.model.TvDevice

@Composable
fun RemoteScreen(
    device: TvDevice,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onMute: () -> Unit,
    onDpad: (dx: Int, dy: Int) -> Unit,
    onSelect: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
    onSendText: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text(device.name) }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(20.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text("Protokol: ${device.protocol}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onVolumeDown) { Text("Ses -") }
                OutlinedButton(onClick = onMute) { Text("Sessiz") }
                Button(onClick = onVolumeUp) { Text("Ses +") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onChannelDown) { Text("Kanal -") }
                Button(onClick = onChannelUp) { Text("Kanal +") }
            }

            Spacer(Modifier.height(20.dp))

            var lastX by remember { mutableStateOf(0f) }
            var lastY by remember { mutableStateOf(0f) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { lastX = it.x; lastY = it.y },
                            onDrag = { change, _ ->
                                val dx = (change.position.x - lastX).toInt()
                                val dy = (change.position.y - lastY).toInt()
                                lastX = change.position.x; lastY = change.position.y
                                onDpad(dx, dy)
                            }
                        )
                    }
            ) {
                Text(
                    "Touchpad — parmağınla kaydır",
                    Modifier.align(androidx.compose.ui.Alignment.Center),
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Geri") }
                Button(onClick = onSelect) { Text("Tamam") }
                OutlinedButton(onClick = onHome) { Text("Ana Ekran") }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("TV'ye yazı gönder") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onSendText(textInput); textInput = "" },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Gönder") }
        }
    }
}
