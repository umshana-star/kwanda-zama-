package com.example.service.whatsapp

import com.example.data.local.ChatMessage
import com.example.data.local.WhatsAppAgentRepository
import com.example.data.local.WhatsAppInteractionEntity
import com.example.service.whatsapp.api.SandboxWhatsAppCommunicationApi
import com.example.service.whatsapp.api.WhatsAppCommunicationApi
import com.example.service.whatsapp.api.WhatsAppDeliveryStatus
import com.example.service.whatsapp.api.WhatsAppInboundMessage
import com.example.service.whatsapp.api.WhatsAppOutboundMessage
import com.example.service.whatsapp.api.WhatsAppSendResult
import com.example.service.whatsapp.intelligence.AgentDecision
import com.example.service.whatsapp.intelligence.AgentIntent
import com.example.service.whatsapp.intelligence.GeminiAgentIntelligence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class WhatsAppAgentServiceStatus {
    object Idle : WhatsAppAgentServiceStatus()
    data class Processing(val customerPhone: String, val stage: String) : WhatsAppAgentServiceStatus()
    data class Replied(val customerPhone: String, val replyText: String) : WhatsAppAgentServiceStatus()
    data class Error(val errorMessage: String) : WhatsAppAgentServiceStatus()
}

/**
 * Core Autonomous WhatsApp Agent Service.
 * Coordinates inbound WhatsApp communication API interactions, runs Gemini AI reasoning,
 * persists the conversation history in the Room database, and dispatches automated replies.
 */
