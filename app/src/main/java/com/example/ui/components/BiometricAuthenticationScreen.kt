package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.BiometricAuthCallback
import com.example.security.BiometricAuthManager
import com.example.security.BiometricCapabilityStatus
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaCardSurface
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaVoid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Explicit visual states for the Biometric Authentication flow.
 */
sealed class BiometricVisualState {
    object Idle : BiometricVisualState()
    data class Authenticating(val message: String = "Sensor active • Scan fingerprint or face") : BiometricVisualState()
    data class Success(val message: String = "Identity verified! Access granted") : BiometricVisualState()
    data class Error(val errorMessage: String, val canRetry: Boolean = true) : BiometricVisualState()
}

/**
 * BiometricAuthenticationScreen Composable providing comprehensive visual states
 * (Idle, Authenticating, Success, Error) and using [BiometricAuthManager]
 * to trigger the Android BiometricPrompt for app access.
 */
@Composable
fun BiometricAuthenticationScreen(
    biometricAuthManager: BiometricAuthManager,
    onAuthenticationSuccess: () -> Unit = {},
    onPinFallbackRequested: (() -> Unit)? = null,
    title: String = "Biometric Security Vault",
    subtitle: String = "Verify identity to access Zama Salon Concierge",
    autoPromptOnLaunch: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var visualState by remember { mutableStateOf<BiometricVisualState>(BiometricVisualState.Idle) }
    var capabilityStatus by remember { mutableStateOf(biometricAuthManager.canAuthenticate()) }
    var showSimulationControls by remember { mutableStateOf(true) }

    val shakeOffset = remember { Animatable(0f) }

    // Infinite animation for authenticating radar ripple
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_scale"
    )

    fun startBiometricScan() {
        val activity = context as? Activity
        if (activity == null) {
            visualState = BiometricVisualState.Error(
                errorMessage = "Authentication requires an active Android Activity context.",
                canRetry = false
            )
            return
        }

        visualState = BiometricVisualState.Authenticating()

        biometricAuthManager.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            description = "Touch your device fingerprint sensor or face scanner to proceed",
            negativeButtonText = if (onPinFallbackRequested != null) "Use PIN Fallback" else "Cancel",
            callback = object : BiometricAuthCallback {
                override fun onAuthenticationStarted() {
                    visualState = BiometricVisualState.Authenticating()
                }

                override fun onAuthenticationSuccess(resultDescription: String) {
                    visualState = BiometricVisualState.Success(resultDescription)
                    coroutineScope.launch {
                        delay(650L)
                        onAuthenticationSuccess()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    visualState = BiometricVisualState.Error(
                        errorMessage = errString.toString(),
                        canRetry = true
                    )
                    coroutineScope.launch {
                        shakeOffset.animateTo(14f, tween(50))
                        shakeOffset.animateTo(-14f, tween(50))
                        shakeOffset.animateTo(8f, tween(50))
                        shakeOffset.animateTo(0f, tween(50))
                    }
                }

                override fun onAuthenticationFailed() {
                    visualState = BiometricVisualState.Error(
                        errorMessage = "Fingerprint not recognized. Please reposition finger and retry.",
                        canRetry = true
                    )
                    coroutineScope.launch {
                        shakeOffset.animateTo(14f, tween(50))
                        shakeOffset.animateTo(-14f, tween(50))
                        shakeOffset.animateTo(8f, tween(50))
                        shakeOffset.animateTo(0f, tween(50))
                    }
                }
            }
        )
    }

    // Cancel in-flight prompt on disposal
    DisposableEffect(Unit) {
        onDispose {
            biometricAuthManager.cancelAuthentication()
        }
    }

    // Optional launch trigger
    LaunchedEffect(Unit) {
        if (autoPromptOnLaunch && capabilityStatus == BiometricCapabilityStatus.AVAILABLE) {
            startBiometricScan()
        }
    }

    // Dynamic state colors
    val ringColor by animateColorAsState(
        targetValue = when (visualState) {
            is BiometricVisualState.Idle -> ZamaElectricCyan
            is BiometricVisualState.Authenticating -> ZamaElectricCyan
            is BiometricVisualState.Success -> ZamaNeonGreen
            is BiometricVisualState.Error -> Color(0xFFFF5252)
        },
        label = "ring_color"
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF060913),
            Color(0xFF0B1324),
            Color(0xFF070B14)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(20.dp)
            .testTag("biometric_screen_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Security Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Surface(
                color = Color(0x2600E5FF),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ZamaElectricCyan.copy(alpha = 0.5f)),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "ENCRYPTED BIOMETRIC LOCK",
                        color = ZamaElectricCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = title,
                color = ZamaChromeLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = ZamaChromeMid,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        // Center Visual Target Scanner
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (visualState !is BiometricVisualState.Authenticating) {
                            startBiometricScan()
                        }
                    }
                    .testTag("biometric_state_indicator")
            ) {
                // Outer pulsing radar ring (when authenticating)
                if (visualState is BiometricVisualState.Authenticating) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(pulseScale)
                            .border(BorderStroke(2.dp, ringColor.copy(alpha = 0.4f)), CircleShape)
                    )
                }

                // Inner Main Sensor Circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFF0F1A2E), CircleShape)
                        .border(
                            BorderStroke(
                                2.5.dp,
                                ringColor
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (visualState) {
                        is BiometricVisualState.Idle -> {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Fingerprint Sensor",
                                tint = ZamaElectricCyan,
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        is BiometricVisualState.Authenticating -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(54.dp),
                                    color = ZamaElectricCyan,
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        is BiometricVisualState.Success -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verification Success",
                                tint = ZamaNeonGreen,
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        is BiometricVisualState.Error -> {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Verification Error",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // State Description Label
            Surface(
                color = when (visualState) {
                    is BiometricVisualState.Idle -> Color(0x1F00E5FF)
                    is BiometricVisualState.Authenticating -> Color(0x3300E5FF)
                    is BiometricVisualState.Success -> Color(0x3300E676)
                    is BiometricVisualState.Error -> Color(0x33FF5252)
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ringColor.copy(alpha = 0.5f))
            ) {
                Text(
                    text = when (visualState) {
                        is BiometricVisualState.Idle -> "SENSOR READY • TAP TO SCAN"
                        is BiometricVisualState.Authenticating -> "SCANNING BIOMETRICS..."
                        is BiometricVisualState.Success -> "IDENTITY VERIFIED"
                        is BiometricVisualState.Error -> "VERIFICATION FAILED"
                    },
                    color = ringColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when (visualState) {
                    is BiometricVisualState.Idle -> "Place your registered finger on the sensor or tap 'Scan Biometrics' below."
                    is BiometricVisualState.Authenticating -> (visualState as BiometricVisualState.Authenticating).message
                    is BiometricVisualState.Success -> (visualState as BiometricVisualState.Success).message
                    is BiometricVisualState.Error -> (visualState as BiometricVisualState.Error).errorMessage
                },
                color = if (visualState is BiometricVisualState.Error) Color(0xFFFF8A80) else ZamaChromeLight,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Bottom Controls Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Primary Action Button
            when (visualState) {
                is BiometricVisualState.Idle, is BiometricVisualState.Error -> {
                    Button(
                        onClick = { startBiometricScan() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZamaElectricCyan,
                            contentColor = ZamaVoid
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("biometric_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (visualState is BiometricVisualState.Error) "RETRY BIOMETRIC SCAN" else "SCAN BIOMETRICS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                is BiometricVisualState.Authenticating -> {
                    OutlinedButton(
                        onClick = {
                            biometricAuthManager.cancelAuthentication()
                            visualState = BiometricVisualState.Idle
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF8A80)
                        ),
                        border = BorderStroke(1.dp, Color(0x66FF5252)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("biometric_cancel_button")
                    ) {
                        Text(
                            text = "CANCEL SCAN",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is BiometricVisualState.Success -> {
                    Button(
                        onClick = onAuthenticationSuccess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZamaNeonGreen,
                            contentColor = ZamaVoid
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("biometric_continue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONTINUE TO VAULT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // PIN Fallback Option
            if (onPinFallbackRequested != null) {
                OutlinedButton(
                    onClick = onPinFallbackRequested,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ZamaChromeLight
                    ),
                    border = BorderStroke(1.dp, ZamaBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("biometric_pin_fallback_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pin,
                        contentDescription = null,
                        tint = ZamaChromeMid,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "USE PIN FALLBACK",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Dev / Emulator Simulator Bar
            Surface(
                color = Color(0x1AFFFFFF),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, ZamaBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EMULATOR SENSOR SIMULATION",
                            color = ZamaChromeMid,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (showSimulationControls) "HIDE" else "SHOW",
                            color = ZamaElectricCyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showSimulationControls = !showSimulationControls }
                        )
                    }

                    if (showSimulationControls) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    biometricAuthManager.simulateSuccess(object : BiometricAuthCallback {
                                        override fun onAuthenticationStarted() {
                                            visualState = BiometricVisualState.Authenticating()
                                        }

                                        override fun onAuthenticationSuccess(resultDescription: String) {
                                            visualState = BiometricVisualState.Success(resultDescription)
                                            coroutineScope.launch {
                                                delay(600L)
                                                onAuthenticationSuccess()
                                            }
                                        }

                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {}
                                        override fun onAuthenticationFailed() {}
                                    })
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x3300E676),
                                    contentColor = ZamaNeonGreen
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("biometric_simulate_success_btn")
                            ) {
                                Text(text = "SIMULATE SUCCESS", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    biometricAuthManager.simulateError(
                                        errorCode = 7,
                                        message = "Fingerprint mismatch. 4 attempts remaining.",
                                        callback = object : BiometricAuthCallback {
                                            override fun onAuthenticationStarted() {
                                                visualState = BiometricVisualState.Authenticating()
                                            }

                                            override fun onAuthenticationSuccess(resultDescription: String) {}
                                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                visualState = BiometricVisualState.Error(errString.toString())
                                            }

                                            override fun onAuthenticationFailed() {}
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x33FF5252),
                                    contentColor = Color(0xFFFF8A80)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("biometric_simulate_error_btn")
                            ) {
                                Text(text = "SIMULATE ERROR", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
