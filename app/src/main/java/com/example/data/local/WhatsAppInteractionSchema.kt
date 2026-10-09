package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Room Entity storing the complete history of interactions between the user/customer
 * and the autonomous WhatsApp AI agent for later retrieval, audit, and multi-turn context.
 */
@Entity(
    tableName = "whatsapp_interactions",
    indices = [
        Index(value = ["thread_id"]),
        Index(value = ["session_id"]),
        Index(value = ["agent_id"]),
        Index(value = ["customer_phone"]),
        Index(value = ["detected_intent"]),
        Index(value = ["timestamp_millis"])
    ]
)
data class WhatsAppInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "interaction_id")
    val interactionId: String = "ix_${System.currentTimeMillis()}",

    @ColumnInfo(name = "thread_id")
    val threadId: String = "default_thread",

    @ColumnInfo(name = "session_id")
    val sessionId: String = "default_session",

    @ColumnInfo(name = "agent_id")
    val agentId: String = "agent_thandiwe_01",

    @ColumnInfo(name = "agent_name")
    val agentName: String = "Thandiwe",

    @ColumnInfo(name = "customer_phone")
    val customerPhone: String = "+27 82 419 8820",

    @ColumnInfo(name = "customer_name")
    val customerName: String = "Customer",

    @ColumnInfo(name = "user_message")
    val userMessage: String,

    @ColumnInfo(name = "agent_reply")
    val agentReply: String,

    @ColumnInfo(name = "detected_intent")
    val detectedIntent: String = "GENERAL_INQUIRY",

    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float = 0.96f,

    @ColumnInfo(name = "latency_ms")
    val latencyMs: Long = 620L,

    @ColumnInfo(name = "model_used")
    val modelUsed: String = "gemini-2.5-flash",

    @ColumnInfo(name = "ai_reasoning_trace")
    val aiReasoningTrace: String? = null,

    @ColumnInfo(name = "reserved_slot_id")
    val reservedSlotId: String? = null,

    @ColumnInfo(name = "requires_human_handoff")
    val requiresHumanHandoff: Boolean = false,

    @ColumnInfo(name = "interaction_status")
    val interactionStatus: String = "COMPLETED", // "COMPLETED", "ESCALATED", "FALLBACK"

    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "formatted_time")
    val formattedTime: String = "14:00"
) {
    val senderPhoneNumber: String get() = customerPhone
    val customerDisplayName: String get() = customerName
    val resolvedIntent: String get() = detectedIntent
    val agentConfidence: Float get() = confidenceScore
    val isEscalatedToHuman: Boolean get() = requiresHumanHandoff
    val replyText: String get() = agentReply
    val deliveryStatus: String get() = interactionStatus

    /**
     * Converts this interaction into a chronological pair of [WhatsAppChatMessageEntity] records
     * (user inquiry turn followed by autonomous WhatsApp AI agent response turn).
     */
    fun toChatMessageEntities(): List<WhatsAppChatMessageEntity> {
        val userTurn = WhatsAppChatMessageEntity(
            messageId = "${interactionId}_user",
            threadId = threadId,
            agentId = agentId,
            agentName = agentName,
            customerPhone = customerPhone,
            customerName = customerName,
            isFromUser = true,
            senderRole = "USER",
            content = userMessage,
            timestampMillis = timestampMillis,
            formattedTime = formattedTime,
            detectedIntent = detectedIntent
        )
        val agentTurn = WhatsAppChatMessageEntity(
            messageId = "${interactionId}_agent",
            threadId = threadId,
            agentId = agentId,
            agentName = agentName,
            customerPhone = customerPhone,
            customerName = customerName,
            isFromUser = false,
            senderRole = "WHATSAPP_AGENT",
            content = agentReply,
            timestampMillis = timestampMillis + 1L,
            formattedTime = formattedTime,
            detectedIntent = detectedIntent,
            confidenceScore = confidenceScore,
            latencyMs = latencyMs,
            aiReasoningTrace = aiReasoningTrace,
            reservedSlotId = reservedSlotId
        )
        return listOf(userTurn, agentTurn)
    }
}

