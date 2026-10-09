package com.example.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WhatsAppAgentRepository(
    private val interactionDao: WhatsAppInteractionDao,
    private val chatDao: ChatDao
) {

    val allChatMessages: Flow<List<ChatMessage>> = chatDao.getChatHistory()
    val allInteractions: Flow<List<WhatsAppInteractionEntity>> = interactionDao.getAllInteractions()
    val interactionCount: Flow<Int> = interactionDao.getInteractionCount()
    val escalatedCount: Flow<Int> = interactionDao.getAllInteractions().map { list ->
        list.count { it.requiresHumanHandoff }
    }

    fun searchChatMessages(query: String, isFromUser: Boolean?): Flow<List<ChatMessage>> {
        return if (isFromUser != null) {
            chatDao.searchMessagesBySender(query, isFromUser)
        } else {
            chatDao.searchChatHistory(query)
        }
    }

    suspend fun saveChatMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun logInteraction(interaction: WhatsAppInteractionEntity) {
        interactionDao.insertInteraction(interaction)
    }

    suspend fun updateDeliveryStatus(interactionId: String, status: String) {
        val existing = interactionDao.getInteractionByStringId(interactionId)
        if (existing != null) {
            interactionDao.updateInteraction(existing.copy(interactionStatus = status))
        }
    }

    suspend fun hasMessage(messageId: String): Boolean {
        return chatDao.countByMessageId(messageId) > 0
    }

    suspend fun getMessageCount(): Int {
        return chatDao.getMessageCount()
    }

    suspend fun clearHistory() {
        chatDao.clearChatHistory()
    }
}
