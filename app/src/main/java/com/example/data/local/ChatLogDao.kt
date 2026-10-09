package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for querying, inserting, and managing
 * chat history logs between the user and the autonomous WhatsApp AI employee.
 */
@Dao
interface ChatLogDao {

    /**
     * Observes all chat history logs in chronological order as a reactive Flow.
     */
    @Query("SELECT * FROM chat_logs ORDER BY timestamp_millis ASC, id ASC")
    fun getAllChatLogs(): Flow<List<ChatLogEntity>>

    /**
     * Retrieves all chat history logs as a one-shot list for Room database JSON backup/export.
     */
    @Query("SELECT * FROM chat_logs ORDER BY timestamp_millis ASC, id ASC")
    suspend fun getAllChatLogsSync(): List<ChatLogEntity>

    /**
     * Observes chat logs filtered by conversation session ID.
     */
    @Query("SELECT * FROM chat_logs WHERE session_id = :sessionId ORDER BY timestamp_millis ASC, id ASC")
    fun getChatLogsBySession(sessionId: String): Flow<List<ChatLogEntity>>

    /**
     * Retrieves a single chat log by primary key ID.
     */
    @Query("SELECT * FROM chat_logs WHERE id = :id LIMIT 1")
    suspend fun getChatLogById(id: Long): ChatLogEntity?

    /**
     * Retrieves the latest chat message log.
     */
    @Query("SELECT * FROM chat_logs ORDER BY timestamp_millis DESC, id DESC LIMIT 1")
    suspend fun getLatestChatLog(): ChatLogEntity?

    /**
     * Observes the total number of stored chat messages.
     */
    @Query("SELECT COUNT(*) FROM chat_logs")
    fun getChatLogCount(): Flow<Int>

    /**
     * Searches stored chat history logs by message text keyword, AI trace, or action detail.
     */
    @Query(
        """
        SELECT * FROM chat_logs 
        WHERE text LIKE '%' || :query || '%' 
           OR ai_trace LIKE '%' || :query || '%' 
           OR action_detail LIKE '%' || :query || '%'
           OR sender_role LIKE '%' || :query || '%'
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchChatLogs(query: String): Flow<List<ChatLogEntity>>

    /**
     * Searches stored chat history logs filtered by sender and keyword.
     */
    @Query(
        """
        SELECT * FROM chat_logs 
        WHERE is_from_customer = :isFromCustomer 
          AND (text LIKE '%' || :query || '%' OR ai_trace LIKE '%' || :query || '%' OR action_detail LIKE '%' || :query || '%')
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchChatLogsBySender(query: String, isFromCustomer: Boolean): Flow<List<ChatLogEntity>>

    /**
     * Inserts a new chat log entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatLog(chatLog: ChatLogEntity): Long

    /**
     * Inserts a list of chat log entries (e.g. for batch seeding).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatLogs(chatLogs: List<ChatLogEntity>): List<Long>

    /**
     * Updates an existing chat log (e.g. updating delivery status or thought trace).
     */
    @Update
    suspend fun updateChatLog(chatLog: ChatLogEntity)

    /**
     * Deletes a specific chat log.
     */
    @Delete
    suspend fun deleteChatLog(chatLog: ChatLogEntity)

    /**
     * Clears all chat history for a specific conversation session.
     */
    @Query("DELETE FROM chat_logs WHERE session_id = :sessionId")
    suspend fun clearSessionChatLogs(sessionId: String): Int

    /**
     * Clears all stored chat history logs across all sessions.
     */
    @Query("DELETE FROM chat_logs")
    suspend fun clearAllChatLogs(): Int

    // ==========================================
    // Methods operating directly on ChatMessage @Entity
    // ==========================================

    @Query("SELECT * FROM chat_messages ORDER BY timestamp_millis ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp_millis ASC, id ASC")
    suspend fun getAllChatMessagesSync(): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessages(messages: List<ChatMessage>)

    @Query("SELECT * FROM chat_messages WHERE is_from_user = :isUser ORDER BY timestamp_millis ASC")
    fun getMessagesBySender(isUser: Boolean): Flow<List<ChatMessage>>

    @Query(
        """
        SELECT * FROM chat_messages 
        WHERE content LIKE '%' || :query || '%' 
           OR ai_trace LIKE '%' || :query || '%' 
           OR sender_role LIKE '%' || :query || '%'
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchChatMessages(query: String): Flow<List<ChatMessage>>

    @Query(
        """
        SELECT * FROM chat_messages 
        WHERE is_from_user = :isUser 
          AND (content LIKE '%' || :query || '%' OR ai_trace LIKE '%' || :query || '%' OR sender_role LIKE '%' || :query || '%')
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchChatMessagesBySender(query: String, isUser: Boolean): Flow<List<ChatMessage>>

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllChatMessages(): Int
}
