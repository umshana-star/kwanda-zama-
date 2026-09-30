package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Database @Entity representing a chat message for Zama AI chat history.
 * Stores message content, sender information (user vs AI), and timestamp.
 */
@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["timestamp_millis"]),
        Index(value = ["is_from_user"])
    ]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "message_id")
    val messageId: String = System.currentTimeMillis().toString(),

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "is_from_user")
    val isFromUser: Boolean,

    @ColumnInfo(name = "sender_role")
    val senderRole: String = if (isFromUser) "USER" else "AI",

    @ColumnInfo(name = "timestamp")
    val timestamp: String,

    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "status_ticks")
    val statusTicks: String = "✓✓",

    @ColumnInfo(name = "is_voice_note")
    val isVoiceNote: Boolean = false,

    @ColumnInfo(name = "audio_model_used")
    val audioModelUsed: String? = null,

    @ColumnInfo(name = "ai_trace")
    val aiTrace: String? = null
) {
    /**
     * Convenience property returning true if sent by the user/customer.
     */
    val isUser: Boolean
        get() = isFromUser

    /**
     * Convenience property returning true if sent by the autonomous Zama AI agent.
     */
    val isAi: Boolean
        get() = !isFromUser

    /**
     * Returns sender classification as "USER" or "AI".
     */
    val senderType: String
        get() = if (isFromUser) "USER" else "AI"

    /**
     * Convert Room entity to UI domain model.
     */
    fun toUiModel(): com.example.model.ChatMessage = com.example.model.ChatMessage(
        id = messageId,
        isFromCustomer = isFromUser,
        text = content,
        timestamp = timestamp,
        statusTicks = statusTicks,
        isVoiceNote = isVoiceNote,
        audioModelUsed = audioModelUsed
    )

    companion object {
        fun fromUiModel(
            uiModel: com.example.model.ChatMessage,
            aiTrace: String? = null
        ): ChatMessage = ChatMessage(
            messageId = uiModel.id,
            content = uiModel.text,
            isFromUser = uiModel.isFromCustomer,
            senderRole = if (uiModel.isFromCustomer) "USER" else "AI",
            timestamp = uiModel.timestamp,
            statusTicks = uiModel.statusTicks,
            isVoiceNote = uiModel.isVoiceNote,
            audioModelUsed = uiModel.audioModelUsed,
            aiTrace = aiTrace
        )
    }
}
