package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.BiometricSecurityViewModel
import com.example.ui.components.AiEmployeeLiveLab
import com.example.ui.components.AgentPerformanceAnalyticsScreen
import com.example.ui.components.BiometricAuthenticationScreen
import com.example.ui.components.BiometricLockScreen
import com.example.ui.components.BiometricPrompt
import com.example.ui.components.BiometricPromptStatusView
import com.example.ui.components.BiometricSecurityStatusPill
import com.example.ui.components.BusinessCalendarDashboardScreen
import com.example.ui.components.SaltedPinFallbackScreen
import com.example.ui.components.SecurityAuditDialog
import com.example.ui.components.SentimentFlowDashboardOverlay
import com.example.ui.components.ChatAnalyticsDashboardScreen
import com.example.ui.components.ChatScreen
import com.example.ui.components.CrystallineAIObject
import com.example.ui.components.EncryptedWorldParticles
import com.example.ui.components.FuturisticTypographyShowcase
import com.example.ui.components.InteractiveTypographicArt
import com.example.ui.components.MovingTypographyPlayground
import com.example.ui.components.ThemeTogglePill
import com.example.ui.components.WhatsAppAgentManagementScreen
import com.example.ui.components.WhatsAppAgentStatusView
import com.example.ui.components.Zama3DZCenterpieceEngine
import com.example.ui.components.Zama3DVisualizer
import com.example.ui.components.ZamaAppSettingsDialog
import com.example.ui.components.ZamaEditorialHero
import com.example.ui.components.ZamaFloatingNav
import com.example.ui.components.ZamaFooter
import com.example.ui.components.ZamaGlMeshVisualizer
import com.example.ui.components.ZamaTechnologyTimeline
import com.example.ui.theme.LocalZamaPalette
import com.example.ui.theme.LocalZamaThemeMode
import com.example.ui.theme.ThemeViewModel
import com.example.ui.theme.ZamaTheme
import com.example.ui.theme.ZamaThemeMode
import com.example.ui.theme.ZamaVoid
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
  val biometricAuthManager: com.example.security.BiometricAuthManager by lazy {
    com.example.security.BiometricAuthManager(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val themeViewModel: ThemeViewModel = viewModel(
        factory = ThemeViewModel.Companion.Factory(application)
      )
      val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

      ZamaTheme(themeMode = themeMode) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          ZamaAppScreen(
            themeMode = themeMode,
            biometricAuthManager = biometricAuthManager,
            onToggleTheme = { themeViewModel.toggleTheme() },
            onSelectThemeMode = { themeViewModel.setThemeMode(it) }
          )
        }
      }
    }
  }
}

