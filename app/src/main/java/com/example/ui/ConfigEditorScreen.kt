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
    var protocol by remember { mutableStateOf(editingProfile?.protocol ?: "VLESS") } // VMESS, VLESS, SHADOWSOCKS, TROJAN, SSH, RAW_JSON
    var address by remember { mutableStateOf(editingProfile?.address ?: "") }
    var portString by remember { mutableStateOf(editingProfile?.port?.toString() ?: "") }
    var uuidOrPassword by remember { mutableStateOf(editingProfile?.uuidOrPassword ?: "") }
    var transport by remember { mutableStateOf(editingProfile?.transport ?: "WS") } // TCP, WS, gRPC, mKCP
    var security by remember { mutableStateOf(editingProfile?.security ?: "TLS") } // None, TLS, XTLS, Reality
    var sni by remember { mutableStateOf(editingProfile?.sni ?: "") }
    var path by remember { mutableStateOf(editingProfile?.path ?: "") }
    var payload by remember { mutableStateOf(editingProfile?.payload ?: "") }
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
                    text = "Customization Panel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Row Block: Profile Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Connection Name") },
            placeholder = { Text("e.g. My Premium Server") },
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
                    "VMESS" -> "VMESS"
                    "VLESS" -> "VLESS"
                    "SHADOWSOCKS" -> "SHADOWSOCKS"
                    "TROJAN" -> "TROJAN"
                    "SSH" -> "SSH"
                    "RAW_JSON" -> "RAW JSON"
                    else -> protocol
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Core Protocol") },
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
                listOf("VLESS", "VMESS", "SHADOWSOCKS", "TROJAN", "SSH", "RAW_JSON").forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (option) {
                                    "VMESS" -> "VMESS"
                                    "VLESS" -> "VLESS"
                                    "SHADOWSOCKS" -> "SHADOWSOCKS"
                                    "TROJAN" -> "TROJAN"
                                    "SSH" -> "SSH"
                                    "RAW_JSON" -> "RAW JSON"
                                    else -> option
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            protocol = option
                            isProtocolDropdownExpanded = false

                            // Intelligent default setting for core parameters based on protocol
                            when (option) {
                                "SSH" -> {
                                    transport = "Direct"
                                    security = "NONE"
                                }
                                "SHADOWSOCKS" -> {
                                    transport = "TCP"
                                    security = "NONE"
                                }
                                else -> {
                                    transport = "WS"
                                    security = "TLS"
                                }
                            }

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
                          text = "JSON Code Editor",
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
                        placeholder = { Text("Declare full custom JSON object here...") },
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
            label = { Text("Server Address") },
            placeholder = { Text("e.g. sg.server.com") },
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
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Password toggle"
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("config_secret_input"),
                singleLine = true
            )

            // Dynamic transport / security selectors
            if (protocol == "VLESS" || protocol == "VMESS" || protocol == "TROJAN") {
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
            } else if (protocol == "SSH") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = when (transport) {
                            "Direct" -> "Direct"
                            "WS" -> "WS"
                            "STunnel4" -> "STunnel"
                            else -> transport
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("SSH Connection Mode") },
                        trailingIcon = {
                            IconButton(onClick = { isTransportDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Open options")
                            }
                        },
                        modifier = Modifier.clickable { isTransportDropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = isTransportDropdownExpanded,
                        onDismissRequest = { isTransportDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        listOf("Direct", "WS", "STunnel4").forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (option) {
                                            "Direct" -> "Direct"
                                            "WS" -> "WS"
                                            "STunnel4" -> "STunnel"
                                            else -> option
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                onClick = {
                                    transport = option
                                    isTransportDropdownExpanded = false
                                    security = if (option == "STunnel4" || option == "WS") "TLS" else "NONE"
                                }
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when (protocol) {
                                "SHADOWSOCKS" -> "Shadowsocks (TCP/UDP)"
                                else -> "Custom network transport handling."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            val showSNI = when (protocol) {
                "VLESS", "VMESS", "TROJAN" -> security != "NONE"
                "SSH" -> transport == "WS" || transport == "STunnel4"
                else -> false
            }

            val showPath = when (protocol) {
                "VLESS", "VMESS", "TROJAN" -> transport == "WS" || transport == "gRPC"
                "SSH" -> transport == "WS"
                else -> false
            }

            val showPayload = protocol == "SSH" && transport == "WS"

            if (showSNI || showPath || showPayload) {
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
                    if (showSNI) {
                        OutlinedTextField(
                            value = sni,
                            onValueChange = { sni = it },
                            label = { Text(if (protocol == "SSH" && transport == "STunnel4") "STunnel SNI" else "Server SNI / Host") },
                            placeholder = { Text("e.g. apple.com") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    if (showPath) {
                        OutlinedTextField(
                            value = path,
                            onValueChange = { path = it },
                            label = {
                                Text(
                                    when {
                                        protocol == "SSH" -> "Connection/WS Path"
                                        transport == "gRPC" -> "gRPC Service Name"
                                        else -> "WS Path"
                                    }
                                )
                            },
                            placeholder = {
                                Text(
                                    when {
                                        protocol == "SSH" -> "e.g. /ssh"
                                        transport == "gRPC" -> "e.g. MyService"
                                        else -> "e.g. /ray-tunnel"
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    } else if (showSNI) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                if (showPayload) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payload,
                        onValueChange = { payload = it },
                        label = { Text("HTTP Payload / Injection Header") },
                        placeholder = { Text("e.g. CONNECT [host_port] HTTP/1.1[crlf]Host: apple.com[crlf][crlf]") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("Supports standard [host], [port], [host_port], [crlf] macros.", style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
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
                    val typedPort = portString.toIntOrNull()
                    if (typedPort == null || typedPort <= 0 || typedPort > 65535) {
                        Toast.makeText(context, "A valid server port (1-65535) is required!", Toast.LENGTH_SHORT).show()
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
                    payload = payload.trim(),
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
