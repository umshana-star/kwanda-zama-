package com.example.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Categorization of events and messages emitted by autonomous WhatsApp agents.
 */
enum class WhatsAppAgentEventType(
    val label: String,
    val iconEmoji: String,
    val colorHex: Long,
    val isMessage: Boolean
) {
    MESSAGE_INBOUND("Customer Inbound", "💬", 0xFF00E5FF, true),
    MESSAGE_OUTBOUND("Autonomous Reply", "🤖", 0xFF00E676, true),
    VOICE_NOTE_TRANSCRIBED("Voice Note Transcribed", "🎙️", 0xFF81D4FA, true),
    STATUS_CHANGED("Status Transition", "🔄", 0xFFFFD600, false),
    WEBHOOK_RECEIVED("Webhook Ingested", "⚡", 0xFF80D8FF, false),
    SLOT_RESERVED("Slot Held on Calendar", "📅", 0xFFB388FF, false),
    PRICING_QUOTE("Pricing Quote Generated", "💎", 0xFF80CBC4, false),
    HANDOFF_TRIGGERED("Escalation Handoff", "⚠️", 0xFFFF1744, false),
    KILL_SWITCH_TOGGLED("Master Kill-Switch", "🛡️", 0xFFFF9100, false),
    CONFIG_UPDATED("Settings Tuned", "⚙️", 0xFF90CAF9, false)
}

/**
 * Severity level for agent event updates.
 */
enum class WhatsAppEventSeverity(val label: String, val colorHex: Long) {
    INFO("Info", 0xFF00E5FF),
    SUCCESS("Success", 0xFF00E676),
    WARNING("Warning", 0xFFFFD600),
    ALERT("Alert", 0xFFFF1744)
}

/**
 * Filter categories for browsing agent history logs.
 */
enum class EventCategoryFilter(val label: String, val iconEmoji: String) {
    ALL("All Activity", "🌐"),
    MESSAGES_ONLY("Recent Messages", "💬"),
    STATUS_EVENTS_ONLY("Status Updates", "🔄"),
    ESCALATIONS_ONLY("Escalations & Alerts", "⚠️")
}

/**
 * Room Database @Entity representing an event log or message record for an autonomous WhatsApp agent.
 */
@Entity(
    tableName = "whatsapp_agent_events",
    indices = [
        Index(value = ["agent_id"]),
        Index(value = ["event_type"]),
        Index(value = ["timestamp_millis"])
    ]
)
data class WhatsAppAgentEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = "evt_${System.currentTimeMillis()}_${(1000..9999).random()}",

    @ColumnInfo(name = "agent_id")
    val agentId: String,

    @ColumnInfo(name = "agent_name")
    val agentName: String,

    @ColumnInfo(name = "event_type")
    val eventType: String = WhatsAppAgentEventType.MESSAGE_INBOUND.name,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "detail")
    val detail: String,

    @ColumnInfo(name = "customer_phone")
    val customerPhone: String? = null,

    @ColumnInfo(name = "customer_name")
    val customerName: String? = null,

    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "formatted_time")
    val formattedTime: String = "14:00",

    @ColumnInfo(name = "severity")
    val severity: String = WhatsAppEventSeverity.INFO.name,

    @ColumnInfo(name = "latency_ms")
    val latencyMs: Long? = null,

    @ColumnInfo(name = "confidence")
    val confidence: Float? = null,

    @ColumnInfo(name = "ai_trace")
    val aiTrace: String? = null,

    @ColumnInfo(name = "metadata_badge")
    val metadataBadge: String? = null
) {
    val typedEventType: WhatsAppAgentEventType
        get() = try {
            WhatsAppAgentEventType.valueOf(eventType)
        } catch (_: Exception) {
            WhatsAppAgentEventType.STATUS_CHANGED
        }

    val typedSeverity: WhatsAppEventSeverity
        get() = try {
            WhatsAppEventSeverity.valueOf(severity)
        } catch (_: Exception) {
            WhatsAppEventSeverity.INFO
        }
}
