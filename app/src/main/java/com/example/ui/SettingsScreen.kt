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
    val dnsPrimary by viewModel.dnsPrimaryState.collectAsState()
    val dnsSecondary by viewModel.dnsSecondaryState.collectAsState()
    val routingMode by viewModel.routingModeState.collectAsState()

    var primaryDnsText by remember { mutableStateOf(dnsPrimary) }
    var secondaryDnsText by remember { mutableStateOf(dnsSecondary) }

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
                text = "DNS & Routing",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure custom routing filters and DNS hosts",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }

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
                    text = "Choose how Xray core handles outbound requests:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Routing mode options
                listOf(
                    Triple("PROXY", "Proxy Global Core", "All internet traffic routes through the secure VPN tunnel."),
                    Triple("DIRECT", "Bypass Local Direct", "Local network and domestic sites bypass the VPN completely."),
                    Triple("BLOCK", "Ad-Block Filter", "Xray core automatically blocks ads and tracking scripts.")
                ).forEach { (modeKey, modeTitle, modeDesc) ->
                    val isSelected = routingMode == modeKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateRoutingMode(modeKey) }
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
                            onClick = { viewModel.updateRoutingMode(modeKey) },
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

        // ================= APP SETTINGS CARD =================
        val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
        val connectOnBoot by viewModel.connectOnBoot.collectAsState()
        val themeColor by viewModel.themeColor.collectAsState()

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
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setHapticsEnabled(!hapticsEnabled) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Haptic Feedback", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Enable structural vibration feedback for buttons.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = hapticsEnabled, onCheckedChange = { viewModel.setHapticsEnabled(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setConnectOnBoot(!connectOnBoot) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Connect on Boot", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Automatically start VPN when device restarts.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = connectOnBoot, onCheckedChange = { viewModel.setConnectOnBoot(it) })
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
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text("Select Primary Theme Color:", style = MaterialTheme.typography.labelMedium)
                
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
                                .clickable { viewModel.setThemeColor(hex) }
                        )
                    }
                }
            }
        }

        // ================= EXPORT SETTINGS CARD =================
        var showExportDialog by remember { mutableStateOf(false) }

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export as APK") },
                text = {
                    Text("Because you are using AI Studio without a PC, you can download the APK directly to your phone via GitHub:\n\n1. Ensure you have saved your configurations.\n2. Tap 'GitHub' at the top right of the AI Studio web interface to publish this project.\n3. The included GitHub Actions workflow will automatically build your signed APK.\n4. Open your GitHub Repository on your phone, go to 'Actions', click the latest workflow, and download the 'XRay-VPN-APK' artifact directly to your phone!")
                },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Got it")
                    }
                }
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SystemUpdateAlt, "Export Option", tint = MaterialTheme.colorScheme.primary)
                    Text("Export & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Bundle current app configurations and download as an installable APK package.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download APK")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export as APK", fontWeight = FontWeight.Bold)
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
