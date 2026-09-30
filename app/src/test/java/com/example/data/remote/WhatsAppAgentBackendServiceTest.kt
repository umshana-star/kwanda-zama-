package com.example.data.remote

import com.example.ui.components.WhatsAppConnectionStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WhatsAppAgentBackendServiceTest {

    private lateinit var backendService: WhatsAppAgentBackendService

    @Before
    fun setup() {
        backendService = WhatsAppAgentBackendService(
            apiService = WhatsAppAgentNetworkModule.apiService,
            moshi = WhatsAppAgentNetworkModule.moshi,
            apiKeyProvider = { "" }
        )
    }

    @Test
    fun moshiSerialization_roundTripsRequestAndResponseModels() {
        val request = WhatsAppAgentMessageRequest(
            agentId = "agent_thandiwe_01",
            customerPhone = "+27 82 419 8820",
            customerName = "Naledi Khumalo",
            messageText = "Do you have Knotless Braids open this Saturday?",
            messageType = "TEXT",
            conversationHistory = listOf(
                WhatsAppConversationTurnDto(
                    role = "customer",
                    content = "Hello Zama!",
                    timestampMillis = 1700000000000L
                )
            ),
            timestampMillis = 1700000005000L
        )

        val serializedJson = backendService.serializeMessageRequestToJson(request)
        assertTrue(serializedJson.contains("\"agent_id\":\"agent_thandiwe_01\""))
        assertTrue(serializedJson.contains("\"customer_name\":\"Naledi Khumalo\""))
        assertTrue(serializedJson.contains("\"conversation_history\""))

        val sampleResponseJson = """
            {
              "message_id": "wamid.zama.12345",
              "agent_id": "agent_thandiwe_01",
              "agent_name": "Thandiwe",
              "reply_text": "Saturday at 14:00 is open for Knotless Braids (R650).",
              "detected_intent": "BOOKING_TRIAGE",
              "confidence_score": 0.98,
              "latency_ms": 620,
              "reasoning_trace": "Intent: BOOKING_TRIAGE",
              "requires_human_handoff": false,
              "reserved_slot_id": "BK-749",
              "status": "DISPATCHED",
              "timestamp_millis": 1700000010000
            }
        """.trimIndent()

        val deserialized = backendService.deserializeMessageResponseFromJson(sampleResponseJson)
        assertNotNull(deserialized)
        assertEquals("wamid.zama.12345", deserialized?.messageId)
        assertEquals("Thandiwe", deserialized?.agentName)
        assertEquals("BOOKING_TRIAGE", deserialized?.detectedIntent)
        assertEquals("BK-749", deserialized?.reservedSlotId)
        assertFalse(deserialized?.requiresHumanHandoff ?: true)
    }

    @Test
    fun exchangeCustomerMessage_usesRetrofitAndMoshiAndUpdatesReactiveFlows() = runBlocking {
        val result = backendService.exchangeCustomerMessage(
            agentId = "agent_thandiwe_01",
            customerPhone = "+27 82 419 8820",
            customerName = "Naledi Khumalo",
            messageText = "Hi! Can I book Knotless Braids this Saturday at 2pm?"
        )

        assertTrue(result is WhatsAppAgentExchangeResult.Success)
        val response = (result as WhatsAppAgentExchangeResult.Success).data
        assertEquals("agent_thandiwe_01", response.agentId)
        assertEquals("Thandiwe", response.agentName)
        assertEquals("BOOKING_TRIAGE", response.detectedIntent)
        assertEquals("BK-749", response.reservedSlotId)
        assertTrue(response.replyText.contains("Knotless Braids"))

        // Verify reactive state flows were updated
        assertEquals(WhatsAppConnectionStatus.CONNECTED, backendService.connectionStatus.value)
        assertEquals("Naledi Khumalo", backendService.activeConversationState.value.activeCustomerName)
        assertEquals(
            response.messageId,
            backendService.incomingAgentMessages.value.first().id
        )
    }

    @Test
    fun fetchStatusUpdateConfigAndSyncWebhooks_succeedViaRetrofitAndMoshi() = runBlocking {
        // 1. Fetch single agent status
        val singleStatus = backendService.fetchAgentStatus("agent_sipho_02")
        assertTrue(singleStatus is WhatsAppAgentExchangeResult.Success)
        assertEquals("Sipho", (singleStatus as WhatsAppAgentExchangeResult.Success).data.agentName)

        // 2. Fetch all agent statuses
        val allStatuses = backendService.fetchAllAgentStatuses()
        assertTrue(allStatuses is WhatsAppAgentExchangeResult.Success)
        assertEquals(4, (allStatuses as WhatsAppAgentExchangeResult.Success).data.size)

        // 3. Update agent configuration
        val configUpdate = backendService.updateAgentConfig(
            WhatsAppAgentConfigUpdateRequest(
                agentId = "agent_nandi_03",
                isAutonomousEnabled = true,
                operationalStatus = "ACTIVE_ONLINE",
                confidenceThreshold = 0.91f,
                aiResponseDelayMs = 520L
            )
        )
        assertTrue(configUpdate is WhatsAppAgentExchangeResult.Success)
        assertEquals(520L, (configUpdate as WhatsAppAgentExchangeResult.Success).data.avgLatencyMs)

        // 4. Sync webhook queue
        val webhookSync = backendService.syncWebhookQueue(agentId = "agent_thandiwe_01", limit = 3)
        assertTrue(webhookSync is WhatsAppAgentExchangeResult.Success)
        assertEquals(3, (webhookSync as WhatsAppAgentExchangeResult.Success).data.incomingMessages.size)
    }

    @Test
    fun periodicHeartbeatCoroutine_verifiesConnectivityMaintainsSessionAndUpdatesState() = runBlocking {
        // 1. Single heartbeat pulse
        val pulseResult = backendService.sendHeartbeatPulse("agent_thandiwe_01")
        assertTrue(pulseResult is WhatsAppAgentExchangeResult.Success)
        val hbResponse = (pulseResult as WhatsAppAgentExchangeResult.Success).data
        assertTrue(hbResponse.sessionActive)
        assertEquals(1L, hbResponse.sequenceNumber)
        assertEquals(1L, backendService.heartbeatState.value.sequenceNumber)
        assertEquals(WhatsAppConnectionStatus.CONNECTED, backendService.connectionStatus.value)

        // 2. Start periodic background heartbeat coroutine with a fast interval
        val job = backendService.startPeriodicHeartbeat(
            scope = this,
            agentId = "agent_thandiwe_01",
            intervalMs = 60L
        )
        assertTrue(backendService.heartbeatState.value.isRunning)

        kotlinx.coroutines.delay(160L)
        backendService.stopPeriodicHeartbeat()
        job.join()

        assertFalse(backendService.heartbeatState.value.isRunning)
        assertTrue(
            "Periodic heartbeat should increment sequence number automatically",
            backendService.heartbeatState.value.sequenceNumber >= 2L
        )
        assertTrue(backendService.heartbeatState.value.sessionActive)
    }
}
