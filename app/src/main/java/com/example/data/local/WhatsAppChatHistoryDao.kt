package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) for storing and querying the chat history and
 * message threads between the user and the autonomous WhatsApp agent.
 */
@Dao
interface WhatsAppChatHistoryDao {

    // =========================================================================
    // Message Thread Operations (whatsapp_chat_threads)
    // =========================================================================

    /**
     * Inserts or replaces a conversation thread metadata record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThread(thread: WhatsAppChatThreadEntity)

    /**
     * Inserts or replaces a batch of conversation threads.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreads(threads: List<WhatsAppChatThreadEntity>)

    /**
     * Updates an existing conversation thread metadata record.
     */
    @Update
    suspend fun updateThread(thread: WhatsAppChatThreadEntity)

    /**
     * Observes all persisted message threads ordered by pinned state and most recent activity.
     */
    @Query(
        "SELECT * FROM whatsapp_chat_threads ORDER BY is_pinned DESC, last_message_timestamp_millis DESC"
    )
    fun getAllThreads(): Flow<List<WhatsAppChatThreadEntity>>

    /**
     * Retrieves all persisted message threads as a one-shot list for Room JSON export.
     */
    @Query(
        "SELECT * FROM whatsapp_chat_threads ORDER BY is_pinned DESC, last_message_timestamp_millis DESC"
    )
    suspend fun getAllThreadsSync(): List<WhatsAppChatThreadEntity>

    /**
     * Observes message threads assigned to a specific autonomous WhatsApp agent.
     */
    @Query(
        "SELECT * FROM whatsapp_chat_threads WHERE agent_id = :agentId ORDER BY is_pinned DESC, last_message_timestamp_millis DESC"
    )
    fun getThreadsByAgent(agentId: String): Flow<List<WhatsAppChatThreadEntity>>

    /**
     * Observes message threads associated with a specific customer phone number.
     */
    @Query(
        "SELECT * FROM whatsapp_chat_threads WHERE customer_phone = :customerPhone ORDER BY last_message_timestamp_millis DESC"
    )
    fun getThreadsByCustomerPhone(customerPhone: String): Flow<List<WhatsAppChatThreadEntity>>

    /**
     * Observes message threads filtered by status (e.g., ACTIVE, RESOLVED, ESCALATED_TO_OWNER).
     */
    @Query(
        "SELECT * FROM whatsapp_chat_threads WHERE thread_status = :status ORDER BY last_message_timestamp_millis DESC"
    )
    fun getThreadsByStatus(status: String): Flow<List<WhatsAppChatThreadEntity>>

    /**
     * Observes a single message thread by its [threadId].
     */
    @Query("SELECT * FROM whatsapp_chat_threads WHERE thread_id = :threadId LIMIT 1")
    fun observeThreadById(threadId: String): Flow<WhatsAppChatThreadEntity?>

    /**
     * Retrieves a single message thread synchronously inside a coroutine.
     */
    @Query("SELECT * FROM whatsapp_chat_threads WHERE thread_id = :threadId LIMIT 1")
    suspend fun getThreadById(threadId: String): WhatsAppChatThreadEntity?

    /**
     * Observes the total number of persisted message threads.
     */
    @Query("SELECT COUNT(*) FROM whatsapp_chat_threads")
    fun getThreadCount(): Flow<Int>

    /**
     * Returns the total number of persisted message threads.
     */
    @Query("SELECT COUNT(*) FROM whatsapp_chat_threads")
    suspend fun getThreadCountSync(): Int

    /**
     * Marks all messages in the given thread as read by resetting unread_count to 0.
     */
    @Query("UPDATE whatsapp_chat_threads SET unread_count = 0 WHERE thread_id = :threadId")
    suspend fun markThreadAsRead(threadId: String)

    /**
     * Updates the operational status of a message thread.
     */
    @Query(
        "UPDATE whatsapp_chat_threads SET thread_status = :status, last_message_timestamp_millis = :timestampMillis WHERE thread_id = :threadId"
    )
    suspend fun updateThreadStatus(
        threadId: String,
        status: String,
        timestampMillis: Long = System.currentTimeMillis()
    )

    /**
     * Deletes a message thread metadata row by its [threadId].
     */
    @Query("DELETE FROM whatsapp_chat_threads WHERE thread_id = :threadId")
    suspend fun deleteThreadById(threadId: String): Int

    /**
     * Clears all message thread metadata rows.
     */
    @Query("DELETE FROM whatsapp_chat_threads")
    suspend fun clearAllThreads(): Int

    // =========================================================================
    // Individual Thread Message Operations (whatsapp_chat_messages)
    // =========================================================================

    /**
     * Inserts a single chat message between the user and the autonomous WhatsApp agent.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: WhatsAppChatMessageEntity): Long

    /**
     * Inserts a batch of chat messages into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<WhatsAppChatMessageEntity>): List<Long>

    /**
     * Updates an existing chat message record (e.g., delivery ticks or AI trace).
     */
    @Update
    suspend fun updateMessage(message: WhatsAppChatMessageEntity)

