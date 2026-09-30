package com.example.security

import android.app.Application
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * Result of setting up or changing the Owner PIN.
 */
sealed class PinSetupResult {
    data object Success : PinSetupResult()
    data class ValidationError(val message: String) : PinSetupResult()
}

/**
 * Wraps the derived PBKDF2 verifier bytes with an Android Keystore AES-256-GCM key
 * when running on an Android device with hardware/OS AndroidKeyStore support.
 */
object AndroidKeystoreEnvelope {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "zama_owner_pin_verifier_aes256_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12
    private const val ENVELOPE_PREFIX = "AKSGCM1:"

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existing != null) {
                existing.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun wrapVerifier(rawVerifier: ByteArray): String {
        val secretKey = getOrCreateSecretKey()
        if (secretKey != null) {
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val cipherText = cipher.doFinal(rawVerifier)
                if (iv != null && iv.size == GCM_IV_BYTES) {
                    val combined = ByteArray(iv.size + cipherText.size)
                    System.arraycopy(iv, 0, combined, 0, iv.size)
                    System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
                    return ENVELOPE_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
                }
            } catch (_: Throwable) {
                // Fall through to standard Base64 encoding when running in local JVM unit tests
            }
        }
        return Base64.encodeToString(rawVerifier, Base64.NO_WRAP)
    }

    fun unwrapVerifier(storedVerifier: String): ByteArray? {
        return try {
            if (storedVerifier.startsWith(ENVELOPE_PREFIX)) {
                val secretKey = getOrCreateSecretKey() ?: return null
                val payload = Base64.decode(storedVerifier.removePrefix(ENVELOPE_PREFIX), Base64.NO_WRAP)
                if (payload.size <= GCM_IV_BYTES) return null
                val iv = payload.copyOfRange(0, GCM_IV_BYTES)
                val cipherText = payload.copyOfRange(GCM_IV_BYTES, payload.size)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                cipher.doFinal(cipherText)
            } else {
                Base64.decode(storedVerifier, Base64.NO_WRAP)
            }
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * Cryptographic helper for salted PBKDF2-HMAC-SHA256 Owner PIN verification.
 * Never stores or logs plaintext PINs.
 */
object PinVerifierCrypto {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val SALT_LENGTH_BYTES = 16
    private const val HASH_ITERATIONS = 65_536
    private const val KEY_LENGTH_BITS = 256
    const val VERIFIER_LENGTH_BYTES = KEY_LENGTH_BITS / 8

    private val DISALLOWED_WEAK_PINS = setOf(
        "0000", "1111", "2222", "3333", "4444",
        "5555", "6666", "7777", "8888", "9999",
        "1234", "4321", "0123", "2580", "123456",
        "654321", "000000", "111111"
    )

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun computeVerifier(pin: String, salt: ByteArray): ByteArray {
        val chars = pin.toCharArray()
        val spec = PBEKeySpec(chars, salt, HASH_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            skf.generateSecret(spec).encoded
        } catch (_: Exception) {
            // Fallback to multi-round salted SHA-256 if PBKDF2 provider is unavailable
            val digest = MessageDigest.getInstance("SHA-256")
            var block = salt + pin.toByteArray(Charsets.UTF_8)
            repeat(4096) {
                digest.reset()
                digest.update(salt)
                block = digest.digest(block)
            }
            block
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    fun isValidStoredCredential(saltBase64: String?, verifierStored: String?): Boolean {
        if (saltBase64.isNullOrBlank() || verifierStored.isNullOrBlank()) return false
        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val verifier = AndroidKeystoreEnvelope.unwrapVerifier(verifierStored)
            salt != null && salt.size == SALT_LENGTH_BYTES &&
                verifier != null && verifier.size == VERIFIER_LENGTH_BYTES
        } catch (_: Throwable) {
            false
        }
    }

    fun verifyPinConstantTime(candidatePin: String, saltBase64: String, verifierStored: String): Boolean {
        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val expectedVerifier = AndroidKeystoreEnvelope.unwrapVerifier(verifierStored) ?: return false
            if (salt.size != SALT_LENGTH_BYTES || expectedVerifier.size != VERIFIER_LENGTH_BYTES) return false
            val candidateVerifier = computeVerifier(candidatePin, salt)
            try {
                MessageDigest.isEqual(candidateVerifier, expectedVerifier)
            } finally {
                candidateVerifier.fill(0)
                expectedVerifier.fill(0)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun validateNewPin(newPin: String, confirmPin: String): PinSetupResult {
        val trimmed = newPin.trim()
        val trimmedConfirm = confirmPin.trim()
        if (trimmed.length !in 4..8 || !trimmed.all { it.isDigit() }) {
            return PinSetupResult.ValidationError("Owner PIN must be 4 to 8 numeric digits.")
        }
        if (trimmed != trimmedConfirm) {
            return PinSetupResult.ValidationError("PIN confirmation does not match.")
        }
        if (trimmed in DISALLOWED_WEAK_PINS || trimmed.toSet().size == 1) {
            return PinSetupResult.ValidationError("Choose a stronger PIN. Repeated or sequential digits are not allowed.")
        }
        return PinSetupResult.Success
    }
}

/**
 * Security audit event recorded when authentication is attempted or settings are changed.
 */
data class SecurityAuditEntry(
    val id: String = System.currentTimeMillis().toString(),
    val timestamp: String,
    val eventType: String,      // "BIOMETRIC_UNLOCK", "PIN_UNLOCK", "PIN_CONFIGURED", "SESSION_LOCKED", "AUTH_FAILED", "LOCKOUT_TRIGGERED"
    val methodUsed: String,     // "Fingerprint / Face Unlock", "Owner PIN (PBKDF2-SHA256)", "Auto-Lock Timer"
    val isSuccess: Boolean,
    val detailNotes: String
)

/**
 * UI State for Biometric & Salted Owner PIN Security.
 */
data class BiometricSecurityUiState(
    val isUnlocked: Boolean = false,
    val isBiometricProtectionEnabled: Boolean = true,
    val isPinConfigured: Boolean = false,
    val hardwareCapability: BiometricCapability = BiometricCapability.NotSupported,
    val autoLockDurationMinutes: Int = 5,
    val lastUnlockedTimestamp: Long = 0L,
    val failedAttempts: Int = 0,
    val isLockedOut: Boolean = false,
    val lockoutUntilEpochMillis: Long = 0L,
    val maxFailedAttempts: Int = 5,
    val statusFeedback: String? = null,
    val isAuthenticating: Boolean = false,
    val auditLogs: List<SecurityAuditEntry> = emptyList()
) {
    fun remainingLockoutSeconds(nowMillis: Long = System.currentTimeMillis()): Int {
        if (lockoutUntilEpochMillis <= nowMillis) return 0
        return ((lockoutUntilEpochMillis - nowMillis + 999L) / 1000L).coerceAtLeast(0L).toInt()
    }
}

/**
 * ViewModel managing biometric authentication session, persistent security settings,
 * audit history, brute-force rate limiting, and salted PBKDF2-SHA256 Owner PIN verification
 * via [AuthRepository].
 */
class BiometricSecurityViewModel(
    application: Application,
    val authRepository: AuthRepository = AuthRepository(application)
) : AndroidViewModel(application) {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<BiometricSecurityUiState> = _uiState.asStateFlow()

    init {
        refreshHardwareCapability()
    }

    private fun buildInitialState(): BiometricSecurityUiState {
        val now = System.currentTimeMillis()
        val biometricEnabled = authRepository.isBiometricProtectionEnabled()
        val pinConfigured = authRepository.isPinConfigured()
        val savedFailed = authRepository.getFailedAttempts()
        val savedLockoutUntil = authRepository.getLockoutUntilMillis()
        val activeLockout = savedLockoutUntil > now

        return BiometricSecurityUiState(
            isUnlocked = !biometricEnabled,
            isBiometricProtectionEnabled = biometricEnabled,
            isPinConfigured = pinConfigured,
            autoLockDurationMinutes = authRepository.getAutoLockDurationMinutes(),
            failedAttempts = savedFailed,
            isLockedOut = activeLockout,
            lockoutUntilEpochMillis = if (activeLockout) savedLockoutUntil else 0L,
            maxFailedAttempts = MAX_FAILED_ATTEMPTS,
            auditLogs = loadInitialAuditLogs(pinConfigured)
        )
    }

    private fun hasConfiguredPinVerifier(): Boolean {
        return authRepository.isPinConfigured()
    }

    fun hasCorruptedCredentials(): Boolean {
        return authRepository.hasCorruptedCredentials()
    }

    fun refreshHardwareCapability() {
        val capability = authRepository.checkBiometricCapability()
        _uiState.update { it.copy(hardwareCapability = capability) }
    }

    /**
     * Initiates native biometric prompt (fingerprint, face unlock, or device credential) via [AuthRepository].
     */
    fun authenticateWithBiometrics(activity: FragmentActivity) {
        if (_uiState.value.isAuthenticating) return

        _uiState.update { it.copy(isAuthenticating = true, statusFeedback = null) }

        authRepository.authenticateWithBiometrics(
            activity = activity,
            title = "Unlock Zama Business Hub",
            subtitle = "Kwanda Zama Salon Owner Authentication",
            description = "Verify your fingerprint, face unlock, or device credential to access private customer triage logs and WhatsApp threads.",
            onResult = { result ->
                viewModelScope.launch {
                    when (result) {
                        is BiometricAuthResult.Success -> {
                            recordAuditEvent(
                                eventType = "BIOMETRIC_UNLOCK",
                                method = "Biometric (Fingerprint / Face)",
                                success = true,
                                details = "Authorized access to private salon triage & customer intelligence"
                            )
                            _uiState.update {
                                it.copy(
                                    isUnlocked = true,
                                    isAuthenticating = false,
                                    failedAttempts = 0,
                                    isLockedOut = false,
                                    lockoutUntilEpochMillis = 0L,
                                    lastUnlockedTimestamp = System.currentTimeMillis(),
                                    statusFeedback = "Biometric verified successfully"
                                )
                            }
                        }
                        is BiometricAuthResult.Failed -> {
                            recordAuditEvent(
                                eventType = "AUTH_FAILED",
                                method = "Biometric Sensor",
                                success = false,
                                details = "Biometric credential not recognized by device sensor"
                            )
                            _uiState.update {
                                val newFailed = it.failedAttempts + 1
                                it.copy(
                                    isAuthenticating = false,
                                    failedAttempts = newFailed,
                                    statusFeedback = "Biometric not recognized. Try again or use your Owner PIN."
                                )
                            }
                        }
                        is BiometricAuthResult.Error -> {
                            recordAuditEvent(
                                eventType = "AUTH_ERROR",
                                method = "Biometric Sensor",
                                success = false,
                                details = "Biometric prompt error code ${result.errorCode}: ${result.message}"
                            )
                            _uiState.update {
                                it.copy(
                                    isAuthenticating = false,
                                    statusFeedback = result.message
                                )
                            }
                        }
                        is BiometricAuthResult.Cancelled -> {
                            _uiState.update {
                                it.copy(
                                    isAuthenticating = false,
                                    statusFeedback = "Authentication cancelled"
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Authenticates using the owner's configured PIN verified via [AuthRepository] salted PBKDF2-HMAC-SHA256
     * with brute-force rate limiting and exponential cooldown.
     * Never logs the entered PIN.
     */
    fun authenticateWithMasterPin(enteredPin: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return when (val result = authRepository.verifyPin(enteredPin, nowMillis)) {
            is PinVerificationResult.Success -> {
                recordAuditEvent(
                    eventType = "PIN_UNLOCK",
                    method = "Owner PIN (PBKDF2-SHA256)",
                    success = true,
                    details = "Authorized access via salted PBKDF2-SHA256 owner PIN verifier"
                )
                _uiState.update {
                    it.copy(
                        isUnlocked = true,
                        failedAttempts = 0,
                        isLockedOut = false,
                        lockoutUntilEpochMillis = 0L,
                        lastUnlockedTimestamp = nowMillis,
                        statusFeedback = "Owner PIN verified successfully"
                    )
                }
                true
            }
            is PinVerificationResult.CorruptedStorage -> {
                recordAuditEvent(
                    eventType = "CREDENTIAL_CORRUPTED",
                    method = "Owner PIN (Integrity Check)",
                    success = false,
                    details = "Stored PIN salt/verifier failed cryptographic integrity check; rejected authentication"
                )
                _uiState.update {
                    it.copy(
                        isPinConfigured = false,
                        statusFeedback = "Stored PIN credential was corrupted or tampered with. Set up a new Owner PIN or use Biometrics."
                    )
                }
                false
            }
            is PinVerificationResult.NotConfigured -> {
                recordAuditEvent(
                    eventType = "AUTH_FAILED",
                    method = "Owner PIN",
                    success = false,
                    details = "PIN authentication attempted before Owner PIN setup"
                )
                _uiState.update {
                    it.copy(
                        isPinConfigured = false,
                        statusFeedback = "No Owner PIN configured yet. Please set up your custom Owner PIN first."
                    )
                }
                false
            }
            is PinVerificationResult.LockedOut -> {
                if (result.newlyTriggered) {
                    recordAuditEvent(
                        eventType = "LOCKOUT_TRIGGERED",
                        method = "Owner PIN (Rate Limiter)",
                        success = false,
                        details = "Brute-force cooldown activated for ${result.remainingSeconds}s after ${result.failedAttempts} failed attempts"
                    )
                    _uiState.update {
                        it.copy(
                            failedAttempts = result.failedAttempts,
                            isLockedOut = true,
                            lockoutUntilEpochMillis = result.lockoutUntilEpochMillis,
                            statusFeedback = "Too many failed attempts (${result.failedAttempts}). Locked for ${result.remainingSeconds}s."
                        )
                    }
                } else {
                    recordAuditEvent(
                        eventType = "LOCKOUT_ACTIVE",
                        method = "Owner PIN (Rate Limiter)",
                        success = false,
                        details = "Rejected PIN attempt during active security cooldown (${result.remainingSeconds}s remaining)"
                    )
                    _uiState.update {
                        it.copy(
                            isLockedOut = true,
                            lockoutUntilEpochMillis = result.lockoutUntilEpochMillis,
                            statusFeedback = "Too many failed attempts. Try again in ${result.remainingSeconds}s or use Biometrics."
                        )
                    }
                }
                false
            }
            is PinVerificationResult.InvalidPin -> {
                val attemptsLeft = result.attemptsRemainingBeforeCooldown
                recordAuditEvent(
                    eventType = "AUTH_FAILED",
                    method = "Owner PIN (PBKDF2-SHA256)",
                    success = false,
                    details = "Invalid PIN attempt #${result.failedAttempts} ($attemptsLeft remaining before cooldown)"
                )
                _uiState.update {
                    it.copy(
                        failedAttempts = result.failedAttempts,
                        isLockedOut = false,
                        lockoutUntilEpochMillis = 0L,
                        statusFeedback = "Incorrect Owner PIN. $attemptsLeft attempt${if (attemptsLeft == 1) "" else "s"} remaining before cooldown."
                    )
                }
                false
            }
        }
    }

    /**
     * First-time setup of a custom Owner PIN via [AuthRepository].
     * Only allowed when no PIN is currently configured (or when called from an already unlocked session).
     */
    fun setupOwnerPin(
        newPin: String,
        confirmPin: String = newPin,
        unlockSessionOnSuccess: Boolean = false
    ): PinSetupResult {
        if (hasConfiguredPinVerifier() && !_uiState.value.isUnlocked) {
            return PinSetupResult.ValidationError("An Owner PIN is already configured. Unlock first to change it.")
        }
        val wasAlreadyConfigured = hasConfiguredPinVerifier()
        val setupResult = authRepository.setupPin(newPin, confirmPin)
        if (setupResult is PinSetupResult.ValidationError) {
            _uiState.update { it.copy(statusFeedback = setupResult.message) }
            return setupResult
        }

        recordAuditEvent(
            eventType = if (wasAlreadyConfigured) "PIN_UPDATED" else "PIN_CONFIGURED",
            method = "Security Settings (PBKDF2-SHA256)",
            success = true,
            details = if (wasAlreadyConfigured) {
                "Owner PIN rotated with new 128-bit cryptographic salt and PBKDF2-SHA256 verifier"
            } else {
                "Initial Owner PIN created and stored as salted PBKDF2-SHA256 verifier"
            }
        )

        _uiState.update {
            it.copy(
                isPinConfigured = true,
                isUnlocked = if (unlockSessionOnSuccess) true else it.isUnlocked,
                failedAttempts = 0,
                isLockedOut = false,
                lockoutUntilEpochMillis = 0L,
                lastUnlockedTimestamp = if (unlockSessionOnSuccess) System.currentTimeMillis() else it.lastUnlockedTimestamp,
                statusFeedback = if (wasAlreadyConfigured) "Owner PIN updated securely" else "Owner PIN configured securely"
            )
        }
        return PinSetupResult.Success
    }

    /**
     * Rotates an existing Owner PIN by verifying the current PIN first.
     */
    fun changeOwnerPin(currentPin: String, newPin: String, confirmPin: String): PinSetupResult {
        if (hasConfiguredPinVerifier()) {
            val verified = authenticateWithMasterPin(currentPin)
            if (!verified) {
                return PinSetupResult.ValidationError(
                    _uiState.value.statusFeedback ?: "Current Owner PIN is incorrect."
                )
            }
        }
        return setupOwnerPin(newPin = newPin, confirmPin = confirmPin, unlockSessionOnSuccess = true)
    }

    /**
     * Updates the Owner PIN and stores only its salted PBKDF2-SHA256 verifier via [AuthRepository].
     * Returns true if the PIN passed security validation and was persisted.
     */
    fun updateMasterPin(newPin: String): Boolean {
        val setupResult = authRepository.setupPin(newPin, newPin)
        if (setupResult is PinSetupResult.ValidationError) {
            _uiState.update { it.copy(statusFeedback = setupResult.message) }
            return false
        }
        recordAuditEvent(
            eventType = "PIN_UPDATED",
            method = "Security Settings",
            success = true,
            details = "Owner Master PIN updated with salted PBKDF2-SHA256 cryptographic verifier"
        )
        _uiState.update {
            it.copy(
                isPinConfigured = true,
                failedAttempts = 0,
                isLockedOut = false,
                lockoutUntilEpochMillis = 0L,
                statusFeedback = "Owner PIN updated securely"
            )
        }
        return true
    }

    /**
     * Resets the configured Owner PIN when the owner is authenticated in an unlocked session.
     */
    fun resetOwnerPin(): Boolean {
        if (!_uiState.value.isUnlocked) {
            _uiState.update { it.copy(statusFeedback = "Unlock session via Biometrics or current PIN before resetting.") }
            return false
        }
        authRepository.clearPin()
        recordAuditEvent(
            eventType = "PIN_RESET",
            method = "Security Settings",
            success = true,
            details = "Owner PIN verifier cleared from secure preferences during authenticated session"
        )
        _uiState.update {
            it.copy(
                isPinConfigured = false,
                failedAttempts = 0,
                isLockedOut = false,
                lockoutUntilEpochMillis = 0L,
                statusFeedback = "Owner PIN cleared. Configure a new PIN anytime."
            )
        }
        return true
    }

    /**
     * Checks whether the session has exceeded its configured auto-lock duration and locks if needed.
     */
    fun checkAutoLockTimeout(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val state = _uiState.value
        if (!state.isBiometricProtectionEnabled || !state.isUnlocked || state.lastUnlockedTimestamp <= 0L) {
            return false
        }
        val timeoutMillis = state.autoLockDurationMinutes.coerceAtLeast(1) * 60_000L
        if (nowMillis - state.lastUnlockedTimestamp >= timeoutMillis) {
            recordAuditEvent(
                eventType = "AUTO_LOCK_TIMEOUT",
                method = "Auto-Lock Timer (${state.autoLockDurationMinutes}m)",
                success = true,
                details = "Session automatically locked after ${state.autoLockDurationMinutes}m inactivity threshold"
            )
            _uiState.update {
                it.copy(
                    isUnlocked = false,
                    statusFeedback = "Session locked after ${state.autoLockDurationMinutes}m inactivity"
                )
            }
            return true
        }
        return false
    }

    /**
     * Immediately locks the business hub session.
     */
    fun lockSession() {
        recordAuditEvent(
            eventType = "SESSION_LOCKED",
            method = "Manual Lock Trigger",
            success = true,
            details = "Session manually locked by user to protect customer privacy"
        )
        _uiState.update {
            it.copy(
                isUnlocked = false,
                statusFeedback = "Session locked"
            )
        }
    }

    /**
     * Toggles whether biometric security is enforced.
     */
    fun setBiometricProtectionEnabled(enabled: Boolean) {
        authRepository.setBiometricProtectionEnabled(enabled)
        recordAuditEvent(
            eventType = if (enabled) "SECURITY_ENABLED" else "SECURITY_DISABLED",
            method = "Security Settings",
            success = true,
            details = if (enabled) "Biometric lock activated for private triage data" else "Biometric lock deactivated"
        )
        _uiState.update {
            it.copy(
                isBiometricProtectionEnabled = enabled,
                isUnlocked = if (!enabled) true else it.isUnlocked
            )
        }
    }

    /**
     * Sets auto-lock duration in minutes.
     */
    fun setAutoLockDuration(minutes: Int) {
        authRepository.setAutoLockDurationMinutes(minutes)
        _uiState.update { it.copy(autoLockDurationMinutes = minutes) }
    }

    private fun recordAuditEvent(eventType: String, method: String, success: Boolean, details: String) {
        val entry = SecurityAuditEntry(
            timestamp = timeFormat.format(Date()),
            eventType = eventType,
            methodUsed = method,
            isSuccess = success,
            detailNotes = details
        )
        _uiState.update {
            it.copy(auditLogs = listOf(entry) + it.auditLogs.take(25))
        }
    }

    private fun loadInitialAuditLogs(pinConfigured: Boolean): List<SecurityAuditEntry> {
        val now = timeFormat.format(Date())
        return listOf(
            SecurityAuditEntry(
                timestamp = now,
                eventType = "SECURITY_ACTIVE",
                methodUsed = if (pinConfigured) "Biometric Enclave + Salted PBKDF2 PIN" else "Biometric Sensor Enclave",
                isSuccess = true,
                detailNotes = "Biometric authentication engine initialized for Kwanda Zama Salon Hub"
            )
        )
    }

    companion object {
        const val PREFS_NAME = "zama_security_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_AUTO_LOCK_MINUTES = "key_auto_lock_minutes"
        private const val KEY_PIN_SALT_B64 = "key_owner_pin_salt_b64"
        private const val KEY_PIN_VERIFIER_B64 = "key_owner_pin_verifier_b64"
        private const val KEY_FAILED_ATTEMPTS = "key_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL_MS = "key_lockout_until_ms"
        private const val LEGACY_KEY_MASTER_PIN = "key_master_pin"
        const val MAX_FAILED_ATTEMPTS = 5

        class Factory(private val app: Application) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BiometricSecurityViewModel(app) as T
            }
        }
    }
}
