package com.example.ui

import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.glassmorphicBackground
import com.example.data.V2rayProfile
import com.example.engine.VpnEngine
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusIdle
import com.example.viewmodel.VpnViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfilesScreen(
    viewModel: VpnViewModel,
    onNavigateToEditor: (V2rayProfile?) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profilesState.collectAsState()
    val pingingSet by viewModel.pingingProfiles.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val options = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
    }
    val scanner = remember(context) {
        GmsBarcodeScanning.getClient(context, options)
    }

    var importText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Servers list, 1 = Ping utility

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rounded sub-tab segment switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { selectedSubTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Servers",
                        tint = if (selectedSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Servers",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { selectedSubTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Ping Tester",
                        tint = if (selectedSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ping Tester",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (selectedSubTab == 1) {
            PingUtilityScreen(
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        } else {
        // Upper Title block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VPN Servers",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add or update configuration keys.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            val value = barcode.rawValue
                            if (!value.isNullOrBlank()) {
                                importText = value
                                showImportDialog = true
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Scan failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }

                IconButton(
                    onClick = { viewModel.testAllProfilesPing() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Test all ping",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { showImportDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPasteGo,
                        contentDescription = "Import from URL",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Custom Share-Link Quick Importer Bar preview
        if (showImportDialog) {
            Card(
                modifier = Modifier.fillMaxWidth().glassmorphicBackground(MaterialTheme.colorScheme.primary, 0.08f),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Import configuration (vmess, vless, ss, raw json):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = importText,
                            onValueChange = { importText = it },
                            placeholder = { Text("vmess://, vless:// or raw JSON", fontSize = 12.sp) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("import_link_input"),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        importText = clip
                                        Toast.makeText(context, "Copied from Clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.ContentPaste, "Paste Link")
                                }
                            }
                        )

                        Button(
                            onClick = {
                                if (importText.isNotBlank()) {
                                    val success = viewModel.importConfigByShareLink(importText)
                                    if (success) {
                                        Toast.makeText(context, "Successfully imported!", Toast.LENGTH_LONG).show()
                                        importText = ""
                                        showImportDialog = false
                                    } else {
                                        Toast.makeText(context, "Error: Invalid link format!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Import")
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showImportDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Empty State Handler
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "No Servers",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "No configurations found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tap the plus (+) button to add manually, or paste a vmess/vless link above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Lazy configurations scroll
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(profiles, key = { it.id }) { item ->
                    ProfileCard(
                        profile = item,
                        isSelected = item.isSelected,
                        isPinging = pingingSet.contains(item.id),
                        onSelect = { viewModel.selectProfile(item) },
                        onPing = { viewModel.testProfilePing(item) },
                        onEdit = { onNavigateToEditor(item) },
                        onDelete = { viewModel.deleteProfile(item) },
                        onExportShare = {
                            val shareUri = VpnEngine.exportToShareLink(item)
                            clipboardManager.setText(AnnotatedString(shareUri))
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // manual config additions launcher fab
        FloatingActionButton(
            onClick = { onNavigateToEditor(null) },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .testTag("add_profile_fab")
        ) {
            Icon(Icons.Default.Add, "New Configuration Code Profile", tint = MaterialTheme.colorScheme.onPrimary)
        }
        }
    }
}

@Composable
fun ProfileCard(
    profile: V2rayProfile,
    isSelected: Boolean,
    isPinging: Boolean,
    onSelect: () -> Unit,
    onPing: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExportShare: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val containerBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = containerBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Protocol Tag & Ping & Select Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    spacedBy = 6.dp,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Protocol Chip branding
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (profile.protocol) {
                                    "VMESS" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    "VLESS" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    "SHADOWSOCKS" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    else -> Color.Gray.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = profile.protocol,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (profile.protocol) {
                                "VMESS" -> MaterialTheme.colorScheme.primary
                                "VLESS" -> MaterialTheme.colorScheme.secondary
                                "SHADOWSOCKS" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Transport tag details
                    Text(
                        text = "[${profile.transport}]",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Latency Ping Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPinging) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                        Text("Pinging", style = MaterialTheme.typography.labelSmall, color = StatusIdle)
                    } else {
                        val pingText = when {
                            profile.pingMs == -1 -> "Check"
                            profile.pingMs == -2 -> "Timeout"
                            else -> "${profile.pingMs} ms"
                        }
                        val pingColor = when {
                            profile.pingMs == -1 -> MaterialTheme.colorScheme.primary
                            profile.pingMs == -2 -> StatusDisconnected
                            profile.pingMs < 100 -> StatusConnected
                            profile.pingMs < 200 -> StatusIdle
                            else -> StatusDisconnected
                        }

                        Text(
                            text = pingText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = pingColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onPing() }
                                .background(pingColor.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    // Is active circle
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Server Name
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Row 3: IP port display
            Text(
                text = "${profile.address}:${profile.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Client tools (Share, Edit, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExportShare, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Config parameters",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete profile configuration",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Extension to avoid Row scope imports if needed helper
@Composable
private fun Row(
    spacedBy: androidx.compose.ui.unit.Dp,
    verticalAlignment: Alignment.Vertical,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacedBy),
        verticalAlignment = verticalAlignment,
        content = content
    )
}
