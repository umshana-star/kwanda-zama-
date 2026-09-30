package com.example.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.PreviewWrapper
import com.example.security.BiometricAuthFlowState
import com.example.security.BiometricAuthManager
import com.example.security.BiometricCapability
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen

/**
 * Composable that reacts to the current authentication flow states
 * ([BiometricAuthFlowState.Idle], [BiometricAuthFlowState.Authenticating],
 * [BiometricAuthFlowState.Success], [BiometricAuthFlowState.Error]) and provides
 * real-time visual feedback while interacting with [BiometricAuthManager].
 *
 * When [state] is null, it automatically observes [BiometricAuthManager.authFlowState].
 */
@Composable
fun BiometricPromptStatusView(
    modifier: Modifier = Modifier,
    state: BiometricAuthFlowState? = null,
    biometricAuthManager: BiometricAuthManager? = null,
    title: String = BiometricAuthManager.DEFAULT_TITLE,
    subtitle: String = BiometricAuthManager.DEFAULT_SUBTITLE,
    description: String = BiometricAuthManager.DEFAULT_DESCRIPTION,
    onAuthenticateClick: (() -> Unit)? = null,
    onRetryClick: (() -> Unit)? = null,
    onSuccessContinue: () -> Unit = {},
    onUseFallbackPin: (() -> Unit)? = null,
    onAuthSuccess: () -> Unit = {},
    onAuthError: (errorCode: Int, errString: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val fragmentActivity = context as? FragmentActivity

    val manager = remember(biometricAuthManager, context) {
        biometricAuthManager ?: BiometricAuthManager(context.applicationContext ?: context)
    }

    val observedFlowState by manager.authFlowState.collectAsStateWithLifecycle()
    val currentState = state ?: observedFlowState

    val capability = remember(manager, context, isInspection) {
        if (isInspection) {
            BiometricCapability.Available(
                canUseBiometric = true,
                canUseDeviceCredential = true,
                sensorTypes = listOf("Fingerprint", "Face Unlock")
            )
        } else {
            manager.checkBiometricCapability(context)
        }
    }

    val launchAuthentication: () -> Unit = {
        if (onAuthenticateClick != null) {
            onAuthenticateClick()
        } else if (fragmentActivity != null) {
            manager.authenticate(
                activity = fragmentActivity,
                title = title,
                subtitle = subtitle,
                description = description,
                onSuccess = {
                    onAuthSuccess()
                },
                onError = { code, msg ->
                    onAuthError(code, msg)
                }
            )
        } else {
            manager.setFlowState(
                BiometricAuthFlowState.Authenticating("Waiting for biometric sensor verification...")
            )
        }
    }

    val isIdle = currentState is BiometricAuthFlowState.Idle ||
        currentState is BiometricAuthFlowState.Idle.Companion
    val isAuthenticating = currentState is BiometricAuthFlowState.Authenticating ||
        currentState is BiometricAuthFlowState.Authenticating.Companion
    val isSuccess = currentState is BiometricAuthFlowState.Success ||
        currentState is BiometricAuthFlowState.Success.Companion
    val isError = currentState is BiometricAuthFlowState.Error ||
        currentState is BiometricAuthFlowState.Error.Companion

    val accentColor = when {
        isIdle -> ZamaElectricCyan
        isAuthenticating -> ZamaNeonGreen
        isSuccess -> ZamaNeonGreen
        else -> Color(0xFFFF5252)
    }

    val badgeLabel = when {
        isIdle -> "IDLE • BIOMETRIC SENSOR READY"
        isAuthenticating -> "AUTHENTICATING • SCANNING BIOMETRICS..."
        isSuccess -> "SUCCESS • IDENTITY VERIFIED"
        else -> "ERROR • AUTHENTICATION FAILED"
    }

    val stateMessage = when (currentState) {
        is BiometricAuthFlowState.Idle -> currentState.message
        is BiometricAuthFlowState.Idle.Companion -> "Biometric sensor ready. Tap to authenticate."
        is BiometricAuthFlowState.Authenticating -> currentState.message
        is BiometricAuthFlowState.Authenticating.Companion -> "Waiting for user biometric verification..."
        is BiometricAuthFlowState.Success -> currentState.message
        is BiometricAuthFlowState.Success.Companion -> "Biometric identity verified."
        is BiometricAuthFlowState.Error -> currentState.message
        is BiometricAuthFlowState.Error.Companion -> "Biometric authentication failed. Please try again."
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("biometric_prompt_status_view"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070C14)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Reactive Flow State Badge (Idle / Authenticating / Success / Error)
            Surface(
                color = accentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("biometric_status_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accentColor, CircleShape)
                    )
                    Text(
                        text = badgeLabel,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.testTag("biometric_status_badge_text")
                    )
                }
            }

            // Animated Sensor Icon & Ring reacting to current state
            BiometricStatusOrb(
                isAuthenticating = isAuthenticating,
                isSuccess = isSuccess,
                isError = isError,
                accentColor = accentColor,
                onClick = {
                    if (isError && onRetryClick != null) {
                        onRetryClick()
                    } else if (!isSuccess) {
                        launchAuthentication()
                    }
                }
            )

            // Title & Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            // Progress bar shown when Authenticating
            AnimatedVisibility(
                visible = isAuthenticating,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("biometric_status_progress_indicator"),
                    color = accentColor,
                    trackColor = Color(0xFF142234)
                )
            }

            // Animated Feedback Banner displaying state-specific message
            AnimatedContent(
                targetState = stateMessage,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "biometric_state_message_transition"
            ) { currentMessage ->
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("biometric_status_message_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isSuccess -> Icons.Default.CheckCircle
                                isError -> Icons.Default.ErrorOutline
                                else -> Icons.Default.Shield
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentMessage,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("biometric_status_message_text")
                        )
                    }
                }
            }

            // Hardware readiness caption
            Text(
                text = when (capability) {
                    is BiometricCapability.Available ->
                        "Hardware Ready: ${capability.sensorTypes.joinToString(" • ")}"
                    is BiometricCapability.NoneEnrolled ->
                        "No Biometrics Enrolled • Device Credential / Owner PIN Ready"
                    is BiometricCapability.HardwareUnavailable ->
                        "Biometric Sensor Temporarily Unavailable"
                    is BiometricCapability.NotSupported ->
                        "Biometric Hardware Not Supported • Use Owner PIN"
                },
                color = ZamaChromeMid,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            // Action Buttons tailored to the current state (Minimum 48.dp touch targets)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            isSuccess -> onSuccessContinue()
                            isError -> {
                                if (onRetryClick != null) {
                                    onRetryClick()
                                } else {
                                    launchAuthentication()
                                }
                            }
                            else -> launchAuthentication()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isError) Color(0xFFFF5252) else ZamaNeonGreen,
                        contentColor = if (isError) Color.White else Color(0xFF040A06)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("biometric_status_action_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isSuccess -> Icons.Default.CheckCircle
                                isError -> Icons.Default.Refresh
                                else -> Icons.Default.Fingerprint
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when {
                                isIdle -> "AUTHENTICATE WITH BIOMETRICS"
                                isAuthenticating -> "WAITING FOR SENSOR..."
                                isSuccess -> "AUTHENTICATION VERIFIED"
                                else -> "RETRY BIOMETRIC SCAN"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                if (onUseFallbackPin != null && !isSuccess) {
                    OutlinedButton(
                        onClick = onUseFallbackPin,
                        border = BorderStroke(1.dp, ZamaBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("biometric_status_fallback_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = "Use Owner PIN Fallback",
                                tint = ZamaElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "USE OWNER PIN FALLBACK",
                                color = ZamaElectricCyan,
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

@Composable
private fun BiometricStatusOrb(
    isAuthenticating: Boolean,
    isSuccess: Boolean,
    isError: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "biometric_status_orb_transition")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_sweep"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .clickable(
                onClickLabel = "Biometric Sensor Action",
                onClick = onClick
            )
            .testTag("biometric_status_orb"),
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
                color = accentColor.copy(alpha = 0.3f),
                radius = radius * 0.76f,
                style = Stroke(width = 1.5f)
            )

            if (isAuthenticating) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.25f),
                            accentColor
                        )
                    ),
                    startAngle = sweepAngle,
                    sweepAngle = 110f,
                    useCenter = false,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .scale(if (isAuthenticating) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accentColor.copy(alpha = 0.22f),
                            Color(0xFF040A06)
                        )
                    )
                )
                .border(1.2.dp, accentColor.copy(alpha = 0.75f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isSuccess -> Icons.Default.CheckCircle
                    isError -> Icons.Default.ErrorOutline
                    else -> Icons.Default.Fingerprint
                },
                contentDescription = "Biometric Status Icon",
                tint = accentColor,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "BiometricPromptStatusView - Idle")
