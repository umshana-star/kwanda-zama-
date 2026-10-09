package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AuthRepository
import com.example.security.PinSetupResult
import com.example.security.PinVerificationResult
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaCardSurface
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaElevated
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaVoid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SaltedPinMode {
    SETUP_CREATE,
    SETUP_CONFIRM,
    VERIFY
}

/**
 * High-security Compose screen for creating and verifying cryptographically salted PINs
 * as a robust fallback to biometric authentication.
 *
 * Ensures the PIN is never stored in plaintext and directly integrates with [AuthRepository].
 */
@Composable
fun SaltedPinAuthScreen(
    authRepository: AuthRepository,
    initialMode: SaltedPinMode = if (authRepository.isPinConfigured()) SaltedPinMode.VERIFY else SaltedPinMode.SETUP_CREATE,
    onAuthenticationSuccess: () -> Unit = {},
    onBiometricFallbackRequested: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(initialMode) }
    var currentPinInput by remember { mutableStateOf("") }
    var initialCreatedPin by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorState by remember { mutableStateOf(false) }
    var isSuccessState by remember { mutableStateOf(false) }

    // Lockout countdown timer
    var lockoutSeconds by remember { mutableLongStateOf(authRepository.getLockoutRemainingSeconds()) }
    val isLockedOut = lockoutSeconds > 0

    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    // Auto-decrement lockout timer
    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = authRepository.getLockoutRemainingSeconds()
            if (lockoutSeconds == 0L) {
                isErrorState = false
                statusMessage = null
            }
        }
    }

    fun triggerShake() {
        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -20f at 50
                    20f at 100
                    -16f at 150
                    16f at 200
                    -8f at 250
                    8f at 300
                    0f at 400
                }
            )
        }
    }

    fun handlePinComplete(pin: String) {
        if (isLockedOut) return

        when (mode) {
            SaltedPinMode.SETUP_CREATE -> {
                val validation = authRepository.createPin(pin)
                // Just dry-run validate the PIN strength before asking for confirmation
                when (validation) {
                    is PinSetupResult.Error -> {
                        isErrorState = true
                        statusMessage = validation.message
                        currentPinInput = ""
                        triggerShake()
                    }
                    is PinSetupResult.Success -> {
                        // Temporarily hold candidate in memory, clear repository until confirmation
                        authRepository.clearPin()
                        initialCreatedPin = pin
                        mode = SaltedPinMode.SETUP_CONFIRM
                        currentPinInput = ""
                        statusMessage = null
                        isErrorState = false
                    }
                }
            }

            SaltedPinMode.SETUP_CONFIRM -> {
                if (pin == initialCreatedPin) {
                    val result = authRepository.createPin(pin)
                    when (result) {
                        is PinSetupResult.Success -> {
                            isSuccessState = true
                            statusMessage = "PIN created and encrypted successfully."
                            coroutineScope.launch {
                                delay(600)
                                onAuthenticationSuccess()
                            }
                        }
                        is PinSetupResult.Error -> {
                            isErrorState = true
                            statusMessage = result.message
                            currentPinInput = ""
                            triggerShake()
                        }
                    }
                } else {
                    isErrorState = true
                    statusMessage = "PINs do not match. Please re-enter."
                    currentPinInput = ""
                    triggerShake()
                    mode = SaltedPinMode.SETUP_CREATE
                    initialCreatedPin = ""
                }
            }

            SaltedPinMode.VERIFY -> {
                val result = authRepository.verifyPin(pin)
                when (result) {
                    is PinVerificationResult.Success -> {
                        isSuccessState = true
                        isErrorState = false
                        statusMessage = "PIN Verified • Access Granted"
                        coroutineScope.launch {
                            delay(500)
                            onAuthenticationSuccess()
                        }
                    }
                    is PinVerificationResult.Incorrect -> {
                        isErrorState = true
                        statusMessage = "Incorrect PIN. ${result.remainingAttempts} attempt(s) remaining."
                        currentPinInput = ""
                        triggerShake()
                    }
                    is PinVerificationResult.LockedOut -> {
                        isErrorState = true
                        lockoutSeconds = result.remainingSeconds
                        statusMessage = "Too many failed attempts. Locked out for ${result.remainingSeconds}s."
                        currentPinInput = ""
                        triggerShake()
                    }
                    is PinVerificationResult.Error -> {
                        isErrorState = true
                        statusMessage = result.message
                        currentPinInput = ""
                        triggerShake()
                    }
                }
            }
        }
    }

    fun onKeypadPress(digit: String) {
        if (isLockedOut || isSuccessState) return
        if (currentPinInput.length < AuthRepository.PIN_LENGTH) {
            val updated = currentPinInput + digit
            currentPinInput = updated
            if (updated.length == AuthRepository.PIN_LENGTH) {
                handlePinComplete(updated)
            }
        }
    }

    fun onBackspace() {
        if (isLockedOut || isSuccessState) return
        if (currentPinInput.isNotEmpty()) {
            currentPinInput = currentPinInput.dropLast(1)
            isErrorState = false
            statusMessage = null
        }
    }

    fun onClear() {
        if (isLockedOut || isSuccessState) return
        currentPinInput = ""
        isErrorState = false
        statusMessage = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        ZamaVoid,
                        ZamaDarkSurface,
                        ZamaCardSurface
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("pin_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Shield / Lock Badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSuccessState) ZamaNeonGreen.copy(alpha = 0.15f)
                            else if (isErrorState) Color(0xFFFF5252).copy(alpha = 0.15f)
                            else ZamaElectricCyan.copy(alpha = 0.12f)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isSuccessState) ZamaNeonGreen
                            else if (isErrorState) Color(0xFFFF5252)
                            else ZamaElectricCyan.copy(alpha = 0.8f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuccessState) Icons.Default.CheckCircle
                        else if (mode == SaltedPinMode.VERIFY) Icons.Default.Lock
                        else Icons.Default.Shield,
                        contentDescription = "Security Status Icon",
                        tint = if (isSuccessState) ZamaNeonGreen
                        else if (isErrorState) Color(0xFFFF5252)
                        else ZamaElectricCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        isLockedOut -> "Security Lockout Active"
                        mode == SaltedPinMode.SETUP_CREATE -> "Create Master PIN"
                        mode == SaltedPinMode.SETUP_CONFIRM -> "Confirm Master PIN"
                        else -> "Enter Security PIN"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZamaChromeLight,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        isLockedOut -> "Please wait $lockoutSeconds seconds before re-attempting."
                        mode == SaltedPinMode.SETUP_CREATE -> "6-digit cryptographically salted master key"
                        mode == SaltedPinMode.SETUP_CONFIRM -> "Re-enter the 6 digits to verify setup"
                        else -> "Biometric fallback authentication"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isLockedOut) Color(0xFFFF8A80) else ZamaChromeMid
                    ),
                    textAlign = TextAlign.Center
                )

                // Salt / Crypto indicator pill
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ZamaElevated.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZamaBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PBKDF2-HMAC-SHA256 • 128-bit Salt",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ZamaElectricCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // PIN Dots Indicator & Status Message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                PinDotsIndicator(
                    pinLength = AuthRepository.PIN_LENGTH,
                    enteredLength = currentPinInput.length,
                    isError = isErrorState,
                    isSuccess = isSuccessState,
                    modifier = Modifier.testTag("pin_dots_row")
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = statusMessage != null) {
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isSuccessState) ZamaNeonGreen else if (isErrorState) Color(0xFFFF5252) else ZamaChromeMid,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("pin_status_message")
                    )
                }
            }

            // Keypad Component
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                PinNumericKeypad(
                    onDigitClick = { onKeypadPress(it) },
                    onBackspaceClick = { onBackspace() },
                    onClearClick = { onClear() },
                    onBiometricClick = onBiometricFallbackRequested,
                    isLockedOut = isLockedOut
                )

                // Optional Bottom Action (Biometric switch or Mode reset)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBiometricFallbackRequested != null && mode == SaltedPinMode.VERIFY) {
                        OutlinedButton(
                            onClick = onBiometricFallbackRequested,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ZamaElectricCyan.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ZamaElectricCyan
                            ),
                            modifier = Modifier.testTag("biometric_fallback_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Use Biometrics",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Biometrics")
                        }
                    }

                    if (onCancel != null) {
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ZamaBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ZamaChromeMid
                            ),
                            modifier = Modifier.testTag("pin_cancel_button")
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated 6-dot indicator displaying the currently entered PIN characters.
 */
