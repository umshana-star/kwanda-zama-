package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authRepository = AuthRepository(context)
        authRepository.clearPin()
    }

    @Test
    fun testIsPinConfigured_initiallyFalse() {
        assertFalse(authRepository.isPinConfigured())
    }

    @Test
    fun testCreatePin_successWithValidPin() {
        val pin = "849201"
        val result = authRepository.createPin(pin)

        assertTrue(result is PinSetupResult.Success)
        assertTrue(authRepository.isPinConfigured())

        // Ensure PIN is NOT stored in plaintext anywhere in SharedPreferences
        val prefs = context.getSharedPreferences("zama_auth_security_vault", Context.MODE_PRIVATE)
        val allValues = prefs.all.values.joinToString(";")
        assertFalse("Plaintext PIN must never be stored in SharedPreferences", allValues.contains(pin))
        assertTrue(prefs.contains("auth_pin_salt"))
        assertTrue(prefs.contains("auth_pin_hash"))
    }

    @Test
    fun testCreatePin_rejectsWeakOrInvalidPins() {
        // Too short
        val shortResult = authRepository.createPin("1234")
        assertTrue(shortResult is PinSetupResult.Error)

        // All identical digits
        val repeatedResult = authRepository.createPin("777777")
        assertTrue(repeatedResult is PinSetupResult.Error)

        // Sequential ascending
        val sequentialResult = authRepository.createPin("123456")
        assertTrue(sequentialResult is PinSetupResult.Error)

        // Sequential descending
        val descendingResult = authRepository.createPin("654321")
        assertTrue(descendingResult is PinSetupResult.Error)

        assertFalse(authRepository.isPinConfigured())
    }

    @Test
    fun testVerifyPin_correctPinReturnsSuccess() {
        val pin = "593817"
        authRepository.createPin(pin)

        val verifyResult = authRepository.verifyPin(pin)
        assertTrue(verifyResult is PinVerificationResult.Success)
        assertEquals(AuthRepository.MAX_FAILED_ATTEMPTS, authRepository.getRemainingAttempts())
        assertFalse(authRepository.isLockedOut())
    }

    @Test
    fun testVerifyPin_incorrectPinDecrementsAttempts() {
        val pin = "593817"
        authRepository.createPin(pin)

        val verifyResult = authRepository.verifyPin("999999")
        assertTrue(verifyResult is PinVerificationResult.Incorrect)
        assertEquals(AuthRepository.MAX_FAILED_ATTEMPTS - 1, (verifyResult as PinVerificationResult.Incorrect).remainingAttempts)
        assertEquals(AuthRepository.MAX_FAILED_ATTEMPTS - 1, authRepository.getRemainingAttempts())
    }

    @Test
    fun testVerifyPin_lockoutEnforcedAfterMaxAttempts() {
        val pin = "593817"
        authRepository.createPin(pin)

        // Exhaust 5 attempts
        for (i in 1..4) {
            val res = authRepository.verifyPin("00000$i")
            assertTrue(res is PinVerificationResult.Incorrect)
        }

        // 5th failed attempt triggers lockout
        val fifthAttempt = authRepository.verifyPin("999999")
        assertTrue("5th failed attempt should trigger lockout", fifthAttempt is PinVerificationResult.LockedOut)
        assertTrue(authRepository.isLockedOut())
        assertTrue(authRepository.getLockoutRemainingSeconds() > 0)

        // Subsequent attempt is blocked by lockout
        val blockedAttempt = authRepository.verifyPin(pin)
        assertTrue(blockedAttempt is PinVerificationResult.LockedOut)
    }

    @Test
    fun testClearPin_removesStoredCredentials() {
        authRepository.createPin("837261")
        assertTrue(authRepository.isPinConfigured())

        authRepository.clearPin()
        assertFalse(authRepository.isPinConfigured())
    }

    @Test
    fun testSaltUniqueness_differentSaltsProduceDifferentHashesForSamePin() {
        val pin = "837261"

        // First instance creates PIN
        authRepository.createPin(pin)
        val prefs = context.getSharedPreferences("zama_auth_security_vault", Context.MODE_PRIVATE)
        val salt1 = prefs.getString("auth_pin_salt", "")
        val hash1 = prefs.getString("auth_pin_hash", "")

        // Recreate same PIN -> must have brand new cryptographically random salt and hash
        authRepository.createPin(pin)
        val salt2 = prefs.getString("auth_pin_salt", "")
        val hash2 = prefs.getString("auth_pin_hash", "")

        assertNotEquals(salt1, salt2)
        assertNotEquals(hash1, hash2)
    }
}
