package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vpn_settings")
data class VpnSetting(
    @PrimaryKey val key: String,
    val value: String
)