@Composable
fun ZamaAppScreen(
  themeMode: ZamaThemeMode = LocalZamaThemeMode.current,
  biometricAuthManager: com.example.security.BiometricAuthManager? = null,
  onToggleTheme: () -> Unit = {},
  onSelectThemeMode: (ZamaThemeMode) -> Unit = {}
) {
  val context = LocalContext.current
  val isInspection = LocalInspectionMode.current
  val app = context.applicationContext as? Application

  if (isInspection || app == null) {
    ZamaAppPreviewContent(
      themeMode = themeMode,
      onToggleTheme = onToggleTheme
    )
    return
  }

  val fragmentActivity = context as? FragmentActivity
  val securityViewModel: BiometricSecurityViewModel = viewModel(
    factory = BiometricSecurityViewModel.Companion.Factory(app)
  )
  val securityState by securityViewModel.uiState.collectAsStateWithLifecycle()

  // Apply FLAG_SECURE only in non-debug builds so the AI Studio Streaming Emulator can stream the preview
  androidx.compose.runtime.LaunchedEffect(securityState.isBiometricProtectionEnabled, securityState.isUnlocked) {
    val window = fragmentActivity?.window ?: return@LaunchedEffect
    if (!BuildConfig.DEBUG && (securityState.isBiometricProtectionEnabled || !securityState.isUnlocked)) {
      window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    } else {
      window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }
  }

  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        securityViewModel.refreshHardwareCapability()
        securityViewModel.checkAutoLockTimeout()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  var showSentimentFlowOverlay by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showSecurityAuditDialog by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ZamaFloatingNav(
            modifier = Modifier.weight(1f),
            activeSection = "home",
            onNavigate = { target ->
              coroutineScope.launch {
                when (target) {
                  "hero" -> listState.animateScrollToItem(0)
                  "3d" -> listState.animateScrollToItem(1) // Jump straight to 3D Z Visual Centerpiece Engine
                  "gl" -> listState.animateScrollToItem(3)
                  "chat" -> listState.animateScrollToItem(4) // Jump straight to Zama AI ChatScreen
                  "lab" -> listState.animateScrollToItem(5) // Jump to Live Lab
                  "agents" -> listState.animateScrollToItem(6) // Jump straight to Autonomous WhatsApp Agents
                  "analytics" -> listState.animateScrollToItem(7) // Jump straight to Chat Analytics Dashboard
                  "perf" -> listState.animateScrollToItem(8) // Jump straight to Agent Performance Analytics
                  "cal" -> listState.animateScrollToItem(9) // Jump straight to Business Calendar Dashboard
                  "flow" -> showSentimentFlowOverlay = true // Open Sentiment Flow Recharts Dashboard Overlay
                  "lock" -> securityViewModel.lockSession() // Lock session immediately via biometric lock
                  "settings" -> showSettingsDialog = true // Open Settings Dialog with Theme Controls
                  else -> listState.animateScrollToItem(0)
                }
              }
            }
          )

          // 1-Tap Theme Toggle Pill (Sunlight / High-Contrast Light vs Futuristic Dark)
          ThemeTogglePill(
            currentMode = themeMode,
            onToggleTheme = onToggleTheme
          )

          BiometricSecurityStatusPill(
            isUnlocked = securityState.isUnlocked,
            onLockClicked = { securityViewModel.lockSession() },
            onUnlockClicked = {
              fragmentActivity?.let { securityViewModel.authenticateWithBiometrics(it) }
            }
          )
        }
      }
    ) { innerPadding ->
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .navigationBarsPadding()
        .testTag("zama_main_scroll_feed"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      // 1. Editorial Hero Manifesto
      item {
        ZamaEditorialHero(
          onExplore3D = {
            coroutineScope.launch { listState.animateScrollToItem(1) }
          },
          onLaunchChat = {
            coroutineScope.launch { listState.animateScrollToItem(4) }
          }
        )
      }

      // 2. The 3D Z Object Rendering Engine — Central Visual Centerpiece for Zama AI
      item {
        Zama3DZCenterpieceEngine()
      }

      // 2-B. The 3D Z Sculpture & 6-Phase Transformation Engine
      item {
        Zama3DVisualizer()
      }

      // 2-C. The 3D Z Object in OpenGL ES 2.0 & Hardware Mesh Framework
      item {
        ZamaGlMeshVisualizer()
      }

      // 3. Modern Dark-Themed ChatScreen for Zama AI Autonomous Agent
      item {
        ChatScreen()
      }

      // 3-B. Flagship Autonomous AI Employee WhatsApp Communication Interface & Neural Dashboard
      item {
        AiEmployeeLiveLab()
      }

      // 3-C. Autonomous WhatsApp Agent Management Screen & Active Status Indicators
      item {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          WhatsAppAgentStatusView(
            agentService = com.example.data.remote.WhatsAppAgentNetworkModule.defaultBackendService
          )
          WhatsAppAgentManagementScreen()
        }
      }

      // 3-D. Chat Activity Frequency & Sentiment Trends Dashboard (D3.js / Recharts + Room DB)
      item {
        ChatAnalyticsDashboardScreen()
      }

      // 3-D. Agent Performance Analytics Dashboard (Recharts: Latency, Resolved vs Pending Triage, Peak Hours)
      item {
        AgentPerformanceAnalyticsScreen()
      }

      // 3-E. Business Calendar & Triage Sync Dashboard (Automated Event Creation from WhatsApp Inquiries)
      item {
        BusinessCalendarDashboardScreen()
      }

      // 4. Section 03 — Crystalline AI Object
      item {
        CrystallineAIObject()
      }

      // 5. Section 04 — Moving Typography Playground (PRIVATE POWERFUL OPEN -> ZAMA)
      item {
        MovingTypographyPlayground()
      }

      // 5-B. Custom Modifier: Glitch & Liquid Typography Lab
      item {
        FuturisticTypographyShowcase()
      }

      // 6. Section 05 — Encrypted World Particle System
      item {
        EncryptedWorldParticles()
      }

      // 7. Section 06 — Interactive Typographic Art (TRUST, PRIVACY, COMPUTE, FREEDOM)
      item {
        InteractiveTypographicArt()
      }

      // 8. Section 07 — ZAMA Technology Vertical Timeline
      item {
        ZamaTechnologyTimeline()
      }

      // 9. Final Footer
      item {
        ZamaFooter(
          onNavigate = { target ->
            coroutineScope.launch {
              when (target) {
                "hero" -> listState.animateScrollToItem(0)
                "3d" -> listState.animateScrollToItem(1)
                "chat" -> listState.animateScrollToItem(4)
                "agents" -> listState.animateScrollToItem(6)
                else -> listState.animateScrollToItem(0)
              }
            }
          }
        )
      }

      item {
        Spacer(modifier = Modifier.height(32.dp))
      }
    }
  }

    // Interactive Recharts Sentiment Flow Dashboard Overlay
    SentimentFlowDashboardOverlay(
      isOpen = showSentimentFlowOverlay,
      onDismiss = { showSentimentFlowOverlay = false }
    )

    // Global App Settings Dialog (Theme toggle & Biometric Security)
    ZamaAppSettingsDialog(
      isOpen = showSettingsDialog,
      currentThemeMode = themeMode,
      onSelectThemeMode = onSelectThemeMode,
      securityState = securityState,
      onToggleBiometrics = { securityViewModel.setBiometricProtectionEnabled(it) },
      onUpdateAutoLock = { securityViewModel.setAutoLockDuration(it) },
      onSetupOwnerPin = { newPin, confirmPin ->
        securityViewModel.setupOwnerPin(newPin, confirmPin, unlockSessionOnSuccess = true)
      },
      onChangeOwnerPin = { currentPin, newPin, confirmPin ->
        securityViewModel.changeOwnerPin(currentPin, newPin, confirmPin)
      },
      onResetOwnerPin = { securityViewModel.resetOwnerPin() },
      onOpenAuditLogs = { showSecurityAuditDialog = true },
      onManageAgents = { coroutineScope.launch { listState.animateScrollToItem(6) } },
      onDismiss = { showSettingsDialog = false }
    )

    // Biometric Security Audit Logs Dialog (Accessible from Global Settings)
    if (showSecurityAuditDialog) {
      SecurityAuditDialog(
        auditLogs = securityState.auditLogs,
        onDismiss = { showSecurityAuditDialog = false }
      )
    }

    // Biometric Security Lock Screen (Guarding salon dashboard, private customer triage & finances)
    if (!securityState.isUnlocked) {
      BiometricLockScreen(
        securityState = securityState,
        onAuthenticateBiometrics = { act -> securityViewModel.authenticateWithBiometrics(act) },
        onAuthenticatePin = { pin -> securityViewModel.authenticateWithMasterPin(pin) },
        onSetupOwnerPin = { newPin, confirmPin ->
          securityViewModel.setupOwnerPin(newPin, confirmPin, unlockSessionOnSuccess = true)
        },
        onLockSession = { securityViewModel.lockSession() },
        onToggleBiometricProtection = { enabled -> securityViewModel.setBiometricProtectionEnabled(enabled) },
        onUpdateAutoLockDuration = { mins -> securityViewModel.setAutoLockDuration(mins) },
        themeMode = themeMode,
        onToggleTheme = onToggleTheme,
        onSelectThemeMode = onSelectThemeMode
      )
    }
  }
}

