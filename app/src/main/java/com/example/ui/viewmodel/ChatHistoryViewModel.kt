package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatHistoryRepository
import com.example.data.local.ChatLogEntity
import com.example.data.local.ZamaDatabase
import com.example.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing chat history logs backed by Room Database.
 */
class ChatHistoryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ChatHistoryRepository = ChatHistoryRepository(ZamaDatabase.getDatabase(application).chatLogDao())
) : AndroidViewModel(application) {

    /**
     * Stored chat entities from Room in chronological order.
     */
    val chatLogs: StateFlow<List<ChatLogEntity>> = repository.allChatLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Stored chat messages mapped for UI rendering.
     */
    val chatMessages: StateFlow<List<ChatMessage>> = chatLogs
        .map { entities -> entities.map { it.toChatMessage() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Total stored message log count in Room.
     */
    val totalMessageCount: StateFlow<Int> = repository.chatLogCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Seed demo initial conversation if Room database is empty
        viewModelScope.launch {
            repository.seedInitialMessagesIfEmpty()
        }
    }

    /**
     * Persists a customer message into Room.
     */
    fun saveCustomerMessage(
        text: String,
        formattedTime: String = "14:05",
        sessionId: String = "default_session",
        isVoiceNote: Boolean = false,
        audioModelUsed: String? = null
    ) {
        val chatMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            isFromCustomer = true,
            text = text,
            timestamp = formattedTime,
            isVoiceNote = isVoiceNote,
            audioModelUsed = audioModelUsed
        )
        viewModelScope.launch {
            repository.saveMessage(chatMessage, sessionId = sessionId)
        }
    }

    /**
     * Persists an AI agent reply into Room alongside its neural reasoning trace.
     */
    fun saveAiAgentReply(
        text: String,
        aiTrace: String,
        formattedTime: String = "14:05",
        sessionId: String = "default_session"
    ) {
        val chatMessage = ChatMessage(
            id = (System.currentTimeMillis() + 1).toString(),
            isFromCustomer = false,
            text = text,
            timestamp = formattedTime,
            statusTicks = "✓✓"
        )
        viewModelScope.launch {
            repository.saveMessage(chatMessage, sessionId = sessionId, aiTrace = aiTrace)
        }
    }

    /**
     * Clears all chat history from Room and reseeds initial greeting.
     */
    fun resetChatHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            repository.seedInitialMessagesIfEmpty()
        }
    }

    /**
     * Injects realistic 24-hour conversational traffic into Room to showcase
     * chat frequency and sentiment trendlines in the D3 / Recharts dashboard.
     */
    fun seedSimulatedTraffic() {
        viewModelScope.launch {
            val traffic = com.example.analytics.ChatAnalyticsEngine.createSimulatedTraffic()
            repository.addLogs(traffic)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatHistoryViewModel::class.java)) {
                val database = ZamaDatabase.getDatabase(application)
                val repository = ChatHistoryRepository(database.chatLogDao())
                return ChatHistoryViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
