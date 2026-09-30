package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
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
import com.example.PreviewWrapper
import com.example.security.BiometricAuthManager
import com.example.security.BiometricAuthResult
import com.example.security.BiometricCapability
import com.example.ui.theme.ZamaAmberPulse
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen

/**
 * Represents the visual authentication state displayed by the [BiometricPrompt] Composable.
 */
enum class BiometricPromptVisualState {
    WAITING_FOR_AUTH,
    AUTHENTICATING_PROMPT_ACTIVE,
    AUTH_SUCCESS,
    AUTH_FAILED,
    AUTH_ERROR
}

/**
 * Reusable [BiometricPrompt] Composable that displays a rich visual state indicating the app
 * is waiting for user authentication, integrated with [BiometricAuthManager].
 *
 * It queries hardware readiness via [BiometricAuthManager.checkBiometricCapability], displays an
 * animated biometric radar visualizer with real-time waiting status indicators, and launches the
 * native `androidx.biometric.BiometricPrompt` dialog when triggered or auto-started.
 */
@Composable
fun BiometricPrompt(
    modifier: Modifier = Modifier,
    biometricAuthManager: BiometricAuthManager? = null,
    title: String = BiometricAuthManager.DEFAULT_TITLE,
    subtitle: String = BiometricAuthManager.DEFAULT_SUBTITLE,
    description: String = BiometricAuthManager.DEFAULT_DESCRIPTION,
    isWaitingForAuthentication: Boolean = true,
    autoTriggerOnLaunch: Boolean = false,
    externalStatusMessage: String? = null,
    onAuthenticationSuccess: () -> Unit = {},
    onAuthenticationError: (errorCode: Int, errString: String) -> Unit = { _, _ -> },
    onAuthenticationFailed: () -> Unit = {},
    onAuthenticationCancelled: () -> Unit = {},
    onResult: (BiometricAuthResult) -> Unit = {},
    onUseFallbackPin: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val fragmentActivity = context as? FragmentActivity

    val manager = remember(biometricAuthManager, context) {
        biometricAuthManager ?: BiometricAuthManager(context.applicationContext ?: context)
    }

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

    var visualState by remember(isWaitingForAuthentication) {
        mutableStateOf(
            if (isWaitingForAuthentication) {
                BiometricPromptVisualState.WAITING_FOR_AUTH
            } else {
                BiometricPromptVisualState.AUTH_SUCCESS
            }
        )
    }
    var feedbackMessage by remember(externalStatusMessage) {
        mutableStateOf(externalStatusMessage)
    }

    val triggerBiometricAuth: () -> Unit = {
        visualState = BiometricPromptVisualState.AUTHENTICATING_PROMPT_ACTIVE
        feedbackMessage = "Waiting for biometric sensor verification..."

        if (fragmentActivity != null) {
            manager.authenticate(
                activity = fragmentActivity,
                title = title,
                subtitle = subtitle,
                description = description,
                onSuccess = {
                    visualState = BiometricPromptVisualState.AUTH_SUCCESS
                    feedbackMessage = "Biometric identity verified."
                    onAuthenticationSuccess()
                    onResult(BiometricAuthResult.Success("Biometric authentication successful"))
                },
                onError = { errorCode, errString ->
                    visualState = BiometricPromptVisualState.AUTH_ERROR
                    feedbackMessage = errString
                    onAuthenticationError(errorCode, errString)
                    onResult(BiometricAuthResult.Error(errorCode, errString))
                },
                onFailed = {
                    visualState = BiometricPromptVisualState.AUTH_FAILED
                    feedbackMessage = "Biometric not recognized. Please try again or use Owner PIN."
                    onAuthenticationFailed()
                    onResult(BiometricAuthResult.Failed)
                },
                onCancelled = {
                    visualState = BiometricPromptVisualState.WAITING_FOR_AUTH
                    feedbackMessage = "Authentication paused. Tap sensor to resume."
                    onAuthenticationCancelled()
                    onResult(BiometricAuthResult.Cancelled)
                }
            )
        } else if (!isInspection) {
            visualState = BiometricPromptVisualState.WAITING_FOR_AUTH
            feedbackMessage = "Awaiting biometric sensor interaction..."
        }
    }

    LaunchedEffect(autoTriggerOnLaunch, capability, fragmentActivity) {
        if (
            autoTriggerOnLaunch &&
            isWaitingForAuthentication &&
            capability is BiometricCapability.Available &&
            fragmentActivity != null
        ) {
            triggerBiometricAuth()
        }
    }

    val statusColor = when (visualState) {
        BiometricPromptVisualState.WAITING_FOR_AUTH -> ZamaElectricCyan
        BiometricPromptVisualState.AUTHENTICATING_PROMPT_ACTIVE -> ZamaNeonGreen
        BiometricPromptVisualState.AUTH_SUCCESS -> ZamaNeonGreen
        BiometricPromptVisualState.AUTH_FAILED -> ZamaAmberPulse
        BiometricPromptVisualState.AUTH_ERROR -> Color(0xFFFF5252)
    }

    val statusHeadline = when (visualState) {
        BiometricPromptVisualState.WAITING_FOR_AUTH -> "WAITING FOR USER AUTHENTICATION"
        BiometricPromptVisualState.AUTHENTICATING_PROMPT_ACTIVE -> "VERIFYING BIOMETRIC CREDENTIALS..."
        BiometricPromptVisualState.AUTH_SUCCESS -> "AUTHENTICATION VERIFIED"
        BiometricPromptVisualState.AUTH_FAILED -> "BIOMETRIC MATCH FAILED • RETRYING"
        BiometricPromptVisualState.AUTH_ERROR -> "AUTHENTICATION REQUIRED"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("biometric_prompt_composable"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF070C14)
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Status Pill indicating waiting state
            Surface(
                color = statusColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("biometric_prompt_waiting_indicator")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = statusHeadline,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.testTag("biometric_prompt_status_text")
                    )
                }
            }

            // Animated Biometric Radar & Sensor Ring
            BiometricWaitingVisualizer(
                visualState = visualState,
                accentColor = statusColor,
                onSensorClick = triggerBiometricAuth
            )

            // Title & Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    color = ZamaNeonGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = description,
                    color = ZamaChromeMid,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Indeterminate scanning bar while waiting for user authentication
            AnimatedVisibility(
                visible = visualState == BiometricPromptVisualState.WAITING_FOR_AUTH ||
                    visualState == BiometricPromptVisualState.AUTHENTICATING_PROMPT_ACTIVE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("biometric_prompt_progress_bar"),
                        color = statusColor,
                        trackColor = Color(0xFF142234)
                    )
                    Text(
                        text = when (capability) {
                            is BiometricCapability.Available ->
                                "Touch fingerprint sensor or look at camera to authenticate"
                            is BiometricCapability.NoneEnrolled ->
                                "No biometrics enrolled • Device credential or Owner PIN ready"
                            else ->
                                "Hardware sensor unavailable • Owner PIN verification active"
                        },
                        color = ZamaChromeMid,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Optional feedback/error banner
            val activeFeedback = externalStatusMessage ?: feedbackMessage
            if (!activeFeedback.isNullOrBlank()) {
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("biometric_prompt_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (visualState) {
                                BiometricPromptVisualState.AUTH_SUCCESS -> Icons.Default.CheckCircle
                                BiometricPromptVisualState.AUTH_ERROR,
                                BiometricPromptVisualState.AUTH_FAILED -> Icons.Default.Warning
                                else -> Icons.Default.Shield
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = activeFeedback,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Action Buttons (Minimum 48.dp touch targets for accessibility)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = triggerBiometricAuth,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZamaNeonGreen,
                        contentColor = Color(0xFF040A06)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("biometric_prompt_authenticate_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Authenticate with Biometrics",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (visualState == BiometricPromptVisualState.AUTHENTICATING_PROMPT_ACTIVE) {
                                "WAITING FOR SENSOR..."
                            } else {
                                "AUTHENTICATE WITH BIOMETRICS"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                if (onUseFallbackPin != null) {
                    OutlinedButton(
                        onClick = onUseFallbackPin,
                        border = BorderStroke(1.dp, ZamaBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("biometric_prompt_fallback_button")
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

/**
 * Animated radar visualizer that communicates the waiting-for-authentication state.
 */
@Composable
private fun BiometricWaitingVisualizer(
    visualState: BiometricPromptVisualState,
    accentColor: Color,
    onSensorClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "biometric_waiting_transition")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sensor_pulse"
    )

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .clickable(
                onClickLabel = "Trigger biometric sensor authentication",
                onClick = onSensorClick
            )
            .testTag("biometric_prompt_sensor_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (size.minDimension / 2f) - 6f

            drawCircle(
                color = accentColor.copy(alpha = 0.18f),
                radius = radius,
                style = Stroke(width = 1.5f)
            )

            drawCircle(
                color = ZamaNeonGreen.copy(alpha = 0.25f),
                radius = radius * 0.76f,
                style = Stroke(width = 1.5f)
            )

            if (
                visualState == BiometricPromptVisualState.WAITING_FOR_AUTH ||
                visualState == BiometricPromptVisualState.AUTHENTICATING_PROMPT_ACTIVE
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.25f),
                            accentColor
                        )
                    ),
                    startAngle = sweepAngle,
                    sweepAngle = 105f,
                    useCenter = false,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(
                    if (
                        visualState == BiometricPromptVisualState.WAITING_FOR_AUTH ||
                        visualState == BiometricPromptVisualState.AUTHENTICATING_PROMPT_ACTIVE
                    ) {
                        pulseScale
                    } else {
                        1f
                    }
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF0D251A), Color(0xFF040A06))
                    )
                )
                .border(1.2.dp, accentColor.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (visualState) {
                    BiometricPromptVisualState.AUTH_SUCCESS -> Icons.Default.CheckCircle
                    BiometricPromptVisualState.AUTH_ERROR -> Icons.Default.Lock
                    else -> Icons.Default.Fingerprint
                },
                contentDescription = "Biometric Sensor Waiting Indicator",
                tint = accentColor,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "BiometricPrompt Waiting State (Dark)")
@Composable
fun BiometricPromptWaitingPreview() {
    PreviewWrapper(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            BiometricPrompt(
                isWaitingForAuthentication = true,
                onUseFallbackPin = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "BiometricPrompt Waiting State (Light)")
@Composable
fun BiometricPromptLightPreview() {
    PreviewWrapper(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            BiometricPrompt(
                isWaitingForAuthentication = true,
                externalStatusMessage = "Waiting for fingerprint or face unlock verification...",
                onUseFallbackPin = {}
            )
        }
    }
}
