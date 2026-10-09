package com.example.service.whatsapp.router

import com.example.service.whatsapp.WhatsAppAgentService
import com.example.service.whatsapp.api.WhatsAppInboundMessage
import com.example.service.whatsapp.api.WhatsAppMessageType
import com.example.service.whatsapp.intelligence.AgentDecision
import org.json.JSONObject
import java.util.UUID

/**
 * Webhook Router responsible for ingesting, validating, and converting raw WhatsApp payloads
 * from external communication APIs into domain models, and routing them to the agent intelligence.
 */
class WhatsAppInteractionRouter(
    private val agentService: WhatsAppAgentService
) {

    /**
     * Parses standard Meta WhatsApp Cloud API webhook JSON payload.
     */
    suspend fun routeWebhookJson(rawJson: String): AgentDecision? {
        try {
            val root = JSONObject(rawJson)
            val entry = root.optJSONArray("entry")?.optJSONObject(0)
            val change = entry?.optJSONArray("changes")?.optJSONObject(0)
            val value = change?.optJSONObject("value")

            val messages = value?.optJSONArray("messages")
            if (messages != null && messages.length() > 0) {
                val msgObj = messages.getJSONObject(0)
                val from = msgObj.optString("from", "Unknown_Number")
                val msgId = msgObj.optString("id", "msg_${UUID.randomUUID().toString().take(8)}")
                val contactName = value.optJSONArray("contacts")?.optJSONObject(0)?.optJSONObject("profile")?.optString("name") ?: "WhatsApp Client"

                val text = when {
                    msgObj.has("text") -> msgObj.getJSONObject("text").optString("body", "")
                    msgObj.has("interactive") -> msgObj.getJSONObject("interactive").optJSONObject("button_reply")?.optString("title", "") ?: ""
                    else -> "Customer shared a media attachment."
                }

                val inbound = WhatsAppInboundMessage(
                    messageId = msgId,
                    fromPhoneNumber = from,
                    senderName = contactName,
                    text = text,
                    messageType = WhatsAppMessageType.TEXT,
                    rawPayload = rawJson
                )
                return agentService.handleInboundInteraction(inbound)
            }
        } catch (_: Exception) {
            // Parse error or non-message webhook (e.g. status update)
        }
        return null
    }

    /**
     * Simulates an incoming customer interaction for testing and UI demonstration.
     */
    suspend fun simulateCustomerMessage(
        messageText: String,
        senderName: String = "Sarah Jenkins",
        senderPhone: String = "+27 82 555 0192",
        isVoiceNote: Boolean = false
    ): AgentDecision {
        val inbound = WhatsAppInboundMessage(
            messageId = "sim_${UUID.randomUUID().toString().take(10)}",
            fromPhoneNumber = senderPhone,
            senderName = senderName,
            text = messageText,
            messageType = if (isVoiceNote) WhatsAppMessageType.AUDIO_VOICE_NOTE else WhatsAppMessageType.TEXT,
            rawPayload = "{\"source\": \"simulated_whatsapp_frontdoor\", \"client\": \"$senderName\"}"
        )
        return agentService.handleInboundInteraction(inbound)
    }
}
