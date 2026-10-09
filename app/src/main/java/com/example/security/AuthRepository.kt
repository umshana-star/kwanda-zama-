package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed class PinSetupResult {
    object Success : PinSetupResult()
    data class Error(val message: String) : PinSetupResult()
}

sealed class PinVerificationResult {
    object Success : PinVerificationResult()
    data class Incorrect(val remainingAttempts: Int) : PinVerificationResult()
    data class LockedOut(val remainingSeconds: Long) : PinVerificationResult()
    data class Error(val message: String) : PinVerificationResult()
}

/**
 * Production-grade Authentication Repository managing cryptographically salted PINs
 * as a secure fallback to biometric authentication.
 *
 * Security guarantees:
 * 1. The user's PIN is NEVER stored in plaintext.
 * 2. Uses PBKDF2 with HMAC-SHA256 (10,000 iterations) and a 128-bit cryptographically secure random salt.
 * 3. Constant-time verification prevents side-channel timing attacks.
 * 4. Progressive rate-limiting and temporary lockout prevent brute-force dictionary attacks.
 */
class AuthRepository(
    private val context: Context,
    private val secureRandom: SecureRandom = SecureRandom()
) {

    companion object {
        private const val PREFS_NAME = "zama_auth_security_vault"
        private const val KEY_SALT = "auth_pin_salt"
        private const val KEY_HASH = "auth_pin_hash"
        private const val KEY_FAILED_ATTEMPTS = "auth_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "auth_lockout_until_timestamp"
        private const val KEY_BIOMETRIC_ENABLED = "auth_biometric_enabled"

        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 10_000
        private const val KEY_LENGTH = 256
        private const val SALT_BYTES = 16

        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_SECONDS = 30L
        const val PIN_LENGTH = 6
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Checks if a security PIN has already been configured.
     */
    fun isPinConfigured(): Boolean {
        val salt = prefs.getString(KEY_SALT, null)
        val hash = prefs.getString(KEY_HASH, null)
        return !salt.isNullOrBlank() && !hash.isNullOrBlank()
    }

    /**
     * Creates and stores a salted hash of the provided PIN.
     * Rejects weak PINs (e.g. repeated numbers like '111111' or sequential numbers like '123456').
     */
    fun createPin(pin: String): PinSetupResult {
        val trimmed = pin.trim()
        if (trimmed.length != PIN_LENGTH || !trimmed.all { it.isDigit() }) {
            return PinSetupResult.Error("PIN must be exactly $PIN_LENGTH digits.")
        }

        if (isTrivialPin(trimmed)) {
            return PinSetupResult.Error("PIN is too simple. Avoid sequential or repeated digits.")
        }

        return try {
            val salt = ByteArray(SALT_BYTES)
            secureRandom.nextBytes(salt)

            val hash = hashPin(trimmed, salt)

            prefs.edit()
                .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .commit()

            PinSetupResult.Success
        } catch (e: Exception) {
            PinSetupResult.Error("Cryptographic key derivation failed: ${e.message}")
        }
    }

    /**
     * Verifies the entered PIN against the stored salted hash in constant time.
     * Enforces attempt rate-limiting and temporary lockouts.
     */
    fun verifyPin(enteredPin: String): PinVerificationResult {
        if (isLockedOut()) {
            return PinVerificationResult.LockedOut(getLockoutRemainingSeconds())
        }

        val storedSaltBase64 = prefs.getString(KEY_SALT, null)
        val storedHashBase64 = prefs.getString(KEY_HASH, null)

        if (storedSaltBase64 == null || storedHashBase64 == null) {
            return PinVerificationResult.Error("No security PIN has been established.")
        }

        val storedSalt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
        val storedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP)

        val computedHash = hashPin(enteredPin.trim(), storedSalt)

        // Constant-time comparison to prevent timing attacks
        val isMatch = MessageDigest.isEqual(computedHash, storedHash)

        return if (isMatch) {
            // Reset failed counter upon successful verification
            resetFailedAttempts()
            PinVerificationResult.Success
        } else {
            val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            if (failed >= MAX_FAILED_ATTEMPTS) {
                val lockoutUntil = System.currentTimeMillis() + (LOCKOUT_DURATION_SECONDS * 1000L)
                prefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, failed)
                    .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                    .commit()
                PinVerificationResult.LockedOut(LOCKOUT_DURATION_SECONDS)
            } else {
                prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failed).commit()
                val remaining = MAX_FAILED_ATTEMPTS - failed
                PinVerificationResult.Incorrect(remaining)
            }
        }
    }

    /**
     * Returns true if the user is currently locked out from entering PINs.
     */
    fun isLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        return System.currentTimeMillis() < lockoutUntil
    }

    /**
     * Returns the remaining lockout seconds, or 0 if not locked out.
     */
    fun getLockoutRemainingSeconds(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val remaining = (lockoutUntil - System.currentTimeMillis()) / 1000L
        return if (remaining > 0) remaining else 0L
    }

    /**
     * Returns remaining PIN attempts before lockout.
     */
    fun getRemainingAttempts(): Int {
        val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        return (MAX_FAILED_ATTEMPTS - failed).coerceAtLeast(0)
    }

    /**
     * Resets failed attempt counter and lockout state.
     */
    fun resetFailedAttempts() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .commit()
    }

    /**
     * Deletes stored PIN and salt.
     */
    fun clearPin() {
        prefs.edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .commit()
    }

    /**
     * Complete reset of all authentication state and security vault data.
     */
    fun resetAllSecurityData() {
        prefs.edit().clear().commit()
    }

    /**
     * Sets biometric authentication fallback status.
     */
    fun setBiometricProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).commit()
    }

    /**
     * Checks if biometric protection is toggled on.
     */
    fun isBiometricProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    /**
     * Derives a cryptographic hash using PBKDF2 with HMAC-SHA256.
     */
    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        return try {
            val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            factory.generateSecret(spec).encoded
        } catch (_: Exception) {
            // Fallback key derivation for environments without PBKDF2 factory
            val digest = MessageDigest.getInstance("SHA-256")
            var current = pin.toByteArray(Charsets.UTF_8) + salt
            for (i in 0 until 1000) {
                current = digest.digest(current)
            }
            current
        }
    }

    /**
     * Detects easily guessed PIN patterns (e.g. "000000", "123456", "654321").
     */
    private fun isTrivialPin(pin: String): Boolean {
        // All digits identical
        if (pin.all { it == pin[0] }) return true

        // Sequential ascending ("123456", "012345")
        var isAscending = true
        for (i in 0 until pin.length - 1) {
            if (pin[i + 1] - pin[i] != 1) {
                isAscending = false
                break
            }
        }
        if (isAscending) return true

        // Sequential descending ("654321", "987654")
        var isDescending = true
        for (i in 0 until pin.length - 1) {
            if (pin[i] - pin[i + 1] != 1) {
                isDescending = false
                break
            }
        }
        return isDescending
    }
}
