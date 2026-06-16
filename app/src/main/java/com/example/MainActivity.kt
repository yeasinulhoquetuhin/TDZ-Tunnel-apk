package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.V2rayProfile
import com.example.ui.AiCompanionScreen
import com.example.ui.ConfigEditorScreen
import com.example.ui.HomeScreen
import com.example.ui.LogsScreen
import com.example.ui.ProfilesScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VpnViewModel

val LocalAppHaptic = compositionLocalOf<com.example.utils.AppHapticManager> {
    error("No AppHapticManager provided")
}

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<VpnViewModel>()

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeColorHex by viewModel.themeColor.collectAsState()
            val hapticEnabled by viewModel.hapticsEnabled.collectAsState()

            val view = androidx.compose.ui.platform.LocalView.current
            val appHaptic = remember(view, hapticEnabled) {
                com.example.utils.AppHapticManager(view, isEnabled = { hapticEnabled })
            }

            CompositionLocalProvider(LocalAppHaptic provides appHaptic) {
                MyApplicationTheme(primaryColorHex = themeColorHex) {
                    // Tracking states for manual config editor
                    var isEditorActive by remember { mutableStateOf(false) }
                var editingProfileTarget by remember { mutableStateOf<V2rayProfile?>(null) }

                // Bottom Tab tracking state
                var currentTab by remember { mutableStateOf("home") } // home, profiles, dns, ai, logs

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Display bottom bar only if we are not actively in the code editor screen!
                        if (!isEditorActive) {
                            NavigationBar(
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 2.dp)
                                    .navigationBarsPadding()
                                    .clip(RoundedCornerShape(32.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                                    .testTag("app_bottom_bar"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == "home",
                                    onClick = {
                                        appHaptic.triggerClick()
                                        currentTab = "home"
                                    },
                                    icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = "Tunnel") },
                                    label = { Text("Home", fontSize = 10.sp) },
                                    modifier = Modifier.testTag("nav_tab_home")
                                )

                                NavigationBarItem(
                                    selected = currentTab == "profiles",
                                    onClick = { 
                                        appHaptic.triggerClick()
                                        currentTab = "profiles" 
                                    },
                                    icon = { Icon(Icons.Default.CloudQueue, contentDescription = "Config Servers") },
                                    label = { Text("Servers", fontSize = 10.sp) },
                                    modifier = Modifier.testTag("nav_tab_profiles")
                                )

                                NavigationBarItem(
                                    selected = currentTab == "dns",
                                    onClick = { 
                                        appHaptic.triggerClick()
                                        currentTab = "dns" 
                                    },
                                    icon = { Icon(Icons.Default.AltRoute, contentDescription = "DNS & Rules") },
                                    label = { Text("Rules", fontSize = 10.sp) },
                                    modifier = Modifier.testTag("nav_tab_dns")
                                )

                                NavigationBarItem(
                                    selected = currentTab == "ai",
                                    onClick = { 
                                        appHaptic.triggerClick()
                                        currentTab = "ai" 
                                    },
                                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Help") },
                                    label = { Text("AI", fontSize = 10.sp) },
                                    modifier = Modifier.testTag("nav_tab_ai")
                                )

                                NavigationBarItem(
                                    selected = currentTab == "logs",
                                    onClick = { 
                                        appHaptic.triggerClick()
                                        currentTab = "logs" 
                                    },
                                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal Logs") },
                                    label = { Text("Logs", fontSize = 10.sp) },
                                    modifier = Modifier.testTag("nav_tab_logs")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = isEditorActive,
                            transitionSpec = {
                                if (targetState) {
                                    slideInVertically { height -> height } + fadeIn() togetherWith
                                            slideOutVertically { height -> -height } + fadeOut()
                                } else {
                                    slideInVertically { height -> -height } + fadeIn() togetherWith
                                            slideOutVertically { height -> height } + fadeOut()
                                }
                            },
                            label = "screen_root_animation"
                        ) { showEditor ->
                            if (showEditor) {
                                ConfigEditorScreen(
                                    viewModel = viewModel,
                                    editingProfile = editingProfileTarget,
                                    onBackToList = {
                                        isEditorActive = false
                                        editingProfileTarget = null
                                    }
                                )
                            } else {
                                when (currentTab) {
                                    "home" -> HomeScreen(
                                        viewModel = viewModel,
                                        onNavigateToProfiles = { currentTab = "profiles" }
                                    )
                                    "profiles" -> ProfilesScreen(
                                        viewModel = viewModel,
                                        onNavigateToEditor = { targetProfile ->
                                            editingProfileTarget = targetProfile
                                            isEditorActive = true
                                        }
                                    )
                                    "dns" -> SettingsScreen(
                                        viewModel = viewModel
                                    )
                                    "ai" -> AiCompanionScreen(
                                        viewModel = viewModel
                                    )
                                    "logs" -> LogsScreen(
                                        viewModel = viewModel
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
