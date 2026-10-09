package com.example

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AgentServiceConsoleScreen
import com.example.ui.components.ChatScreen
import com.example.ui.theme.LocalChatThemePalette
import com.example.ui.theme.ThemeViewModel
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaTheme
import com.example.ui.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeViewModel.Companion.Factory(application)
    }

    private val chatViewModel: ChatViewModel by viewModels {
        ChatViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val globalThemeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val chatThemeMode by chatViewModel.chatThemeMode.collectAsStateWithLifecycle()

            ZamaTheme(
                themeMode = globalThemeMode,
                chatThemeMode = chatThemeMode
            ) {
                MainAppContent(
                    chatViewModel = chatViewModel,
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    chatViewModel: ChatViewModel,
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val palette = LocalChatThemePalette.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Microphone enabled for Voice-to-Text", Toast.LENGTH_SHORT).show()
            chatViewModel.startVoiceDictation()
        } else {
            Toast.makeText(context, "Microphone permission required for voice dictation", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        bottomBar = {
            NavigationBar(
                containerColor = palette.headerBackground,
                contentColor = palette.textPrimary,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.AutoMirrored.Filled.Chat else Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Chat",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "AGENT CHAT",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFF4F4F5),
                        selectedTextColor = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFF4F4F5),
                        indicatorColor = if (palette.isNeonGlow) Color(0x3300E5FF) else Color(0xFF27272A),
                        unselectedIconColor = palette.textSecondary,
                        unselectedTextColor = palette.textSecondary
                    ),
                    modifier = Modifier.testTag("tab_agent_chat")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Dns else Icons.Outlined.Dns,
                            contentDescription = "Service",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "SERVICE HUB",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (palette.isNeonGlow) ZamaNeonGreen else Color(0xFFF4F4F5),
                        selectedTextColor = if (palette.isNeonGlow) ZamaNeonGreen else Color(0xFFF4F4F5),
                        indicatorColor = if (palette.isNeonGlow) Color(0x3300E676) else Color(0xFF27272A),
                        unselectedIconColor = palette.textSecondary,
                        unselectedTextColor = palette.textSecondary
                    ),
                    modifier = Modifier.testTag("tab_service_hub")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription = "Security Vault",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "SECURITY",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFF4F4F5),
                        selectedTextColor = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFF4F4F5),
                        indicatorColor = if (palette.isNeonGlow) Color(0x3300E5FF) else Color(0xFF27272A),
                        unselectedIconColor = palette.textSecondary,
                        unselectedTextColor = palette.textSecondary
                    ),
                    modifier = Modifier.testTag("tab_pin_security")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab != 0) {
                androidx.activity.compose.BackHandler {
                    selectedTab = 0
                }
            }

            when (selectedTab) {
                0 -> ChatScreen(
                    chatViewModel = chatViewModel,
                    onRequestMicrophonePermission = {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
                1 -> AgentServiceConsoleScreen(
                    chatViewModel = chatViewModel
                )
                2 -> {
                    var authMode by remember { mutableStateOf("BIOMETRIC") } // "BIOMETRIC", "PIN", "PRIVACY_POLICY"
                    val authRepo = remember { com.example.security.AuthRepository(context) }
                    val biometricAuthManager = remember { com.example.security.BiometricAuthManager(context) }

                    when (authMode) {
                        "DATA_SAFETY" -> {
                            androidx.activity.compose.BackHandler {
                                authMode = "PRIVACY_POLICY"
                            }
                            com.example.ui.components.DataSafetyInfoScreen(
                                onBack = { authMode = "PRIVACY_POLICY" },
                                onNavigateToPrivacySettings = { authMode = "PRIVACY_POLICY" }
                            )
                        }
                        "PRIVACY_POLICY" -> {
                            androidx.activity.compose.BackHandler {
                                authMode = "BIOMETRIC"
                            }
                            com.example.ui.components.PrivacySettingsScreen(
                                chatViewModel = chatViewModel,
                                onBack = { authMode = "BIOMETRIC" },
                                onDataWiped = {
                                    Toast.makeText(context, "All local data wiped & app reset to clean state!", Toast.LENGTH_LONG).show()
                                    authMode = "BIOMETRIC"
                                },
                                onNavigateToDataSafety = {
                                    authMode = "DATA_SAFETY"
                                }
                            )
                        }
                        "BIOMETRIC" -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    com.example.ui.components.BiometricAuthenticationScreen(
                                        biometricAuthManager = biometricAuthManager,
                                        onAuthenticationSuccess = {
                                            Toast.makeText(context, "Biometric Authentication Verified!", Toast.LENGTH_SHORT).show()
                                        },
                                        onPinFallbackRequested = {
                                            authMode = "PIN"
                                        }
                                    )
                                }
                                TextButton(
                                    onClick = { authMode = "PRIVACY_POLICY" },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .testTag("nav_to_privacy_screen")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFF4F4F5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DATA FOUNDATION & PRIVACY POLICY",
                                        color = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFF4F4F5),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        else -> {
                            androidx.activity.compose.BackHandler {
                                authMode = "BIOMETRIC"
                            }
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    com.example.ui.components.SaltedPinAuthScreen(
                                        authRepository = authRepo,
                                        onAuthenticationSuccess = {
                                            Toast.makeText(context, "PIN Authentication Verified!", Toast.LENGTH_SHORT).show()
                                        },
                                        onBiometricFallbackRequested = {
                                            authMode = "BIOMETRIC"
                                        }
                                    )
                                }
                                TextButton(
                                    onClick = { authMode = "PRIVACY_POLICY" },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .testTag("nav_to_privacy_screen_pin")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFF4F4F5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DATA FOUNDATION & PRIVACY POLICY",
                                        color = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFFF4F4F5),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
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
