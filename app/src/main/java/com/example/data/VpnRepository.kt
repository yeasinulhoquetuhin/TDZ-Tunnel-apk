package com.example.data

import kotlinx.coroutines.flow.Flow

class VpnRepository(private val vpnDao: VpnDao) {

    val allProfiles: Flow<List<V2rayProfile>> = vpnDao.getAllProfilesFlow()
    val allLogs: Flow<List<ConnectionLog>> = vpnDao.getAllLogsFlow()

    suspend fun getProfileById(id: Int): V2rayProfile? = vpnDao.getProfileById(id)

    suspend fun insertProfile(profile: V2rayProfile): Long = vpnDao.insertProfile(profile)

    suspend fun updateProfile(profile: V2rayProfile) = vpnDao.updateProfile(profile)

    suspend fun deleteProfileById(id: Int) = vpnDao.deleteProfileById(id)

    suspend fun selectProfile(id: Int) = vpnDao.selectProfile(id)

    suspend fun deselectAllProfiles() = vpnDao.deselectAllProfiles()

    suspend fun getSelectedProfile(): V2rayProfile? = vpnDao.getSelectedProfile()

    suspend fun getAllProfilesList(): List<V2rayProfile> = vpnDao.getAllProfiles()

    // === SETTINGS HELPERS ===
    suspend fun getSetting(key: String, default: String): String {
        return vpnDao.getSettingValue(key) ?: default
    }

    suspend fun saveSetting(key: String, value: String) {
        vpnDao.insertSetting(VpnSetting(key, value))
    }

    suspend fun getDnsPrimary(): String = getSetting("dns_primary", "1.1.1.1")
    suspend fun saveDnsPrimary(value: String) = saveSetting("dns_primary", value)
    
    suspend fun getDnsSecondary(): String = getSetting("dns_secondary", "8.8.8.8")
    suspend fun saveDnsSecondary(value: String) = saveSetting("dns_secondary", value)
    
    suspend fun getRoutingMode(): String = getSetting("routing_mode", "PROXY")
    suspend fun saveRoutingMode(value: String) = saveSetting("routing_mode", value)

    // === LOG HELPERS ===
    suspend fun logInfo(message: String) {
        vpnDao.insertLog(ConnectionLog(level = "INFO", message = message))
    }

    suspend fun logDebug(message: String) {
        vpnDao.insertLog(ConnectionLog(level = "DEBUG", message = message))
    }

    suspend fun logSuccess(message: String) {
        vpnDao.insertLog(ConnectionLog(level = "SUCCESS", message = message))
    }

    suspend fun logError(message: String) {
        vpnDao.insertLog(ConnectionLog(level = "ERROR", message = message))
    }

    suspend fun clearLogs() {
        vpnDao.clearLogs()
    }
}
