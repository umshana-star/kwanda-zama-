package com.example.service.whatsapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WhatsAppAgentRepository
import com.example.data.local.ZamaDatabase
import com.example.service.whatsapp.api.SandboxWhatsAppCommunicationApi
import com.example.service.whatsapp.api.WhatsAppApiConfig
import com.example.service.whatsapp.api.WhatsAppInboundMessage
import com.example.service.whatsapp.api.WhatsAppMessageType
import com.example.service.whatsapp.api.WhatsAppOutboundMessage
import com.example.service.whatsapp.api.WhatsAppSendResult
import com.example.service.whatsapp.intelligence.AgentIntent
import com.example.service.whatsapp.intelligence.GeminiAgentIntelligence
import com.example.service.whatsapp.router.WhatsAppInteractionRouter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
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
class WhatsAppServiceTest {

    private lateinit var database: ZamaDatabase
    private lateinit var repository: WhatsAppAgentRepository
    private lateinit var communicationApi: SandboxWhatsAppCommunicationApi
    private lateinit var agentService: WhatsAppAgentService
    private lateinit var router: WhatsAppInteractionRouter

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ZamaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WhatsAppAgentRepository(database.whatsAppInteractionDao(), database.chatDao())
        communicationApi = SandboxWhatsAppCommunicationApi(WhatsAppApiConfig())
        agentService = WhatsAppAgentService(
            repository = repository,
            communicationApi = communicationApi,
            intelligence = GeminiAgentIntelligence { "" } // forces local intelligence engine
        )
        router = WhatsAppInteractionRouter(agentService)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInboundBookingInquiry_handledAndPersisted() = runBlocking {
        val inbound = WhatsAppInboundMessage(
            messageId = "inbound_001",
            fromPhoneNumber = "+27821234567",
            senderName = "Thando Cele",
            text = "Hi Zama! I want to book Knotless Braids for this Saturday at 2:00 PM."
        )

        val decision = agentService.handleInboundInteraction(inbound)

        // Verify Intent and reply formulation
        assertEquals(AgentIntent.BOOKING_INQUIRY, decision.intent)
        assertTrue(decision.isSlotTentativelyLocked)
        assertTrue(decision.replyText.contains("Saturday at 2:00 PM"))
        assertFalse(decision.requiresHumanEscalation)

        // Verify messages persisted in Room
        val chatMessages = repository.allChatMessages.first()
        assertEquals(2, chatMessages.size) // Customer message + AI reply
        assertEquals(inbound.text, chatMessages[0].content)
        assertTrue(chatMessages[0].isFromUser)
        assertEquals(decision.replyText, chatMessages[1].content)
        assertFalse(chatMessages[1].isFromUser)

        // Verify interaction audit log in Room
        val interactions = repository.allInteractions.first()
        assertEquals(1, interactions.size)
        assertEquals(inbound.fromPhoneNumber, interactions[0].senderPhoneNumber)
        assertEquals("Booking & Appointments", interactions[0].resolvedIntent)
        assertFalse(interactions[0].isEscalatedToHuman)
        assertEquals(1, repository.interactionCount.first())
        assertEquals(0, repository.escalatedCount.first())
    }

    @Test
    fun testInboundComplaint_escalatedToHumanManager() = runBlocking {
        val inbound = WhatsAppInboundMessage(
            messageId = "inbound_complaint_002",
            fromPhoneNumber = "+27839998877",
            senderName = "Angry Client",
            text = "This is unacceptable service, my stylist was terrible and I demand a refund from the manager!"
        )

        val decision = agentService.handleInboundInteraction(inbound)

        // Verify Escalation intent and flag
        assertEquals(AgentIntent.COMPLAINT_ESCALATION, decision.intent)
        assertTrue(decision.requiresHumanEscalation)
        assertTrue(decision.replyText.contains("salon manager"))

        // Verify audit log has escalation flag
        val interactions = repository.allInteractions.first()
        assertEquals(1, interactions.size)
        assertTrue(interactions[0].isEscalatedToHuman)
        assertEquals(1, repository.escalatedCount.first())
    }

