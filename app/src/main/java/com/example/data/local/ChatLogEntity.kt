package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.ChatMessage

/**
 * Room Entity representing a stored chat message log between the user (customer)
 * and the autonomous WhatsApp AI agent.
 */
@Entity(
    tableName = "chat_logs",
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["timestamp_millis"])
    ]
)
data class ChatLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "session_id", defaultValue = "default_session")
    val sessionId: String = "default_session",

    @ColumnInfo(name = "is_from_customer")
    val isFromCustomer: Boolean,

    @ColumnInfo(name = "sender_role")
    val senderRole: String, // "CUSTOMER" or "AI_AGENT"

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "timestamp_formatted")
    val timestampFormatted: String,

    @ColumnInfo(name = "status_ticks")
    val statusTicks: String = "✓✓",

    @ColumnInfo(name = "is_action_card")
    val isActionCard: Boolean = false,

    @ColumnInfo(name = "action_detail")
    val actionDetail: String? = null,

    @ColumnInfo(name = "ai_trace")
    val aiTrace: String? = null
) {
    /**
     * Maps this Room entity to the lightweight UI ChatMessage model.
     */
    fun toChatMessage(): ChatMessage {
        val isVoice = actionDetail?.startsWith("VOICE_INPUT") == true
        val model = if (isVoice) actionDetail?.substringAfter("VOICE_INPUT:", "gemini-3.5-flash") else null
        return ChatMessage(
            id = messageId,
            isFromCustomer = isFromCustomer,
            text = text,
            timestamp = timestampFormatted,
            statusTicks = statusTicks,
            isActionCard = isActionCard,
            actionDetail = actionDetail,
            isVoiceNote = isVoice,
            audioModelUsed = model
        )
    }

    companion object {
        fun fromChatMessage(
            chatMessage: ChatMessage,
            sessionId: String = "default_session",
            aiTrace: String? = null,
            timestampMillis: Long = System.currentTimeMillis()
        ): ChatLogEntity {
            val finalActionDetail = if (chatMessage.isVoiceNote) {
                "VOICE_INPUT:${chatMessage.audioModelUsed ?: "gemini-3.5-flash"}"
            } else {
                chatMessage.actionDetail
            }
            return ChatLogEntity(
                messageId = chatMessage.id,
                sessionId = sessionId,
                isFromCustomer = chatMessage.isFromCustomer,
                senderRole = if (chatMessage.isFromCustomer) "CUSTOMER" else "AI_AGENT",
                text = chatMessage.text,
                timestampMillis = timestampMillis,
                timestampFormatted = chatMessage.timestamp,
                statusTicks = chatMessage.statusTicks,
                isActionCard = chatMessage.isActionCard,
                actionDetail = finalActionDetail,
                aiTrace = aiTrace
            )
        }
    }
}
