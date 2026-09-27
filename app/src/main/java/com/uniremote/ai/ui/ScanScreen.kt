package com.uniremote.ai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uniremote.ai.discovery.SsdpDiscovery
import com.uniremote.ai.model.TvDevice
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

@Composable
fun ScanScreen(discovery: SsdpDiscovery, onDeviceSelected: (TvDevice) -> Unit) {
    var devices by remember { mutableStateOf(listOf<TvDevice>()) }
    var scanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun startScan() {
        scanning = true
        devices = emptyList()
        scope.launch {
            val found = mutableListOf<TvDevice>()
            discovery.scan().collect { d ->
                found += d
                devices = found.toList()
            }
            scanning = false
        }
    }

    LaunchedEffect(Unit) { startScan() } // otomatik tarama: uygulama açılır açılmaz başlar

    Scaffold(topBar = { TopAppBar(title = { Text("UniRemote AI") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (scanning) "Ağdaki TV'ler taranıyor…" else "${devices.size} cihaz bulundu",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (scanning) CircularProgressIndicator(Modifier.size(20.dp))
                else TextButton(onClick = { startScan() }) { Text("Yeniden tara") }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(devices) { device ->
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text("${device.ip} · ${device.protocol}") },
                        modifier = Modifier.clickable { onDeviceSelected(device) }
                    )
                    Divider()
                }
            }
        }
    }
}
