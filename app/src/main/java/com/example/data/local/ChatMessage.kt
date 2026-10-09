package com.example.data.local

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Model representing a chat message stored in Room and displayed in the UI.
 */
@Keep
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
    val messageId: String = java.util.UUID.randomUUID().toString(),

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
    val aiTrace: String? = null,

    @ColumnInfo(name = "intent_tag")
    val intentTag: String? = null
) {
    fun toUiModel(): ChatMessage = this
}
