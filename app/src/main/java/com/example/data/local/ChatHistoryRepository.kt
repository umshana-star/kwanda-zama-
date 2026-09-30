package com.example.data.local

import com.example.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository abstracting Room database operations for chat history logs.
 */
class ChatHistoryRepository(
    private val chatLogDao: ChatLogDao
) {

    /**
     * Observes all chat history logs as a reactive Flow.
     */
    val allChatLogs: Flow<List<ChatLogEntity>> = chatLogDao.getAllChatLogs()

    /**
     * Observes the count of stored chat logs.
     */
    val chatLogCount: Flow<Int> = chatLogDao.getChatLogCount()

    /**
     * Retrieves chat logs by session ID.
     */
    fun getChatLogsForSession(sessionId: String): Flow<List<ChatLogEntity>> {
        return chatLogDao.getChatLogsBySession(sessionId)
    }

    /**
     * Searches chat logs by keyword.
     */
    fun searchChatLogs(query: String): Flow<List<ChatLogEntity>> {
        return chatLogDao.searchChatLogs(query)
    }

    /**
     * Inserts a customer or AI agent chat message into Room.
     */
    suspend fun saveMessage(
        message: ChatMessage,
        sessionId: String = "default_session",
        aiTrace: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val entity = ChatLogEntity.fromChatMessage(
            chatMessage = message,
            sessionId = sessionId,
            aiTrace = aiTrace
        )
        chatLogDao.insertChatLog(entity)
    }

    /**
     * Clears all chat messages from Room.
     */
    suspend fun clearHistory(sessionId: String? = null) = withContext(Dispatchers.IO) {
        if (sessionId != null) {
            chatLogDao.clearSessionChatLogs(sessionId)
        } else {
            chatLogDao.clearAllChatLogs()
        }
    }

    /**
     * Seeds initial conversation logs into Room if the database is empty.
     */
    suspend fun seedInitialMessagesIfEmpty(sessionId: String = "default_session") = withContext(Dispatchers.IO) {
        val count = chatLogDao.getChatLogCount().first()
        if (count == 0) {
            val initialEntities = listOf(
                ChatLogEntity(
                    messageId = "seed_1",
                    sessionId = sessionId,
                    isFromCustomer = true,
                    senderRole = "CUSTOMER",
                    text = "Hi, how much for braids and do you have Saturday at 2pm?",
                    timestampMillis = System.currentTimeMillis() - 120_000,
                    timestampFormatted = "14:02",
                    statusTicks = "✓✓"
                ),
                ChatLogEntity(
                    messageId = "seed_2",
                    sessionId = sessionId,
                    isFromCustomer = false,
                    senderRole = "AI_AGENT",
                    text = "Hi Sarah 👋 Braids start from R650. We have Saturday at 2pm available. Would you like me to book it?",
                    timestampMillis = System.currentTimeMillis() - 60_000,
                    timestampFormatted = "14:02",
                    statusTicks = "✓✓",
                    aiTrace = "Intent: PricingInquiry + BookingRequest (Confidence: 99.4%)\n" +
                            "Catalog Match: Knotless Braids (Base: R650, Duration: 2.5h)\n" +
                            "Calendar Check: Saturday @ 14:00 -> 1 Stylist Free (Slot ID: #BK-749)"
                )
            )
            chatLogDao.insertChatLogs(initialEntities)
        }
    }

    /**
     * Inserts multiple chat logs into Room (used for traffic simulation or batch import).
     */
    suspend fun addLogs(logs: List<ChatLogEntity>) = withContext(Dispatchers.IO) {
        chatLogDao.insertChatLogs(logs)
    }
}
