package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.V2rayProfile
import com.example.viewmodel.VpnViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigEditorScreen(
    viewModel: VpnViewModel,
    editingProfile: V2rayProfile?,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNewProfile = editingProfile == null

    // Fields
    var name by remember { mutableStateOf(editingProfile?.name ?: "New Server") }
    var protocol by remember { mutableStateOf(editingProfile?.protocol ?: "VLESS") } // VMESS, VLESS, SHADOWSOCKS, TROJAN, SSH, WIREGUARD, RAW_JSON
    var address by remember { mutableStateOf(editingProfile?.address ?: "") }
    var portString by remember { mutableStateOf(editingProfile?.port?.toString() ?: "443") }
    var uuidOrPassword by remember { mutableStateOf(editingProfile?.uuidOrPassword ?: "") }
    var transport by remember { mutableStateOf(editingProfile?.transport ?: "WS") } // TCP, WS, gRPC, mKCP
    var security by remember { mutableStateOf(editingProfile?.security ?: "TLS") } // None, TLS, XTLS, Reality
    var sni by remember { mutableStateOf(editingProfile?.sni ?: "") }
    var path by remember { mutableStateOf(editingProfile?.path ?: "") }
    var rawJson by remember { mutableStateOf(editingProfile?.rawJson ?: "") }

    var isPasswordVisible by remember { mutableStateOf(false) }

    // Dropdowns triggers
    var isProtocolDropdownExpanded by remember { mutableStateOf(false) }
    var isTransportDropdownExpanded by remember { mutableStateOf(false) }
    var isSecurityDropdownExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation & Status bar title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBackToList,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Column {
                Text(
                    text = if (isNewProfile) "Create Custom Profile" else "Customize Settings",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Xray Customization Panel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Row Block: Profile Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Connection Label Name") },
            placeholder = { Text("e.g. Singapore HighSpeed VLESS") },
            leadingIcon = { Icon(Icons.Default.Label, "Name") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("config_name_input"),
            singleLine = true
        )

        // Dropdown Selector: Protocol Mode Menu selection
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = when (protocol) {
                    "VMESS" -> "VMess Protocol (Standard proxy node)"
                    "VLESS" -> "VLess Protocol (Lightweight zero-encryption)"
                    "SHADOWSOCKS" -> "Shadowsocks Core (AEAD ciphers)"
                    "TROJAN" -> "Trojan (TLS mimic proxy)"
                    "SSH" -> "SSH Tunnel"
                    "WIREGUARD" -> "Wireguard Protocol"
                    "RAW_JSON" -> "⚡ Custom Xray Core Raw Config (EDIT JSON CODE)"
                    else -> protocol
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("VPN Core Protocol") },
                leadingIcon = { Icon(Icons.Default.SettingsSuggest, "Protocol selector") },
                trailingIcon = {
                    IconButton(onClick = { isProtocolDropdownExpanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, "Open dropdown")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isProtocolDropdownExpanded = true }
            )

            DropdownMenu(
                expanded = isProtocolDropdownExpanded,
                onDismissRequest = { isProtocolDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                listOf("VLESS", "VMESS", "SHADOWSOCKS", "TROJAN", "SSH", "WIREGUARD", "RAW_JSON").forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (option) {
                                    "RAW_JSON" -> "⚡ RAW JSON Xray config (Direct Code Edit)"
                                    else -> "$option Protocol"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            protocol = option
                            isProtocolDropdownExpanded = false

                            // auto fill json placeholder if empty
                            if (option == "RAW_JSON" && rawJson.isBlank()) {
                                rawJson = """{
  "log": { "loglevel": "warning" },
  "dns": { "servers": [ "1.1.1.1", "8.8.8.8" ] },
  "inbounds": [],
  "outbounds": [
    {
      "protocol": "vless",
      "settings": {
        "vnext": [{
          "address": "my-server.com",
          "port": 443,
          "users": [{ "id": "00000000-0000", "encryption": "none" }]
        }]
      },
      "streamSettings": {
        "network": "ws",
        "security": "tls",
        "wsSettings": { "path": "/graphql" }
      }
    }
  ]
}"""
                            }
                        }
                    )
                }
            }
        }

        // Conditional Layout Rendering
        if (protocol == "RAW_JSON") {
            // CODE EDITOR LAYOUT: RAW CODE ACCORDING TO USER REQUIREMENTS
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                          text = "Xray Core JSON Code Editor",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary,
                          fontWeight = FontWeight.Bold
                        )
                        
                        TextButton(
                            onClick = {
                                rawJson = """{
  "log": { "loglevel": "warning" },
  "dns": { "servers": [ "8.8.8.8", "8.8.4.4" ] },
  "outbounds": [
    {
      "protocol": "trojan",
      "settings": {
        "servers": [{
          "address": "fra.trojanservice.net",
          "port": 443,
          "password": "my-trojan-password"
        }]
      },
      "streamSettings": { "network": "tcp", "security": "tls" }
    }
  ]
}"""
                            }
                        ) {
                            Text("Reset Preset Layout", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = rawJson,
                        onValueChange = { rawJson = it },
                        placeholder = { Text("Declare full custom Xray JSON object here...") },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF00FF41) // Cool Coding terminal matrix feel
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp)
                            .background(Color(0xFF00050B)),
                        maxLines = 30,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }
            }
        } else {
            // PARAMETERS FORM FIELDS
            Text(
                text = "Server Configuration Details & Parameters",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Server Host / IP") },
                    placeholder = { Text("sg.server.com or 12.345...") },
                    leadingIcon = { Icon(Icons.Default.Dns, "IP") },
                    modifier = Modifier
                        .weight(1.7f)
                        .testTag("config_address_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = portString,
                    onValueChange = { portString = it },
                    label = { Text("Port") },
                    placeholder = { Text("443") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("config_port_input"),
                    singleLine = true
                )
            }

            // UUID / Secret field with custom generate option!
            OutlinedTextField(
                value = uuidOrPassword,
                onValueChange = { uuidOrPassword = it },
                label = {
                    Text(
                        if (protocol == "SHADOWSOCKS" || protocol == "TROJAN" || protocol == "SSH") "Server Password"
                        else "UUID / User Token"
                    )
                },
                placeholder = { Text("v2ray-uuid-or-secret-handshake") },
                leadingIcon = { Icon(Icons.Default.VpnKey, "Secret ID") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (protocol == "VLESS" || protocol == "VMESS") {
                            IconButton(onClick = {
                                uuidOrPassword = UUID.randomUUID().toString()
                                Toast.makeText(context, "Random UUID Generated!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Refresh, "Auto-Generate UUID")
                            }
                        }
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Password toggle"
                            )
                        }
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("config_secret_input"),
                singleLine = true
            )

            // Dynamic transport / security selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dropdown transport network channel
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = transport,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transport Network") },
                        trailingIcon = {
                            IconButton(onClick = { isTransportDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Open details")
                            }
                        },
                        modifier = Modifier.clickable { isTransportDropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = isTransportDropdownExpanded,
                        onDismissRequest = { isTransportDropdownExpanded = false }
                    ) {
                        listOf("WS", "gRPC", "TCP", "mKCP", "QUIC").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    transport = option
                                    isTransportDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Dropdown security TLS layers
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = security,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Security Layer") },
                        trailingIcon = {
                            IconButton(onClick = { isSecurityDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Open details")
                            }
                        },
                        modifier = Modifier.clickable { isSecurityDropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = isSecurityDropdownExpanded,
                        onDismissRequest = { isSecurityDropdownExpanded = false }
                    ) {
                        listOf("TLS", "Reality", "XTLS", "NONE").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    security = option
                                    isSecurityDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Advanced custom fields SNI / Websocket Path
            Text(
                text = "Advanced Transmission Settings (Optional)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = sni,
                    onValueChange = { sni = it },
                    label = { Text("Server SNI / Host") },
                    placeholder = { Text("e.g. apple.com") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("WS/gRPC Path") },
                    placeholder = { Text("e.g. /ray-tunnel") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saving buttons block
        Button(
            onClick = {
                // Validation constraints checks
                if (name.isBlank()) {
                    Toast.makeText(context, "Profile name is required!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (protocol == "RAW_JSON") {
                    if (rawJson.isBlank() || !rawJson.trim().startsWith("{")) {
                        Toast.makeText(context, "Invalid RAW JSON block!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                } else {
                    if (address.isBlank()) {
                        Toast.makeText(context, "Server host or IP is required!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                }

                val targetPort = portString.toIntOrNull() ?: 443

                val resultProfile = V2rayProfile(
                    id = editingProfile?.id ?: 0,
                    name = name.trim(),
                    protocol = protocol,
                    address = address.trim(),
                    port = targetPort,
                    uuidOrPassword = uuidOrPassword.trim(),
                    transport = transport,
                    security = security,
                    sni = sni.trim(),
                    path = path.trim(),
                    rawJson = if (protocol == "RAW_JSON") rawJson.trim() else "",
                    isSelected = editingProfile?.isSelected ?: false,
                    pingMs = editingProfile?.pingMs ?: -1
                )

                viewModel.addOrUpdateProfile(resultProfile)
                onBackToList()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_config_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Save, "Save Parameters")
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE CONFIGURATION", fontWeight = FontWeight.Bold)
        }
    }
}
