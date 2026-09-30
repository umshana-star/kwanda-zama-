package com.example.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Operational state for an autonomous WhatsApp agent.
 */
enum class AgentOperationalStatus(
    val label: String,
    val subtitle: String,
    val colorHex: Long
) {
    ACTIVE_ONLINE("Online & Serving", "Autonomous auto-reply active", 0xFF00E676),
    BUSY_HANDLING("In Triage", "Synthesizing conversational reply", 0xFF00E5FF),
    STANDBY_IDLE("Standby Mode", "Awaiting incoming customer webhook", 0xFFFFD600),
    OFFLINE_PAUSED("Paused by Owner", "Manual intervention required", 0xFF9E9E9E),
    ESCALATION_ALERT("Handoff Alert", "Escalated to salon owner", 0xFFFF1744)
}

/**
 * Specialized business roles for salon WhatsApp autonomous agents.
 */
enum class AgentSpecialization(
    val displayName: String,
    val description: String,
    val avatarEmoji: String
) {
    SALON_CONCIERGE(
        "Salon Concierge & Slots",
        "Greets clients, explains hairstyles, checks availability, and reserves booking slots.",
        "💇‍♀️"
    ),
    APPOINTMENT_SCHEDULER(
        "Slot & Calendar Sync",
        "Checks real-time calendar availability, holds tentative slots, and auto-commits appointments.",
        "📅"
    ),
    PRICING_QUOTE_SPECIALIST(
        "Pricing & Quotations",
        "Provides custom estimates based on hair length, density, human hair fiber, and wash add-ons.",
        "💎"
    ),
    AFTERCARE_SUPPORT(
        "Aftercare & Maintenance",
        "Follows up post-treatment with maintenance tips (silk bonnets, braid sprays) and retention checks.",
        "✨"
    ),
    VIP_TRIAGE_ESCALATION(
        "VIP & Dispute Desk",
        "Detects frustrated clients or high-ticket VIP bookings and executes warm owner handoffs.",
        "🛡️"
    )
}

/**
 * Trigger conditions under which the autonomous agent hands over to human owner.
 */
enum class HandoffTriggerMode(val label: String, val thresholdDescription: String) {
    NEGATIVE_SENTIMENT("Negative Sentiment (<40%)", "Triggers if customer displays frustration"),
    PAYMENT_DISPUTE("Payment & Refund Claims", "Immediate supervisor alert on refund requests"),
    VIP_HIGH_VALUE("High-Value Orders (>R1,500)", "Hands over for executive white-glove booking"),
    EXPLICIT_HUMAN_REQUEST("Client Requests Human", "Transfers when client says 'talk to human'"),
    LOW_CONFIDENCE("Low AI Confidence (<70%)", "Transfers when intent is ambiguous")
}

/**
 * Room Database @Entity representing an autonomous WhatsApp agent.
 */
@Entity(tableName = "whatsapp_agents")
data class WhatsAppAgentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "specialization")
    val specialization: String = AgentSpecialization.SALON_CONCIERGE.name,

    @ColumnInfo(name = "avatar_emoji")
    val avatarEmoji: String = "💇‍♀️",

    @ColumnInfo(name = "phone_line")
    val phoneLine: String = "+27 82 555 0192 [WhatsApp Line 1]",

    @ColumnInfo(name = "webhook_url")
    val webhookUrl: String = "https://api.zama.ai/v1/whatsapp/webhook",

    @ColumnInfo(name = "status")
    val status: String = AgentOperationalStatus.ACTIVE_ONLINE.name,

    @ColumnInfo(name = "is_autonomous_enabled")
    val isAutonomousEnabled: Boolean = true,

    @ColumnInfo(name = "current_active_chats")
    val currentActiveChats: Int = 3,

    @ColumnInfo(name = "max_concurrent_chats")
    val maxConcurrentChats: Int = 20,

    @ColumnInfo(name = "total_chats_today")
    val totalChatsToday: Int = 42,

    @ColumnInfo(name = "avg_response_latency_ms")
    val avgResponseLatencyMs: Long = 850L,

    @ColumnInfo(name = "confidence_threshold")
    val confidenceThreshold: Float = 0.85f,

    @ColumnInfo(name = "simulated_typing_delay_sec")
    val simulatedTypingDelaySec: Float = 1.8f,

    @ColumnInfo(name = "sentiment_score")
    val sentimentScore: Float = 0.94f,

    @ColumnInfo(name = "uptime_percentage")
    val uptimePercentage: Float = 99.8f,

    @ColumnInfo(name = "allow_voice_note_replies")
    val allowVoiceNoteReplies: Boolean = true,

    @ColumnInfo(name = "allow_auto_calendar_sync")
    val allowAutoCalendarSync: Boolean = true,

    @ColumnInfo(name = "supported_languages")
    val supportedLanguages: String = "EN, ZU, XH, AF",

    @ColumnInfo(name = "handoff_trigger")
    val handoffTrigger: String = HandoffTriggerMode.NEGATIVE_SENTIMENT.name,

    @ColumnInfo(name = "system_prompt_directive")
    val systemPromptDirective: String = "Maintain warm, high-vibe salon concierge etiquette. Strictly confirm available slots.",

    @ColumnInfo(name = "last_active_epoch_millis")
    val lastActiveEpochMillis: Long = System.currentTimeMillis()
) {
    val operationalStatus: AgentOperationalStatus
        get() = try {
            AgentOperationalStatus.valueOf(status)
        } catch (_: Exception) {
            AgentOperationalStatus.ACTIVE_ONLINE
        }

    val agentSpecialization: AgentSpecialization
        get() = try {
            AgentSpecialization.valueOf(specialization)
        } catch (_: Exception) {
            AgentSpecialization.SALON_CONCIERGE
        }

    val triggerMode: HandoffTriggerMode
        get() = try {
            HandoffTriggerMode.valueOf(handoffTrigger)
        } catch (_: Exception) {
            HandoffTriggerMode.NEGATIVE_SENTIMENT
        }
}

/**
 * Global configuration settings for the salon WhatsApp autonomous channel.
 */
data class WhatsAppGlobalSettings(
    val masterAutonomousActive: Boolean = true,
    val businessHoursOnly: Boolean = false,
    val businessHoursStart: String = "08:00",
    val businessHoursEnd: String = "20:00",
    val autoSyncGoogleCalendar: Boolean = true,
    val requireOwnerApprovalForRefunds: Boolean = true,
    val typingIndicatorSimulation: Boolean = true,
    val zeroKnowledgeEncryption: Boolean = true,
    val emergencyEscalationPhone: String = "+27 82 000 8410",
    val dailyChatBudgetCap: Int = 500,
    val aiResponseDelayMs: Long = 850L
)
