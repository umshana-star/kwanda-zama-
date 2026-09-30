package com.example.security

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.PreviewWrapper
import com.example.ui.components.SaltedPinFallbackScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1200dp")
class SaltedPinFallbackScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences(AuthRepository.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        authRepository = AuthRepository(app)
    }

    @Test
    fun saltedPinFallbackScreen_createsSaltedPinWithoutPlaintextAndVerifiesWithAuthRepository() {
        var pinCreatedCalled = false
        var pinAuthenticatedCalled = false

        composeRule.setContent {
            PreviewWrapper {
                SaltedPinFallbackScreen(
                    authRepository = authRepository,
                    onPinCreated = { pinCreatedCalled = true },
                    onPinAuthenticated = { pinAuthenticatedCalled = true },
                    onBackToBiometrics = {}
                )
            }
        }

        // Step 1: Enter new 4-digit PIN (7391)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("salted_pin_fallback_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("salted_pin_stage_title")
            .assertTextContains("CREATE SALTED OWNER PIN", substring = true)

        listOf("7", "3", "9", "1").forEach { digit ->
            composeRule.onNodeWithTag("salted_pin_key_$digit").performClick()
        }
        composeRule.waitForIdle()

        // Step 2: Confirm new 4-digit PIN (7391)
        composeRule.onNodeWithTag("salted_pin_stage_title")
            .assertTextContains("CONFIRM SALTED OWNER PIN", substring = true)

        listOf("7", "3", "9", "1").forEach { digit ->
            composeRule.onNodeWithTag("salted_pin_key_$digit").performClick()
        }
        composeRule.waitForIdle()

        assertTrue("onPinCreated should be triggered", pinCreatedCalled)
        assertTrue("onPinAuthenticated should be triggered", pinAuthenticatedCalled)
        assertTrue("AuthRepository must report PIN configured", authRepository.isPinConfigured())

        // Verify PIN is NEVER stored in plaintext in SharedPreferences
        val storedSalt = authRepository.encryptedPrefs.getString(AuthRepository.KEY_PIN_SALT_B64, null)
        val storedVerifier = authRepository.encryptedPrefs.getString(AuthRepository.KEY_PIN_VERIFIER_B64, null)
        assertNotNull("Cryptographic salt must be stored", storedSalt)
        assertNotNull("PBKDF2 verifier must be stored", storedVerifier)
        assertNull("Legacy plaintext PIN key must not exist", authRepository.encryptedPrefs.getString(AuthRepository.LEGACY_KEY_MASTER_PIN, null))

        val allValues = authRepository.encryptedPrefs.all.values.map { it.toString() }
        assertFalse("Plaintext PIN '7391' must never appear in stored preferences", allValues.any { it.contains("7391") })
    }

    @Test
    fun saltedPinFallbackScreen_verifiesExistingSaltedPinAndRejectsInvalidPin() {
        authRepository.setupPin("8462", "8462")
        var authenticated = false

        composeRule.setContent {
            PreviewWrapper {
                SaltedPinFallbackScreen(
                    authRepository = authRepository,
                    onPinAuthenticated = { authenticated = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("salted_pin_stage_title")
            .assertTextContains("VERIFY OWNER PIN FALLBACK", substring = true)

        // Enter wrong PIN (1928)
        listOf("1", "9", "2", "8").forEach { digit ->
            composeRule.onNodeWithTag("salted_pin_key_$digit").performClick()
        }
        composeRule.waitForIdle()
        assertFalse("Wrong PIN must not authenticate", authenticated)
        composeRule.onNodeWithTag("salted_pin_feedback_text")
            .assertTextContains("Incorrect Owner PIN", substring = true)

        // Enter valid salted PIN (8462)
        listOf("8", "4", "6", "2").forEach { digit ->
            composeRule.onNodeWithTag("salted_pin_key_$digit").performClick()
        }
        composeRule.waitForIdle()
        assertTrue("Valid PIN must authenticate via AuthRepository", authenticated)
        composeRule.onNodeWithTag("salted_pin_stage_title")
            .assertTextContains("PIN CREDENTIAL VERIFIED", substring = true)
    }
}
