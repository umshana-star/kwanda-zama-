package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.PreviewWrapper
import com.example.ui.components.WhatsAppAgentStatusView
import com.example.ui.components.WhatsAppConnectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1200dp")
class WhatsAppAgentStatusViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun whatsAppAgentStatusView_displaysConnectionStatusActiveConversationAndIncomingMessages() {
        var simulatedPingCalled = false
        var latestCycledStatus: WhatsAppConnectionStatus? = null

        composeRule.setContent {
            PreviewWrapper {
                WhatsAppAgentStatusView(
                    connectionStatus = WhatsAppConnectionStatus.CONNECTED,
                    onSimulateIncomingMessage = { simulatedPingCalled = true },
                    onCycleConnectionStatus = { latestCycledStatus = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("whatsapp_agent_status_view").assertIsDisplayed()
        composeRule.onNodeWithTag("whatsapp_agent_connection_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("whatsapp_agent_connection_text")
            .assertTextContains("CONNECTED", substring = true)

        composeRule.onNodeWithTag("whatsapp_agent_active_conversation_card").assertIsDisplayed()
        composeRule.onNodeWithTag("whatsapp_agent_active_threads_badge")
            .assertTextContains("4/20 LIVE THREADS", substring = true)

        composeRule.onNodeWithTag("whatsapp_agent_incoming_messages_section").assertIsDisplayed()
        composeRule.onNodeWithTag("whatsapp_incoming_message_item_0").assertIsDisplayed()

        // Simulate a new incoming WhatsApp ping
        composeRule.onNodeWithTag("whatsapp_agent_simulate_message_button").performClick()
        composeRule.waitForIdle()
        assertTrue(simulatedPingCalled)
        composeRule.onNodeWithTag("whatsapp_agent_active_threads_badge")
            .assertTextContains("5/20 LIVE THREADS", substring = true)

        // Cycle connection status
        composeRule.onNodeWithTag("whatsapp_agent_cycle_status_button").performClick()
        composeRule.waitForIdle()
        assertEquals(WhatsAppConnectionStatus.SYNCING, latestCycledStatus)
        composeRule.onNodeWithTag("whatsapp_agent_connection_text")
            .assertTextContains("SYNCING", substring = true)

        // Verify heartbeat telemetry bar and pulse trigger
        composeRule.onNodeWithTag("whatsapp_agent_heartbeat_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("whatsapp_agent_heartbeat_text")
            .assertTextContains("HEARTBEAT #1", substring = true)
        composeRule.onNodeWithTag("whatsapp_agent_heartbeat_ping_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("whatsapp_agent_heartbeat_text")
            .assertTextContains("HEARTBEAT #2", substring = true)
    }
}
