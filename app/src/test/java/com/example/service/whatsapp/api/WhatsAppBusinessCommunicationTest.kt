package com.example.service.whatsapp.api

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WhatsAppAgentRepository
import com.example.data.local.ZamaDatabase
import com.example.service.whatsapp.WhatsAppAgentService
import com.example.service.whatsapp.intelligence.GeminiAgentIntelligence
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class WhatsAppBusinessCommunicationTest {

    private lateinit var database: ZamaDatabase
    private lateinit var repository: WhatsAppAgentRepository
    private lateinit var agentService: WhatsAppAgentService
    private lateinit var communicationService: WhatsAppBusinessCommunicationService
    private lateinit var manager: WhatsAppBusinessCommunicationManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ZamaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WhatsAppAgentRepository(database.whatsAppInteractionDao(), database.chatDao())
        communicationService = WhatsAppBusinessCommunicationService()
        agentService = WhatsAppAgentService(
            repository = repository,
            communicationApi = communicationService,
            intelligence = GeminiAgentIntelligence { "" } // deterministic fallback engine
        )
        manager = WhatsAppBusinessCommunicationManager(
            communicationService = communicationService,
            agentService = agentService
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testWebhookVerificationHandshake_validTokenReturnsChallenge() {
        val expectedToken = "zama_autonomous_agent_token_2026"
        val challenge = "random_challenge_string_91823"

        val verified = WhatsAppWebhookParser.verifyWebhookHandshake(
            hubMode = "subscribe",
            hubVerifyToken = expectedToken,
            hubChallenge = challenge,
            expectedVerifyToken = expectedToken
        )

        assertEquals(challenge, verified)
    }

    @Test
    fun testWebhookVerificationHandshake_invalidTokenReturnsNull() {
        val verified = WhatsAppWebhookParser.verifyWebhookHandshake(
            hubMode = "subscribe",
            hubVerifyToken = "wrong_token",
            hubChallenge = "challenge_123",
            expectedVerifyToken = "zama_autonomous_agent_token_2026"
        )

        assertNull(verified)
    }

    @Test
    fun testParseInboundMessages_parsesTextMessageCorrectly() {
        val sampleJson = """
        {
          "object": "whatsapp_business_account",
          "entry": [{
            "id": "982341209384",
            "changes": [{
              "value": {
                "messaging_product": "whatsapp",
                "metadata": {
                  "display_phone_number": "27820000000",
                  "phone_number_id": "109847291823901"
                },
                "contacts": [{
                  "profile": { "name": "Lindiwe Ndlovu" },
                  "wa_id": "27831234567"
                }],
                "messages": [{
                  "from": "27831234567",
                  "id": "wamid.HBgLMjc4MzEyMzQ1NjcVAgASGBQzQTkyREI4QkEwOUI1QzFBMkQ1NgA=",
                  "timestamp": "1700000100",
                  "text": { "body": "Hi Zama! Do you have slots for Knotless Braids on Saturday?" },
                  "type": "text"
                }]
              },
              "field": "messages"
            }]
          }]
        }
        """.trimIndent()

        val parsedList = WhatsAppWebhookParser.parseInboundMessages(sampleJson)
        assertEquals(1, parsedList.size)

        val msg = parsedList[0]
        assertEquals("27831234567", msg.fromPhoneNumber)
        assertEquals("Lindiwe Ndlovu", msg.senderName)
        assertEquals("Hi Zama! Do you have slots for Knotless Braids on Saturday?", msg.text)
        assertEquals(WhatsAppMessageType.TEXT, msg.messageType)
    }

    @Test
    fun testParseInboundMessages_parsesInteractiveButtonReply() {
        val buttonJson = """
        {
          "object": "whatsapp_business_account",
          "entry": [{
            "changes": [{
              "value": {
                "messaging_product": "whatsapp",
                "contacts": [{ "profile": { "name": "Ayanda D." }, "wa_id": "27829998877" }],
                "messages": [{
                  "from": "27829998877",
                  "id": "wamid.btn_reply_001",
                  "type": "interactive",
                  "interactive": {
                    "type": "button_reply",
                    "button_reply": { "id": "btn_confirm_slot", "title": "Confirm Saturday 2pm" }
                  }
                }]
              }
            }]
          }]
        }
        """.trimIndent()

        val messages = WhatsAppWebhookParser.parseInboundMessages(buttonJson)
        assertEquals(1, messages.size)
        assertEquals("Confirm Saturday 2pm", messages[0].text)
        assertEquals(WhatsAppMessageType.INTERACTIVE_BUTTON, messages[0].messageType)
        assertEquals("Ayanda D.", messages[0].senderName)
    }

    @Test
    fun testParseDeliveryReceipts_extractsDeliveryStatuses() {
        val receiptsJson = """
        {
          "object": "whatsapp_business_account",
          "entry": [{
            "changes": [{
              "value": {
                "messaging_product": "whatsapp",
                "statuses": [
                  {
                    "id": "wamid.outbound_001",
                    "status": "delivered",
                    "timestamp": "1700000200",
                    "recipient_id": "27831234567"
                  },
                  {
                    "id": "wamid.outbound_002",
                    "status": "read",
                    "timestamp": "1700000210",
                    "recipient_id": "27831234567"
                  }
                ]
              }
            }]
          }]
        }
        """.trimIndent()

        val receipts = WhatsAppWebhookParser.parseDeliveryReceipts(receiptsJson)
        assertEquals(2, receipts.size)
        assertEquals("wamid.outbound_001", receipts[0].messageId)
        assertEquals(WhatsAppDeliveryStatus.DELIVERED, receipts[0].status)
        assertEquals("wamid.outbound_002", receipts[1].messageId)
        assertEquals(WhatsAppDeliveryStatus.READ, receipts[1].status)
    }

    @Test
    fun testCommunicationManager_processesInboundWebhookAndDispatchesReplies(): Unit = runBlocking {
        val samplePayload = """
        {
          "object": "whatsapp_business_account",
          "entry": [{
            "changes": [{
              "value": {
                "messaging_product": "whatsapp",
                "contacts": [{ "profile": { "name": "Kgomotso" }, "wa_id": "27845550192" }],
                "messages": [{
                  "from": "27845550192",
                  "id": "wamid_inbound_test_100",
                  "text": { "body": "What are your prices for Silk Press?" },
                  "type": "text"
                }]
              }
            }]
          }]
        }
        """.trimIndent()

        val decisions = manager.processInboundWebhook(samplePayload)
        assertEquals(1, decisions.size)

        val decision = decisions[0]
        assertNotNull(decision.replyText)
        assertTrue(decision.replyText.contains("Silk Press"))

        val telemetry = manager.telemetry.value
        assertEquals(1, telemetry.totalInboundMessages)
        assertEquals(1, telemetry.totalOutboundMessages)
        assertEquals("27845550192", telemetry.lastReceivedSenderPhone)

        // Verify that interaction was stored into the Room database
        val interactions = repository.allInteractions.first()
        assertEquals(1, interactions.size)
        assertEquals("Kgomotso", interactions[0].customerName)
    }

    @Test
    fun testCommunicationManager_sendDirectMessageUpdatesTelemetry(): Unit = runBlocking {
        val result = manager.sendDirectMessage(
            recipientPhoneNumber = "+27821112233",
            bodyText = "Proactive Appointment Reminder from Zama Salon",
            buttons = listOf("Confirm Slot", "Reschedule")
        )

        assertTrue(result is WhatsAppSendResult.Success)
        val success = result as WhatsAppSendResult.Success
        assertNotNull(success.messageId)
        assertEquals(WhatsAppDeliveryStatus.SENT, success.deliveryStatus)

        val telemetry = manager.telemetry.value
        assertEquals(1, telemetry.totalOutboundMessages)
    }

    @Test
    fun testCommunicationManager_toggleLiveModeSwitchesState() {
        manager.toggleLiveMode(true)
        assertTrue(manager.telemetry.value.isLiveCloudApiActive)
        assertEquals(WhatsAppConnectionMode.LiveMetaCloudApi, communicationService.activeMode.value)

        manager.toggleLiveMode(false)
        assertEquals(false, manager.telemetry.value.isLiveCloudApiActive)
        assertEquals(WhatsAppConnectionMode.SandboxSimulation, communicationService.activeMode.value)
    }
}
