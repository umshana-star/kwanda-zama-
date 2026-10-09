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
     * Searches chat logs by keyword in Room.
     */
    fun searchChatLogs(query: String): Flow<List<ChatLogEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isBlank()) {
            chatLogDao.getAllChatLogs()
        } else {
            chatLogDao.searchChatLogs(trimmed)
        }
    }

    /**
     * Searches chat logs in Room filtered by both keyword and sender role.
     */
    fun searchChatLogsBySender(query: String, isFromCustomer: Boolean): Flow<List<ChatLogEntity>> {
        return chatLogDao.searchChatLogsBySender(query.trim(), isFromCustomer)
    }

    /**
     * Queries the Room `chat_messages` table to filter past messages with the WhatsApp agent
     * by keyword and optional sender role (`isFromUser`).
     */
    fun searchChatMessages(
        query: String,
        isFromUser: Boolean? = null
    ): Flow<List<com.example.data.local.ChatMessage>> {
        val trimmed = query.trim()
        return when {
            trimmed.isBlank() && isFromUser == null -> chatLogDao.getAllChatMessages()
            trimmed.isBlank() && isFromUser != null -> chatLogDao.getMessagesBySender(isFromUser)
            isFromUser != null -> chatLogDao.searchChatMessagesBySender(trimmed, isFromUser)
            else -> chatLogDao.searchChatMessages(trimmed)
        }
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

    /**
     * Exports all stored chat history from Room (`chat_logs` and `chat_messages`) into a
     * formatted JSON backup string.
     */
    suspend fun exportChatHistoryToJsonString(filterQuery: String = ""): String = withContext(Dispatchers.IO) {
        val trimmed = filterQuery.trim()
        val logs = chatLogDao.getAllChatLogsSync().let { list ->
            if (trimmed.isBlank()) list else list.filter { it.text.contains(trimmed, ignoreCase = true) }
        }
        val messages = chatLogDao.getAllChatMessagesSync().let { list ->
            if (trimmed.isBlank()) list else list.filter { it.content.contains(trimmed, ignoreCase = true) }
        }
        com.example.export.ChatExportManager.generateJsonBackup(
            messages = logs.map { it.toChatMessage() },
            scope = if (trimmed.isBlank()) com.example.export.ExportScope.FULL_CHAT else com.example.export.ExportScope.FILTERED_VIEW,
            filterQuery = filterQuery,
            roomChatLogs = logs,
            roomChatMessages = messages
        )
    }

    /**
     * Exports all stored chat history from Room (`chat_logs` and `chat_messages`) to a
     * shareable `.json` file in `context.cacheDir/exports/`.
     */
    suspend fun exportChatHistoryToJsonFile(
        context: android.content.Context,
        filterQuery: String = ""
    ): java.io.File = withContext(Dispatchers.IO) {
        val trimmed = filterQuery.trim()
        val logs = chatLogDao.getAllChatLogsSync().let { list ->
            if (trimmed.isBlank()) list else list.filter { it.text.contains(trimmed, ignoreCase = true) }
        }
        val messages = chatLogDao.getAllChatMessagesSync().let { list ->
            if (trimmed.isBlank()) list else list.filter { it.content.contains(trimmed, ignoreCase = true) }
        }
        com.example.export.ChatExportManager.createJsonFile(
            context = context,
            messages = logs.map { it.toChatMessage() },
            scope = if (trimmed.isBlank()) com.example.export.ExportScope.FULL_CHAT else com.example.export.ExportScope.FILTERED_VIEW,
            filterQuery = filterQuery,
            roomChatLogs = logs,
            roomChatMessages = messages
        )
    }
}