@Composable
private fun ZamaAppPreviewContent(
  themeMode: ZamaThemeMode = LocalZamaThemeMode.current,
  onToggleTheme: () -> Unit = {}
) {
  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ZamaFloatingNav(
            modifier = Modifier.weight(1f),
            activeSection = "home",
            onNavigate = {}
          )
          ThemeTogglePill(
            currentMode = themeMode,
            onToggleTheme = onToggleTheme
          )
          BiometricSecurityStatusPill(
            isUnlocked = true,
            onLockClicked = {},
            onUnlockClicked = {}
          )
        }
      }
    ) { innerPadding ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .testTag("zama_main_scroll_feed"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
      ) {
        item {
          ZamaEditorialHero(
            onExplore3D = {},
            onLaunchChat = {}
          )
        }
        item {
          Zama3DZCenterpieceEngine()
        }
        item {
          ZamaFooter(onNavigate = {})
        }
      }
    }
  }
}

class GreetingPreviewParameterProvider : PreviewParameterProvider<String> {
  override val values: Sequence<String> = sequenceOf("Android", "Zama AI Studio")
}

class ZamaThemePreviewParameterProvider : PreviewParameterProvider<ZamaThemeMode> {
  override val values: Sequence<ZamaThemeMode> = sequenceOf(
    ZamaThemeMode.FUTURISTIC_DARK,
    ZamaThemeMode.HIGH_CONTRAST_LIGHT
  )
}

/**
 * Reusable preview wrapper that explicitly provides a [MaterialTheme] (via [ZamaTheme])
 * and a [Surface] container to wrap UI components for reliable IDE Compose Previews.
 */
@Composable
fun PreviewWrapper(
  modifier: Modifier = Modifier,
  darkTheme: Boolean = true,
  themeMode: ZamaThemeMode = if (darkTheme) ZamaThemeMode.FUTURISTIC_DARK else ZamaThemeMode.HIGH_CONTRAST_LIGHT,
  content: @Composable () -> Unit
) {
  ZamaTheme(themeMode = themeMode) {
    Surface(
      modifier = modifier,
      color = MaterialTheme.colorScheme.background,
      contentColor = MaterialTheme.colorScheme.onBackground
    ) {
      content()
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = "Hello $name!",
    modifier = modifier,
    color = MaterialTheme.colorScheme.onBackground
  )
}

@Preview(showBackground = true, name = "Greeting Preview")
@Composable
fun GreetingPreview(
  @PreviewParameter(GreetingPreviewParameterProvider::class) name: String = "Android"
) {
  PreviewWrapper {
    Greeting(name)
  }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891, name = "Zama App Preview")
@Composable
fun ZamaAppScreenPreview(
  @PreviewParameter(ZamaThemePreviewParameterProvider::class) themeMode: ZamaThemeMode = ZamaThemeMode.FUTURISTIC_DARK
) {
  PreviewWrapper(
    modifier = Modifier.fillMaxSize(),
    themeMode = themeMode
  ) {
    ZamaAppScreen(themeMode = themeMode)
  }
}

@Preview(showBackground = true, name = "BiometricPrompt Waiting Preview")
@Composable
fun BiometricPromptPreview() {
  PreviewWrapper {
    BiometricPrompt(
      isWaitingForAuthentication = true,
      onUseFallbackPin = {}
    )
  }
}

