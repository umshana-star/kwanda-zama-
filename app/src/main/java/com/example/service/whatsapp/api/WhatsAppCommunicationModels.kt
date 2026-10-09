package com.example.service.whatsapp.api

/**
 * Delivery status for WhatsApp communication messages.
 */
enum class WhatsAppDeliveryStatus {
    RECEIVED,
    PROCESSING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

/**
 * Message types supported by WhatsApp communication APIs.
 */
enum class WhatsAppMessageType {
    TEXT,
    AUDIO_VOICE_NOTE,
    INTERACTIVE_BUTTON,
    LOCATION,
    TEMPLATE
}

/**
 * Inbound WhatsApp message received via webhook from a communication API.
 */
data class WhatsAppInboundMessage(
    val messageId: String,
    val fromPhoneNumber: String,
    val senderName: String,
    val text: String,
    val messageType: WhatsAppMessageType = WhatsAppMessageType.TEXT,
    val timestampMillis: Long = System.currentTimeMillis(),
    val rawPayload: String = "",
    val mediaUrl: String? = null
)

/**
 * Outbound message to be sent via WhatsApp communication API.
 */
data class WhatsAppOutboundMessage(
    val recipientPhoneNumber: String,
    val bodyText: String,
    val replyToMessageId: String? = null,
    val quickActionButtons: List<String> = emptyList(),
    val mediaUrl: String? = null
)

/**
 * Configuration for connecting to external WhatsApp communication APIs (Meta Graph API / Twilio / Infobip).
 */
data class WhatsAppApiConfig(
    val providerName: String = "Meta WhatsApp Cloud API",
    val apiEndpointUrl: String = "https://graph.facebook.com/v21.0/messages",
    val phoneNumberId: String = "109847291823901",
    val businessAccountId: String = "982341209384",
    val webhookVerifyToken: String = "zama_autonomous_agent_token_2026",
    val isSandboxMode: Boolean = true,
    val autoReplyEnabled: Boolean = true
)

/**
 * Result of transmitting an outbound message through a WhatsApp communication API.
 */
sealed class WhatsAppSendResult {
    data class Success(
        val messageId: String,
        val deliveryStatus: WhatsAppDeliveryStatus,
        val timestampMillis: Long = System.currentTimeMillis()
    ) : WhatsAppSendResult()

    data class Error(
        val errorMessage: String,
        val errorCode: Int = -1,
        val isTransient: Boolean = false
    ) : WhatsAppSendResult()
}