@Composable
fun PinDotsIndicator(
    pinLength: Int,
    enteredLength: Int,
    isError: Boolean,
    isSuccess: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until pinLength) {
            val isFilled = i < enteredLength
            val dotColor = when {
                isSuccess -> ZamaNeonGreen
                isError -> Color(0xFFFF5252)
                isFilled -> ZamaElectricCyan
                else -> Color.Transparent
            }
            val borderColor = when {
                isSuccess -> ZamaNeonGreen
                isError -> Color(0xFFFF5252)
                isFilled -> ZamaElectricCyan
                else -> ZamaBorder
            }

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * 3x4 numeric keypad tailored for security PIN authentication with minimum 48.dp touch targets.
 */
@Composable
fun PinNumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onBiometricClick: (() -> Unit)?,
    isLockedOut: Boolean,
    modifier: Modifier = Modifier
) {
    val keypadLayout = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keypadLayout.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    KeypadButton(
                        text = digit,
                        testTag = "pin_key_$digit",
                        enabled = !isLockedOut,
                        onClick = { onDigitClick(digit) }
                    )
                }
            }
        }

        // Bottom Row: [Biometric / Clear], [0], [Backspace]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action: Biometrics or Clear
            if (onBiometricClick != null) {
                KeypadIconButton(
                    icon = Icons.Default.Fingerprint,
                    contentDescription = "Authenticate with Biometrics",
                    testTag = "pin_key_biometric",
                    enabled = !isLockedOut,
                    onClick = onBiometricClick
                )
            } else {
                KeypadIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Clear PIN",
                    testTag = "pin_key_clear",
                    enabled = !isLockedOut,
                    onClick = onClearClick
                )
            }

            // Digit 0
            KeypadButton(
                text = "0",
                testTag = "pin_key_0",
                enabled = !isLockedOut,
                onClick = { onDigitClick("0") }
            )

            // Right Action: Backspace
            KeypadIconButton(
                icon = Icons.Default.Backspace,
                contentDescription = "Delete last digit",
                testTag = "pin_key_backspace",
                enabled = !isLockedOut,
                onClick = onBackspaceClick
            )
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    testTag: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) ZamaElevated.copy(alpha = 0.85f) else ZamaDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ZamaBorder),
        modifier = Modifier
            .size(72.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) ZamaChromeLight else ZamaChromeMid.copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .size(72.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) ZamaElectricCyan else ZamaChromeMid.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
