package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.VpnViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.logsState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Log console Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Diagnostic Console",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real-time process and tunnel routing logs",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Copy full debug terminal dump logic
                IconButton(
                    onClick = {
                        if (logs.isNotEmpty()) {
                            val dump = logs.joinToString("\n") { logItem ->
                                "[${sdf.format(Date(logItem.timestamp))}][${logItem.level}] ${logItem.message}"
                            }
                            clipboardManager.setText(AnnotatedString(dump))
                            Toast.makeText(context, "Logs dump copied to clipboard!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No logs to copy", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy all logs dump",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Clear logs histories button
                IconButton(
                    onClick = { viewModel.clearLogHistory() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear all console histories",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Real terminal visual display area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF070B11)) // Pitch deep cosmic space black
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            if (logs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Empty Terminal",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Terminal Session Idle",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Connect a VPN server to view diagnostic routing data here.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("terminal_logs_column"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true // Show newest at the bottom so it flows like a standard terminal!
                ) {
                    items(logs) { logItem ->
                        val logColor = when (logItem.level) {
                            "SUCCESS" -> Color(0xFF00FF41) // Emerald Neon Green
                            "ERROR" -> Color(0xFFEF5350)   // Danger Coral Red
                            "DEBUG" -> Color(0xFFFFCC00)   // Amber Alert debug Yellow
                            else -> Color(0xFFE2E8F0)      // Slate Grey White for INFO
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "[${sdf.format(Date(logItem.timestamp))}] ",
                                color = Color(0xFF475569), // Dim time prefix
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = logItem.message,
                                color = logColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
