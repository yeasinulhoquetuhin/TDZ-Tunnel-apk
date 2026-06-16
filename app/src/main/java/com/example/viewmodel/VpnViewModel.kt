package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.V2rayProfile
import com.example.data.VpnRepository
import com.example.engine.VpnEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VpnRepository
    val vpnStatus = VpnEngine.status
    val uploadSpeedKb = VpnEngine.uploadSpeedKb
    val downloadSpeedKb = VpnEngine.downloadSpeedKb
    val tunnelPingMs = VpnEngine.pingTimeMs
    val currentSelectedProfile = VpnEngine.selectedProfile

    val profilesState: StateFlow<List<V2rayProfile>>
    val logsState: StateFlow<List<com.example.data.ConnectionLog>>

    // Configuration Settings
    private val _dnsPrimaryState = MutableStateFlow("1.1.1.1")
    val dnsPrimaryState = _dnsPrimaryState.asStateFlow()

    private val _dnsSecondaryState = MutableStateFlow("8.8.8.8")
    val dnsSecondaryState = _dnsSecondaryState.asStateFlow()

    private val _routingModeState = MutableStateFlow("PROXY") // PROXY, DIRECT, BLOCK (Ad-Block)
    val routingModeState = _routingModeState.asStateFlow()

    // App Preferences
    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled = _hapticsEnabled.asStateFlow()

    private val _connectOnBoot = MutableStateFlow(false)
    val connectOnBoot = _connectOnBoot.asStateFlow()

    private val _themeColor = MutableStateFlow("#0052FF") // Default primary color
    val themeColor = _themeColor.asStateFlow()


    // Ping testing state
    private val _pingingProfiles = MutableStateFlow<Set<Int>>(emptySet())
    val pingingProfiles = _pingingProfiles.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = VpnRepository(database.vpnDao())
        VpnEngine.initialize(application, repository)

        profilesState = repository.allProfiles
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        logsState = repository.allLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Fetch basic VPN settings during launch
        viewModelScope.launch {
            _dnsPrimaryState.value = repository.getDnsPrimary()
            _dnsSecondaryState.value = repository.getDnsSecondary()
            _routingModeState.value = repository.getRoutingMode()
            
            _hapticsEnabled.value = repository.getSetting("haptics_enabled", "true") == "true"
            _connectOnBoot.value = repository.getSetting("connect_on_boot", "false") == "true"
            _themeColor.value = repository.getSetting("theme_primary_color", "#0052FF")

            // Pre-fill DB with standard helpful premium servers on first install
            val existing = repository.getAllProfilesList()
            if (existing.isEmpty()) {
                seedInitialProfiles()
            }
        }
    }

    private suspend fun seedInitialProfiles() {
        val default1 = V2rayProfile(
            name = "⚡ Singapore Premium VLESS (Xray Cloud)",
            protocol = "VLESS",
            address = "sg-premium.xraydns.net",
            port = 443,
            uuidOrPassword = "8a5c3912-32b5-4a11-bc6e-fbc711425ea3",
            transport = "gRPC",
            security = "TLS",
            sni = "sg-premium.xraydns.net",
            path = "grpc-tunnel-service",
            isSelected = true
        )

        val default2 = V2rayProfile(
            name = "🚀 Tokyo HighSpeed VMess (Direct TLS)",
            protocol = "VMESS",
            address = "jp-tokyo.xraydns.net",
            port = 443,
            uuidOrPassword = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            transport = "WS",
            security = "TLS",
            sni = "jp-tokyo.xraydns.net",
            path = "/vmess-ws-premium"
        )

        val default3 = V2rayProfile(
            name = "🛡️ Frankfurt Shadowsocks Core",
            protocol = "SHADOWSOCKS",
            address = "de-frankfurt.xraydns.net",
            port = 8388,
            uuidOrPassword = "highlysecureaes256gcmpassword",
            transport = "TCP",
            security = "NONE"
        )

        repository.insertProfile(default1)
        repository.insertProfile(default2)
        repository.insertProfile(default3)

        // Select the first one in the engine initially
        VpnEngine.initialize(getApplication(), repository)
    }

    // === ACTION DISPATCHERS ===

    fun toggleVpnConnection(profile: V2rayProfile) {
        if (vpnStatus.value == VpnEngine.ConnectionStatus.CONNECTED) {
            VpnEngine.stopVpn(repository)
        } else {
            VpnEngine.startVpn(getApplication(), profile, repository)
        }
    }

    fun stopVpnConnection() {
        VpnEngine.stopVpn(repository)
    }

    fun selectProfile(profile: V2rayProfile) {
        viewModelScope.launch {
            repository.selectProfile(profile.id)
            repository.logInfo("Active profile selection: [${profile.name}]")
            // Check if VPN is running, we might need a reconnect
            if (vpnStatus.value == VpnEngine.ConnectionStatus.CONNECTED) {
                repository.logInfo("VPN is currently connected, please reconnect to apply new routing configurations")
            }
        }
    }

    fun testProfilePing(profile: V2rayProfile) {
        viewModelScope.launch {
            _pingingProfiles.update { it + profile.id }
            val latency = VpnEngine.testSingleProfilePing(profile, repository)
            repository.updateProfile(profile.copy(pingMs = latency))
            _pingingProfiles.update { it - profile.id }
        }
    }

    fun testAllProfilesPing() {
        viewModelScope.launch {
            val list = repository.getAllProfilesList()
            list.forEach { profile ->
                testProfilePing(profile)
            }
        }
    }

    fun deleteProfile(profile: V2rayProfile) {
        viewModelScope.launch {
            repository.deleteProfileById(profile.id)
            repository.logInfo("Profile deleted: [${profile.name}]")
            if (currentSelectedProfile.value?.id == profile.id) {
                // If we deleted selected config, find another one
                val remaining = repository.getAllProfilesList()
                if (remaining.isNotEmpty()) {
                    selectProfile(remaining.first())
                }
            }
        }
    }

    fun addOrUpdateProfile(profile: V2rayProfile) {
        viewModelScope.launch {
            if (profile.id == 0) {
                val newId = repository.insertProfile(profile)
                repository.logSuccess("Successfully saved new Xray profile config: [${profile.name}]")
                // Autoselect if it's the first or user checks it
                val remaining = repository.getAllProfilesList()
                if (remaining.size == 1) {
                    selectProfile(remaining.first())
                }
            } else {
                repository.updateProfile(profile)
                repository.logSuccess("Updated profile parameters: [${profile.name}]")
                // If it is currently selected, trigger reload in engine
                if (currentSelectedProfile.value?.id == profile.id) {
                    selectProfile(profile)
                }
            }
        }
    }

    fun importConfigByShareLink(link: String): Boolean {
        val parsed = VpnEngine.parseShareLink(link)
        return if (parsed != null) {
            addOrUpdateProfile(parsed)
            true
        } else {
            viewModelScope.launch {
                repository.logError("Decoding profile URI failed. Invalid VMess, VLess, Shadowsocks or custom JSON structure.")
            }
            false
        }
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearLogs()
            repository.logInfo("Logs cleared.")
        }
    }

    // === SETTINGS SAVING ===
    fun updateDnsSettings(primary: String, secondary: String) {
        viewModelScope.launch {
            _dnsPrimaryState.value = primary
            _dnsSecondaryState.value = secondary
            repository.saveDnsPrimary(primary)
            repository.saveDnsSecondary(secondary)
            repository.logInfo("Saved DNS Servers: Primary=$primary, Secondary=$secondary")
        }
    }

    fun updateRoutingMode(mode: String) {
        viewModelScope.launch {
            _routingModeState.value = mode
            repository.saveRoutingMode(mode)
            repository.logSuccess("Routing engine set to: $mode mode")
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _hapticsEnabled.value = enabled
            repository.saveSetting("haptics_enabled", enabled.toString())
        }
    }

    fun setConnectOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            _connectOnBoot.value = enabled
            repository.saveSetting("connect_on_boot", enabled.toString())
        }
    }

    fun setThemeColor(hexValue: String) {
        viewModelScope.launch {
            _themeColor.value = hexValue
            repository.saveSetting("theme_primary_color", hexValue)
        }
    }
}
