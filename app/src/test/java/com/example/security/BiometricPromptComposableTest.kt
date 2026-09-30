package com.example.security

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.PreviewWrapper
import com.example.ui.components.BiometricPrompt
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h900dp")
class BiometricPromptComposableTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun biometricPrompt_displaysWaitingForAuthenticationStateAndUsesBiometricAuthManager() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authManager = BiometricAuthManager(app)
        var fallbackClicked = false

        composeRule.setContent {
            PreviewWrapper {
                BiometricPrompt(
                    biometricAuthManager = authManager,
                    isWaitingForAuthentication = true,
                    onUseFallbackPin = { fallbackClicked = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_prompt_composable").assertIsDisplayed()
        composeRule.onNodeWithTag("biometric_prompt_waiting_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("biometric_prompt_status_text")
            .assertIsDisplayed()
            .assertTextContains("WAITING FOR USER AUTHENTICATION", substring = true)
        composeRule.onNodeWithTag("biometric_prompt_sensor_visualizer").assertIsDisplayed()
        composeRule.onNodeWithTag("biometric_prompt_authenticate_button").assertIsDisplayed()
        composeRule.onNodeWithTag("biometric_prompt_fallback_button").assertIsDisplayed()

        composeRule.onNodeWithTag("biometric_prompt_fallback_button").performClick()
        assertTrue("Fallback PIN callback should be invoked", fallbackClicked)
    }

    @Test
    fun biometricPromptStatusView_reactsToIdleAuthenticatingSuccessAndErrorStates() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authManager = BiometricAuthManager(app)

        composeRule.setContent {
            PreviewWrapper {
                com.example.ui.components.BiometricPromptStatusView(
                    biometricAuthManager = authManager,
                    onUseFallbackPin = {}
                )
            }
        }

        // 1. Default Idle state
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_prompt_status_view").assertIsDisplayed()
        composeRule.onNodeWithTag("biometric_status_badge_text")
            .assertTextContains("IDLE", substring = true)

        // 2. Transition to Authenticating state via BiometricAuthManager
        authManager.setFlowState(BiometricAuthFlowState.Authenticating("Scanning fingerprint sensor..."))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_status_badge_text")
            .assertTextContains("AUTHENTICATING", substring = true)
        composeRule.onNodeWithTag("biometric_status_progress_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("biometric_status_message_text")
            .assertTextContains("Scanning fingerprint sensor...", substring = true)

        // 3. Transition to Error state via BiometricAuthManager
        authManager.setFlowState(BiometricAuthFlowState.Error(errorCode = 7, message = "Too many attempts. Try again later."))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_status_badge_text")
            .assertTextContains("ERROR", substring = true)
        composeRule.onNodeWithTag("biometric_status_message_text")
            .assertTextContains("Too many attempts", substring = true)

        // 4. Transition to Success state via BiometricAuthManager
        authManager.setFlowState(BiometricAuthFlowState.Success("Biometric identity verified."))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_status_badge_text")
            .assertTextContains("SUCCESS", substring = true)
        composeRule.onNodeWithTag("biometric_status_message_text")
            .assertTextContains("Biometric identity verified.", substring = true)
    }

    @Test
    fun biometricAuthenticationScreen_triggersOnLoadAndDisplaysAuthenticatingSuccessAndFailureStates() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authManager = BiometricAuthManager(app)
        var successTriggered = false

        composeRule.setContent {
            PreviewWrapper {
                com.example.ui.components.BiometricAuthenticationScreen(
                    biometricAuthManager = authManager,
                    triggerPromptOnLoad = true,
                    onAuthenticatedSuccess = { successTriggered = true }
                )
            }
        }

        // 1. Triggered on load -> Authenticating state
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_authentication_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("biometric_auth_screen_status_text")
            .assertTextContains("AUTHENTICATING", substring = true)
        composeRule.onNodeWithTag("biometric_auth_screen_headline")
            .assertTextContains("Authenticating Identity...", substring = true)
        composeRule.onNodeWithTag("biometric_auth_screen_progress").assertIsDisplayed()

        // 2. Failure state via BiometricAuthManager
        authManager.setFlowState(
            BiometricAuthFlowState.Error(
                errorCode = 11,
                message = "Biometric sensor could not verify fingerprint."
            )
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_auth_screen_status_text")
            .assertTextContains("FAILURE", substring = true)
        composeRule.onNodeWithTag("biometric_auth_screen_headline")
            .assertTextContains("Biometric Authentication Failed", substring = true)
        composeRule.onNodeWithTag("biometric_auth_screen_message_text")
            .assertTextContains("Biometric sensor could not verify fingerprint.", substring = true)
        composeRule.onNodeWithTag("biometric_auth_screen_retry_button").assertIsDisplayed()
        composeRule.onNodeWithTag("biometric_auth_screen_pin_fallback_button").assertIsDisplayed()

        // 3. Success state via BiometricAuthManager
        authManager.setFlowState(
            BiometricAuthFlowState.Success("Biometric identity verified.")
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_auth_screen_status_text")
            .assertTextContains("SUCCESS", substring = true)
        composeRule.onNodeWithTag("biometric_auth_screen_headline")
            .assertTextContains("Biometric Authentication Successful", substring = true)
        composeRule.onNodeWithTag("biometric_auth_screen_continue_button").assertIsDisplayed()
        composeRule.onNodeWithTag("biometric_auth_screen_continue_button").performClick()
        assertTrue("onAuthenticatedSuccess should be invoked", successTriggered)
    }
}
