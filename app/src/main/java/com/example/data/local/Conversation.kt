package com.example.data.local

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Core Room Database Entity representing a customer WhatsApp conversation session.
 * Preserved for R8/ProGuard shrinking and minification.
 */
@Keep
@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["customerPhoneNumber"], unique = true),
        Index(value = ["lastMessageTimestamp"])
    ]
)
data class Conversation(
    @PrimaryKey
    @ColumnInfo(name = "conversationId")
    val conversationId: String,

    @ColumnInfo(name = "customerPhoneNumber")
    val customerPhoneNumber: String,

    @ColumnInfo(name = "customerName")
    val customerName: String,

    @ColumnInfo(name = "lastMessageText")
    val lastMessageText: String = "",

    @ColumnInfo(name = "lastMessageTimestamp")
    val lastMessageTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "lastMessageDate")
    val lastMessageDate: Date = Date(lastMessageTimestamp),

    @ColumnInfo(name = "unreadCount")
    val unreadCount: Int = 0,

    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "isPinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "agentAssigned")
    val agentAssigned: String = "Zama AI Concierge",

    @ColumnInfo(name = "activeIntent")
    val activeIntent: String? = null
)