@Composable
fun BiometricPromptStatusViewIdlePreview() {
    PreviewWrapper {
        Box(modifier = Modifier.padding(16.dp)) {
            BiometricPromptStatusView(
                state = BiometricAuthFlowState.Idle(),
                onUseFallbackPin = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "BiometricPromptStatusView - Authenticating")
@Composable
fun BiometricPromptStatusViewAuthenticatingPreview() {
    PreviewWrapper {
        Box(modifier = Modifier.padding(16.dp)) {
            BiometricPromptStatusView(
                state = BiometricAuthFlowState.Authenticating(),
                onUseFallbackPin = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "BiometricPromptStatusView - Success")
@Composable
fun BiometricPromptStatusViewSuccessPreview() {
    PreviewWrapper {
        Box(modifier = Modifier.padding(16.dp)) {
            BiometricPromptStatusView(
                state = BiometricAuthFlowState.Success()
            )
        }
    }
}

@Preview(showBackground = true, name = "BiometricPromptStatusView - Error")
@Composable
fun BiometricPromptStatusViewErrorPreview() {
    PreviewWrapper {
        Box(modifier = Modifier.padding(16.dp)) {
            BiometricPromptStatusView(
                state = BiometricAuthFlowState.Error(
                    errorCode = 10,
                    message = "Fingerprint sensor timed out. Tap Retry or use Owner PIN."
                ),
                onUseFallbackPin = {}
            )
        }
    }
}