typealias AgentInteractionEntity = WhatsAppInteractionEntity
typealias WhatsAppAgentInteractionEntity = WhatsAppInteractionEntity
typealias InteractionHistoryEntity = WhatsAppInteractionEntity

/**
 * Room DAO for persisting and retrieving the history of interactions between the user
 * and the autonomous WhatsApp AI agent.
 */
@Dao
interface WhatsAppInteractionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: WhatsAppInteractionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractions(interactions: List<WhatsAppInteractionEntity>): List<Long>

    @Update
    suspend fun updateInteraction(interaction: WhatsAppInteractionEntity)

    @Delete
    suspend fun deleteInteraction(interaction: WhatsAppInteractionEntity)

    /**
     * Observes all persisted user-agent interactions ordered chronologically (oldest to newest).
     */
    @Query("SELECT * FROM whatsapp_interactions ORDER BY timestamp_millis ASC, id ASC")
    fun getAllInteractions(): Flow<List<WhatsAppInteractionEntity>>

    /**
     * Observes all persisted user-agent interactions in reverse chronological order (newest first).
     */
    @Query("SELECT * FROM whatsapp_interactions ORDER BY timestamp_millis DESC, id DESC")
    fun getAllInteractionsDescending(): Flow<List<WhatsAppInteractionEntity>>

    /**
     * Retrieves all stored interactions as a one-shot list for context building or export.
     */
    @Query("SELECT * FROM whatsapp_interactions ORDER BY timestamp_millis ASC, id ASC")
    suspend fun getAllInteractionsSync(): List<WhatsAppInteractionEntity>

    /**
     * Observes interactions for a specific conversation [threadId].
     */
    @Query("SELECT * FROM whatsapp_interactions WHERE thread_id = :threadId ORDER BY timestamp_millis ASC, id ASC")
    fun getInteractionsByThread(threadId: String): Flow<List<WhatsAppInteractionEntity>>

    /**
     * Observes interactions for a specific [sessionId].
     */
    @Query("SELECT * FROM whatsapp_interactions WHERE session_id = :sessionId ORDER BY timestamp_millis ASC, id ASC")
    fun getInteractionsBySession(sessionId: String): Flow<List<WhatsAppInteractionEntity>>

    /**
     * Observes interactions handled by a specific autonomous WhatsApp [agentId].
     */
    @Query("SELECT * FROM whatsapp_interactions WHERE agent_id = :agentId ORDER BY timestamp_millis DESC, id DESC")
    fun getInteractionsByAgent(agentId: String): Flow<List<WhatsAppInteractionEntity>>

    /**
     * Observes interactions filtered by [detectedIntent] (e.g., BOOKING_REQUEST, PRICING_INQUIRY).
     */
    @Query("SELECT * FROM whatsapp_interactions WHERE detected_intent = :detectedIntent ORDER BY timestamp_millis DESC, id DESC")
    fun getInteractionsByIntent(detectedIntent: String): Flow<List<WhatsAppInteractionEntity>>

    /**
     * Searches past user-agent interactions by keyword across user message, agent reply,
     * customer name/phone, detected intent, or AI reasoning trace.
     */
    @Query(
        """
        SELECT * FROM whatsapp_interactions 
        WHERE user_message LIKE '%' || :query || '%' 
           OR agent_reply LIKE '%' || :query || '%' 
           OR customer_name LIKE '%' || :query || '%' 
           OR customer_phone LIKE '%' || :query || '%' 
           OR detected_intent LIKE '%' || :query || '%' 
           OR ai_reasoning_trace LIKE '%' || :query || '%'
        ORDER BY timestamp_millis ASC, id ASC
        """
    )
    fun searchInteractions(query: String): Flow<List<WhatsAppInteractionEntity>>

    /**
     * Retrieves a single interaction by its primary key [id].
     */
    @Query("SELECT * FROM whatsapp_interactions WHERE id = :id LIMIT 1")
    suspend fun getInteractionById(id: Long): WhatsAppInteractionEntity?

    /**
     * Retrieves a single interaction by its string [interactionId].
     */
    @Query("SELECT * FROM whatsapp_interactions WHERE interaction_id = :interactionId LIMIT 1")
    suspend fun getInteractionByStringId(interactionId: String): WhatsAppInteractionEntity?

    /**
     * Retrieves the most recent interaction between the user and the WhatsApp AI agent.
     */
    @Query("SELECT * FROM whatsapp_interactions ORDER BY timestamp_millis DESC, id DESC LIMIT 1")
    suspend fun getLatestInteraction(): WhatsAppInteractionEntity?

    /**
     * Observes the total number of stored interactions.
     */
    @Query("SELECT COUNT(*) FROM whatsapp_interactions")
    fun getInteractionCount(): Flow<Int>

    /**
     * Deletes all interactions for a specific [threadId].
     */
    @Query("DELETE FROM whatsapp_interactions WHERE thread_id = :threadId")
    suspend fun deleteInteractionsForThread(threadId: String): Int

    /**
     * Clears the entire interaction history table.
     */
    @Query("DELETE FROM whatsapp_interactions")
    suspend fun clearAllInteractions(): Int
}

