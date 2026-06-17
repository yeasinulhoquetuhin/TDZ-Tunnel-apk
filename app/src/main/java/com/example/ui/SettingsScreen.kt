package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.VpnViewModel
import com.example.ui.theme.glassmorphicBackground
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = com.example.LocalAppHaptic.current
    val dnsPrimary by viewModel.dnsPrimaryState.collectAsState()
    val dnsSecondary by viewModel.dnsSecondaryState.collectAsState()
    val routingMode by viewModel.routingModeState.collectAsState()

    var primaryDnsText by remember { mutableStateOf(dnsPrimary) }
    var secondaryDnsText by remember { mutableStateOf(dnsSecondary) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Sync state if databases update
    LaunchedEffect(dnsPrimary, dnsSecondary) {
        primaryDnsText = dnsPrimary
        secondaryDnsText = dnsSecondary
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Title Header
        Column {
            Text(
                text = "Control Center",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure custom routing filters, DNS hosts and system preferences",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Beautiful tab division for Routing Rules and general App Settings
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Routing Rules", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.AltRoute, contentDescription = "Routing Rules", modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Settings & UI", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings & UI", modifier = Modifier.size(20.dp)) }
            )
        }

        if (selectedTabIndex == 0) {
            // ================= ROUTING RULES CARD =================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AltRoute, "Routing Option", tint = MaterialTheme.colorScheme.primary)
                    Text("Outbound Routing Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Choose how the core handles outbound requests:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Routing mode options
                listOf(
                    Triple("PROXY", "Global Proxy", "All internet traffic (all apps) routes through the VPN tunnel."),
                    Triple("DIRECT", "Direct Connection", "Bypass VPN entirely for local/domestic traffic globally."),
                    Triple("BLOCK", "Ad-Block Filter", "Blocks tracking scripts and ads across all apps globally.")
                ).forEach { (modeKey, modeTitle, modeDesc) ->
                    val isSelected = routingMode == modeKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.triggerClick()
                                viewModel.updateRoutingMode(modeKey)
                            }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                haptic.triggerClick()
                                viewModel.updateRoutingMode(modeKey)
                            },
                            modifier = Modifier.testTag("routing_radio_$modeKey")
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(modeTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(modeDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // ================= CUSTOM DNS CARD =================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Dns, "DNS servers", tint = MaterialTheme.colorScheme.secondary)
                    Text("DNS Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Configure custom DNS addresses to prevent leaks and bypass censorship:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = primaryDnsText,
                    onValueChange = { primaryDnsText = it },
                    label = { Text("Primary DNS Server (IPv4 / DoH)") },
                    placeholder = { Text("e.g. 1.1.1.1") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("primary_dns_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = secondaryDnsText,
                    onValueChange = { secondaryDnsText = it },
                    label = { Text("Secondary DNS Server") },
                    placeholder = { Text("e.g. 8.8.4.4") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secondary_dns_input"),
                    singleLine = true
                )

                // Speed preset chips suggestions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Cloudflare" to ("1.1.1.1" to "1.0.0.1"),
                        "Google" to ("8.8.8.8" to "8.8.4.4"),
                        "AdGuard" to ("94.140.14.14" to "94.140.15.15")
                    ).forEach { (name, pair) ->
                        SuggestionChip(
                            onClick = {
                                haptic.triggerClick()
                                primaryDnsText = pair.first
                                secondaryDnsText = pair.second
                                Toast.makeText(context, "$name DNS preset selected", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text(name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // DNS Save button
                Button(
                    onClick = {
                        haptic.triggerClick()
                        if (primaryDnsText.isBlank() || secondaryDnsText.isBlank()) {
                            Toast.makeText(context, "DNS servers cannot be empty!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateDnsSettings(primaryDnsText.trim(), secondaryDnsText.trim())
                            Toast.makeText(context, "DNS settings saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_dns_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.SaveAlt, "Save DNS")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save DNS Settings", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // ================= APP SETTINGS CARD =================
        val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
        val connectOnBoot by viewModel.connectOnBoot.collectAsState()
        val themeColor by viewModel.themeColor.collectAsState()
        val themeModeSetting by viewModel.themeMode.collectAsState()

        val glowStyleSetting by viewModel.homeGlowStyle.collectAsState()
        val customHeaderSetting by viewModel.homeHeaderTitle.collectAsState()
        val borderStyleSetting by viewModel.cardBorderStyle.collectAsState()

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Settings, "App Settings Option", tint = MaterialTheme.colorScheme.tertiary)
                    Text("Application Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        haptic.triggerClick()
                        viewModel.setHapticsEnabled(!hapticsEnabled)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Haptic Feedback", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Enable structural vibration feedback for buttons.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = hapticsEnabled, onCheckedChange = {
                        haptic.triggerClick()
                        viewModel.setHapticsEnabled(it)
                    })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        haptic.triggerClick()
                        viewModel.setConnectOnBoot(!connectOnBoot)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Connect on Boot", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Automatically start VPN when device restarts.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = connectOnBoot, onCheckedChange = {
                        haptic.triggerClick()
                        viewModel.setConnectOnBoot(it)
                    })
                }
            }
        }

        // ================= APPEARANCE CARD =================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, "Theme SettingsOption", tint = MaterialTheme.colorScheme.tertiary)
                    Text("Appearance & Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text("Select Primary Palette Accent:", style = MaterialTheme.typography.labelMedium)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        "#0052FF" to "Blue",
                        "#FF3B30" to "Red",
                        "#34C759" to "Green",
                        "#FF9500" to "Orange",
                        "#AF52DE" to "Purple"
                    ).forEach { (hex, name) ->
                        val isSelected = themeColor == hex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.triggerClick()
                                    viewModel.setThemeColor(hex)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Theme Display Mode:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "SYSTEM" to "System Defaults",
                        "LIGHT" to "Light Mode",
                        "DARK" to "Dark Mode"
                    ).forEach { (mode, label) ->
                        val isSelected = themeModeSetting == mode
                        Button(
                            onClick = {
                                haptic.triggerClick()
                                viewModel.setThemeMode(mode)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ================= ADVANCED THEME & CUSTOMIZATION =================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tune, "Advanced Theme Option", tint = MaterialTheme.colorScheme.secondary)
                    Text("Advanced Core Customization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))

                // 1. Home screen Pulsing halo glow intensity options
                Column {
                    Text("Home Screen Pulsing Glow", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Customize background animated waves on connections", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "VIBRANT" to "Vibrant Ring",
                            "SOFT" to "Soft Echo",
                            "MINIMAL" to "Disabled"
                        ).forEach { (styleKey, label) ->
                            val isSelected = glowStyleSetting == styleKey
                            Button(
                                onClick = {
                                    haptic.triggerClick()
                                    viewModel.setHomeGlowStyle(styleKey)
                                    Toast.makeText(context, "Neon glow intensity set to $styleKey", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2. Custom App Title text customized over the main dashboard!
                Column {
                    Text("Dashboard Label Customization", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Custom title displayed on top of the Home Dashboard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customHeaderSetting,
                        onValueChange = {
                            viewModel.setHomeHeaderTitle(it)
                        },
                        placeholder = { Text("e.g. My Secure Tunnel") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Edit, "Edit Label") }
                    )
                }

                // 3. Card shape Corner Border Styling options
                Column {
                    Text("Surface Edge Style", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Set corners for Cards and Layout boundaries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ROUNDED" to "Curved Glass (Modern)",
                            "SHARP" to "Compact Slate (Retro)"
                        ).forEach { (styleKey, label) ->
                            val isSelected = borderStyleSetting == styleKey
                            Button(
                                onClick = {
                                    haptic.triggerClick()
                                    viewModel.setCardBorderStyle(styleKey)
                                    Toast.makeText(context, "Interface edge style changed!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ================= DEVELOPER PROFILE =================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, "Developer Info", tint = MaterialTheme.colorScheme.primary)
                    Text("Developer Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Lead Developer:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Yeasinul Hoque Tuhin",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Engineering Core:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Secure Tunnel Protocol",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Developer Website:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "https://tuhinbro.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

        // Diagnostic information card info
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, "Details Info", tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "DNS adjustments during an active session might require you to reconnect for changes to take effect.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
