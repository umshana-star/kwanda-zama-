package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO interface named 'ChatDao' to handle inserting new messages
 * and retrieving the chat history ordered by timestamp.
 */
@Dao
interface ChatDao {

    /**
     * Inserts a new chat message into the database.
     * Returns the generated row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    /**
     * Alias for inserting a new message into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage): Long

    /**
     * Inserts a list of new chat messages into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>): List<Long>

    /**
     * Retrieves the complete chat history ordered chronologically by timestamp (ascending).
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp_millis ASC, id ASC")
    fun getChatHistory(): Flow<List<ChatMessage>>

    /**
     * Retrieves the chat history ordered by timestamp (ascending).
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp_millis ASC, id ASC")
    fun getChatHistoryOrderedByTimestamp(): Flow<List<ChatMessage>>

    /**
     * Retrieves the chat history ordered chronologically by timestamp in descending order.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp_millis DESC, id DESC")
    fun getChatHistoryDescending(): Flow<List<ChatMessage>>

    /**
     * Retrieves the latest message inserted into the chat history.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp_millis DESC, id DESC LIMIT 1")
    suspend fun getLatestMessage(): ChatMessage?

    /**
     * Retrieves all chat messages as a direct List for background scanning.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp_millis ASC")
    suspend fun getAllMessagesList(): List<ChatMessage>

    /**
     * Searches past chat messages with the WhatsApp agent in Room by keyword across content,
     * AI reasoning trace, and sender role, ordered chronologically.
     */
    @Query(
        """
        SELECT * FROM chat_messages 
        WHERE content LIKE '%' || :query || '%' 
           OR ai_trace LIKE '%' || :query || '%' 
           OR sender_role LIKE '%' || :query || '%'
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchChatHistory(query: String): Flow<List<ChatMessage>>

    /**
     * Alias for searching past chat messages in Room by keyword.
     */
    @Query(
        """
        SELECT * FROM chat_messages 
        WHERE content LIKE '%' || :query || '%' 
           OR ai_trace LIKE '%' || :query || '%' 
           OR sender_role LIKE '%' || :query || '%'
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchMessages(query: String): Flow<List<ChatMessage>>

    /**
     * Searches past chat messages in Room filtered by both keyword and sender (user vs WhatsApp agent).
     */
    @Query(
        """
        SELECT * FROM chat_messages 
        WHERE is_from_user = :isFromUser 
          AND (content LIKE '%' || :query || '%' OR ai_trace LIKE '%' || :query || '%')
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchMessagesBySender(query: String, isFromUser: Boolean): Flow<List<ChatMessage>>

    /**
     * Deletes all messages in the chat history.
     */
    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory(): Int

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int

    @Query("SELECT COUNT(*) FROM chat_messages WHERE message_id = :messageId")
    suspend fun countByMessageId(messageId: String): Int
}
