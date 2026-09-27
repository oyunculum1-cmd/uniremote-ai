package com.uniremote.ai.protocols

import com.uniremote.ai.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Generic DLNA/UPnP control. This is the fallback that works on almost any
 * "no-name" smart TV, because it doesn't depend on a vendor cloud service —
 * only on the standard UPnP device-description + SOAP control mechanism.
 *
 * Flow:
 *  1. GET the device-description XML from the SSDP LOCATION URL.
 *  2. Find the <serviceType> for RenderingControl (volume/mute) and
 *     AVTransport (play/pause/stop/cast), and their <controlURL>.
 *  3. POST SOAP envelopes to those control URLs.
 */
class DlnaController(private val client: OkHttpClient = OkHttpClient()) {

    private data class ServiceInfo(val serviceType: String, val controlUrl: String)

    /** Resolves and caches control URLs into device.extra so we only fetch the XML once. */
    private suspend fun resolveServices(device: TvDevice): Map<String, ServiceInfo> = withContext(Dispatchers.IO) {
        val location = device.extra["ssdpLocation"] ?: error("No SSDP LOCATION for ${device.name}")
        val base = location.substringBefore("://").plus("://").plus(location.substringAfter("://").substringBefore("/"))

        val xml = client.newCall(Request.Builder().url(location).build()).execute().use { resp ->
            resp.body?.string() ?: error("Empty device description")
        }

        val services = mutableMapOf<String, ServiceInfo>()
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(StringReader(xml))
        }

        var serviceType: String? = null
        var controlUrl: String? = null
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.name) {
                    "service" -> { serviceType = null; controlUrl = null }
                    "serviceType" -> serviceType = parser.nextText()
                    "controlURL" -> controlUrl = parser.nextText()
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> if (parser.name == "service" && serviceType != null && controlUrl != null) {
                    val absolute = if (controlUrl!!.startsWith("http")) controlUrl!! else base + (if (controlUrl!!.startsWith("/")) "" else "/") + controlUrl
                    when {
                        serviceType!!.contains("RenderingControl") -> services["RenderingControl"] = ServiceInfo(serviceType!!, absolute)
                        serviceType!!.contains("AVTransport") -> services["AVTransport"] = ServiceInfo(serviceType!!, absolute)
                    }
                }
            }
            event = parser.next()
        }
        services
    }

    private suspend fun soapCall(svc: ServiceInfo, action: String, argsXml: String) = withContext(Dispatchers.IO) {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:$action xmlns:u="${svc.serviceType}">
                  <InstanceID>0</InstanceID>
                  $argsXml
                </u:$action>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val req = Request.Builder()
            .url(svc.controlUrl)
            .addHeader("SOAPACTION", "\"${svc.serviceType}#$action\"")
            .post(body.toRequestBody("text/xml; charset=utf-8".toMediaType()))
            .build()
        client.newCall(req).execute().close()
    }

    suspend fun setVolume(device: TvDevice, volume0to100: Int) {
        val svc = resolveServices(device)["RenderingControl"] ?: return
        soapCall(svc, "SetVolume", "<Channel>Master</Channel><DesiredVolume>$volume0to100</DesiredVolume>")
    }

    suspend fun setMute(device: TvDevice, mute: Boolean) {
        val svc = resolveServices(device)["RenderingControl"] ?: return
        soapCall(svc, "SetMute", "<Channel>Master</Channel><DesiredMute>${if (mute) 1 else 0}</DesiredMute>")
    }

    suspend fun play(device: TvDevice) {
        resolveServices(device)["AVTransport"]?.let { soapCall(it, "Play", "<Speed>1</Speed>") }
    }

    suspend fun pause(device: TvDevice) {
        resolveServices(device)["AVTransport"]?.let { soapCall(it, "Pause", "") }
    }

    suspend fun stop(device: TvDevice) {
        resolveServices(device)["AVTransport"]?.let { soapCall(it, "Stop", "") }
    }

    /** Casts a direct media URL (e.g. an MP4/JPEG your phone is serving or a public link) to the TV. */
    suspend fun castUrl(device: TvDevice, mediaUrl: String, mimeType: String = "video/mp4") {
        val svc = resolveServices(device)["AVTransport"] ?: return
        val metadata = """&lt;DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"&gt;&lt;item&gt;&lt;res protocolInfo="http-get:*:$mimeType:*"&gt;$mediaUrl&lt;/res&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;"""
        soapCall(svc, "SetAVTransportURI", "<CurrentURI>$mediaUrl</CurrentURI><CurrentURIMetaData>$metadata</CurrentURIMetaData>")
        play(device)
    }
}
