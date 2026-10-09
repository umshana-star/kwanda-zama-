package com.example.service.whatsapp.api

import com.example.service.whatsapp.WhatsAppAgentService
import com.example.service.whatsapp.intelligence.AgentDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WhatsAppBusinessTelemetry(
    val totalInboundMessages: Int = 0,
    val totalOutboundMessages: Int = 0,
    val totalDeliveryReceipts: Int = 0,
    val totalFailures: Int = 0,
    val lastReceivedSenderPhone: String? = null,
    val lastReceivedTimestamp: Long? = null,
    val isLiveCloudApiActive: Boolean = false
)

/**
 * High-level Service Manager coordinating all WhatsApp Business API communication
 * with the autonomous agent intelligence and Room database layers.
 */
class WhatsAppBusinessCommunicationManager(
    val communicationService: WhatsAppBusinessCommunicationService,
    private val agentService: WhatsAppAgentService,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private val _telemetry = MutableStateFlow(WhatsAppBusinessTelemetry())
    val telemetry: StateFlow<WhatsAppBusinessTelemetry> = _telemetry.asStateFlow()

    init {
        // Collect delivery status receipts and update telemetry
        scope.launch {
            communicationService.deliveryReceipts.collect { receipt ->
                val current = _telemetry.value
                val isFailure = receipt.second == WhatsAppDeliveryStatus.FAILED
                _telemetry.value = current.copy(
                    totalDeliveryReceipts = current.totalDeliveryReceipts + 1,
                    totalFailures = if (isFailure) current.totalFailures + 1 else current.totalFailures
                )
            }
        }
    }

    /**
     * Ingests a raw Meta WhatsApp Business Webhook JSON payload.
     * Extracts incoming customer messages, triggers the autonomous agent reasoning loop,
     * and updates delivery receipts.
     */
    suspend fun processInboundWebhook(rawJson: String): List<AgentDecision> {
        val decisions = mutableListOf<AgentDecision>()

        // 1. Process delivery receipts if present
        val receipts = WhatsAppWebhookParser.parseDeliveryReceipts(rawJson)
        receipts.forEach { receipt ->
            communicationService.onDeliveryReceiptReceived(receipt)
        }

        // 2. Process incoming customer messages
        val inboundMessages = WhatsAppWebhookParser.parseInboundMessages(rawJson)
        inboundMessages.forEach { msg ->
            val current = _telemetry.value
            _telemetry.value = current.copy(
                totalInboundMessages = current.totalInboundMessages + 1,
                lastReceivedSenderPhone = msg.fromPhoneNumber,
                lastReceivedTimestamp = msg.timestampMillis
            )

            val decision = agentService.handleInboundInteraction(msg)
            decisions.add(decision)

            _telemetry.value = _telemetry.value.copy(
                totalOutboundMessages = _telemetry.value.totalOutboundMessages + 1
            )
        }

        return decisions
    }

    /**
     * Directly sends a proactive outbound message through the WhatsApp Business API.
     */
    suspend fun sendDirectMessage(
        recipientPhoneNumber: String,
        bodyText: String,
        buttons: List<String> = emptyList()
    ): WhatsAppSendResult {
        val outbound = WhatsAppOutboundMessage(
            recipientPhoneNumber = recipientPhoneNumber,
            bodyText = bodyText,
            quickActionButtons = buttons
        )
        val result = communicationService.sendMessage(outbound)
        if (result is WhatsAppSendResult.Success) {
            _telemetry.value = _telemetry.value.copy(
                totalOutboundMessages = _telemetry.value.totalOutboundMessages + 1
            )
        }
        return result
    }

    /**
     * Toggles between Live WhatsApp Cloud API and Sandbox simulation mode.
     */
    fun toggleLiveMode(isLive: Boolean) {
        communicationService.setLiveMode(isLive)
        _telemetry.value = _telemetry.value.copy(
            isLiveCloudApiActive = isLive
        )
    }

    /**
     * Verifies a Meta webhook handshake challenge.
     */
    fun verifyWebhookHandshake(mode: String?, token: String?, challenge: String?): String? {
        return communicationService.verifyWebhookToken(
            hubMode = mode ?: "",
            hubVerifyToken = token ?: "",
            hubChallenge = challenge ?: ""
        )
    }
}
