package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceRecordingState
import com.example.audio.VoiceToTextManager
import com.example.data.local.ChatMessage
import com.example.data.local.WhatsAppAgentRepository
import com.example.data.local.WhatsAppInteractionEntity
import com.example.data.local.ZamaDatabase
import com.example.export.ChatExportManager
import com.example.service.whatsapp.WhatsAppAgentService
import com.example.service.whatsapp.WhatsAppAgentServiceStatus
import com.example.service.whatsapp.api.SandboxWhatsAppCommunicationApi
import com.example.service.whatsapp.api.WhatsAppCommunicationApi
import com.example.service.whatsapp.api.WhatsAppInboundMessage
import com.example.service.whatsapp.api.WhatsAppMessageType
import com.example.service.whatsapp.intelligence.AgentDecision
import com.example.service.whatsapp.intelligence.GeminiAgentIntelligence
import com.example.service.whatsapp.router.WhatsAppInteractionRouter
import com.example.ui.theme.ChatThemeMode
import com.example.ui.theme.ChatThemePalette
import com.example.ui.theme.toPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ChatViewModel(
    application: Application,
    private val repository: WhatsAppAgentRepository,
    val agentService: WhatsAppAgentService,
    val interactionRouter: WhatsAppInteractionRouter,
    val voiceToTextManager: VoiceToTextManager = VoiceToTextManager(application),
    val whatsAppBusinessManager: com.example.service.whatsapp.api.WhatsAppBusinessCommunicationManager? = null
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("zama_theme_preferences", Context.MODE_PRIVATE)
    val settingsManager = com.example.model.WhatsAppAgentSettingsManager(application)

    // Chat messages stream from Room
    val chatMessages: StateFlow<List<ChatMessage>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Interaction audit logs stream from Room
    val interactions: StateFlow<List<WhatsAppInteractionEntity>> = repository.allInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Metrics
    val interactionCount: StateFlow<Int> = repository.interactionCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val escalatedCount: StateFlow<Int> = repository.escalatedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Agent service state
    val serviceStatus: StateFlow<WhatsAppAgentServiceStatus> = agentService.serviceStatus
    val isAutoReplyActive: StateFlow<Boolean> = agentService.isAutoReplyActive
    val isAwayModeActive: StateFlow<Boolean> = agentService.isAwayModeActive
    val awayMessage: StateFlow<String> = agentService.awayMessage
    val latestDecision: StateFlow<AgentDecision?> = agentService.latestDecision

    // Theme state
    private val _chatThemeMode = MutableStateFlow(loadInitialChatTheme())
    val chatThemeMode: StateFlow<ChatThemeMode> = _chatThemeMode.asStateFlow()

    private val _chatThemePalette = MutableStateFlow(_chatThemeMode.value.toPalette())
    val chatThemePalette: StateFlow<ChatThemePalette> = _chatThemePalette.asStateFlow()

    // Voice-to-Text state
    val voiceRecordingState: StateFlow<VoiceRecordingState> = voiceToTextManager.state
    val isListening: StateFlow<Boolean> = voiceToTextManager.isListening

    // UI input fields
    private val _inputMessageText = MutableStateFlow("")
    val inputMessageText: StateFlow<String> = _inputMessageText.asStateFlow()

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

    init {
        // Sync agent service with persistent settings
        agentService.setAwayMode(settingsManager.isAwayModeActive.value, settingsManager.awayMessage.value)
        agentService.setAutoReplyActive(settingsManager.isAutoReplyActive.value)

        // Seed introductory message if room is empty
        viewModelScope.launch {
            if (!repository.hasMessage("msg_welcome_001") && repository.getMessageCount() == 0) {
                val seedGreeting = ChatMessage(
                    messageId = "msg_welcome_001",
                    content = "Hello! I am Zama AI, your autonomous WhatsApp concierge. I can answer inquiries, verify service pricing, book salon appointments, and interface with communication APIs.",
                    isFromUser = false,
                    senderRole = "AI",
                    timestamp = "09:00",
                    statusTicks = "✓✓",
                    aiTrace = "Autonomous WhatsApp Concierge initialized. Ready for incoming webhooks.",
                    intentTag = "GENERAL_SALON_QUERY"
                )
                repository.saveChatMessage(seedGreeting)
            }
        }
    }

    private fun loadInitialChatTheme(): ChatThemeMode {
        val saved = prefs.getString("key_chat_theme_mode", ChatThemeMode.FUTURISTIC_NEON.id)
        return ChatThemeMode.fromId(saved)
    }

    fun setChatThemeMode(mode: ChatThemeMode) {
        prefs.edit().putString("key_chat_theme_mode", mode.id).apply()
        _chatThemeMode.value = mode
        _chatThemePalette.value = mode.toPalette()
    }

    fun toggleChatTheme(): ChatThemeMode {
        val next = when (_chatThemeMode.value) {
            ChatThemeMode.FUTURISTIC_NEON -> ChatThemeMode.MINIMALIST_DARK
            ChatThemeMode.MINIMALIST_DARK -> ChatThemeMode.FUTURISTIC_NEON
        }
        setChatThemeMode(next)
        return next
    }

    fun onInputTextChanged(text: String) {
        _inputMessageText.value = text
    }

    fun toggleAutoReply(active: Boolean? = null) {
        val next = active ?: !agentService.isAutoReplyActive.value
        agentService.setAutoReplyActive(next)
        settingsManager.setAutoReplyActive(next)
    }

    fun toggleAwayMode(active: Boolean? = null) {
        val next = active ?: !agentService.isAwayModeActive.value
        agentService.setAwayMode(next)
        settingsManager.setAwayModeActive(next)
    }

    fun setAwayMessage(message: String) {
        agentService.setAwayMessage(message)
        settingsManager.setAwayMessage(message)
    }

    fun resetAwayMessage() {
        agentService.setAwayMessage(com.example.service.whatsapp.WhatsAppAgentService.DEFAULT_AWAY_MESSAGE)
        settingsManager.resetAwayMessage()
    }

    fun simulateAwayMessageTest() {
        simulateInboundCustomerMessage(
            messageText = "Hi Zama! Is your team available to take appointments right now?",
            senderName = "Nosipho Mthembu",
            senderPhone = "+27 83 456 7890"
        )
    }

    /**
     * Sends user message from the interactive chat and routes it through the autonomous agent service.
     */
    fun sendUserMessage(text: String = _inputMessageText.value, isVoice: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _inputMessageText.value = ""
        viewModelScope.launch {
            val inbound = WhatsAppInboundMessage(
                messageId = "msg_usr_${UUID.randomUUID().toString().take(10)}",
                fromPhoneNumber = "+27 82 001 9283",
                senderName = "Client",
                text = trimmed,
                messageType = if (isVoice) WhatsAppMessageType.AUDIO_VOICE_NOTE else WhatsAppMessageType.TEXT,
                rawPayload = "{\"source\": \"client_chat_ui\", \"input\": \"$trimmed\"}"
            )
            agentService.handleInboundInteraction(inbound)
        }
    }

    /**
     * Simulates an inbound customer WhatsApp message coming from external communication APIs.
     */
    fun simulateInboundCustomerMessage(
        messageText: String,
        senderName: String = "Sarah Jenkins",
        senderPhone: String = "+27 82 555 0192",
        isVoice: Boolean = false
    ) {
        viewModelScope.launch {
            interactionRouter.simulateCustomerMessage(
                messageText = messageText,
                senderName = senderName,
                senderPhone = senderPhone,
                isVoiceNote = isVoice
            )
        }
    }

    /**
     * Ingests a raw webhook JSON payload from a WhatsApp communication API.
     */
    fun processWebhookPayload(rawJson: String) {
        viewModelScope.launch {
            if (whatsAppBusinessManager != null) {
                whatsAppBusinessManager.processInboundWebhook(rawJson)
            } else {
                interactionRouter.routeWebhookJson(rawJson)
            }
        }
    }

    /**
     * Starts Voice-to-Text dictation using Android SpeechRecognizer.
     */
    fun startVoiceDictation(): Result<Unit> {
        return voiceToTextManager.startDictation(
            existingText = _inputMessageText.value
        ) { text, isFinal ->
            _inputMessageText.value = text
        }
    }

    fun stopVoiceDictation() {
        voiceToTextManager.stopDictation()
    }

    fun cancelVoiceDictation() {
        voiceToTextManager.cancelDictation()
    }

    /**
     * Exports chat messages and interaction logs to a shareable JSON file.
     */
    fun exportChatHistory(context: Context): Pair<File, Intent> {
        val messages = chatMessages.value
        val inters = interactions.value
        val (file, intent) = ChatExportManager.exportToJson(context, messages, inters)
        _exportResult.value = "Exported ${messages.size} messages and ${inters.size} audit logs to ${file.name}"
        return Pair(file, intent)
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    /**
     * Synchronous suspension method to purge all local data.
     */
    suspend fun purgeAllDataSync() {
        withContext(Dispatchers.IO) {
            val db = ZamaDatabase.getDatabase(getApplication())
            db.clearAllTables()
            try {
                val convDb = com.example.data.local.ConversationDatabase.getInstance(getApplication())
                convDb.clearAllTables()
            } catch (_: Exception) {}
        }
        repository.clearHistory()
        val authRepo = com.example.security.AuthRepository(getApplication())
        authRepo.resetAllSecurityData()
        val keyStoreManager = com.example.security.EncryptedKeyStoreManager(getApplication())
        keyStoreManager.clearApiKey()
        settingsManager.clearAllSettings()
        agentService.setAwayMode(false, WhatsAppAgentService.DEFAULT_AWAY_MESSAGE)
        agentService.setAutoReplyActive(true)
        _inputMessageText.value = ""
        _exportResult.value = null
    }

    /**
     * Purges all locally stored data: Room SQLite database tables,
     * security vault PIN/salt records, settings preferences, and in-memory UI state.
     */
    fun deleteAllLocalDataAndReset(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            purgeAllDataSync()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceToTextManager.destroy()
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = ZamaDatabase.getDatabase(application)
                    val repository = WhatsAppAgentRepository(db.whatsAppInteractionDao(), db.chatDao())
                    val agentLogRepo = com.example.data.local.AutonomousAgentLogRepository(db.autonomousAgentLogDao())
                    val communicationService = com.example.service.whatsapp.api.WhatsAppBusinessCommunicationService()
                    val secureKeyProvider = com.example.security.SecureApiKeyProvider(application)
                    val intelligence = GeminiAgentIntelligence(secureKeyProvider)
                    val agentService = WhatsAppAgentService(
                        repository = repository,
                        communicationApi = communicationService,
                        intelligence = intelligence,
                        agentLogRepository = agentLogRepo
                    )
                    val router = WhatsAppInteractionRouter(agentService)
                    val communicationManager = com.example.service.whatsapp.api.WhatsAppBusinessCommunicationManager(
                        communicationService = communicationService,
                        agentService = agentService
                    )

                    return ChatViewModel(
                        application = application,
                        repository = repository,
                        agentService = agentService,
                        interactionRouter = router,
                        whatsAppBusinessManager = communicationManager
                    ) as T
                }
            }
        }
    }
}
