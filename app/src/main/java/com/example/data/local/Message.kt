package com.example.data.local

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Core Room Database Entity representing an individual message within a conversation.
 * Preserved for R8/ProGuard shrinking and minification.
 */
@Keep
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["conversationId"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestampMillis"])
    ]
)
data class Message(
    @PrimaryKey
    @ColumnInfo(name = "messageId")
    val messageId: String,

    @ColumnInfo(name = "conversationId")
    val conversationId: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "sender")
    val sender: String, // "USER" or "AI"

    @ColumnInfo(name = "isFromUser")
    val isFromUser: Boolean,

    @ColumnInfo(name = "timestampMillis")
    val timestampMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "timestampDate")
    val timestampDate: Date = Date(timestampMillis),

    @ColumnInfo(name = "timestampFormatted")
    val timestampFormatted: String = "",

    @ColumnInfo(name = "status")
    val status: String = "DELIVERED", // "PENDING", "SENT", "DELIVERED", "READ", "FAILED"

    @ColumnInfo(name = "isVoiceNote")
    val isVoiceNote: Boolean = false,

    @ColumnInfo(name = "mediaUrl")
    val mediaUrl: String? = null,

    @ColumnInfo(name = "aiReasoningTrace")
    val aiReasoningTrace: String? = null,

    @ColumnInfo(name = "intentTag")
    val intentTag: String? = null,

    @ColumnInfo(name = "quickRepliesJson")
    val quickRepliesJson: String? = null
)