typealias AgentInteractionDao = WhatsAppInteractionDao
typealias InteractionHistoryDao = WhatsAppInteractionDao

/**
 * Repository for recording and retrieving interactions between the user and the
 * autonomous WhatsApp AI agent, keeping both `whatsapp_interactions` and
 * `whatsapp_chat_threads` / `whatsapp_chat_messages` synchronized in Room.
 */
class WhatsAppInteractionRepository(
    private val interactionDao: WhatsAppInteractionDao,
    private val chatHistoryDao: WhatsAppChatHistoryDao? = null
) {
    val allInteractions: Flow<List<WhatsAppInteractionEntity>> = interactionDao.getAllInteractions()
    val allInteractionsDescending: Flow<List<WhatsAppInteractionEntity>> = interactionDao.getAllInteractionsDescending()
    val interactionCount: Flow<Int> = interactionDao.getInteractionCount()

    fun getInteractionsForThread(threadId: String): Flow<List<WhatsAppInteractionEntity>> =
        interactionDao.getInteractionsByThread(threadId)

    fun getInteractionsForSession(sessionId: String): Flow<List<WhatsAppInteractionEntity>> =
        interactionDao.getInteractionsBySession(sessionId)

    fun getInteractionsForAgent(agentId: String): Flow<List<WhatsAppInteractionEntity>> =
        interactionDao.getInteractionsByAgent(agentId)

    fun searchInteractions(query: String): Flow<List<WhatsAppInteractionEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isBlank()) {
            interactionDao.getAllInteractions()
        } else {
            interactionDao.searchInteractions(trimmed)
        }
    }

    /**
     * Persists a complete user <-> WhatsApp AI agent interaction to Room and optionally
     * appends the individual user and agent turns to the thread history tables.
     */
    suspend fun recordInteraction(
        interaction: WhatsAppInteractionEntity,
        syncToThreadHistory: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        val rowId = interactionDao.insertInteraction(interaction)
        if (syncToThreadHistory && chatHistoryDao != null) {
            val turns = interaction.toChatMessageEntities()
            turns.forEach { turn ->
                chatHistoryDao.appendMessageToThread(
                    message = turn,
                    topicSummary = interaction.detectedIntent
                )
            }
        }
        rowId
    }

    suspend fun getLatestInteraction(): WhatsAppInteractionEntity? = withContext(Dispatchers.IO) {
        interactionDao.getLatestInteraction()
    }

    suspend fun getAllInteractionsList(): List<WhatsAppInteractionEntity> = withContext(Dispatchers.IO) {
        interactionDao.getAllInteractionsSync()
    }

    suspend fun clearHistory(): Int = withContext(Dispatchers.IO) {
        interactionDao.clearAllInteractions()
    }
}
