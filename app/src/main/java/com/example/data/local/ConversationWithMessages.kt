package com.example.data.local

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Relation

/**
 * 1-to-N Room Relationship model joining Conversation with its child Messages.
 * Preserved for R8/ProGuard shrinking and minification.
 */
@Keep
data class ConversationWithMessages(
    @Embedded
    val conversation: Conversation,

    @Relation(
        parentColumn = "conversationId",
        entityColumn = "conversationId"
    )
    val messages: List<Message> = emptyList()
)
