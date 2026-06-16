package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnDao {
    // === PROFILES ===
    @Query("SELECT * FROM v2ray_profiles ORDER BY id DESC")
    fun getAllProfilesFlow(): Flow<List<V2rayProfile>>

    @Query("SELECT * FROM v2ray_profiles ORDER BY id DESC")
    suspend fun getAllProfiles(): List<V2rayProfile>

    @Query("SELECT * FROM v2ray_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Int): V2rayProfile?

    @Query("SELECT * FROM v2ray_profiles WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedProfile(): V2rayProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: V2rayProfile): Long

    @Update
    suspend fun updateProfile(profile: V2rayProfile)

    @Query("DELETE FROM v2ray_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)

    @Query("UPDATE v2ray_profiles SET isSelected = 0")
    suspend fun deselectAllProfiles()

    @Transaction
    suspend fun selectProfile(id: Int) {
        deselectAllProfiles()
        val profile = getProfileById(id)
        if (profile != null) {
            updateProfile(profile.copy(isSelected = true))
        }
    }

    // === SETTINGS ===
    @Query("SELECT value FROM vpn_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: VpnSetting)

    // === LOGS ===
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT 500")
    fun getAllLogsFlow(): Flow<List<ConnectionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConnectionLog)

    @Query("DELETE FROM connection_logs")
    suspend fun clearLogs()
}
