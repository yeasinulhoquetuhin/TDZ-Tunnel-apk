package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.LocalAppHaptic
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.glassmorphicBackground
import com.example.data.V2rayProfile
import com.example.engine.VpnEngine
import com.example.viewmodel.VpnViewModel

@Composable
fun HomeScreen(
    viewModel: VpnViewModel,
    onNavigateToProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalAppHaptic.current
    val scope = rememberCoroutineScope()
    
    val status by viewModel.vpnStatus.collectAsState()
    val dlSpeed by viewModel.downloadSpeedKb.collectAsState()
    val ulSpeed by viewModel.uploadSpeedKb.collectAsState()
    val ping by viewModel.tunnelPingMs.collectAsState()
    val profile by viewModel.currentSelectedProfile.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val statusColor = when (status) {
        VpnEngine.ConnectionStatus.CONNECTED -> Color(0xFF00A36C) // Classic Emerald Green
        VpnEngine.ConnectionStatus.CONNECTING -> Color(0xFFF59E0B) // Warm Amber
        else -> Color(0xFFDC2626) // Ruby Red
    }

    // Moving window history records for the real-time Recharts-like Speed tracker
    val downloadHistory = remember { mutableStateListOf<Float>() }
    val uploadHistory = remember { mutableStateListOf<Float>() }
    val maxSamples = 20

    // Live update the speed telemetry histories
    LaunchedEffect(dlSpeed, ulSpeed, status) {
        if (status == VpnEngine.ConnectionStatus.CONNECTED) {
            downloadHistory.add(dlSpeed)
            uploadHistory.add(ulSpeed)
            if (downloadHistory.size > maxSamples) {
                downloadHistory.removeAt(0)
            }
            if (uploadHistory.size > maxSamples) {
                uploadHistory.removeAt(0)
            }
        } else {
            // Fill with smooth idle flatlines on disconnect state
            if (downloadHistory.size < maxSamples) {
                while (downloadHistory.size < maxSamples) {
                    downloadHistory.add(0f)
                }
            } else {
                downloadHistory.clear()
                repeat(maxSamples) { downloadHistory.add(0f) }
            }
            
            if (uploadHistory.size < maxSamples) {
                while (uploadHistory.size < maxSamples) {
                    uploadHistory.add(0f)
                }
            } else {
                uploadHistory.clear()
                repeat(maxSamples) { uploadHistory.add(0f) }
            }
        }
    }

    // Breathing pulse for connection rings
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale2"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. HEADER (Styled like mock)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header brand icon block
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Brand Shield",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "V2ray Ultra",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Secure VPN Tunnel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // User profile outline status dot nodes
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = 2.dp, y = 2.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                    )
                }
            }
        }

        // 2. MAIN POWER CENTER (Fixed height for scrolling)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Concentric glow rings
                val isConnectingOrConnected = status == VpnEngine.ConnectionStatus.CONNECTED || status == VpnEngine.ConnectionStatus.CONNECTING
                val ringColor = if (isConnectingOrConnected) statusColor else primaryColor

                Box(
                    modifier = Modifier
                        .size((180f * if (isConnectingOrConnected) pulseScale2 else 1f).dp)
                        .clip(CircleShape)
                        .background(ringColor.copy(alpha = 0.08f))
                )

                Box(
                    modifier = Modifier
                        .size((140f * if (isConnectingOrConnected) pulseScale1 else 1f).dp)
                        .clip(CircleShape)
                        .background(ringColor.copy(alpha = 0.15f))
                )

                // Central Active Button
                Button(
                    onClick = {
                        profile?.let { 
                            scope.launch {
                                haptic.triggerConnectionPulse()
                            }
                            viewModel.toggleVpnConnection(it) 
                        }
                    },
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(elevation = 16.dp, shape = CircleShape, spotColor = ringColor, ambientColor = ringColor)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                        .testTag("vpn_toggle_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnectingOrConnected) statusColor else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        contentColor = if (isConnectingOrConnected) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = CircleShape,
                    enabled = profile != null
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (status == VpnEngine.ConnectionStatus.CONNECTED) "Active" else if (status == VpnEngine.ConnectionStatus.CONNECTING) "Connecting" else "Connect",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Connection state details text
            Text(
                text = when (status) {
                    VpnEngine.ConnectionStatus.CONNECTED -> "CONNECTED"
                    VpnEngine.ConnectionStatus.CONNECTING -> "CONNECTING..."
                    else -> "DISCONNECTED"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (status == VpnEngine.ConnectionStatus.CONNECTED) statusColor else MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = when (status) {
                    VpnEngine.ConnectionStatus.CONNECTED -> "Your device connection is secure."
                    VpnEngine.ConnectionStatus.CONNECTING -> "Establishing tunnel channel..."
                    else -> if (profile == null) "Import a server profile to connect" else "Tap power icon to secure your session"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, start = 32.dp, end = 32.dp)
            )
        }

        // 3. STAT GRID (2x2 rounded cards formatted beautifully with Clean Minimalism layout)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Protocol
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Protocol",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PROTOCOL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = profile?.protocol ?: "VLESS/Xray",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Card 2: Ping/Latency
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Latency",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "LATENCY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (ping > 0) "${ping}ms" else "0ms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 3: Download Speed Info
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Download Stats",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "DOWNLOAD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (dlSpeed > 1024) String.format("%.1f MB/s", dlSpeed / 1024f) else String.format("%.0f KB/s", dlSpeed),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Card 4: Upload Speed Info
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Upload Stats",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "UPLOAD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (ulSpeed > 1024) String.format("%.1f MB/s", ulSpeed / 1024f) else String.format("%.0f KB/s", ulSpeed),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 4. REAL-TIME SPEED RECHARTS STYLE CHART PANEL (Requested!)
        SpeedChart(
            downloadHistory = downloadHistory,
            uploadHistory = uploadHistory,
            modifier = Modifier.fillMaxWidth()
        )

        // 5. SERVER LOCATION/pill list trigger (Styled exactly like mockup wide pill)
        Card(
            onClick = { onNavigateToProfiles() },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Location",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Selected VPN Server",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = profile?.name ?: "No server selected",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate Profiles",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SpeedChart(
    downloadHistory: List<Float>,
    uploadHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val uploadColor = Color(0xFFF59E0B) // Warm Amber Color
    val gridLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

    Card(
        modifier = modifier.fillMaxWidth().glassmorphicBackground(MaterialTheme.colorScheme.primary, 0.05f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            // Header row inside Speed Dash Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00A36C)) // Glowing Green Heartbeat Node
                    )
                    Text(
                        text = "Real-time Traffic Monitor",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Dynamic metric Peak value tracker
                val maxDown = downloadHistory.maxOrNull() ?: 0f
                val maxUp = uploadHistory.maxOrNull() ?: 0f
                val peekVal = maxOf(maxDown, maxUp)
                val peekLabel = if (peekVal > 1024) String.format("%.1f MB/s", peekVal / 1024f) else String.format("%.0f KB/s", peekVal)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Peak: $peekLabel",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Spacer(modifier = Modifier.height(4.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Plotting Sandbox bounds
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    
                    // Safely define peak boundaries to avoid zero divide issues
                    val maxVal = maxOf(
                        downloadHistory.maxOrNull() ?: 100f,
                        uploadHistory.maxOrNull() ?: 100f,
                        100f
                    )

                    // Draw Horizontal Gridlines mimicking Recharts Cartesians
                    val gridLinesCount = 3
                    for (i in 0..gridLinesCount) {
                        val gridY = height * i / gridLinesCount
                        drawLine(
                            color = gridLineColor,
                            start = androidx.compose.ui.geometry.Offset(0f, gridY),
                            end = androidx.compose.ui.geometry.Offset(width, gridY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    }

                    // Plot Download Path (Smooth gradient styled fill area)
                    if (downloadHistory.isNotEmpty()) {
                        val dlPath = Path()
                        val dlAreaPath = Path()
                        val stepX = width / (downloadHistory.size - 1).coerceAtLeast(1)

                        // Smooth interpolation (cubic bezier)
                        downloadHistory.forEachIndexed { index, value ->
                            val x = index * stepX
                            val ratio = value / maxVal
                            val y = height - (ratio * height * 0.85f)

                            if (index == 0) {
                                dlPath.moveTo(x, y)
                                dlAreaPath.moveTo(x, height)
                                dlAreaPath.lineTo(x, y)
                            } else {
                                val prevX = (index - 1) * stepX
                                val prevVal = downloadHistory[index - 1]
                                val prevY = height - ((prevVal / maxVal) * height * 0.85f)
                                
                                val cp1x = prevX + (x - prevX) / 2
                                val cp1y = prevY
                                val cp2x = prevX + (x - prevX) / 2
                                val cp2y = y
                                
                                dlPath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
                                dlAreaPath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
                            }

                            if (index == downloadHistory.lastIndex) {
                                dlAreaPath.lineTo(x, height)
                                dlAreaPath.close()
                            }
                        }

                        // Fill translucent blue gradient under path
                        drawPath(
                            path = dlAreaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.22f),
                                    primaryColor.copy(alpha = 0.00f)
                                )
                            )
                        )

                        // Outline stroke line
                        drawPath(
                            path = dlPath,
                            color = primaryColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // Plot Upload Path (Smooth styled dashes pattern)
                    if (uploadHistory.isNotEmpty()) {
                        val ulPath = Path()
                        val ulAreaPath = Path()
                        val stepX = width / (uploadHistory.size - 1).coerceAtLeast(1)

                        uploadHistory.forEachIndexed { index, value ->
                            val x = index * stepX
                            val ratio = value / maxVal
                            val y = height - (ratio * height * 0.85f)

                            if (index == 0) {
                                ulPath.moveTo(x, y)
                                ulAreaPath.moveTo(x, height)
                                ulAreaPath.lineTo(x, y)
                            } else {
                                val prevX = (index - 1) * stepX
                                val prevVal = uploadHistory[index - 1]
                                val prevY = height - ((prevVal / maxVal) * height * 0.85f)
                                
                                val cp1x = prevX + (x - prevX) / 2
                                val cp1y = prevY
                                val cp2x = prevX + (x - prevX) / 2
                                val cp2y = y
                                
                                ulPath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
                                ulAreaPath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
                            }

                            if (index == uploadHistory.lastIndex) {
                                ulAreaPath.lineTo(x, height)
                                ulAreaPath.close()
                            }
                        }

                        // Fill translucent warm amber under path
                        drawPath(
                            path = ulAreaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    uploadColor.copy(alpha = 0.16f),
                                    uploadColor.copy(alpha = 0.00f)
                                )
                            )
                        )

                        // Outline stroke line (dashed style for distinct contrast)
                        drawPath(
                            path = ulPath,
                            color = uploadColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 5f), 0f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Graph legend keys row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DL item
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Text(
                        text = "Download",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // UL item
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(uploadColor)
                    )
                    Text(
                        text = "Upload",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
