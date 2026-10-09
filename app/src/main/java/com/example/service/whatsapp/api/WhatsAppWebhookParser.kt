package com.example.service.whatsapp.api

import org.json.JSONObject
import java.util.UUID

/**
 * Parsed delivery status receipt from WhatsApp Business API webhook callbacks.
 */
data class WhatsAppDeliveryReceipt(
    val messageId: String,
    val recipientPhoneNumber: String,
    val status: WhatsAppDeliveryStatus,
    val timestampSeconds: Long,
    val rawPayload: String = ""
)

/**
 * Enterprise-grade parser for Meta WhatsApp Business Cloud API webhook payloads.
 * Handles:
 * 1. Incoming customer messages (text, interactive buttons, audio/voice, media).
 * 2. Status update receipts (sent, delivered, read, failed).
 * 3. Webhook subscription verification handshakes (GET hub.challenge).
 */
object WhatsAppWebhookParser {

    /**
     * Verifies the initial Meta Webhook handshake request.
     * When Meta verifies the callback URL, it sends:
     * - hub.mode = "subscribe"
     * - hub.verify_token = <configured verify token>
     * - hub.challenge = <random challenge string>
     * Returns the challenge string if token matches, or null otherwise.
     */
    fun verifyWebhookHandshake(
        hubMode: String?,
        hubVerifyToken: String?,
        hubChallenge: String?,
        expectedVerifyToken: String
    ): String? {
        return if (hubMode == "subscribe" && hubVerifyToken == expectedVerifyToken && !hubChallenge.isNullOrBlank()) {
            hubChallenge
        } else {
            null
        }
    }

