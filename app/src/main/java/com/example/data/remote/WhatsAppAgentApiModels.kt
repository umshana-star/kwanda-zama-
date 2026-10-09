package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a single conversational turn exchanged between a WhatsApp customer and an autonomous agent.
 * Serialized and deserialized via Moshi (`KotlinJsonAdapterFactory`).
 */
@JsonClass(generateAdapter = false)
data class WhatsAppConversationTurnDto(
    @Json(name = "role")
    val role: String, // "customer" or "agent"
    @Json(name = "content")
    val content: String,
    @Json(name = "timestamp_millis")
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Request payload sent over Retrofit + Moshi to the autonomous WhatsApp agent backend
 * when a customer sends a text inquiry, voice note transcript, or booking request.
 */
@JsonClass(generateAdapter = false)
data class WhatsAppAgentMessageRequest(
    @Json(name = "agent_id")
    val agentId: String,
    @Json(name = "customer_phone")
    val customerPhone: String,
    @Json(name = "customer_name")
    val customerName: String,
    @Json(name = "message_text")
    val messageText: String,
    @Json(name = "message_type")
    val messageType: String = "TEXT",
    @Json(name = "language_code")
    val languageCode: String = "en",
    @Json(name = "conversation_history")
    val conversationHistory: List<WhatsAppConversationTurnDto> = emptyList(),
    @Json(name = "timestamp_millis")
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Structured response returned by the autonomous WhatsApp agent via Retrofit + Moshi
 * after processing a customer message or webhook payload.
 */
@JsonClass(generateAdapter = false)
data class WhatsAppAgentMessageResponse(
    @Json(name = "message_id")
    val messageId: String,
    @Json(name = "agent_id")
    val agentId: String,
    @Json(name = "agent_name")
    val agentName: String,
    @Json(name = "reply_text")
    val replyText: String,
    @Json(name = "detected_intent")
    val detectedIntent: String,
    @Json(name = "confidence_score")
    val confidenceScore: Float,
    @Json(name = "latency_ms")
    val latencyMs: Long,
    @Json(name = "reasoning_trace")
    val reasoningTrace: String,
    @Json(name = "requires_human_handoff")
    val requiresHumanHandoff: Boolean = false,
    @Json(name = "reserved_slot_id")
    val reservedSlotId: String? = null,
    @Json(name = "status")
    val status: String = "DISPATCHED",
    @Json(name = "timestamp_millis")
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Real-time status telemetry DTO for an autonomous WhatsApp agent node.
 */
@JsonClass(generateAdapter = false)
data class WhatsAppAgentStatusDto(
    @Json(name = "agent_id")
    val agentId: String,
    @Json(name = "agent_name")
    val agentName: String,
    @Json(name = "connection_status")
    val connectionStatus: String = "CONNECTED",
    @Json(name = "operational_status")
    val operationalStatus: String = "ACTIVE_ONLINE",
    @Json(name = "is_autonomous_enabled")
    val isAutonomousEnabled: Boolean = true,
    @Json(name = "active_threads_count")
    val activeThreadsCount: Int = 4,
    @Json(name = "max_concurrency")
    val maxConcurrency: Int = 20,
    @Json(name = "avg_latency_ms")
    val avgLatencyMs: Long = 640L,
    @Json(name = "uptime_percentage")
    val uptimePercentage: Float = 99.9f,
    @Json(name = "webhook_url")
    val webhookUrl: String = "https://api.zama.ai/v1/whatsapp/webhook",
    @Json(name = "last_heartbeat_millis")
    val lastHeartbeatMillis: Long = System.currentTimeMillis()
)

/**
 * Request DTO for updating an autonomous WhatsApp agent's operational parameters on the backend.
 */
@JsonClass(generateAdapter = false)
data class WhatsAppAgentConfigUpdateRequest(
    @Json(name = "agent_id")
    val agentId: String,
    @Json(name = "is_autonomous_enabled")
    val isAutonomousEnabled: Boolean = true,
    @Json(name = "operational_status")
    val operationalStatus: String = "ACTIVE_ONLINE",
    @Json(name = "confidence_threshold")
    val confidenceThreshold: Float = 0.88f,
    @Json(name = "ai_response_delay_ms")
    val aiResponseDelayMs: Long = 850L,
    @Json(name = "allow_voice_note_replies")
    val allowVoiceNoteReplies: Boolean = true,
    @Json(name = "allow_auto_calendar_sync")
    val allowAutoCalendarSync: Boolean = true,
    @Json(name = "system_prompt_directive")
    val systemPromptDirective: String = ""
)

/**
 * DTO representing an inbound or queued WhatsApp Cloud API webhook event.
 */
@JsonClass(generateAdapter = false)
data class WhatsAppWebhookEventDto(
    @Json(name = "event_id")
    val eventId: String,
    @Json(name = "agent_id")
    val agentId: String,
    @Json(name = "customer_phone")
    val customerPhone: String,
    @Json(name = "customer_name")
    val customerName: String,
    @Json(name = "incoming_text")
    val incomingText: String,
    @Json(name = "agent_drafted_reply")
    val agentDraftedReply: String,
    @Json(name = "intent_tag")
    val intentTag: String,
    @Json(name = "confidence_percent")
    val confidencePercent: Int = 98,
    @Json(name = "timestamp_millis")
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Response DTO returned when synchronizing incoming WhatsApp agent webhook events.
 */
@JsonClass(generateAdapter = false)
data class WhatsAppWebhookSyncResponse(
    @Json(name = "connection_status")
    val connectionStatus: String,
    @Json(name = "active_agents_count")
    val activeAgentsCount: Int,
    @Json(name = "incoming_messages")
    val incomingMessages: List<WhatsAppWebhookEventDto>,
    @Json(name = "synced_at_millis")
    val syncedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Moshi DTOs for Gemini REST API (`v1beta/models/gemini-2.5-flash:generateContent`)
 * used by the autonomous WhatsApp agent backend bridge.
 */
@JsonClass(generateAdapter = false)
data class GeminiPartDto(
    @Json(name = "text")
    val text: String? = null
)

@JsonClass(generateAdapter = false)
data class GeminiContentDto(
    @Json(name = "role")
    val role: String? = null,
    @Json(name = "parts")
    val parts: List<GeminiPartDto>
)

@JsonClass(generateAdapter = false)
data class GeminiGenerationConfigDto(
    @Json(name = "temperature")
    val temperature: Double = 0.7,
    @Json(name = "maxOutputTokens")
    val maxOutputTokens: Int = 500
)

@JsonClass(generateAdapter = false)
data class GeminiGenerateContentRequestDto(
    @Json(name = "systemInstruction")
    val systemInstruction: GeminiContentDto? = null,
    @Json(name = "contents")
    val contents: List<GeminiContentDto>,
    @Json(name = "generationConfig")
    val generationConfig: GeminiGenerationConfigDto? = GeminiGenerationConfigDto()
)

@JsonClass(generateAdapter = false)
data class GeminiCandidateDto(
    @Json(name = "content")
    val content: GeminiContentDto? = null,
    @Json(name = "finishReason")
    val finishReason: String? = null
)

@JsonClass(generateAdapter = false)
data class GeminiGenerateContentResponseDto(
    @Json(name = "candidates")
    val candidates: List<GeminiCandidateDto>? = null
)

/**
 * Request DTO sent periodically by the autonomous WhatsApp agent background heartbeat coroutine
 * to verify connectivity and keep the cloud bridge session alive.
 */
@JsonClass(generateAdapter = false)
data class WhatsAppHeartbeatRequest(
    @Json(name = "agent_id")
    val agentId: String = "agent_thandiwe_01",
    @Json(name = "session_id")
    val sessionId: String,
    @Json(name = "sequence_number")
    val sequenceNumber: Long,
    @Json(name = "client_timestamp_millis")
    val clientTimestampMillis: Long = System.currentTimeMillis(),
    @Json(name = "active_threads_count")
    val activeThreadsCount: Int = 4
)

/**
 * Response DTO returned by the WhatsApp Cloud Bridge upon verifying a periodic heartbeat signal.
 */
@JsonClass(generateAdapter = false)
data class WhatsAppHeartbeatResponse(
    @Json(name = "heartbeat_id")
    val heartbeatId: String,
    @Json(name = "agent_id")
    val agentId: String,
    @Json(name = "session_id")
    val sessionId: String,
    @Json(name = "sequence_number")
    val sequenceNumber: Long,
    @Json(name = "connection_status")
    val connectionStatus: String = "CONNECTED",
    @Json(name = "session_active")
    val sessionActive: Boolean = true,
    @Json(name = "round_trip_latency_ms")
    val roundTripLatencyMs: Long = 112L,
    @Json(name = "active_threads_count")
    val activeThreadsCount: Int = 4,
    @Json(name = "uptime_percentage")
    val uptimePercentage: Float = 99.98f,
    @Json(name = "server_timestamp_millis")
    val serverTimestampMillis: Long = System.currentTimeMillis()
)

/**
 * Reactive state representing the periodic background heartbeat and session connectivity health.
 */
data class WhatsAppHeartbeatState(
    val isRunning: Boolean = false,
    val agentId: String = "agent_thandiwe_01",
    val sessionId: String = "wa_sess_alpha_01",
    val sequenceNumber: Long = 0L,
    val lastHeartbeatMillis: Long = System.currentTimeMillis(),
    val roundTripLatencyMs: Long = 112L,
    val sessionActive: Boolean = true,
    val consecutiveFailures: Int = 0,
    val intervalMs: Long = 10_000L
)

/**
 * Typed result wrapper for autonomous WhatsApp agent backend operations.
 */
sealed class WhatsAppAgentExchangeResult<out T> {
    data class Success<T>(
        val data: T,
        val fromLiveEndpoint: Boolean = true
    ) : WhatsAppAgentExchangeResult<T>()

    data class Error(
        val errorCode: Int = -1,
        val message: String,
        val cause: Throwable? = null
    ) : WhatsAppAgentExchangeResult<Nothing>()
}