class WhatsAppAgentService(
    private val repository: WhatsAppAgentRepository,
    val communicationApi: WhatsAppCommunicationApi = SandboxWhatsAppCommunicationApi(),
    private val intelligence: GeminiAgentIntelligence = GeminiAgentIntelligence(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    val agentLogRepository: com.example.data.local.AutonomousAgentLogRepository? = null
) {

    companion object {
        const val DEFAULT_AWAY_MESSAGE =
            "Thank you for contacting Zama Luxury Hair Studio. Our team is currently away from the desk. We have received your message and will attend to you as soon as we return. For pricing or booking requests, reply 'BOOK' or 'CATALOG'."
    }

    private val _serviceStatus = MutableStateFlow<WhatsAppAgentServiceStatus>(WhatsAppAgentServiceStatus.Idle)
    val serviceStatus: StateFlow<WhatsAppAgentServiceStatus> = _serviceStatus.asStateFlow()

    private val _isAutoReplyActive = MutableStateFlow(true)
    val isAutoReplyActive: StateFlow<Boolean> = _isAutoReplyActive.asStateFlow()

    private val _isAwayModeActive = MutableStateFlow(false)
    val isAwayModeActive: StateFlow<Boolean> = _isAwayModeActive.asStateFlow()

    private val _awayMessage = MutableStateFlow(DEFAULT_AWAY_MESSAGE)
    val awayMessage: StateFlow<String> = _awayMessage.asStateFlow()

    private val _latestDecision = MutableStateFlow<AgentDecision?>(null)
    val latestDecision: StateFlow<AgentDecision?> = _latestDecision.asStateFlow()

    private val _processedInteractionsCount = MutableStateFlow(0)
    val processedInteractionsCount: StateFlow<Int> = _processedInteractionsCount.asStateFlow()

    private val _escalatedCount = MutableStateFlow(0)
    val escalatedCount: StateFlow<Int> = _escalatedCount.asStateFlow()

    fun setAutoReplyActive(active: Boolean) {
        _isAutoReplyActive.value = active
    }

    fun setAwayMode(active: Boolean, customMessage: String? = null) {
        _isAwayModeActive.value = active
        if (!customMessage.isNullOrBlank()) {
            _awayMessage.value = customMessage.trim()
        }
    }

    fun setAwayMessage(message: String) {
        if (message.isNotBlank()) {
            _awayMessage.value = message.trim()
        }
    }

    /**
     * Entry point to handle any incoming WhatsApp customer interaction (text, voice, quick reply).
     */
    suspend fun handleInboundInteraction(inboundMessage: WhatsAppInboundMessage): AgentDecision {
        val startTime = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        _serviceStatus.value = WhatsAppAgentServiceStatus.Processing(
            customerPhone = inboundMessage.fromPhoneNumber,
            stage = "Marking message as read on WhatsApp..."
        )
        // Mark as READ via communication API
        communicationApi.markAsRead(inboundMessage.messageId)

        // 1. Persist inbound customer message in Room database
        val customerChatMessage = ChatMessage(
            messageId = inboundMessage.messageId,
            content = inboundMessage.text,
            isFromUser = true,
            senderRole = "USER",
            timestamp = formattedTime,
            timestampMillis = inboundMessage.timestampMillis,
            statusTicks = "✓✓",
            isVoiceNote = inboundMessage.messageType == com.example.service.whatsapp.api.WhatsAppMessageType.AUDIO_VOICE_NOTE,
            audioModelUsed = if (inboundMessage.messageType == com.example.service.whatsapp.api.WhatsAppMessageType.AUDIO_VOICE_NOTE) "WhatsApp Voice" else null
        )
        repository.saveChatMessage(customerChatMessage)

        // 2. Synthesize reply (Automated Away Response vs Gemini Agent Intelligence)
        val decision = if (_isAwayModeActive.value) {
            _serviceStatus.value = WhatsAppAgentServiceStatus.Processing(
                customerPhone = inboundMessage.fromPhoneNumber,
                stage = "Generating automated Away response..."
            )
            val awayBody = _awayMessage.value
            val personalizedAwayReply = if (awayBody.contains("{name}")) {
                awayBody.replace("{name}", inboundMessage.senderName)
            } else {
                "Hi ${inboundMessage.senderName}! $awayBody"
            }
            AgentDecision(
                intent = AgentIntent.GENERAL_SALON_QUERY,
                confidence = 1.0f,
                replyText = personalizedAwayReply,
                reasoningTrace = "Mode: AUTOMATED AWAY RESPONSE (Active)\n" +
                        "Status: Reception Away / Salon Closed\n" +
                        "Trigger: Inbound WhatsApp message received while Away Mode is ON\n" +
                        "Recipient: ${inboundMessage.senderName} (${inboundMessage.fromPhoneNumber})",
                suggestedQuickActions = listOf("View Catalog", "Business Hours", "Request Callback"),
                requiresHumanEscalation = false
            )
        } else {
            _serviceStatus.value = WhatsAppAgentServiceStatus.Processing(
                customerPhone = inboundMessage.fromPhoneNumber,
                stage = "Analyzing intent & consulting salon catalog..."
            )
            intelligence.analyzeAndFormulateReply(
                customerMessage = inboundMessage.text,
                senderName = inboundMessage.senderName
            )
        }
        _latestDecision.value = decision

        if (decision.requiresHumanEscalation) {
            _escalatedCount.value = _escalatedCount.value + 1
        }

        // 3. Dispatch outbound response via WhatsApp communication API if auto-reply is enabled
        var deliveryStatus = WhatsAppDeliveryStatus.PROCESSING
        if (_isAutoReplyActive.value) {
            _serviceStatus.value = WhatsAppAgentServiceStatus.Processing(
                customerPhone = inboundMessage.fromPhoneNumber,
                stage = "Transmitting response via WhatsApp API..."
            )
            val outboundMessage = WhatsAppOutboundMessage(
                recipientPhoneNumber = inboundMessage.fromPhoneNumber,
                bodyText = decision.replyText,
                replyToMessageId = inboundMessage.messageId,
                quickActionButtons = decision.suggestedQuickActions
            )
            val sendResult = communicationApi.sendMessage(outboundMessage)
            deliveryStatus = when (sendResult) {
                is WhatsAppSendResult.Success -> sendResult.deliveryStatus
                is WhatsAppSendResult.Error -> WhatsAppDeliveryStatus.FAILED
            }

            // Persist AI agent message in Room chat table
            val aiMessageId = "msg_ai_${UUID.randomUUID().toString().take(10)}"
            val aiChatMessage = ChatMessage(
                messageId = aiMessageId,
                content = decision.replyText,
                isFromUser = false,
                senderRole = "AI",
                timestamp = formattedTime,
                timestampMillis = System.currentTimeMillis(),
                statusTicks = "✓✓",
                aiTrace = decision.reasoningTrace,
                intentTag = if (_isAwayModeActive.value) "AWAY_AUTO_REPLY" else decision.intent.name
            )
            repository.saveChatMessage(aiChatMessage)
        }

        val latency = System.currentTimeMillis() - startTime

        // 4. Log complete interaction audit record in Room database
        val interactionEntity = WhatsAppInteractionEntity(
            interactionId = "int_${UUID.randomUUID().toString().take(12)}",
            threadId = inboundMessage.fromPhoneNumber,
            customerPhone = inboundMessage.fromPhoneNumber,
            customerName = inboundMessage.senderName,
            userMessage = inboundMessage.text,
            agentReply = decision.replyText,
            detectedIntent = if (_isAwayModeActive.value) "Away Auto-Reply" else decision.intent.displayName,
            confidenceScore = decision.confidence,
            latencyMs = latency,
            aiReasoningTrace = decision.reasoningTrace,
            requiresHumanHandoff = decision.requiresHumanEscalation,
            interactionStatus = deliveryStatus.name,
            timestampMillis = System.currentTimeMillis()
        )
        repository.logInteraction(interactionEntity)

        // 5. Persist in AutonomousAgentLogRepository to manage message logs & state
        val currentState = if (_isAwayModeActive.value) "AWAY" else if (!_isAutoReplyActive.value) "PAUSED" else if (decision.requiresHumanEscalation) "ESCALATED" else "AUTONOMOUS"
        agentLogRepository?.logAgentInteraction(
            threadId = inboundMessage.fromPhoneNumber,
            customerPhone = inboundMessage.fromPhoneNumber,
            customerName = inboundMessage.senderName,
            messageText = inboundMessage.text,
            agentReply = decision.replyText,
            detectedIntent = if (_isAwayModeActive.value) "Away Auto-Reply" else decision.intent.displayName,
            confidenceScore = decision.confidence,
            reasoningTrace = decision.reasoningTrace,
            latencyMs = latency,
            deliveryStatus = deliveryStatus.name,
            agentState = currentState,
            isEscalatedToHuman = decision.requiresHumanEscalation,
            quickActions = decision.suggestedQuickActions
        )

        _processedInteractionsCount.value = _processedInteractionsCount.value + 1
        _serviceStatus.value = WhatsAppAgentServiceStatus.Replied(
            customerPhone = inboundMessage.fromPhoneNumber,
            replyText = decision.replyText
        )

        return decision
    }
}
