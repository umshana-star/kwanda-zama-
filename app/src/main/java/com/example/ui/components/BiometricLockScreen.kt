package com.example.ui.components

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricCapability
import com.example.security.BiometricSecurityUiState
import com.example.security.BiometricSecurityViewModel
import com.example.ui.theme.LocalZamaPalette
import com.example.ui.theme.LocalZamaThemeMode
import com.example.ui.theme.ZamaAmberPulse
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaThemeMode
import com.example.ui.theme.ZamaVoid

/**
 * Cyber-luxurious Fullscreen Security Shield protecting the Zama AI business management
 * dashboard, revenue telemetry, and private customer triage communications.
 */
@Composable
fun BiometricLockScreen(
    securityState: BiometricSecurityUiState,
    onAuthenticateBiometrics: (FragmentActivity) -> Unit,
    onAuthenticatePin: (String) -> Boolean,
    onSetupOwnerPin: (newPin: String, confirmPin: String) -> com.example.security.PinSetupResult = { _, _ -> com.example.security.PinSetupResult.Success },
    onLockSession: () -> Unit,
    onToggleBiometricProtection: (Boolean) -> Unit,
    onUpdateAutoLockDuration: (Int) -> Unit,
    themeMode: ZamaThemeMode = LocalZamaThemeMode.current,
    onToggleTheme: () -> Unit = {},
    onSelectThemeMode: (ZamaThemeMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity

    var showPinKeypad by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var firstSetupPinDraft by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showSecurityAuditSheet by remember { mutableStateOf(false) }
    var showSecuritySettingsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showSecurityAuditSheet || showSecuritySettingsDialog || showPinKeypad) {
        when {
            showSecurityAuditSheet -> showSecurityAuditSheet = false
            showSecuritySettingsDialog -> showSecuritySettingsDialog = false
            showPinKeypad -> {
                showPinKeypad = false
                enteredPin = ""
                firstSetupPinDraft = null
                pinError = null
            }
        }
    }

    // Auto-prompt on launch only if biometric hardware is enrolled and Owner PIN is already configured
    LaunchedEffect(securityState.isUnlocked, securityState.hardwareCapability, securityState.isPinConfigured) {
        if (
            !securityState.isUnlocked &&
            securityState.isPinConfigured &&
            securityState.hardwareCapability is BiometricCapability.Available &&
            fragmentActivity != null
        ) {
            onAuthenticateBiometrics(fragmentActivity)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF904070D))
            .testTag("biometric_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
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
                            .size(10.dp)
                            .background(Color(0xFFFF1744), CircleShape)
                    )
                    Text(
                        text = "SECURE ENCLAVE LOCKED",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThemeTogglePill(
                        currentMode = themeMode,
                        onToggleTheme = onToggleTheme
                    )

                    IconButton(
                        onClick = { showSecurityAuditSheet = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_view_security_audit")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Security Audit Logs",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSecuritySettingsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_security_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Security Settings",
                            tint = ZamaChromeMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Center: Futuristic Biometric Scanning Visualizer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                BiometricScannerVisualizer(
                    isAuthenticating = securityState.isAuthenticating,
                    onClick = {
                        if (fragmentActivity != null) {
                            onAuthenticateBiometrics(fragmentActivity)
                        }
                    }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "KWANDA ZAMA BUSINESS HUB",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Private Customer Triage & Salon Analytics Shield",
                        color = ZamaNeonGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }

                // Sensor Hardware status pill
                Surface(
                    color = Color(0x1F00E676),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, Color(0x5500E676))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ZamaNeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when (securityState.hardwareCapability) {
                                is BiometricCapability.Available -> "BIOMETRIC SENSOR READY • FINGERPRINT / FACE"
                                is BiometricCapability.NoneEnrolled -> "BIOMETRIC SENSOR READY • PIN FALLBACK ACTIVE"
                                else -> "SECURE ACCESS ENCLAVE • MASTER PIN ACTIVE"
                            },
                            color = ZamaNeonGreen,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Error / Feedback Banner
                if (securityState.statusFeedback != null || pinError != null) {
                    Surface(
                        color = Color(0x33FF1744),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x88FF1744)),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = pinError ?: securityState.statusFeedback.orEmpty(),
                                color = Color(0xFFFF8A80),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Bottom Actions & Keypad Area
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Biometric Unlock Button
                Button(
                    onClick = {
                        if (fragmentActivity != null) {
                            pinError = null
                            onAuthenticateBiometrics(fragmentActivity)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZamaNeonGreen,
                        contentColor = Color(0xFF040A06)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_trigger_biometric")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Scan Biometrics",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "SCAN FINGERPRINT / FACE UNLOCK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // PIN Keypad or Toggle Button
                if (!showPinKeypad) {
                    OutlinedButton(
                        onClick = {
                            showPinKeypad = true
                            enteredPin = ""
                            firstSetupPinDraft = null
                            pinError = null
                        },
                        border = BorderStroke(1.dp, ZamaBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_show_pin_keypad")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = null,
                                tint = ZamaElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (securityState.isPinConfigured) {
                                    "UNLOCK WITH OWNER PIN"
                                } else {
                                    "SET UP OWNER PIN (FIRST-TIME SETUP)"
                                },
                                color = ZamaElectricCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    val keypadTitle = when {
                        securityState.isPinConfigured -> "ENTER 4-DIGIT OWNER PIN"
                        firstSetupPinDraft == null -> "CREATE 4-DIGIT OWNER PIN"
                        else -> "CONFIRM NEW 4-DIGIT OWNER PIN"
                    }
                    // Numeric PIN Pad
                    MasterPinKeypad(
                        titleText = keypadTitle,
                        enteredPin = enteredPin,
                        onPinChange = { newPin ->
                            enteredPin = newPin
                            pinError = null
                            if (newPin.length == 4) {
                                if (securityState.isPinConfigured) {
                                    val success = onAuthenticatePin(newPin)
                                    if (!success) {
                                        pinError = securityState.statusFeedback ?: "Incorrect Owner PIN."
                                        enteredPin = ""
                                    }
                                } else {
                                    val draft = firstSetupPinDraft
                                    if (draft == null) {
                                        firstSetupPinDraft = newPin
                                        enteredPin = ""
                                        pinError = "Re-enter your 4-digit PIN to confirm setup"
                                    } else {
                                        when (val res = onSetupOwnerPin(draft, newPin)) {
                                            is com.example.security.PinSetupResult.Success -> {
                                                firstSetupPinDraft = null
                                                enteredPin = ""
                                                pinError = null
                                            }
                                            is com.example.security.PinSetupResult.ValidationError -> {
                                                firstSetupPinDraft = null
                                                enteredPin = ""
                                                pinError = res.message
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onClose = {
                            showPinKeypad = false
                            enteredPin = ""
                            firstSetupPinDraft = null
                            pinError = null
                        }
                    )
                }

                // Protected Assets Disclaimer
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ZamaChromeMid,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Encrypted • WhatsApp Transcripts, Triage & Client Phone Data Shielded",
                        color = ZamaChromeMid,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Security Audit Sheet Dialog
        if (showSecurityAuditSheet) {
            SecurityAuditDialog(
                auditLogs = securityState.auditLogs,
                onDismiss = { showSecurityAuditSheet = false }
            )
        }

        // Security Settings Dialog
        if (showSecuritySettingsDialog) {
            SecuritySettingsDialog(
                securityState = securityState,
                onToggleBiometrics = onToggleBiometricProtection,
                onUpdateAutoLock = onUpdateAutoLockDuration,
                themeMode = themeMode,
                onSelectThemeMode = onSelectThemeMode,
                onDismiss = { showSecuritySettingsDialog = false }
            )
        }
    }
}

/**
 * Animated Scanning Ring & Biometric Fingerprint Visualizer.
 */
@Composable
private fun BiometricScannerVisualizer(
    isAuthenticating: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "biometric_pulse")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .size(130.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag("biometric_visualizer_circle"),
        contentAlignment = Alignment.Center
    ) {
        // Scanning radar circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 6f

            // Outer ring
            drawCircle(
                color = Color(0x2200E5FF),
                radius = radius,
                style = Stroke(width = 1.5f)
            )

            // Inner dashed ring
            drawCircle(
                color = Color(0x3300E676),
                radius = radius * 0.75f,
                style = Stroke(width = 1.5f)
            )

            // Rotating scanner arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        ZamaElectricCyan.copy(alpha = 0.2f),
                        ZamaNeonGreen
                    )
                ),
                startAngle = sweepAngle,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
        }

        // Center Fingerprint Icon Box
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF0D251A), Color(0xFF040A06))
                    )
                )
                .border(1.dp, ZamaNeonGreen.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Fingerprint Sensor",
                tint = if (isAuthenticating) ZamaElectricCyan else ZamaNeonGreen,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

/**
 * Tactical Master PIN Keypad for emulator & emergency fallback.
 */
@Composable
private fun MasterPinKeypad(
    titleText: String = "ENTER 4-DIGIT OWNER PIN",
    enteredPin: String,
    onPinChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1019)),
        border = BorderStroke(1.dp, Color(0x3300E5FF)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pin_keypad_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    color = ZamaElectricCyan,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Keypad",
                        tint = ZamaChromeMid,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) ZamaNeonGreen else Color(0x22FFFFFF))
                            .border(1.dp, if (isFilled) ZamaNeonGreen else Color(0x44FFFFFF), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Keypad Grid
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "DEL")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        Surface(
                            color = Color(0x18FFFFFF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, Color(0x22FFFFFF)),
                            modifier = Modifier
                                .size(width = 65.dp, height = 42.dp)
                                .padding(vertical = 2.dp)
                                .clickable {
                                    when (key) {
                                        "C" -> onPinChange("")
                                        "DEL" -> if (enteredPin.isNotEmpty()) onPinChange(enteredPin.dropLast(1))
                                        else -> if (enteredPin.length < 4) onPinChange(enteredPin + key)
                                    }
                                }
                                .testTag("pin_key_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "DEL") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
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

/**
 * Security Audit Log Sheet Dialog.
 */
@Composable
fun SecurityAuditDialog(
    auditLogs: List<com.example.security.SecurityAuditEntry>,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF090E17),
        border = BorderStroke(1.dp, ZamaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "BIOMETRIC AUDIT LOGS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(auditLogs) { log ->
                    Surface(
                        color = Color(0xFF0E1624),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, Color(0x221E2D42)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.eventType,
                                    color = if (log.isSuccess) ZamaNeonGreen else Color(0xFFFF5252),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = log.timestamp,
                                    color = ZamaChromeMid,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Method: ${log.methodUsed}",
                                color = ZamaElectricCyan,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = log.detailNotes,
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Security Settings Dialog.
 */
@Composable
private fun SecuritySettingsDialog(
    securityState: BiometricSecurityUiState,
    onToggleBiometrics: (Boolean) -> Unit,
    onUpdateAutoLock: (Int) -> Unit,
    themeMode: ZamaThemeMode,
    onSelectThemeMode: (ZamaThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF090E17),
        border = BorderStroke(1.dp, ZamaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ZamaNeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ZAMA SETTINGS & THEMES",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Theme Display Mode Section (Futuristic Dark vs High-Contrast Daylight)
            ThemeSettingsSection(
                currentMode = themeMode,
                onSelectMode = onSelectThemeMode
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0x331E2D42), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Toggle Biometric Lock
            Surface(
                color = Color(0xFF0E1624),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, Color(0x331E2D42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enforce Biometric Authentication",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Requires fingerprint or face unlock before accessing triage data",
                            color = ZamaChromeMid,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = securityState.isBiometricProtectionEnabled,
                        onCheckedChange = onToggleBiometrics,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ZamaNeonGreen,
                            checkedTrackColor = Color(0xFF0B291A)
                        ),
                        modifier = Modifier.testTag("switch_biometric_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-Lock Timer Options
            Text(
                text = "AUTO-LOCK TIMEOUT",
                color = ZamaElectricCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val durations = listOf(1 to "1 Min", 5 to "5 Mins", 15 to "15 Mins", 30 to "30 Mins")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durations.forEach { (mins, label) ->
                    val isSelected = securityState.autoLockDurationMinutes == mins
                    Surface(
                        color = if (isSelected) Color(0x3300E676) else Color(0x14FFFFFF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) ZamaNeonGreen else Color(0x22FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onUpdateAutoLock(mins) }
                            .testTag("autolock_btn_$mins")
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) ZamaNeonGreen else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Owner PIN Cryptographic Verifier Status
            Surface(
                color = Color(0x1400E5FF),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0x3300E5FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "OWNER PIN CRYPTOGRAPHIC VERIFIER",
                        color = ZamaElectricCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (securityState.isPinConfigured) {
                            "Status: Active (Salted PBKDF2-HMAC-SHA256 verifier • 5-attempt brute-force cooldown)"
                        } else {
                            "Status: Not Configured — Create your custom Owner PIN from the Lock Screen or Global Hub Settings"
                        },
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Top Navigation Security Pill Component.
 * Shows current security status and allows immediate 1-tap lock or unlock.
 */
@Composable
fun BiometricSecurityStatusPill(
    isUnlocked: Boolean,
    onLockClicked: () -> Unit,
    onUnlockClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isUnlocked) Color(0x2600E676) else Color(0x33FF1744),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, if (isUnlocked) Color(0x6600E676) else Color(0x88FF1744)),
        modifier = modifier
            .clickable {
                if (isUnlocked) onLockClicked() else onUnlockClicked()
            }
            .testTag("security_status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = "Security Status",
                tint = if (isUnlocked) ZamaNeonGreen else Color(0xFFFF5252),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (isUnlocked) "UNLOCKED" else "LOCK 🔒",
                color = if (isUnlocked) ZamaNeonGreen else Color(0xFFFF5252),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