    /**
     * Deletes a specific chat message record.
     */
    @Delete
    suspend fun deleteMessage(message: WhatsAppChatMessageEntity)

    /**
     * Observes the chronological chat history for a specific [threadId] (oldest to newest).
     */
    @Query(
        "SELECT * FROM whatsapp_chat_messages WHERE thread_id = :threadId ORDER BY timestamp_millis ASC, id ASC"
    )
    fun getMessagesForThread(threadId: String): Flow<List<WhatsAppChatMessageEntity>>

    /**
     * Observes the chat history for a specific [threadId] in reverse chronological order.
     */
    @Query(
        "SELECT * FROM whatsapp_chat_messages WHERE thread_id = :threadId ORDER BY timestamp_millis DESC, id DESC"
    )
    fun getMessagesForThreadDescending(threadId: String): Flow<List<WhatsAppChatMessageEntity>>

    /**
     * Retrieves all messages for a specific [threadId] as a one-shot list for context building.
     */
    @Query(
        "SELECT * FROM whatsapp_chat_messages WHERE thread_id = :threadId ORDER BY timestamp_millis ASC, id ASC"
    )
    suspend fun getMessagesForThreadSync(threadId: String): List<WhatsAppChatMessageEntity>

    /**
     * Observes all chat messages handled by a specific autonomous WhatsApp agent.
     */
    @Query(
        "SELECT * FROM whatsapp_chat_messages WHERE agent_id = :agentId ORDER BY timestamp_millis ASC, id ASC"
    )
    fun getMessagesByAgent(agentId: String): Flow<List<WhatsAppChatMessageEntity>>

    /**
     * Observes the entire chat history across all threads ordered chronologically.
     */
    @Query("SELECT * FROM whatsapp_chat_messages ORDER BY timestamp_millis ASC, id ASC")
    fun getAllMessages(): Flow<List<WhatsAppChatMessageEntity>>

    /**
     * Retrieves the entire chat history across all threads as a one-shot list for Room JSON export.
     */
    @Query("SELECT * FROM whatsapp_chat_messages ORDER BY timestamp_millis ASC, id ASC")
    suspend fun getAllMessagesSync(): List<WhatsAppChatMessageEntity>

    /**
     * Retrieves the latest message in a specific [threadId].
     */
    @Query(
        "SELECT * FROM whatsapp_chat_messages WHERE thread_id = :threadId ORDER BY timestamp_millis DESC, id DESC LIMIT 1"
    )
    suspend fun getLatestMessageForThread(threadId: String): WhatsAppChatMessageEntity?

