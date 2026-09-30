package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(AuthRepository.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        authRepository = AuthRepository(context)
    }

    @Test
    fun `initial state has no hardcoded PIN and purges legacy plaintext PIN key`() {
        val rawPrefs = context.getSharedPreferences(AuthRepository.PREFS_NAME, Context.MODE_PRIVATE)
        rawPrefs.edit().putString(AuthRepository.LEGACY_KEY_MASTER_PIN, "legacy_plaintext").commit()

        val repo = AuthRepository(context, rawPrefs)
        assertNull(repo.encryptedPrefs.getString(AuthRepository.LEGACY_KEY_MASTER_PIN, null))
        assertFalse(repo.isPinConfigured())
        assertEquals(PinVerificationResult.NotConfigured, repo.verifyPin("7391"))
    }

    @Test
    fun `setupPin stores salted hash in encryptedPrefs and never stores plaintext PIN`() {
        val pin = "7391"
        val setupResult = authRepository.setupPin(pin, pin)
        assertTrue(setupResult is PinSetupResult.Success)
        assertTrue(authRepository.isPinConfigured())

        val saltB64 = authRepository.encryptedPrefs.getString(AuthRepository.KEY_PIN_SALT_B64, null)
        val verifierB64 = authRepository.encryptedPrefs.getString(AuthRepository.KEY_PIN_VERIFIER_B64, null)
        assertNotNull(saltB64)
        assertNotNull(verifierB64)

        // Ensure plaintext PIN never appears in any stored preference value
        val allStoredValues = authRepository.encryptedPrefs.all.values.map { it.toString() }
        assertFalse(allStoredValues.any { it.contains(pin) })

        // Setting the same PIN again must generate a new random cryptographic salt and distinct verifier
        authRepository.setupPin(pin, pin)
        val secondSaltB64 = authRepository.encryptedPrefs.getString(AuthRepository.KEY_PIN_SALT_B64, null)
        val secondVerifierB64 = authRepository.encryptedPrefs.getString(AuthRepository.KEY_PIN_VERIFIER_B64, null)
        assertNotEquals(saltB64, secondSaltB64)
        assertNotEquals(verifierB64, secondVerifierB64)
    }

    @Test
    fun `verifyPin succeeds for valid PIN and enforces exponential lockout after 5 failures`() {
        assertTrue(authRepository.setPin("6482"))
        val baseTime = 1_700_000_000_000L

        // 4 wrong attempts return InvalidPin with decreasing remaining attempts
        for (i in 1..4) {
            val result = authRepository.verifyPin("9012", nowMillis = baseTime + i * 100L)
            assertTrue(result is PinVerificationResult.InvalidPin)
            val invalid = result as PinVerificationResult.InvalidPin
            assertEquals(i, invalid.failedAttempts)
            assertEquals(5 - i, invalid.attemptsRemainingBeforeCooldown)
        }

        // 5th wrong attempt triggers LockedOut for 30 seconds
        val fifthResult = authRepository.verifyPin("9012", nowMillis = baseTime + 500L)
        assertTrue(fifthResult is PinVerificationResult.LockedOut)
        val lockedOut = fifthResult as PinVerificationResult.LockedOut
        assertTrue(lockedOut.newlyTriggered)
        assertEquals(30L, lockedOut.remainingSeconds)

        // Valid PIN is rejected while lockout is active
        val duringLockout = authRepository.verifyPin("6482", nowMillis = baseTime + 5_000L)
        assertTrue(duringLockout is PinVerificationResult.LockedOut)
        assertFalse(authRepository.verifyPinBoolean("6482", nowMillis = baseTime + 5_000L))

        // Valid PIN succeeds after cooldown expires and resets failed counter
        val afterLockout = authRepository.verifyPin("6482", nowMillis = baseTime + 31_000L)
        assertEquals(PinVerificationResult.Success, afterLockout)
        assertEquals(0, authRepository.getFailedAttempts())
    }

    @Test
    fun `changePin verifies current PIN before rotating credentials and clearPin removes credentials`() {
        assertTrue(authRepository.setPin("7391"))

        // Wrong current PIN fails rotation
        val failedChange = authRepository.changePin("5820", "8492", "8492")
        assertTrue(failedChange is PinSetupResult.ValidationError)
        assertTrue(authRepository.verifyPinBoolean("7391"))

        // Correct current PIN rotates credentials
        val successChange = authRepository.changePin("7391", "8492", "8492")
        assertTrue(successChange is PinSetupResult.Success)
        assertFalse(authRepository.verifyPinBoolean("7391"))
        assertTrue(authRepository.verifyPinBoolean("8492"))

        // Clear PIN removes stored salt and hash
        authRepository.clearPin()
        assertFalse(authRepository.isPinConfigured())
        assertEquals(PinVerificationResult.NotConfigured, authRepository.verifyPin("8492"))
    }

    @Test
    fun `corrupted credential storage is detected and rejected`() {
        assertTrue(authRepository.setPin("7391"))
        authRepository.encryptedPrefs.edit()
            .putString(AuthRepository.KEY_PIN_VERIFIER_B64, "INVALID_CORRUPTED_HASH")
            .commit()

        assertTrue(authRepository.hasCorruptedCredentials())
        assertFalse(authRepository.isPinConfigured())
        assertEquals(PinVerificationResult.CorruptedStorage, authRepository.verifyPin("7391"))
    }

    @Test
    fun `biometric capability check queries androidx biometric manager`() {
        val capability = authRepository.checkBiometricCapability()
        assertNotNull(capability)
    }
}
