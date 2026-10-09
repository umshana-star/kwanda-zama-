package com.example.data.local

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Conversation and Message entities.
 * Preserved for R8/ProGuard shrinking and minification.
 */
@Keep
@Dao
interface ConversationDao {

    // --- Conversation operations ---
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE conversationId = :id LIMIT 1")
    fun getConversationById(id: String): Flow<Conversation?>

    @Query("SELECT * FROM conversations WHERE customerPhoneNumber = :phone LIMIT 1")
    suspend fun getConversationByPhone(phone: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<Conversation>)

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Delete
    suspend fun deleteConversation(conversation: Conversation)

    @Query("DELETE FROM conversations WHERE conversationId = :id")
    suspend fun deleteConversationById(id: String)

    @Query("UPDATE conversations SET lastMessageText = :lastMessage, lastMessageTimestamp = :timestamp, activeIntent = :activeIntent WHERE conversationId = :id")
    suspend fun updateLastMessage(id: String, lastMessage: String, timestamp: Long, activeIntent: String?)

    // --- Message operations ---
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestampMillis ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE messageId = :id LIMIT 1")
    suspend fun getMessageById(id: String): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Update
    suspend fun updateMessage(message: Message)

    @Query("DELETE FROM messages WHERE messageId = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    // --- Relational Queries ---
    @Transaction
    @Query("SELECT * FROM conversations WHERE conversationId = :conversationId LIMIT 1")
    fun getConversationWithMessages(conversationId: String): Flow<ConversationWithMessages?>

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversationsWithMessages(): Flow<List<ConversationWithMessages>>

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    suspend fun getAllConversationsWithMessagesSync(): List<ConversationWithMessages>

    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    suspend fun getAllConversationsSync(): List<Conversation>

    @Query("SELECT * FROM messages ORDER BY timestampMillis ASC")
    suspend fun getAllMessagesSync(): List<Message>
}