    @Test
    fun testVoiceNoteInboundMessage_properlyFlagged() = runBlocking {
        val inbound = WhatsAppInboundMessage(
            messageId = "inbound_voice_003",
            fromPhoneNumber = "+27823334444",
            senderName = "Voice Caller",
            text = "Voice message asking about hair treatments",
            messageType = WhatsAppMessageType.AUDIO_VOICE_NOTE
        )

        agentService.handleInboundInteraction(inbound)

        val messages = repository.allChatMessages.first()
        assertTrue(messages.first().isVoiceNote)
        assertEquals("WhatsApp Voice", messages.first().audioModelUsed)
    }

    @Test
    fun testCommunicationApi_sendMessageAndDeliveryStatus() = runBlocking {
        val outbound = WhatsAppOutboundMessage(
            recipientPhoneNumber = "+27829990000",
            bodyText = "Your booking for Saturday is confirmed."
        )

        val result = communicationApi.sendMessage(outbound)
        assertTrue(result is WhatsAppSendResult.Success)
        val success = result as WhatsAppSendResult.Success
        assertTrue(success.messageId.startsWith("wamid_"))

        // Test Mark as Read
        val readSuccess = communicationApi.markAsRead("inbound_msg_99")
        assertTrue(readSuccess)

        // Test Webhook Verification Handshake
        val validChallenge = communicationApi.verifyWebhookToken(
            hubMode = "subscribe",
            hubVerifyToken = "zama_autonomous_agent_token_2026",
            hubChallenge = "11559955"
        )
        assertEquals("11559955", validChallenge)

        val invalidChallenge = communicationApi.verifyWebhookToken(
            hubMode = "subscribe",
            hubVerifyToken = "wrong_token",
            hubChallenge = "11559955"
        )
        assertNull(invalidChallenge)
    }

    @Test
    fun testInteractionRouter_parseWebhookJson() = runBlocking {
        val metaWebhookJson = """
            {
              "object": "whatsapp_business_account",
              "entry": [{
                "changes": [{
                  "value": {
                    "messaging_product": "whatsapp",
                    "contacts": [{"profile": {"name": "Zintle Mokoena"}}],
                    "messages": [{
                      "from": "+27825559999",
                      "id": "wamid_webhook_123",
                      "text": {"body": "How much for a Silk Press?"}
                    }]
                  }
                }]
              }]
            }
        """.trimIndent()

        val decision = router.routeWebhookJson(metaWebhookJson)
        assertNotNull(decision)
        assertEquals(AgentIntent.PRICING_REQUEST, decision?.intent)
        assertTrue(decision!!.replyText.contains("Silk Press"))

        val chatMessages = repository.allChatMessages.first()
        assertTrue(chatMessages.any { it.content == "How much for a Silk Press?" })
    }

    @Test
    fun testAutoReplyDisabled_onlyLogsWithoutSending() = runBlocking {
        agentService.setAutoReplyActive(false)
        assertFalse(agentService.isAutoReplyActive.value)

        val inbound = WhatsAppInboundMessage(
            messageId = "inbound_noreply_005",
            fromPhoneNumber = "+27821112222",
            senderName = "Mbali",
            text = "Can I change my time?"
        )

        agentService.handleInboundInteraction(inbound)

        // With auto-reply false, only the customer's message was saved
        val messages = repository.allChatMessages.first()
        assertEquals(1, messages.size)
        assertTrue(messages[0].isFromUser)

        // The interaction log is still preserved in the database
        val interactions = repository.allInteractions.first()
        assertEquals(1, interactions.size)
    }

