package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.PreviewWrapper
import com.example.security.AuthRepository
import com.example.security.PinSetupResult
import com.example.security.PinVerificationResult
import com.example.ui.theme.ZamaAmberPulse
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen

/**
 * Represents the active stage in the cryptographically salted PIN screen.
 */
enum class SaltedPinScreenStage {
    CREATE_ENTER_NEW,
    CREATE_CONFIRM_NEW,
    VERIFY_EXISTING,
    AUTHENTICATED_SUCCESS
}

/**
 * Secure, cryptographically salted PIN creation and verification screen designed as a fallback
 * to biometric authentication.
 *
 * Integrates directly with [AuthRepository] to ensure:
 * 1. The Owner PIN is never stored or logged in plaintext.
 * 2. Creation derives a 256-bit PBKDF2-HMAC-SHA256 verifier using a unique 128-bit cryptographic
 *    salt (`SecureRandom`) wrapped in Android Keystore AES-256-GCM inside `EncryptedSharedPreferences`.
 * 3. Verification uses constant-time byte comparison (`MessageDigest.isEqual`) and enforces
 *    exponential brute-force lockout cooldowns.
 */
@Composable
fun SaltedPinFallbackScreen(
    modifier: Modifier = Modifier,
    authRepository: AuthRepository? = null,
    pinLength: Int = 4,
    autoSubmitOnPinLength: Boolean = true,
    onPinAuthenticated: () -> Unit = {},
    onPinCreated: () -> Unit = {},
    onBackToBiometrics: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current

    val repository = remember(authRepository, context, isInspection) {
        if (authRepository != null) {
            authRepository
        } else if (isInspection) {
            null
        } else {
            AuthRepository(context.applicationContext ?: context)
        }
    }

    var isPinAlreadyConfigured by remember(repository) {
        mutableStateOf(repository?.isPinConfigured() ?: false)
    }

    var stage by remember(isPinAlreadyConfigured) {
        mutableStateOf(
            if (isPinAlreadyConfigured) {
                SaltedPinScreenStage.VERIFY_EXISTING
            } else {
                SaltedPinScreenStage.CREATE_ENTER_NEW
            }
        )
    }

    var enteredPin by remember { mutableStateOf("") }
    var draftPinForConfirmation by remember { mutableStateOf<String?>(null) }
    var statusBannerText by remember { mutableStateOf<String?>(null) }
    var isErrorBanner by remember { mutableStateOf(false) }
    var failedAttemptsCount by remember(repository) {
        mutableIntStateOf(repository?.getFailedAttempts() ?: 0)
    }
    var isLockedOut by remember(repository) {
        mutableStateOf(repository?.isLockedOut() ?: false)
    }

    BackHandler(enabled = stage == SaltedPinScreenStage.CREATE_CONFIRM_NEW || onBackToBiometrics != null) {
        if (stage == SaltedPinScreenStage.CREATE_CONFIRM_NEW) {
            draftPinForConfirmation = null
            enteredPin = ""
            statusBannerText = "PIN setup reset. Enter a new 4-digit Owner PIN."
            isErrorBanner = false
            stage = SaltedPinScreenStage.CREATE_ENTER_NEW
        } else {
            onBackToBiometrics?.invoke()
        }
    }

    val submitCurrentPin: (String) -> Unit = { candidatePin ->
        when (stage) {
            SaltedPinScreenStage.CREATE_ENTER_NEW -> {
                draftPinForConfirmation = candidatePin
                enteredPin = ""
                isErrorBanner = false
                statusBannerText = "Re-enter your $pinLength-digit Owner PIN to generate salted PBKDF2 hash."
                stage = SaltedPinScreenStage.CREATE_CONFIRM_NEW
            }

            SaltedPinScreenStage.CREATE_CONFIRM_NEW -> {
                val firstDraft = draftPinForConfirmation.orEmpty()
                val setupResult = repository?.setupPin(firstDraft, candidatePin)
                    ?: if (firstDraft == candidatePin) PinSetupResult.Success else PinSetupResult.ValidationError("PINs do not match.")

                when (setupResult) {
                    is PinSetupResult.Success -> {
                        draftPinForConfirmation = null
                        enteredPin = ""
                        isPinAlreadyConfigured = true
                        isErrorBanner = false
                        failedAttemptsCount = 0
                        isLockedOut = false
                        statusBannerText = "Salted PBKDF2-HMAC-SHA256 verifier stored in EncryptedSharedPreferences."
                        stage = SaltedPinScreenStage.AUTHENTICATED_SUCCESS
                        onPinCreated()
                        onPinAuthenticated()
                    }
                    is PinSetupResult.ValidationError -> {
                        draftPinForConfirmation = null
                        enteredPin = ""
                        isErrorBanner = true
                        statusBannerText = setupResult.message
                        stage = SaltedPinScreenStage.CREATE_ENTER_NEW
                    }
                }
            }

            SaltedPinScreenStage.VERIFY_EXISTING -> {
                val verifyResult = repository?.verifyPin(candidatePin)
                    ?: PinVerificationResult.Success

                when (verifyResult) {
                    is PinVerificationResult.Success -> {
                        enteredPin = ""
                        isErrorBanner = false
                        failedAttemptsCount = 0
                        isLockedOut = false
                        statusBannerText = "Owner PIN verified via constant-time salted hash comparison."
                        stage = SaltedPinScreenStage.AUTHENTICATED_SUCCESS
                        onPinAuthenticated()
                    }
                    is PinVerificationResult.InvalidPin -> {
                        enteredPin = ""
                        isErrorBanner = true
                        failedAttemptsCount = verifyResult.failedAttempts
                        isLockedOut = false
                        statusBannerText = "Incorrect Owner PIN (${verifyResult.attemptsRemainingBeforeCooldown} attempts left before cooldown)."
                    }
                    is PinVerificationResult.LockedOut -> {
                        enteredPin = ""
                        isErrorBanner = true
                        failedAttemptsCount = verifyResult.failedAttempts
                        isLockedOut = true
                        statusBannerText = "Too many failed attempts. Locked out for ${verifyResult.remainingSeconds}s."
                    }
                    is PinVerificationResult.CorruptedStorage -> {
                        enteredPin = ""
                        isErrorBanner = true
                        statusBannerText = "Credential integrity check failed. Reset and create a new Owner PIN."
                        repository?.clearPin()
                        isPinAlreadyConfigured = false
                        stage = SaltedPinScreenStage.CREATE_ENTER_NEW
                    }
                    is PinVerificationResult.NotConfigured -> {
                        enteredPin = ""
                        isErrorBanner = false
                        isPinAlreadyConfigured = false
                        stage = SaltedPinScreenStage.CREATE_ENTER_NEW
                    }
                }
            }

            SaltedPinScreenStage.AUTHENTICATED_SUCCESS -> {
                onPinAuthenticated()
            }
        }
    }

    val handleDigitInput: (String) -> Unit = { key ->
        if (!isLockedOut || stage != SaltedPinScreenStage.VERIFY_EXISTING) {
            when (key) {
                "C" -> {
                    enteredPin = ""
                }
                "DEL" -> {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                    }
                }
                else -> {
                    if (enteredPin.length < pinLength) {
                        val updated = enteredPin + key
                        enteredPin = updated
                        if (autoSubmitOnPinLength && updated.length == pinLength) {
                            submitCurrentPin(updated)
                        }
                    }
                }
            }
        }
    }

    val accentColor = when {
        stage == SaltedPinScreenStage.AUTHENTICATED_SUCCESS -> ZamaNeonGreen
        isErrorBanner || isLockedOut -> Color(0xFFFF5252)
        stage == SaltedPinScreenStage.VERIFY_EXISTING -> ZamaElectricCyan
        else -> ZamaNeonGreen
    }

    val stageHeadline = when (stage) {
        SaltedPinScreenStage.CREATE_ENTER_NEW -> "CREATE SALTED OWNER PIN"
        SaltedPinScreenStage.CREATE_CONFIRM_NEW -> "CONFIRM SALTED OWNER PIN"
        SaltedPinScreenStage.VERIFY_EXISTING -> "VERIFY OWNER PIN FALLBACK"
        SaltedPinScreenStage.AUTHENTICATED_SUCCESS -> "PIN CREDENTIAL VERIFIED"
    }

    val stageSubtitle = when (stage) {
        SaltedPinScreenStage.CREATE_ENTER_NEW ->
            "Step 1 of 2 • Choose a strong $pinLength-digit PIN (never stored in plaintext)"
        SaltedPinScreenStage.CREATE_CONFIRM_NEW ->
            "Step 2 of 2 • Re-enter your $pinLength-digit PIN to derive salted PBKDF2 hash"
        SaltedPinScreenStage.VERIFY_EXISTING ->
            "Enter your $pinLength-digit Owner PIN to unlock Zama Business Hub"
        SaltedPinScreenStage.AUTHENTICATED_SUCCESS ->
            "Cryptographic PBKDF2-HMAC-SHA256 verification succeeded"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("salted_pin_fallback_screen"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070C15)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cryptographic Enclave Telemetry Badge
            Surface(
                color = accentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("salted_pin_crypto_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "PBKDF2-HMAC-SHA256 • 128-BIT SALT • ZERO PLAINTEXT",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Stage Title & Subtitle
            AnimatedContent(
                targetState = stageHeadline to stageSubtitle,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "salted_pin_stage_header"
            ) { (headline, sub) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = headline,
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("salted_pin_stage_title")
                    )
                    Text(
                        text = sub,
                        color = ZamaChromeLight,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("salted_pin_stage_subtitle")
                    )
                }
            }

            // Masked PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .testTag("salted_pin_dots_row")
            ) {
                for (i in 0 until pinLength) {
                    val isFilled = i < enteredPin.length || stage == SaltedPinScreenStage.AUTHENTICATED_SUCCESS
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) accentColor else Color(0x1FFFFFFF)
                            )
                            .border(
                                width = 1.2.dp,
                                color = if (isFilled) accentColor else Color(0x55FFFFFF),
                                shape = CircleShape
                            )
                            .testTag("salted_pin_dot_$i")
                    )
                }
            }

            // Feedback / Validation / Lockout Banner
            if (!statusBannerText.isNullOrBlank()) {
                val bannerColor = if (isErrorBanner || isLockedOut) Color(0xFFFF5252) else ZamaNeonGreen
                Surface(
                    color = bannerColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("salted_pin_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                stage == SaltedPinScreenStage.AUTHENTICATED_SUCCESS -> Icons.Default.CheckCircle
                                isErrorBanner || isLockedOut -> Icons.Default.Warning
                                else -> Icons.Default.Key
                            },
                            contentDescription = null,
                            tint = bannerColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusBannerText.orEmpty(),
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("salted_pin_feedback_text")
                        )
                    }
                }
            }

            // Numeric Tactical Keypad Grid (Minimum 48.dp touch targets)
            val keypadRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "DEL")
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("salted_pin_keypad_grid"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                keypadRows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowKeys.forEach { key ->
                            Surface(
                                color = Color(0xFF0E1726),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ZamaBorder.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        enabled = stage != SaltedPinScreenStage.AUTHENTICATED_SUCCESS,
                                        onClick = { handleDigitInput(key) }
                                    )
                                    .testTag("salted_pin_key_$key")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete Digit",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            color = if (key == "C") ZamaAmberPulse else Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Manual Submit Button when autoSubmitOnPinLength is false or for accessibility
            if (!autoSubmitOnPinLength) {
                Button(
                    onClick = {
                        if (enteredPin.length >= 4) {
                            submitCurrentPin(enteredPin)
                        }
                    },
                    enabled = enteredPin.length >= 4,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZamaNeonGreen,
                        contentColor = Color(0xFF040A06)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("salted_pin_submit_button")
                ) {
                    Text(
                        text = if (stage == SaltedPinScreenStage.VERIFY_EXISTING) {
                            "VERIFY SALTED PIN"
                        } else {
                            "SAVE SALTED PIN"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Return to Biometrics Button
            if (onBackToBiometrics != null) {
                HorizontalDivider(color = ZamaBorder.copy(alpha = 0.45f))
                OutlinedButton(
                    onClick = onBackToBiometrics,
                    border = BorderStroke(1.dp, ZamaElectricCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("salted_pin_back_to_biometrics_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Return to Biometric Authentication",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "USE BIOMETRIC SENSOR INSTEAD",
                            color = ZamaElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Cryptographic Storage Guarantee Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = ZamaChromeMid,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "60,000-Iteration PBKDF2 + Android Keystore AES-256-GCM",
                    color = ZamaChromeMid,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "SaltedPinFallbackScreen - Creation Flow")
@Composable
fun SaltedPinFallbackScreenCreatePreview() {
    PreviewWrapper(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            SaltedPinFallbackScreen(
                onBackToBiometrics = {}
            )
        }
    }
}
