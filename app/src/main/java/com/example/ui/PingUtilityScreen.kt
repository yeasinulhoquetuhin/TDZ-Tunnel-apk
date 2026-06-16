package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.V2rayProfile
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusIdle
import com.example.viewmodel.VpnViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PingUtilityScreen(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val profiles by viewModel.profilesState.collectAsState()
    val pingingSet by viewModel.pingingProfiles.collectAsState()
    
    // Manage local checklist UI state
    val selectedIds = remember { mutableStateMapOf<Int, Boolean>() }
    var searchQuery by remember { mutableStateOf("") }
    
    // Synchronize newly added profiles with default selected if they aren't configured yet
    LaunchedEffect(profiles) {
        profiles.forEach { profile ->
            if (!selectedIds.containsKey(profile.id)) {
                selectedIds[profile.id] = true // Select by default initially
            }
        }
    }

    // Filter server list
    val filteredProfiles = remember(profiles, searchQuery) {
        profiles.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true) ||
            it.protocol.contains(searchQuery, ignoreCase = true)
        }
    }

    val isAnyPinging = pingingSet.isNotEmpty()

    // Breathing pulse for Radar Wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    // Compute Summary Stats
    val selectedProfiles = profiles.filter { selectedIds[it.id] == true }
    val testedProfiles = selectedProfiles.filter { it.pingMs > 0 }
    val averagePing = if (testedProfiles.isNotEmpty()) {
        testedProfiles.map { it.pingMs }.average().toInt()
    } else 0
    val timeoutCount = selectedProfiles.count { it.pingMs == -2 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Latency Test",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Measure real-time servers response times.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Animated Radar Scanner Pulse
            if (isAnyPinging) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.4f))
                    )
                    IconButton(
                        onClick = {},
                        enabled = false,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Pinging",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = {
                        val toPing = profiles.filter { selectedIds[it.id] == true }
                        if (toPing.isEmpty()) {
                            Toast.makeText(context, "Please select servers to test", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                Toast.makeText(context, "Testing ${toPing.size} servers...", Toast.LENGTH_SHORT).show()
                                toPing.forEach { profile ->
                                    viewModel.testProfilePing(profile)
                                    delay(100) // slight parallel offset
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("start_bulk_ping_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run All Test")
                }
            }
        }

        // 2. SUMMARY REPORT WIDGET
        if (selectedProfiles.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stat 1: Total Selected
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${selectedProfiles.size}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Divider segment
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                    )

                    // Stat 2: Avg latency
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val speedRating = when {
                            averagePing == 0 -> "Idle"
                            averagePing < 100 -> "Fast"
                            averagePing < 200 -> "Medium"
                            else -> "Slow"
                        }
                        Text(
                            text = if (averagePing > 0) "${averagePing}ms" else "-- ms",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (averagePing > 0 && averagePing < 100) StatusConnected else if (averagePing >= 200) StatusDisconnected else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Avg Ping ($speedRating)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Divider segment
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                    )

                    // Stat 3: Packets Lost / Timeout Server count
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$timeoutCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (timeoutCount > 0) StatusDisconnected else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Timeout",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. CONTROL BAR: Search, Toggle checkboxes, bulk indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Search inputs
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search servers (name, protocol)...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("ping_search_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            )

            // Select all trigger
            Button(
                onClick = {
                    val allSelected = filteredProfiles.all { selectedIds[it.id] == true }
                    filteredProfiles.forEach {
                        selectedIds[it.id] = !allSelected
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Select All",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val isSelectAll = filteredProfiles.isNotEmpty() && filteredProfiles.all { selectedIds[it.id] == true }
                Text(
                    text = if (isSelectAll) "Deselect All" else "Select All",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. LIST FORMAT DISPLAY (Satisfies requirement explicitly)
        if (filteredProfiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = "No configs found",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (profiles.isEmpty()) "No servers found" else "No matching servers found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProfiles, key = { it.id }) { profile ->
                    val isChecked = selectedIds[profile.id] == true
                    val isPingingNow = pingingSet.contains(profile.id)

                    // Card borders based on test status
                    val testStatusBorder = when {
                        isPingingNow -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        profile.pingMs == -2 -> StatusDisconnected.copy(alpha = 0.4f)
                        profile.pingMs in 1..99 -> StatusConnected.copy(alpha = 0.4f)
                        profile.pingMs >= 100 -> StatusIdle.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, testStatusBorder, RoundedCornerShape(16.dp))
                            .clickable { selectedIds[profile.id] = !isChecked },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Checkbox selector
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { selectedIds[profile.id] = it },
                                modifier = Modifier.testTag("ping_checkbox_${profile.id}")
                            )

                            // Main configuration metadata info
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Protocol pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = profile.protocol,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    // Address and port tag
                                    Text(
                                        text = "${profile.address}:${profile.port}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Server title label Bengali & English
                                Text(
                                    text = profile.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // LATENCY BADGE ELEMENT (Color-coded visual outcomes)
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                if (isPingingNow) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 1.8.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Testing...",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    val (pingText, badgeColor) = when {
                                        profile.pingMs == -1 -> "Idle" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        profile.pingMs == -2 -> "Timeout" to StatusDisconnected
                                        profile.pingMs < 100 -> "${profile.pingMs} ms (Fast)" to StatusConnected
                                        profile.pingMs < 200 -> "${profile.pingMs} ms (Good)" to StatusIdle
                                        else -> "${profile.pingMs} ms (Slow)" to Color(0xFFF97316)
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(badgeColor.copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = pingText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor
                                            )
                                        }

                                        // Mini linear spark bars showing progress rating
                                        if (profile.pingMs > 0) {
                                            val barRatio = (profile.pingMs / 300f).coerceIn(0.1f, 1.0f)
                                            Box(
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(barRatio)
                                                        .background(badgeColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. BULK ACTION FOOTER BUTTONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Deselect all
            OutlinedButton(
                onClick = {
                    selectedIds.clear()
                    profiles.forEach { selectedIds[it.id] = false }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Deselect")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Deselect All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Run bulk scan
            Button(
                onClick = {
                    val toPing = profiles.filter { selectedIds[it.id] == true }
                    if (toPing.isEmpty()) {
                        Toast.makeText(context, "Please select servers to test", Toast.LENGTH_SHORT).show()
                    } else {
                        scope.launch {
                            Toast.makeText(context, "Testing ${toPing.size} servers...", Toast.LENGTH_SHORT).show()
                            toPing.forEach { profile ->
                                viewModel.testProfilePing(profile)
                                delay(120) // slight delay sequential progression
                            }
                            Toast.makeText(context, "Test completed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isAnyPinging,
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("bulk_ping_test_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = "Start Ping Scan")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ping Selected", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