    /**
     * Searches chat history across all threads by message content, customer name, agent name,
     * customer phone, or AI reasoning trace.
     */
    @Query(
        """
        SELECT * FROM whatsapp_chat_messages 
        WHERE content LIKE '%' || :query || '%' 
           OR customer_name LIKE '%' || :query || '%' 
           OR customer_phone LIKE '%' || :query || '%'
           OR agent_name LIKE '%' || :query || '%'
           OR ai_reasoning_trace LIKE '%' || :query || '%'
           OR detected_intent LIKE '%' || :query || '%'
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchMessages(query: String): Flow<List<WhatsAppChatMessageEntity>>

    /**
     * Searches chat history filtered by sender role (user vs WhatsApp agent) and keyword.
     */
    @Query(
        """
        SELECT * FROM whatsapp_chat_messages 
        WHERE is_from_user = :isFromUser 
          AND (content LIKE '%' || :query || '%' OR customer_name LIKE '%' || :query || '%' OR agent_name LIKE '%' || :query || '%' OR ai_reasoning_trace LIKE '%' || :query || '%')
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchMessagesBySender(query: String, isFromUser: Boolean): Flow<List<WhatsAppChatMessageEntity>>

    /**
     * Searches persisted conversation threads by thread metadata or matching message content.
     */
    @Query(
        """
        SELECT * FROM whatsapp_chat_threads 
        WHERE customer_name LIKE '%' || :query || '%' 
           OR customer_phone LIKE '%' || :query || '%' 
           OR agent_name LIKE '%' || :query || '%' 
           OR topic_summary LIKE '%' || :query || '%' 
           OR last_message_preview LIKE '%' || :query || '%'
           OR thread_id IN (
               SELECT DISTINCT thread_id FROM whatsapp_chat_messages 
               WHERE content LIKE '%' || :query || '%'
           )
        ORDER BY is_pinned DESC, last_message_timestamp_millis DESC
        """
    )
    fun searchThreads(query: String): Flow<List<WhatsAppChatThreadEntity>>

    /**
     * Searches chat history within a single [threadId] by message content.
     */
    @Query(
        """
        SELECT * FROM whatsapp_chat_messages 
        WHERE thread_id = :threadId AND content LIKE '%' || :query || '%'
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchMessagesInThread(threadId: String, query: String): Flow<List<WhatsAppChatMessageEntity>>

    /**
     * Observes the message count for a specific [threadId].
     */
    @Query("SELECT COUNT(*) FROM whatsapp_chat_messages WHERE thread_id = :threadId")
    fun getMessageCountForThread(threadId: String): Flow<Int>

    /**
     * Returns the message count for a specific [threadId].
     */
    @Query("SELECT COUNT(*) FROM whatsapp_chat_messages WHERE thread_id = :threadId")
    suspend fun getMessageCountForThreadSync(threadId: String): Int

    /**
     * Deletes all messages belonging to a specific [threadId].
     */
    @Query("DELETE FROM whatsapp_chat_messages WHERE thread_id = :threadId")
    suspend fun deleteMessagesForThread(threadId: String): Int

    /**
     * Clears all stored chat messages across all threads.
     */
    @Query("DELETE FROM whatsapp_chat_messages")
    suspend fun clearAllMessages(): Int

    // =========================================================================
    // Relational & Transactional Thread + Message Operations
    // =========================================================================

    /**
     * Observes a single [WhatsAppThreadWithMessages] (thread metadata + full message history).
     */
    @Transaction
    @Query("SELECT * FROM whatsapp_chat_threads WHERE thread_id = :threadId LIMIT 1")
    fun observeThreadWithMessages(threadId: String): Flow<WhatsAppThreadWithMessages?>

    /**
     * Retrieves a single [WhatsAppThreadWithMessages] synchronously inside a coroutine.
     */
    @Transaction
    @Query("SELECT * FROM whatsapp_chat_threads WHERE thread_id = :threadId LIMIT 1")
    suspend fun getThreadWithMessages(threadId: String): WhatsAppThreadWithMessages?

    /**
     * Observes all message threads together with their complete message lists.
     */
    @Transaction
    @Query(
        "SELECT * FROM whatsapp_chat_threads ORDER BY is_pinned DESC, last_message_timestamp_millis DESC"
    )
    fun observeAllThreadsWithMessages(): Flow<List<WhatsAppThreadWithMessages>>

    /**
     * Retrieves all message threads together with their complete message lists as a one-shot list for JSON backup.
     */
    @Transaction
    @Query(
        "SELECT * FROM whatsapp_chat_threads ORDER BY is_pinned DESC, last_message_timestamp_millis DESC"
    )
    suspend fun getAllThreadsWithMessagesSync(): List<WhatsAppThreadWithMessages>

    /**
     * Observes all message threads and their messages for a specific [agentId].
     */
    @Transaction
    @Query(
        "SELECT * FROM whatsapp_chat_threads WHERE agent_id = :agentId ORDER BY is_pinned DESC, last_message_timestamp_millis DESC"
    )
    fun observeAgentThreadsWithMessages(agentId: String): Flow<List<WhatsAppThreadWithMessages>>

    /**
     * Atomically appends a message (from the user or autonomous WhatsApp agent) to a thread,
     * creating the [WhatsAppChatThreadEntity] if it does not yet exist and updating its preview,
     * message count, unread badge, and slot reservation metadata.
     */
    @Transaction
    suspend fun appendMessageToThread(
        message: WhatsAppChatMessageEntity,
        topicSummary: String? = null
    ): Long {
        val insertedRowId = insertMessage(message)
        val existingThread = getThreadById(message.threadId)
        val updatedCount = getMessageCountForThreadSync(message.threadId)
        val newUnreadCount = if (message.isFromUser) {
            (existingThread?.unreadCount ?: 0) + 1
        } else {
            0
        }

        val updatedThread = WhatsAppChatThreadEntity(
            threadId = message.threadId,
            agentId = message.agentId,
            agentName = message.agentName,
            customerPhone = message.customerPhone,
            customerName = message.customerName,
            topicSummary = topicSummary
                ?: existingThread?.topicSummary
                ?: message.detectedIntent
                ?: "Salon Inquiry & Booking",
            lastMessagePreview = message.content,
            lastMessageSender = message.senderRole,
            lastMessageTimestampMillis = message.timestampMillis,
            unreadCount = newUnreadCount,
            messageCount = updatedCount,
            threadStatus = existingThread?.threadStatus ?: "ACTIVE",
            reservedSlotId = message.reservedSlotId ?: existingThread?.reservedSlotId,
            isPinned = existingThread?.isPinned ?: false,
            createdAtMillis = existingThread?.createdAtMillis ?: message.timestampMillis
        )
        upsertThread(updatedThread)
        return insertedRowId
    }

    /**
     * Atomically deletes a thread and all of its associated chat messages.
     */
    @Transaction
    suspend fun deleteThreadAndMessages(threadId: String) {
        deleteMessagesForThread(threadId)
        deleteThreadById(threadId)
    }
}

typealias WhatsAppChatDao = WhatsAppChatHistoryDao
typealias ChatThreadDao = WhatsAppChatHistoryDao
typealias MessageThreadDao = WhatsAppChatHistoryDao
