package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.PreviewWrapper
import com.example.security.BiometricAuthFlowState
import com.example.security.BiometricAuthManager
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen

/**
 * High-level status states displayed by [BiometricAuthenticationScreen].
 */
enum class BiometricAuthenticationScreenStatus {
    AUTHENTICATING,
    SUCCESS,
    FAILURE
}

typealias BiometricAuthenticationStatus = BiometricAuthenticationScreenStatus

/**
 * Composable screen that uses [BiometricAuthManager] to automatically trigger the native
 * `androidx.biometric.BiometricPrompt` on load and displays clear visual status states
 * (`Authenticating`, `Success`, `Failure`) to the user.
 */
@Composable
fun BiometricAuthenticationScreen(
    modifier: Modifier = Modifier,
    biometricAuthManager: BiometricAuthManager? = null,
    statusOverride: BiometricAuthenticationScreenStatus? = null,
    failureMessageOverride: String? = null,
    triggerPromptOnLoad: Boolean = true,
    title: String = BiometricAuthManager.DEFAULT_TITLE,
    subtitle: String = BiometricAuthManager.DEFAULT_SUBTITLE,
    description: String = BiometricAuthManager.DEFAULT_DESCRIPTION,
    onAuthenticatedSuccess: () -> Unit = {},
    onAuthenticationFailure: (Int, String) -> Unit = { _, _ -> },
    onUseFallbackPin: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity
    val manager = remember(biometricAuthManager, context) {
        biometricAuthManager ?: BiometricAuthManager(context.applicationContext ?: context)
    }

    val observedFlowState by manager.authFlowState.collectAsStateWithLifecycle()
    var showInlinePinFallback by remember { mutableStateOf(false) }

    val launchBiometricPrompt: () -> Unit = {
        if (fragmentActivity != null) {
            manager.authenticate(
                activity = fragmentActivity,
                title = title,
                subtitle = subtitle,
                description = description,
                onSuccess = {
                    manager.setFlowState(
                        BiometricAuthFlowState.Success("Biometric identity verified.")
                    )
                    onAuthenticatedSuccess()
                },
                onError = { code, msg ->
                    manager.setFlowState(
                        BiometricAuthFlowState.Error(errorCode = code, message = msg)
                    )
                    onAuthenticationFailure(code, msg)
                },
                onFailed = {
                    val failMsg = "Biometric credential not recognized. Please try again or use Owner PIN."
                    manager.setFlowState(
                        BiometricAuthFlowState.Error(errorCode = -2, message = failMsg)
                    )
                    onAuthenticationFailure(-2, failMsg)
                },
                onCancelled = {
                    val cancelMsg = "Biometric authentication cancelled by user."
                    manager.setFlowState(
                        BiometricAuthFlowState.Error(errorCode = 10, message = cancelMsg)
                    )
                    onAuthenticationFailure(10, cancelMsg)
                }
            )
        } else {
            manager.setFlowState(
                BiometricAuthFlowState.Authenticating(
                    "Waiting for user biometric verification..."
                )
            )
        }
    }

    // Automatically trigger the biometric prompt on load via BiometricAuthManager
    LaunchedEffect(triggerPromptOnLoad, statusOverride) {
        if (triggerPromptOnLoad && statusOverride == null) {
            launchBiometricPrompt()
        }
    }

    if (showInlinePinFallback) {
        SaltedPinFallbackScreen(
            modifier = modifier,
            onPinAuthenticated = {
                showInlinePinFallback = false
                manager.setFlowState(BiometricAuthFlowState.Success("Authenticated via Salted Owner PIN."))
                onAuthenticatedSuccess()
            },
            onBackToBiometrics = {
                showInlinePinFallback = false
                launchBiometricPrompt()
            }
        )
        return
    }

    val effectiveStatus: BiometricAuthenticationScreenStatus = statusOverride ?: when (observedFlowState) {
        is BiometricAuthFlowState.Success,
        BiometricAuthFlowState.Success -> BiometricAuthenticationScreenStatus.SUCCESS
        is BiometricAuthFlowState.Error,
        BiometricAuthFlowState.Error -> BiometricAuthenticationScreenStatus.FAILURE
        is BiometricAuthFlowState.Idle,
        BiometricAuthFlowState.Idle,
        is BiometricAuthFlowState.Authenticating,
        BiometricAuthFlowState.Authenticating -> BiometricAuthenticationScreenStatus.AUTHENTICATING
    }

    val detailMessage: String = when {
        !failureMessageOverride.isNullOrBlank() && effectiveStatus == BiometricAuthenticationScreenStatus.FAILURE ->
            failureMessageOverride
        observedFlowState is BiometricAuthFlowState.Error ->
            (observedFlowState as BiometricAuthFlowState.Error).message
        observedFlowState is BiometricAuthFlowState.Success ->
            (observedFlowState as BiometricAuthFlowState.Success).message
        observedFlowState is BiometricAuthFlowState.Authenticating ->
            (observedFlowState as BiometricAuthFlowState.Authenticating).message
        else -> "Touch the fingerprint sensor or look at the front camera to verify your identity."
    }

    val accentColor by animateColorAsState(
        targetValue = when (effectiveStatus) {
            BiometricAuthenticationScreenStatus.AUTHENTICATING -> ZamaElectricCyan
            BiometricAuthenticationScreenStatus.SUCCESS -> ZamaNeonGreen
            BiometricAuthenticationScreenStatus.FAILURE -> Color(0xFFFF5252)
        },
        animationSpec = tween(260),
        label = "biometric_auth_screen_accent"
    )

    val statusBadgeLabel = when (effectiveStatus) {
        BiometricAuthenticationScreenStatus.AUTHENTICATING -> "AUTHENTICATING • SCANNING BIOMETRICS"
        BiometricAuthenticationScreenStatus.SUCCESS -> "SUCCESS • IDENTITY VERIFIED"
        BiometricAuthenticationScreenStatus.FAILURE -> "FAILURE • AUTHENTICATION FAILED"
    }

    val statusHeadline = when (effectiveStatus) {
        BiometricAuthenticationScreenStatus.AUTHENTICATING -> "Authenticating Identity..."
        BiometricAuthenticationScreenStatus.SUCCESS -> "Biometric Authentication Successful"
        BiometricAuthenticationScreenStatus.FAILURE -> "Biometric Authentication Failed"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "biometric_auth_screen_transition")
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep_angle"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("biometric_authentication_screen"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070C14)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Status State Pill (Authenticating, Success, Failure)
            Surface(
                color = accentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("biometric_auth_screen_status_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accentColor, CircleShape)
                    )
                    Text(
                        text = statusBadgeLabel,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.7.sp,
                        modifier = Modifier.testTag("biometric_auth_screen_status_text")
                    )
                }
            }

            // 2. Animated Biometric Scanner Orb
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .testTag("biometric_auth_screen_orb"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = (size.minDimension / 2f) - 5f
                    drawCircle(
                        color = accentColor.copy(alpha = 0.2f),
                        radius = radius,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = 0.32f),
                        radius = radius * 0.76f,
                        style = Stroke(width = 1.5f)
                    )
                    if (effectiveStatus == BiometricAuthenticationScreenStatus.AUTHENTICATING) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.Transparent,
                                    accentColor.copy(alpha = 0.35f),
                                    accentColor
                                )
                            ),
                            startAngle = radarSweepAngle,
                            sweepAngle = 110f,
                            useCenter = false,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .scale(
                            if (effectiveStatus == BiometricAuthenticationScreenStatus.AUTHENTICATING) pulseScale else 1f
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(accentColor.copy(alpha = 0.24f), Color(0xFF050A11))
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (effectiveStatus) {
                            BiometricAuthenticationScreenStatus.AUTHENTICATING -> Icons.Default.Fingerprint
                            BiometricAuthenticationScreenStatus.SUCCESS -> Icons.Default.CheckCircle
                            BiometricAuthenticationScreenStatus.FAILURE -> Icons.Default.ErrorOutline
                        },
                        contentDescription = statusHeadline,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // 3. Headline & Subtitle
            AnimatedContent(
                targetState = statusHeadline,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "biometric_auth_screen_headline_anim"
            ) { headline ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = headline,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("biometric_auth_screen_headline")
                    )
                    Text(
                        text = subtitle,
                        color = ZamaChromeLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 4. Progress Indicator (shown when Authenticating)
            if (effectiveStatus == BiometricAuthenticationScreenStatus.AUTHENTICATING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("biometric_auth_screen_progress"),
                    color = ZamaElectricCyan,
                    trackColor = Color(0xFF132235)
                )
            }

            // 5. Status Feedback / Error Detail Banner
            Surface(
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("biometric_auth_screen_message_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (effectiveStatus) {
                            BiometricAuthenticationScreenStatus.AUTHENTICATING -> Icons.Default.Shield
                            BiometricAuthenticationScreenStatus.SUCCESS -> Icons.Default.CheckCircle
                            BiometricAuthenticationScreenStatus.FAILURE -> Icons.Default.ErrorOutline
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = detailMessage,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp,
                        modifier = Modifier.testTag("biometric_auth_screen_message_text")
                    )
                }
            }

            // 6. State-Specific Action Buttons (48.dp minimum touch targets)
            when (effectiveStatus) {
                BiometricAuthenticationScreenStatus.AUTHENTICATING -> {
                    OutlinedButton(
                        onClick = {
                            if (onUseFallbackPin != null) {
                                onUseFallbackPin()
                            } else {
                                showInlinePinFallback = true
                            }
                        },
                        border = BorderStroke(1.dp, ZamaBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("biometric_auth_screen_pin_fallback_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Use Owner PIN Fallback",
                                tint = ZamaChromeLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "USE OWNER PIN FALLBACK",
                                color = ZamaChromeLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                BiometricAuthenticationScreenStatus.SUCCESS -> {
                    Button(
                        onClick = onAuthenticatedSuccess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZamaNeonGreen,
                            contentColor = Color(0xFF040A06)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("biometric_auth_screen_continue_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Continue to Workspace",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ENTER SECURE WORKSPACE",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                BiometricAuthenticationScreenStatus.FAILURE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = launchBiometricPrompt,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZamaNeonGreen,
                                contentColor = Color(0xFF040A06)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("biometric_auth_screen_retry_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry Biometric Authentication",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "RETRY BIOMETRIC PROMPT",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (onUseFallbackPin != null) {
                                    onUseFallbackPin()
                                } else {
                                    showInlinePinFallback = true
                                }
                            },
                            border = BorderStroke(1.dp, ZamaElectricCyan.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("biometric_auth_screen_pin_fallback_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Use Owner PIN Fallback",
                                    tint = ZamaElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "USE OWNER PIN FALLBACK",
                                    color = ZamaElectricCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = ZamaBorder.copy(alpha = 0.45f))

            Row(
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
                    text = "BiometricAuthManager • AndroidX BiometricPrompt Hardware Gate",
                    color = ZamaChromeMid,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "BiometricAuthenticationScreen - Authenticating")
@Composable
fun BiometricAuthenticationScreenAuthenticatingPreview() {
    PreviewWrapper(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            BiometricAuthenticationScreen(
                statusOverride = BiometricAuthenticationScreenStatus.AUTHENTICATING,
                triggerPromptOnLoad = false
            )
        }
    }
}

@Preview(showBackground = true, name = "BiometricAuthenticationScreen - Success")
@Composable
fun BiometricAuthenticationScreenSuccessPreview() {
    PreviewWrapper(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            BiometricAuthenticationScreen(
                statusOverride = BiometricAuthenticationScreenStatus.SUCCESS,
                triggerPromptOnLoad = false
            )
        }
    }
}

@Preview(showBackground = true, name = "BiometricAuthenticationScreen - Failure")
@Composable
fun BiometricAuthenticationScreenFailurePreview() {
    PreviewWrapper(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            BiometricAuthenticationScreen(
                statusOverride = BiometricAuthenticationScreenStatus.FAILURE,
                failureMessageOverride = "Biometric sensor could not verify fingerprint.",
                triggerPromptOnLoad = false
            )
        }
    }
}
