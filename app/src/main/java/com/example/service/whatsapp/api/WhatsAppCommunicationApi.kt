package com.example.service.whatsapp.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

/**
 * Standard contract for interacting with WhatsApp Communication APIs (Meta Cloud API, Twilio, Infobip).
 */
interface WhatsAppCommunicationApi {

    /**
     * Sends an outbound WhatsApp message to a customer.
     */
    suspend fun sendMessage(message: WhatsAppOutboundMessage): WhatsAppSendResult

    /**
     * Marks an incoming customer message as READ on WhatsApp.
     */
    suspend fun markAsRead(messageId: String): Boolean

    /**
     * Verifies webhook subscription token for Meta/Twilio webhook handshake.
     */
    fun verifyWebhookToken(hubMode: String, hubVerifyToken: String, hubChallenge: String): String?

    /**
     * Reactive stream of delivery status updates (SENT -> DELIVERED -> READ).
     */
    val deliveryReceipts: Flow<Pair<String, WhatsAppDeliveryStatus>>
}

/**
 * Robust sandbox communication API implementation for local testing, live demos, and UI monitoring.
 */
class SandboxWhatsAppCommunicationApi(
    private val config: WhatsAppApiConfig = WhatsAppApiConfig()
) : WhatsAppCommunicationApi {

    private val _deliveryReceipts = MutableSharedFlow<Pair<String, WhatsAppDeliveryStatus>>(extraBufferCapacity = 64)
    override val deliveryReceipts: Flow<Pair<String, WhatsAppDeliveryStatus>> = _deliveryReceipts.asSharedFlow()

    override suspend fun sendMessage(message: WhatsAppOutboundMessage): WhatsAppSendResult {
        // Simulate realistic network transmission delay
        delay(180)

        if (message.bodyText.isBlank()) {
            return WhatsAppSendResult.Error("Message body cannot be empty", errorCode = 400)
        }

        val generatedMessageId = "wamid_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        // Simulate progression: SENT -> DELIVERED -> READ
        _deliveryReceipts.tryEmit(Pair(generatedMessageId, WhatsAppDeliveryStatus.SENT))

        // Background simulate delivery receipts
        kotlinx.coroutines.GlobalScope.let {
            // Emitted synchronously into buffer for immediate reflection
            _deliveryReceipts.tryEmit(Pair(generatedMessageId, WhatsAppDeliveryStatus.DELIVERED))
            _deliveryReceipts.tryEmit(Pair(generatedMessageId, WhatsAppDeliveryStatus.READ))
        }

        return WhatsAppSendResult.Success(
            messageId = generatedMessageId,
            deliveryStatus = WhatsAppDeliveryStatus.SENT
        )
    }

    override suspend fun markAsRead(messageId: String): Boolean {
        delay(50)
        _deliveryReceipts.tryEmit(Pair(messageId, WhatsAppDeliveryStatus.READ))
        return true
    }

    override fun verifyWebhookToken(hubMode: String, hubVerifyToken: String, hubChallenge: String): String? {
        return if (hubMode == "subscribe" && hubVerifyToken == config.webhookVerifyToken) {
            hubChallenge
        } else {
            null
        }
    }
}
