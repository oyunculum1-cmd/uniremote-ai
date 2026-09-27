package com.uniremote.ai.discovery

import android.content.Context
import android.net.wifi.WifiManager
import com.uniremote.ai.model.Protocol
import com.uniremote.ai.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket

class SsdpDiscovery(private val context: Context) {

    private val searchTargets = listOf(
        "ssdp:all",
        "urn:schemas-upnp-org:device:MediaRenderer:1",
        "urn:dial-multiscreen-org:service:dial:1"
    )

    fun scan(timeoutMs: Long = 4000): Flow<TvDevice> = callbackFlow {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("uniremote-ssdp")
        lock.setReferenceCounted(true)
        lock.acquire()

        val seen = mutableSetOf<String>()
        val socket = MulticastSocket().apply {
            reuseAddress = true
            soTimeout = timeoutMs.toInt()
        }

        try {
            val group = InetAddress.getByName("239.255.255.250")
            for (target in searchTargets) {
                val msg = buildString {
                    append("M-SEARCH * HTTP/1.1\r\n")
                    append("HOST: 239.255.255.250:1900\r\n")
                    append("MAN: \"ssdp:discover\"\r\n")
                    append("MX: 3\r\n")
                    append("ST: $target\r\n\r\n")
                }
                val bytes = msg.toByteArray()
                socket.send(DatagramPacket(bytes, bytes.size, InetSocketAddress(group, 1900)))
            }

            val buf = ByteArray(4096)
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                val remaining = (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1)
                socket.soTimeout = remaining
                val packet = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(packet)
                } catch (e: java.net.SocketTimeoutException) {
                    break
                }
                val ip = packet.address.hostAddress ?: continue
                if (ip in seen) continue
                seen += ip

                val response = String(packet.data, 0, packet.length)
                val headers = parseHeaders(response)
                val server = headers["server"] ?: headers["Server"]
                val location = headers["location"] ?: headers["Location"]

                trySend(
                    TvDevice(
                        name = guessName(server, ip),
                        ip = ip,
                        port = 1900,
                        protocol = guessProtocol(server, location),
                        friendlyServer = server,
                        extra = mutableMapOf<String, String>().apply { location?.let { put("ssdpLocation", it) } }
                    )
                )
            }
        } finally {
            socket.close()
            if (lock.isHeld) lock.release()
        }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    private fun parseHeaders(raw: String): Map<String, String> =
        raw.lineSequence()
            .drop(1)
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
            .toMap()

    private fun guessProtocol(server: String?, location: String?): Protocol {
        val hay = ((server ?: "") + " " + (location ?: "")).lowercase()
        return when {
            "roku" in hay -> Protocol.ROKU
            "lg" in hay || "webos" in hay -> Protocol.LG_WEBOS
            "samsung" in hay || "tizen" in hay -> Protocol.SAMSUNG_TIZEN
            else -> Protocol.DLNA
        }
    }

    private fun guessName(server: String?, ip: String): String =
        server?.substringBefore("/")?.trim()?.takeIf { it.isNotBlank() } ?: "TV ($ip)"
}
