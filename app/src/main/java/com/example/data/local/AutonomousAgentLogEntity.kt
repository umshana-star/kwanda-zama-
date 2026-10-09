package com.example.data.local

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Database @Entity representing an autonomous agent message log and its operational state
 * for WhatsApp Business API integration.
 */
@Keep
@Entity(
    tableName = "autonomous_agent_logs",
    indices = [
        Index(value = ["thread_id"]),
        Index(value = ["customer_phone"]),
        Index(value = ["detected_intent"]),
        Index(value = ["agent_state"]),
        Index(value = ["timestamp_millis"])
    ]
)
data class AutonomousAgentLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "log_id")
    val logId: String = "log_${UUID.randomUUID().toString().take(12)}",

    @ColumnInfo(name = "thread_id")
    val threadId: String,

    @ColumnInfo(name = "customer_phone")
    val customerPhone: String,

    @ColumnInfo(name = "customer_name")
    val customerName: String,

    @ColumnInfo(name = "direction")
    val direction: String = "INBOUND_OUTBOUND_PAIR", // "INBOUND", "OUTBOUND", "INBOUND_OUTBOUND_PAIR"

    @ColumnInfo(name = "message_text")
    val messageText: String,

    @ColumnInfo(name = "agent_reply")
    val agentReply: String,

    @ColumnInfo(name = "detected_intent")
    val detectedIntent: String,

    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float = 0.95f,

    @ColumnInfo(name = "reasoning_trace")
    val reasoningTrace: String? = null,

    @ColumnInfo(name = "latency_ms")
    val latencyMs: Long = 0L,

    @ColumnInfo(name = "delivery_status")
    val deliveryStatus: String = "DELIVERED", // "PROCESSING", "SENT", "DELIVERED", "READ", "FAILED"

    @ColumnInfo(name = "agent_state")
    val agentState: String = "AUTONOMOUS", // "AUTONOMOUS", "AWAY", "ESCALATED", "PAUSED", "IDLE"

    @ColumnInfo(name = "is_escalated_to_human")
    val isEscalatedToHuman: Boolean = false,

    @ColumnInfo(name = "escalation_reason")
    val escalationReason: String? = null,

    @ColumnInfo(name = "quick_actions_json")
    val quickActionsJson: String? = null,

    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "formatted_time")
    val formattedTime: String = "12:00"
)
