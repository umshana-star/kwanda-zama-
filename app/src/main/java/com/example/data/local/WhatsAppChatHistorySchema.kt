package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.data.remote.WhatsAppConversationTurnDto
import com.example.model.ChatMessage as UiChatMessage

/**
 * Room Entity representing a persistent WhatsApp conversation thread between a user/customer
 * and an autonomous Zama WhatsApp agent.
 */
@Entity(
    tableName = "whatsapp_chat_threads",
    indices = [
        Index(value = ["agent_id"]),
        Index(value = ["customer_phone"]),
        Index(value = ["last_message_timestamp_millis"]),
        Index(value = ["thread_status"])
    ]
)
data class WhatsAppChatThreadEntity(
    @PrimaryKey
    @ColumnInfo(name = "thread_id")
    val threadId: String,

    @ColumnInfo(name = "agent_id")
    val agentId: String,

    @ColumnInfo(name = "agent_name")
    val agentName: String,

    @ColumnInfo(name = "customer_phone")
    val customerPhone: String,

    @ColumnInfo(name = "customer_name")
    val customerName: String,

    @ColumnInfo(name = "topic_summary")
    val topicSummary: String = "Salon Inquiry & Booking",

    @ColumnInfo(name = "last_message_preview")
    val lastMessagePreview: String = "",

    @ColumnInfo(name = "last_message_sender")
    val lastMessageSender: String = "USER", // "USER" or "WHATSAPP_AGENT"

    @ColumnInfo(name = "last_message_timestamp_millis")
    val lastMessageTimestampMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0,

    @ColumnInfo(name = "message_count")
    val messageCount: Int = 0,

    @ColumnInfo(name = "thread_status")
    val threadStatus: String = "ACTIVE", // "ACTIVE", "RESOLVED", "ESCALATED_TO_OWNER", "ARCHIVED"

    @ColumnInfo(name = "reserved_slot_id")
    val reservedSlotId: String? = null,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * Room Entity representing an individual message in a persistent conversation thread
 * between the user/customer and the autonomous WhatsApp agent.
 */
@Entity(
    tableName = "whatsapp_chat_messages",
    indices = [
        Index(value = ["thread_id"]),
        Index(value = ["agent_id"]),
        Index(value = ["timestamp_millis"]),
        Index(value = ["sender_role"])
    ]
)
data class WhatsAppChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "message_id")
    val messageId: String = "wamid.${System.currentTimeMillis()}",

    @ColumnInfo(name = "thread_id")
    val threadId: String = "default_thread",

    @ColumnInfo(name = "agent_id")
    val agentId: String = "agent_thandiwe_01",

    @ColumnInfo(name = "agent_name")
    val agentName: String = "Thandiwe",

    @ColumnInfo(name = "customer_phone")
    val customerPhone: String = "+27 82 000 0000",

    @ColumnInfo(name = "customer_name")
    val customerName: String = "Customer",

    @ColumnInfo(name = "is_from_user")
    val isFromUser: Boolean,

    @ColumnInfo(name = "sender_role")
    val senderRole: String = if (isFromUser) "USER" else "WHATSAPP_AGENT",

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "message_type")
    val messageType: String = "TEXT", // "TEXT", "VOICE_NOTE", "BOOKING_CONFIRMATION", "QUOTE_CARD"

    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "formatted_time")
    val formattedTime: String = "",

    @ColumnInfo(name = "delivery_status")
    val deliveryStatus: String = "✓✓",

    @ColumnInfo(name = "detected_intent")
    val detectedIntent: String? = null,

    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float? = null,

    @ColumnInfo(name = "latency_ms")
    val latencyMs: Long? = null,

    @ColumnInfo(name = "ai_reasoning_trace")
    val aiReasoningTrace: String? = null,

    @ColumnInfo(name = "reserved_slot_id")
    val reservedSlotId: String? = null
) {
    /**
     * Converts this persisted Room message entity into a [WhatsAppConversationTurnDto]
     * for Retrofit + Moshi context exchange with the autonomous WhatsApp agent backend.
     */
    fun toConversationTurnDto(): WhatsAppConversationTurnDto = WhatsAppConversationTurnDto(
        role = if (isFromUser) "customer" else "agent",
        content = content,
        timestampMillis = timestampMillis
    )

    /**
     * Converts this persisted Room message entity into the UI [UiChatMessage] model.
     */
    fun toUiChatMessage(): UiChatMessage = UiChatMessage(
        id = messageId,
        isFromCustomer = isFromUser,
        text = content,
        timestamp = formattedTime,
        statusTicks = deliveryStatus,
        isActionCard = reservedSlotId != null,
        actionDetail = reservedSlotId?.let { "Reserved Slot #$it" },
        isVoiceNote = messageType == "VOICE_NOTE"
    )
}

/**
 * Room relational model joining a [WhatsAppChatThreadEntity] with its complete list of
 * [WhatsAppChatMessageEntity] history records.
 */
data class WhatsAppThreadWithMessages(
    @Embedded
    val thread: WhatsAppChatThreadEntity,

    @Relation(
        parentColumn = "thread_id",
        entityColumn = "thread_id"
    )
    val messages: List<WhatsAppChatMessageEntity>
) {
    /**
     * Returns the thread's messages ordered chronologically (oldest to newest).
     */
    val chronologicalMessages: List<WhatsAppChatMessageEntity>
        get() = messages.sortedWith(
            compareBy<WhatsAppChatMessageEntity> { it.timestampMillis }.thenBy { it.id }
        )
}

typealias ChatThreadEntity = WhatsAppChatThreadEntity
typealias MessageThreadEntity = WhatsAppChatThreadEntity
typealias WhatsAppMessageThreadEntity = WhatsAppChatThreadEntity
typealias ThreadMessageEntity = WhatsAppChatMessageEntity
typealias WhatsAppThreadMessageEntity = WhatsAppChatMessageEntity
typealias ChatThreadWithMessages = WhatsAppThreadWithMessages
typealias MessageThreadWithHistory = WhatsAppThreadWithMessages