    /**
     * Parses an incoming WhatsApp Business Cloud API webhook JSON payload
     * and extracts all customer messages.
     */
    fun parseInboundMessages(rawJson: String): List<WhatsAppInboundMessage> {
        val result = mutableListOf<WhatsAppInboundMessage>()
        try {
            val root = JSONObject(rawJson)
            val entryArr = root.optJSONArray("entry") ?: return emptyList()

            for (i in 0 until entryArr.length()) {
                val entryObj = entryArr.getJSONObject(i)
                val changesArr = entryObj.optJSONArray("changes") ?: continue

                for (j in 0 until changesArr.length()) {
                    val changeObj = changesArr.getJSONObject(j)
                    val valueObj = changeObj.optJSONObject("value") ?: continue

                    // Check if value contains messages
                    val messagesArr = valueObj.optJSONArray("messages") ?: continue
                    val contactsArr = valueObj.optJSONArray("contacts")

                    // Resolve sender contact profile name if available
                    val contactsMap = mutableMapOf<String, String>()
                    if (contactsArr != null) {
                        for (k in 0 until contactsArr.length()) {
                            val contact = contactsArr.getJSONObject(k)
                            val waId = contact.optString("wa_id", "")
                            val profileName = contact.optJSONObject("profile")?.optString("name", "WhatsApp Client")
                            if (waId.isNotBlank() && profileName != null) {
                                contactsMap[waId] = profileName
                            }
                        }
                    }

                    for (m in 0 until messagesArr.length()) {
                        val msgObj = messagesArr.getJSONObject(m)
                        val fromPhone = msgObj.optString("from", "")
                        val messageId = msgObj.optString("id", "wamid_${UUID.randomUUID().toString().take(12)}")
                        val typeStr = msgObj.optString("type", "text")
                        val timestampSeconds = msgObj.optLong("timestamp", System.currentTimeMillis() / 1000L)
                        val timestampMillis = timestampSeconds * 1000L

                        val senderName = contactsMap[fromPhone] ?: "WhatsApp Client"

                        var bodyText = ""
                        var messageType = WhatsAppMessageType.TEXT
                        var mediaUrl: String? = null

                        when (typeStr) {
                            "text" -> {
                                bodyText = msgObj.optJSONObject("text")?.optString("body", "") ?: ""
                                messageType = WhatsAppMessageType.TEXT
                            }
                            "interactive" -> {
                                val interactiveObj = msgObj.optJSONObject("interactive")
                                val buttonReply = interactiveObj?.optJSONObject("button_reply")
                                val listReply = interactiveObj?.optJSONObject("list_reply")
                                bodyText = buttonReply?.optString("title")
                                    ?: listReply?.optString("title")
                                    ?: "Interactive selection"
                                messageType = WhatsAppMessageType.INTERACTIVE_BUTTON
                            }
                            "audio", "voice" -> {
                                val audioObj = msgObj.optJSONObject("audio") ?: msgObj.optJSONObject("voice")
                                mediaUrl = audioObj?.optString("id")
                                bodyText = "🎤 Voice note received"
                                messageType = WhatsAppMessageType.AUDIO_VOICE_NOTE
                            }
                            "image" -> {
                                val imageObj = msgObj.optJSONObject("image")
                                mediaUrl = imageObj?.optString("id")
                                bodyText = imageObj?.optString("caption", "📷 Image received") ?: "📷 Image received"
                                messageType = WhatsAppMessageType.TEXT
                            }
                            "location" -> {
                                val locObj = msgObj.optJSONObject("location")
                                val name = locObj?.optString("name", "")
                                val addr = locObj?.optString("address", "")
                                bodyText = "📍 Location: $name $addr".trim()
                                messageType = WhatsAppMessageType.LOCATION
                            }
                            "button" -> {
                                val btnObj = msgObj.optJSONObject("button")
                                bodyText = btnObj?.optString("text", "Button clicked") ?: "Button clicked"
                                messageType = WhatsAppMessageType.INTERACTIVE_BUTTON
                            }
                            else -> {
                                bodyText = "Message of type: $typeStr"
                                messageType = WhatsAppMessageType.TEXT
                            }
                        }

                        if (fromPhone.isNotBlank()) {
                            result.add(
                                WhatsAppInboundMessage(
                                    messageId = messageId,
                                    fromPhoneNumber = fromPhone,
                                    senderName = senderName,
                                    text = bodyText,
                                    messageType = messageType,
                                    timestampMillis = timestampMillis,
                                    rawPayload = rawJson,
                                    mediaUrl = mediaUrl
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Safe JSON parse failure handling
        }
        return result
    }

    /**
     * Parses delivery status update receipts from Meta WhatsApp Webhooks
     * (e.g. status: sent, delivered, read, failed).
     */
    fun parseDeliveryReceipts(rawJson: String): List<WhatsAppDeliveryReceipt> {
        val receipts = mutableListOf<WhatsAppDeliveryReceipt>()
        try {
            val root = JSONObject(rawJson)
            val entryArr = root.optJSONArray("entry") ?: return emptyList()

            for (i in 0 until entryArr.length()) {
                val entryObj = entryArr.getJSONObject(i)
                val changesArr = entryObj.optJSONArray("changes") ?: continue

                for (j in 0 until changesArr.length()) {
                    val changeObj = changesArr.getJSONObject(j)
                    val valueObj = changeObj.optJSONObject("value") ?: continue
                    val statusesArr = valueObj.optJSONArray("statuses") ?: continue

                    for (s in 0 until statusesArr.length()) {
                        val statusObj = statusesArr.getJSONObject(s)
                        val messageId = statusObj.optString("id", "")
                        val recipientId = statusObj.optString("recipient_id", "")
                        val statusStr = statusObj.optString("status", "").lowercase()
                        val timestampSeconds = statusObj.optLong("timestamp", System.currentTimeMillis() / 1000L)

                        val parsedStatus = when (statusStr) {
                            "sent" -> WhatsAppDeliveryStatus.SENT
                            "delivered" -> WhatsAppDeliveryStatus.DELIVERED
                            "read" -> WhatsAppDeliveryStatus.READ
                            "failed" -> WhatsAppDeliveryStatus.FAILED
                            else -> WhatsAppDeliveryStatus.PROCESSING
                        }

                        if (messageId.isNotBlank()) {
                            receipts.add(
                                WhatsAppDeliveryReceipt(
                                    messageId = messageId,
                                    recipientPhoneNumber = recipientId,
                                    status = parsedStatus,
                                    timestampSeconds = timestampSeconds,
                                    rawPayload = statusObj.toString()
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Handled gracefully
        }
        return receipts
    }
}
