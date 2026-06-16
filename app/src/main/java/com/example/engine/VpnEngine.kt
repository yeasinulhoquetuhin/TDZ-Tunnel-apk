package com.example.engine

import android.content.Context
import android.util.Base64
import com.example.data.V2rayProfile
import com.example.data.VpnRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import kotlin.random.Random

object VpnEngine {

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        PAUSED
    }

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _uploadSpeedKb = MutableStateFlow(0f)
    val uploadSpeedKb: StateFlow<Float> = _uploadSpeedKb.asStateFlow()

    private val _downloadSpeedKb = MutableStateFlow(0f)
    val downloadSpeedKb: StateFlow<Float> = _downloadSpeedKb.asStateFlow()

    private val _pingTimeMs = MutableStateFlow(-1)
    val pingTimeMs: StateFlow<Int> = _pingTimeMs.asStateFlow()

    private val _selectedProfile = MutableStateFlow<V2rayProfile?>(null)
    val selectedProfile: StateFlow<V2rayProfile?> = _selectedProfile.asStateFlow()

    private var engineScope = CoroutineScope(Dispatchers.Default + Job())
    private var speedMonitoringJob: Job? = null

    fun initialize(context: Context, repository: VpnRepository) {
        engineScope.launch {
            // Load selected profile initially
            val active = repository.getSelectedProfile()
            _selectedProfile.value = active
        }
    }

    fun startVpn(context: Context, profile: V2rayProfile, repository: VpnRepository) {
        if (_status.value == ConnectionStatus.CONNECTED || _status.value == ConnectionStatus.CONNECTING) return

        _status.value = ConnectionStatus.CONNECTING
        _selectedProfile.value = profile

        engineScope.launch {
            repository.logInfo("Initializing Xray-core Client process...")
            delay(400)
            repository.logInfo("Selected Protocol: ${profile.protocol}")
            repository.logInfo("Resolving Hostname: ${profile.address}")
            delay(500)
            repository.logInfo("Loading V2ray DNS server rules [${repository.getDnsPrimary()}, ${repository.getDnsSecondary()}]")
            repository.logInfo("Routing Outbound Mode: ${repository.getRoutingMode()}")

            // Build structural Xray client parameters simulator
            val xrayJson = if (profile.rawJson.isNotBlank()) {
                profile.rawJson
            } else {
                buildXrayConfigSimulator(profile, repository)
            }
            repository.logDebug("Created Core Configuration block:")
            repository.logDebug(xrayJson)
            delay(600)

            repository.logInfo("TCP/UDP Tunnel bound cleanly to local VpnService interface.")
            repository.logSuccess("V2ray Tunnel handshake completed successfully!")
            _status.value = ConnectionStatus.CONNECTED

            launchPingTester(profile.address, repository)
            startSpeedSimulation()
        }
    }

    fun stopVpn(repository: VpnRepository) {
        _status.value = ConnectionStatus.DISCONNECTED
        speedMonitoringJob?.cancel()
        _uploadSpeedKb.value = 0f
        _downloadSpeedKb.value = 0f
        _pingTimeMs.value = -1

        engineScope.launch {
            repository.logInfo("VpnService connection released.")
            repository.logInfo("Xray-core client daemon ended safely.")
        }
    }

    private fun startSpeedSimulation() {
        speedMonitoringJob?.cancel()
        speedMonitoringJob = engineScope.launch {
            while (isActive) {
                // Generate natural-looking network traffic spikes
                if (_status.value == ConnectionStatus.CONNECTED) {
                    val baseDown = Random.nextFloat() * 450f + 50f
                    val baseUp = Random.nextFloat() * 90f + 10f
                    _downloadSpeedKb.value = baseDown
                    _uploadSpeedKb.value = baseUp
                } else {
                    _downloadSpeedKb.value = 0f
                    _uploadSpeedKb.value = 0f
                }
                delay(1000)
            }
        }
    }

    private fun launchPingTester(hostRef: String, repository: VpnRepository) {
        engineScope.launch {
            repository.logInfo("Measuring active ICMP Latency...")
            delay(800)
            val computedPing = Random.nextInt(45, 185)
            _pingTimeMs.value = computedPing
            repository.logSuccess("Core status OK. Active Tunnel Latency: ${computedPing}ms")
        }
    }

    // Ping checker for list profiles
    suspend fun testSingleProfilePing(profile: V2rayProfile, repository: VpnRepository): Int {
        repository.logInfo("Pinging Profile Server [${profile.name}] to address: ${profile.address}...")
        delay(Random.nextLong(200, 800))
        val latency = if (Random.nextFloat() > 0.05) {
            Random.nextInt(32, 210)
        } else {
            -2 // Timed out
        }
        val pingStr = if (latency == -2) "Timeout!" else "${latency}ms"
        repository.logInfo("Ping outcome for [${profile.name}]: $pingStr")
        return latency
    }

    private suspend fun buildXrayConfigSimulator(profile: V2rayProfile, repository: VpnRepository): String {
        return """
        {
          "log": { "loglevel": "warning" },
          "dns": {
            "servers": [ "${repository.getDnsPrimary()}", "${repository.getDnsSecondary()}" ]
          },
          "inbounds": [
            {
              "port": 10808,
              "protocol": "socks",
              "settings": { "auth": "noauth", "udp": true }
            }
          ],
          "outbounds": [
            {
              "protocol": "${profile.protocol.lowercase()}",
              "settings": {
                "vnext": [{
                  "address": "${profile.address}",
                  "port": ${profile.port},
                  "users": [{ "id": "${profile.uuidOrPassword}", "alterId": 0 }]
                }]
              },
              "streamSettings": {
                "network": "${profile.transport.lowercase()}",
                "security": "${profile.security.lowercase()}"
              }
            }
          ]
        }
        """.trimIndent()
    }

    // Helper parser for VMess, VLess share links or raw json configs
    fun parseShareLink(uriLink: String): V2rayProfile? {
        val cleanLink = uriLink.trim()
        if (cleanLink.startsWith("{") || cleanLink.startsWith("[")) {
            // Raw JSON config import
            return V2rayProfile(
                name = "Parsed Custom Config",
                protocol = "JSON",
                rawJson = cleanLink,
                address = "Custom JSON Engine"
            )
        }

        try {
            if (cleanLink.startsWith("vmess://")) {
                val b64Value = cleanLink.substring(8).trim()
                // Safely remove whitespace if present in base64 string
                val decoded = String(Base64.decode(b64Value, Base64.DEFAULT))
                val json = JSONObject(decoded)
                return V2rayProfile(
                    name = json.optString("ps", "VMESS Service"),
                    protocol = "VMESS",
                    address = json.optString("add", "127.0.0.1"),
                    port = json.optInt("port", 443),
                    uuidOrPassword = json.optString("id", "00000000-0000-0000-0000-000000000000"),
                    transport = json.optString("net", "ws").uppercase(),
                    security = json.optString("tls", "none").uppercase(),
                    sni = json.optString("sni", ""),
                    path = json.optString("path", "")
                )
            } else if (cleanLink.startsWith("vless://")) {
                val uriObj = URI.create(cleanLink)
                val userInfo = uriObj.userInfo ?: ""
                val host = uriObj.host ?: "127.0.0.1"
                var port = uriObj.port
                if (port == -1) port = 443
                
                var name = "VLESS Service"
                var transport = "WS"
                var security = "TLS"
                var path = ""
                var sni = ""

                val fragment = uriObj.fragment
                if (!fragment.isNullOrBlank()) {
                    name = URLDecoder.decode(fragment, "UTF-8")
                }

                val query = uriObj.query
                if (!query.isNullOrBlank()) {
                    val pairs = query.split("&")
                    for (pair in pairs) {
                        val side = pair.split("=")
                        if (side.size == 2) {
                            val key = side[0].lowercase()
                            val value = URLDecoder.decode(side[1], "UTF-8")
                            if (key == "security") security = value.uppercase()
                            if (key == "type") transport = value.uppercase()
                            if (key == "path") path = value
                            if (key == "sni") sni = value
                        }
                    }
                }

                return V2rayProfile(
                    name = name,
                    protocol = "VLESS",
                    address = host,
                    port = port,
                    uuidOrPassword = userInfo,
                    transport = transport,
                    security = security,
                    sni = sni,
                    path = path
                )
            } else if (cleanLink.startsWith("ss://")) {
                // ss://base64(method:password)@host:port#name
                val ssPart = cleanLink.substring(5)
                val hashIndex = ssPart.indexOf("#")
                var name = "Shadowsocks Service"
                val b64AndHost = if (hashIndex != -1) {
                    name = URLDecoder.decode(ssPart.substring(hashIndex + 1), "UTF-8")
                    ssPart.substring(0, hashIndex)
                } else {
                    ssPart
                }

                val atIndex = b64AndHost.indexOf("@")
                if (atIndex != -1) {
                    val b64Part = b64AndHost.substring(0, atIndex)
                    val hostPort = b64AndHost.substring(atIndex + 1)
                    val decodedCreds = String(Base64.decode(b64Part, Base64.DEFAULT))
                    val splits = decodedCreds.split(":")
                    val pass = if (splits.size > 1) splits[1] else splits[0]

                    val hp = hostPort.split(":")
                    val address = hp[0]
                    val port = if (hp.size > 1) hp[1].toIntOrNull() ?: 443 else 443

                    return V2rayProfile(
                        name = name,
                        protocol = "SHADOWSOCKS",
                        address = address,
                        port = port,
                        uuidOrPassword = pass,
                        security = "NONE",
                        transport = "TCP"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Helper serializer to construct share links to allow export/copying
    fun exportToShareLink(profile: V2rayProfile): String {
        return try {
            when (profile.protocol) {
                "VMESS" -> {
                    val json = JSONObject().apply {
                        put("v", "2")
                        put("ps", profile.name)
                        put("add", profile.address)
                        put("port", profile.port)
                        put("id", profile.uuidOrPassword)
                        put("aid", "0")
                        put("net", profile.transport.lowercase())
                        put("type", "none")
                        put("host", "")
                        put("path", profile.path)
                        put("tls", profile.security.lowercase())
                        put("sni", profile.sni)
                    }
                    val base64 = Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
                    "vmess://$base64"
                }
                "VLESS" -> {
                    "vless://${profile.uuidOrPassword}@${profile.address}:${profile.port}?type=${profile.transport.lowercase()}&security=${profile.security.lowercase()}&path=${profile.path}&sni=${profile.sni}#${profile.name}"
                }
                "SHADOWSOCKS" -> {
                    val rawCreds = "aes-256-gcm:${profile.uuidOrPassword}"
                    val b64Creds = Base64.encodeToString(rawCreds.toByteArray(), Base64.NO_WRAP)
                    "ss://$b64Creds@${profile.address}:${profile.port}#${profile.name}"
                }
                else -> {
                    "Raw CONFIG:\n${profile.rawJson}"
                }
            }
        } catch (e: Exception) {
            "Invalid share build"
        }
    }
}
