package com.example.security

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class BiometricSecurityTest {

    private lateinit var app: Application
    private lateinit var viewModel: BiometricSecurityViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        // Clear preferences before each test
        app.getSharedPreferences(BiometricSecurityViewModel.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        viewModel = BiometricSecurityViewModel(app)
    }

    @Test
    fun testDefaultSecurityStateAndNoHardcodedPin() {
        val state = viewModel.uiState.value
        // By default, biometric protection is enabled, session starts locked, and NO default PIN exists
        assertFalse(state.isUnlocked)
        assertTrue(state.isBiometricProtectionEnabled)
        assertFalse(state.isPinConfigured)
        assertEquals(5, state.autoLockDurationMinutes)
        assertTrue(state.auditLogs.isNotEmpty())
        assertEquals("SECURITY_ACTIVE", state.auditLogs.first().eventType)

        // Attempting PIN unlock before configuring an Owner PIN must fail
        val preSetupAttempt = viewModel.authenticateWithMasterPin("7391")
        assertFalse(preSetupAttempt)
        assertFalse(viewModel.uiState.value.isUnlocked)
    }

    @Test
    fun testOwnerPinSetupRejectsWeakAndMismatchedPins() {
        // Reject repeated digits
        val weakRepeated = viewModel.setupOwnerPin("1111", "1111")
        assertTrue(weakRepeated is PinSetupResult.ValidationError)
        assertFalse(viewModel.uiState.value.isPinConfigured)

        // Reject sequential digits
        val weakSequential = viewModel.setupOwnerPin("1234", "1234")
        assertTrue(weakSequential is PinSetupResult.ValidationError)

        // Reject mismatched confirmation
        val mismatch = viewModel.setupOwnerPin("7391", "7399")
        assertTrue(mismatch is PinSetupResult.ValidationError)
        assertFalse(viewModel.uiState.value.isPinConfigured)

        // Valid strong PIN setup succeeds
        val valid = viewModel.setupOwnerPin("7391", "7391", unlockSessionOnSuccess = false)
        assertTrue(valid is PinSetupResult.Success)
        assertTrue(viewModel.uiState.value.isPinConfigured)
        assertEquals("PIN_CONFIGURED", viewModel.uiState.value.auditLogs.first().eventType)
    }

    @Test
    fun testSaltedPbkdf2PinUnlockAndNoPlaintextStorageOrLogging() {
        val ownerPin = "7391"
        assertTrue(viewModel.setupOwnerPin(ownerPin, ownerPin) is PinSetupResult.Success)

        // Verify plaintext PIN is NEVER stored in SharedPreferences
        val prefs = app.getSharedPreferences(BiometricSecurityViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        assertNull(prefs.getString("key_master_pin", null))
        val allValues = prefs.all.values.map { it.toString() }
        assertFalse("Plaintext PIN must never be stored in SharedPreferences", allValues.any { it.contains(ownerPin) })

        // Test failed PIN attempt
        val wrongPin = "4926"
        val failedResult = viewModel.authenticateWithMasterPin(wrongPin)
        assertFalse(failedResult)
        assertFalse(viewModel.uiState.value.isUnlocked)
        assertEquals(1, viewModel.uiState.value.failedAttempts)

        // Verify failed PIN is NEVER logged in audit trail
        assertFalse(
            "Entered PIN must never appear in audit logs",
            viewModel.uiState.value.auditLogs.any { it.detailNotes.contains(wrongPin) || it.detailNotes.contains(ownerPin) }
        )

        // Test valid Owner PIN unlock
        val successResult = viewModel.authenticateWithMasterPin(ownerPin)
        assertTrue(successResult)
        assertTrue(viewModel.uiState.value.isUnlocked)
        assertEquals(0, viewModel.uiState.value.failedAttempts)
        assertEquals("PIN_UNLOCK", viewModel.uiState.value.auditLogs.first().eventType)
    }

    @Test
    fun testBruteForceRateLimitingAndLockoutCooldown() {
        val ownerPin = "6482"
        assertTrue(viewModel.setupOwnerPin(ownerPin, ownerPin) is PinSetupResult.Success)
        val baseTime = 1_700_000_000_000L

        // 5 consecutive failed attempts trigger a 30-second cooldown
        for (attempt in 1..5) {
            val res = viewModel.authenticateWithMasterPin("9012", nowMillis = baseTime + attempt * 100L)
            assertFalse(res)
        }

        val lockedState = viewModel.uiState.value
        assertTrue(lockedState.isLockedOut)
        assertEquals(5, lockedState.failedAttempts)
        assertTrue(lockedState.remainingLockoutSeconds(baseTime + 1000L) > 0)
        assertEquals("LOCKOUT_TRIGGERED", lockedState.auditLogs.first().eventType)

        // Even the correct PIN must be rejected while cooldown is active
        val rejectedDuringCooldown = viewModel.authenticateWithMasterPin(ownerPin, nowMillis = baseTime + 5_000L)
        assertFalse(rejectedDuringCooldown)
        assertFalse(viewModel.uiState.value.isUnlocked)
        assertEquals("LOCKOUT_ACTIVE", viewModel.uiState.value.auditLogs.first().eventType)

        // After 31 seconds (cooldown expired), valid Owner PIN succeeds and clears lockout
        val unlockedAfterCooldown = viewModel.authenticateWithMasterPin(ownerPin, nowMillis = baseTime + 31_000L)
        assertTrue(unlockedAfterCooldown)
        assertTrue(viewModel.uiState.value.isUnlocked)
        assertFalse(viewModel.uiState.value.isLockedOut)
        assertEquals(0, viewModel.uiState.value.failedAttempts)
    }

    @Test
    fun testLockSessionAndToggleProtection() {
        viewModel.setupOwnerPin("7391", "7391", unlockSessionOnSuccess = true)
        assertTrue(viewModel.uiState.value.isUnlocked)

        // Lock session manually
        viewModel.lockSession()
        assertFalse(viewModel.uiState.value.isUnlocked)
        assertEquals("SESSION_LOCKED", viewModel.uiState.value.auditLogs.first().eventType)

        // Disable biometric protection -> unlocks automatically
        viewModel.setBiometricProtectionEnabled(false)
        assertFalse(viewModel.uiState.value.isBiometricProtectionEnabled)
        assertTrue(viewModel.uiState.value.isUnlocked)

        // Update auto-lock duration
        viewModel.setAutoLockDuration(15)
        assertEquals(15, viewModel.uiState.value.autoLockDurationMinutes)
    }

    @Test
    fun testUpdateAndChangeCustomOwnerPin() {
        val updated = viewModel.updateMasterPin("9944")
        assertTrue(updated)
        assertTrue(viewModel.uiState.value.isPinConfigured)

        // Old or wrong PIN should fail
        assertFalse(viewModel.authenticateWithMasterPin("7391"))
        // New PIN should succeed
        assertTrue(viewModel.authenticateWithMasterPin("9944"))
        assertTrue(viewModel.uiState.value.isUnlocked)

        // Rotate PIN via changeOwnerPin requiring current PIN verification
        val rotated = viewModel.changeOwnerPin("9944", "5839", "5839")
        assertTrue(rotated is PinSetupResult.Success)
        viewModel.lockSession()
        assertFalse(viewModel.authenticateWithMasterPin("9944"))
        assertTrue(viewModel.authenticateWithMasterPin("5839"))
    }

    @Test
    fun testCorruptedCredentialStorageDetectedAndRejected() {
        assertTrue(viewModel.setupOwnerPin("7391", "7391") is PinSetupResult.Success)
        assertFalse(viewModel.hasCorruptedCredentials())

        // Corrupt stored verifier bytes in SharedPreferences
        val prefs = app.getSharedPreferences(BiometricSecurityViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("key_owner_pin_verifier_b64", "CORRUPTED_BASE64_PAYLOAD!!!").commit()

        assertTrue(viewModel.hasCorruptedCredentials())
        val attempt = viewModel.authenticateWithMasterPin("7391")
        assertFalse(attempt)
        assertFalse(viewModel.uiState.value.isUnlocked)
        assertFalse(viewModel.uiState.value.isPinConfigured)
        assertEquals("CREDENTIAL_CORRUPTED", viewModel.uiState.value.auditLogs.first().eventType)
    }

    @Test
    fun testAutoLockTimeoutAndProcessRestartPersistence() {
        val unlockTime = 1_700_000_000_000L
        assertTrue(viewModel.setupOwnerPin("7391", "7391") is PinSetupResult.Success)
        viewModel.setAutoLockDuration(5)
        assertTrue(viewModel.authenticateWithMasterPin("7391", nowMillis = unlockTime))
        assertTrue(viewModel.uiState.value.isUnlocked)

        // Before 5 minutes (4 minutes elapsed): remains unlocked
        assertFalse(viewModel.checkAutoLockTimeout(nowMillis = unlockTime + 4 * 60_000L))
        assertTrue(viewModel.uiState.value.isUnlocked)

        // At 5 minutes elapsed: automatically locks session
        assertTrue(viewModel.checkAutoLockTimeout(nowMillis = unlockTime + 5 * 60_000L))
        assertFalse(viewModel.uiState.value.isUnlocked)
        assertEquals("AUTO_LOCK_TIMEOUT", viewModel.uiState.value.auditLogs.first().eventType)

        // Simulate app restart / process death by instantiating a fresh ViewModel
        val restartedViewModel = BiometricSecurityViewModel(app)
        assertFalse(restartedViewModel.uiState.value.isUnlocked)
        assertTrue(restartedViewModel.uiState.value.isPinConfigured)
        assertEquals(5, restartedViewModel.uiState.value.autoLockDurationMinutes)
        assertTrue(restartedViewModel.authenticateWithMasterPin("7391"))
    }

    @Test
    fun testResetOwnerPinRequiresUnlockedSession() {
        assertTrue(viewModel.setupOwnerPin("7391", "7391", unlockSessionOnSuccess = false) is PinSetupResult.Success)
        assertFalse(viewModel.uiState.value.isUnlocked)

        // Unauthenticated reset attempt must be rejected
        assertFalse(viewModel.resetOwnerPin())
        assertTrue(viewModel.uiState.value.isPinConfigured)

        // Unlock session first, then reset succeeds
        assertTrue(viewModel.authenticateWithMasterPin("7391"))
        assertTrue(viewModel.resetOwnerPin())
        assertFalse(viewModel.uiState.value.isPinConfigured)
        assertEquals("PIN_RESET", viewModel.uiState.value.auditLogs.first().eventType)
    }

    @Test
    fun testBiometricCapabilityCheckOnDevice() {
        val manager = BiometricAuthManager(app)
        val capability = manager.checkBiometricCapability()
        assertNotNull(capability)

        val promptInfo = manager.buildPromptInfo(
            context = app,
            title = "Test Title",
            subtitle = "Test Subtitle",
            description = "Test Description"
        )
        assertEquals("Test Title", promptInfo.title)
        assertEquals("Test Subtitle", promptInfo.subtitle)
        assertEquals("Test Description", promptInfo.description)

        var errorTriggeredCode = 0
        var errorTriggeredMessage = ""
        var failedTriggered = false
        var cancelledTriggered = false

        val callback = manager.createAuthenticationCallback(
            onSuccess = {},
            onError = { code, msg ->
                errorTriggeredCode = code
                errorTriggeredMessage = msg
            },
            onFailed = { failedTriggered = true },
            onCancelled = { cancelledTriggered = true }
        )

        callback.onAuthenticationFailed()
        assertTrue(failedTriggered)

        callback.onAuthenticationError(androidx.biometric.BiometricPrompt.ERROR_HW_UNAVAILABLE, "Sensor busy")
        assertEquals(androidx.biometric.BiometricPrompt.ERROR_HW_UNAVAILABLE, errorTriggeredCode)
        assertEquals("Sensor busy", errorTriggeredMessage)

        callback.onAuthenticationError(androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED, "Cancelled by user")
        assertTrue(cancelledTriggered)
    }
}
