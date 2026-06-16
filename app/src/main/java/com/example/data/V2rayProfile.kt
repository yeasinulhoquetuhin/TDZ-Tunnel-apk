package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "v2ray_profiles")
data class V2rayProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val protocol: String = "VMESS", // VMESS, VLESS, SHADOWSOCKS, TROJAN, SOCKS
    val address: String = "",
    val port: Int = 443,
    val uuidOrPassword: String = "",
    val transport: String = "WS", // TCP, WS, gRPC, mKCP, QUIC
    val security: String = "TLS", // None, TLS, XTLS, Reality
    val sni: String = "",
    val path: String = "",
    val rawJson: String = "", // Holds full custom Xray JSON configuration if user edits RAW config!
    val isSelected: Boolean = false,
    val pingMs: Int = -1 // -1 means not tested, -2 means timeout, >0 is latency in ms
) : Serializable