    @Test
    fun testInboundMessage_whenAwayModeActive_dispatchesAutomatedAwayResponse() = runBlocking {
        // Given Away Mode is active
        agentService.setAwayMode(true)
        assertTrue(agentService.isAwayModeActive.value)

        val inbound = WhatsAppInboundMessage(
            messageId = "inbound_away_001",
            fromPhoneNumber = "+27827778899",
            senderName = "Zinhle",
            text = "Hello! Are you open right now? Need braids done today."
        )

        // When inbound message is received
        val decision = agentService.handleInboundInteraction(inbound)

        // Then verify automated Away response is dispatched
        assertNotNull(decision)
        assertTrue(decision.replyText.contains("Zinhle"))
        assertTrue(decision.replyText.contains("away from the desk"))
        assertTrue(decision.reasoningTrace.contains("AUTOMATED AWAY RESPONSE"))

        // Verify messages persisted in Room
        val chatMessages = repository.allChatMessages.first()
        assertEquals(2, chatMessages.size) // Customer message + Outbound Away reply
        assertEquals(inbound.text, chatMessages[0].content)
        assertTrue(chatMessages[0].isFromUser)

        val aiMessage = chatMessages[1]
        assertFalse(aiMessage.isFromUser)
        assertEquals(decision.replyText, aiMessage.content)
        assertEquals("AWAY_AUTO_REPLY", aiMessage.intentTag)

        // Verify interaction audit log
        val interactions = repository.allInteractions.first()
        assertEquals(1, interactions.size)
        assertEquals("Away Auto-Reply", interactions[0].resolvedIntent)
    }

    @Test
    fun testAwayMode_customMessageWithPersonalization() = runBlocking {
        val customNotice = "Hey {name}! The salon is currently closed for a team workshop. We will reopen tomorrow at 8am."
        agentService.setAwayMode(true, customNotice)
        assertEquals(customNotice, agentService.awayMessage.value)

        val inbound = WhatsAppInboundMessage(
            messageId = "inbound_away_custom_002",
            fromPhoneNumber = "+27831119988",
            senderName = "Nomvula",
            text = "Can I come in now?"
        )

        val decision = agentService.handleInboundInteraction(inbound)

        assertEquals("Hey Nomvula! The salon is currently closed for a team workshop. We will reopen tomorrow at 8am.", decision.replyText)
    }

    @Test
    fun testAwayMode_toggleBackToAutonomousMode_resumesStandardAi() = runBlocking {
        // Given away mode is enabled and then toggled off
        agentService.setAwayMode(true)
        assertTrue(agentService.isAwayModeActive.value)

        agentService.setAwayMode(false)
        assertFalse(agentService.isAwayModeActive.value)

        val inbound = WhatsAppInboundMessage(
            messageId = "inbound_resume_ai_003",
            fromPhoneNumber = "+27823331122",
            senderName = "Sipho",
            text = "How much for a Silk Press?"
        )

        val decision = agentService.handleInboundInteraction(inbound)

        // Standard AI intelligence resumes
        assertEquals(AgentIntent.PRICING_REQUEST, decision.intent)
        assertTrue(decision.replyText.contains("Silk Press"))
    }

    @Test
    fun testWhatsAppAgentSettingsManager_persistsSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsManager = com.example.model.WhatsAppAgentSettingsManager(context)

        // Toggle away mode and set custom message
        settingsManager.setAwayModeActive(true)
        val customText = "Our reception is out of office for lunch. Back in 30 mins."
        settingsManager.setAwayMessage(customText)

        // Create a new instance representing app restart
        val newSettingsManager = com.example.model.WhatsAppAgentSettingsManager(context)
        assertTrue(newSettingsManager.isAwayModeActive.value)
        assertEquals(customText, newSettingsManager.awayMessage.value)

        // Reset
        newSettingsManager.resetAwayMessage()
        assertEquals(WhatsAppAgentService.DEFAULT_AWAY_MESSAGE, newSettingsManager.awayMessage.value)
        newSettingsManager.setAwayModeActive(false)
        assertFalse(newSettingsManager.isAwayModeActive.value)
    }
}
