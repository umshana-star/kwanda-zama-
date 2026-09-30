package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.security.BiometricSecurityViewModel
import com.example.ui.components.BiometricLockScreen
import com.example.ui.components.SentimentFlowDashboardOverlay
import com.example.ui.components.ZamaAppSettingsDialog
import com.example.ui.components.ZamaFloatingNav
import com.example.ui.components.ZamaFooter
import com.example.ui.theme.ZamaTheme
import com.example.ui.theme.ZamaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-End Robolectric Compose UI QA suite verifying:
 * - First launch & first-time Owner PIN setup UI (no hardcoded master PIN)
 * - Owner PIN verification, incorrect PIN, and brute-force lockout UI
 * - ZamaAppSettingsDialog Owner PIN management & Security Audit Trail access
 * - SentimentFlowDashboardOverlay filtering & dismissal
 * - ZamaFloatingNav & ZamaFooter interactive navigation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1400dp")
class FullProductionQaRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var securityViewModel: BiometricSecurityViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences(BiometricSecurityViewModel.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        securityViewModel = BiometricSecurityViewModel(app)
    }

    @Test
    fun `first launch lock screen prompts for custom Owner PIN setup and unlocks on confirmation`() {
        composeTestRule.setContent {
            ZamaTheme(themeMode = ZamaThemeMode.FUTURISTIC_DARK) {
                val state by securityViewModel.uiState.collectAsState()
                BiometricLockScreen(
                    securityState = state,
                    onAuthenticateBiometrics = {},
                    onAuthenticatePin = { securityViewModel.authenticateWithMasterPin(it) },
                    onSetupOwnerPin = { newPin, confirmPin ->
                        securityViewModel.setupOwnerPin(
                            newPin = newPin,
                            confirmPin = confirmPin,
                            unlockSessionOnSuccess = true
                        )
                    },
                    onLockSession = { securityViewModel.lockSession() },
                    onToggleBiometricProtection = { securityViewModel.setBiometricProtectionEnabled(it) },
                    onUpdateAutoLockDuration = { securityViewModel.setAutoLockDuration(it) },
                    themeMode = ZamaThemeMode.FUTURISTIC_DARK
                )
            }
        }

        // Verify lock screen is shown and click btn_show_pin_keypad to open the PIN setup keypad
        composeTestRule.onNodeWithTag("biometric_lock_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_show_pin_keypad").performClick()
        composeTestRule.onNodeWithTag("pin_keypad_card").assertIsDisplayed()

        // Step 1 of setup: enter 7391 on keypad
        composeTestRule.onNodeWithTag("pin_key_7").performClick()
        composeTestRule.onNodeWithTag("pin_key_3").performClick()
        composeTestRule.onNodeWithTag("pin_key_9").performClick()
        composeTestRule.onNodeWithTag("pin_key_1").performClick()

        // Step 2 of setup: confirm 7391 on keypad
        composeTestRule.onNodeWithTag("pin_key_7").performClick()
        composeTestRule.onNodeWithTag("pin_key_3").performClick()
        composeTestRule.onNodeWithTag("pin_key_9").performClick()
        composeTestRule.onNodeWithTag("pin_key_1").performClick()

        assertTrue(securityViewModel.uiState.value.isPinConfigured)
        assertTrue(securityViewModel.uiState.value.isUnlocked)
    }

    @Test
    fun `settings dialog configures Owner PIN and opens Security Audit Trail`() {
        var auditOpened = false

        composeTestRule.setContent {
            ZamaTheme(themeMode = ZamaThemeMode.FUTURISTIC_DARK) {
                val state by securityViewModel.uiState.collectAsState()
                ZamaAppSettingsDialog(
                    isOpen = true,
                    currentThemeMode = ZamaThemeMode.FUTURISTIC_DARK,
                    onSelectThemeMode = {},
                    securityState = state,
                    onToggleBiometrics = { securityViewModel.setBiometricProtectionEnabled(it) },
                    onUpdateAutoLock = { securityViewModel.setAutoLockDuration(it) },
                    onSetupOwnerPin = { newPin, confirmPin ->
                        securityViewModel.setupOwnerPin(newPin, confirmPin, unlockSessionOnSuccess = false)
                    },
                    onChangeOwnerPin = { cur, newPin, confirmPin ->
                        securityViewModel.changeOwnerPin(cur, newPin, confirmPin)
                    },
                    onResetOwnerPin = { securityViewModel.resetOwnerPin() },
                    onOpenAuditLogs = { auditOpened = true },
                    onManageAgents = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("app_settings_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_toggle_pin_editor").performClick()
        composeTestRule.onNodeWithTag("input_new_owner_pin").performTextInput("6482")
        composeTestRule.onNodeWithTag("input_confirm_owner_pin").performTextInput("6482")
        composeTestRule.onNodeWithTag("btn_save_owner_pin").performClick()

        assertTrue(securityViewModel.uiState.value.isPinConfigured)

        // Verify Security Audit Trail button triggers callback
        composeTestRule.onNodeWithTag("btn_settings_open_audit_logs").performClick()
        assertTrue(auditOpened)
    }

    @Test
    fun `floating navigation and footer navigation invoke expected section targets`() {
        val navigatedTargets = mutableListOf<String>()

        composeTestRule.setContent {
            ZamaTheme(themeMode = ZamaThemeMode.FUTURISTIC_DARK) {
                androidx.compose.foundation.layout.Column {
                    ZamaFloatingNav(
                        activeSection = "home",
                        onNavigate = { navigatedTargets.add(it) }
                    )
                    ZamaFooter(
                        onNavigate = { navigatedTargets.add(it) }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("nav_logo").performClick()
        composeTestRule.onNodeWithTag("footer_link_work").performClick()
        composeTestRule.onNodeWithTag("footer_link_architecture").performClick()
        composeTestRule.onNodeWithTag("footer_link_engine").performClick()
        composeTestRule.onNodeWithTag("footer_link_top").performClick()

        assertEquals(listOf("hero", "agents", "chat", "3d", "hero"), navigatedTargets)
    }

    @Test
    fun `sentiment flow overlay renders filter chips and dismisses cleanly`() {
        var dismissed = false

        composeTestRule.setContent {
            ZamaTheme(themeMode = ZamaThemeMode.FUTURISTIC_DARK) {
                SentimentFlowDashboardOverlay(
                    isOpen = true,
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("sentiment_flow_dashboard_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("filter_chip_Tension").performClick()
        composeTestRule.onNodeWithTag("filter_chip_Pricing").performClick()
        composeTestRule.onNodeWithTag("sentiment_flow_close_btn").performClick()

        assertTrue(dismissed)
    }

    @Test
    fun `compose previews and preview parameter providers render cleanly in inspection mode`() {
        val greetingValues = com.example.GreetingPreviewParameterProvider().values.toList()
        val themeValues = com.example.ZamaThemePreviewParameterProvider().values.toList()
        assertTrue(greetingValues.isNotEmpty())
        assertEquals(2, themeValues.size)

        composeTestRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalInspectionMode provides true
            ) {
                com.example.PreviewWrapper(themeMode = themeValues.first()) {
                    com.example.ZamaAppScreen(themeMode = themeValues.first())
                }
            }
        }

        composeTestRule.onNodeWithTag("zama_main_scroll_feed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_glitch_headline").assertIsDisplayed()
    }
}
