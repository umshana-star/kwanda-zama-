package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.analytics.WhatsAppAgentHourlyEngine
import com.example.analytics.WhatsAppAgentHourlyMetrics
import com.example.data.local.WhatsAppAgentRepository
import com.example.data.local.ZamaDatabase
import com.example.model.AgentOperationalStatus
import com.example.model.AgentSpecialization
import com.example.model.EventCategoryFilter
import com.example.model.HandoffTriggerMode
import com.example.model.WhatsAppAgentEntity
import com.example.model.WhatsAppAgentEventEntity
import com.example.model.WhatsAppAgentEventType
import com.example.model.WhatsAppGlobalSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel powering the Autonomous WhatsApp Agent Management Screen and Status Indicators.
 */
class WhatsAppAgentViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WhatsAppAgentRepository = WhatsAppAgentRepository(
        agentDao = ZamaDatabase.getDatabase(application).whatsAppAgentDao(),
        context = application
    )
) : AndroidViewModel(application) {

    val globalSettings: StateFlow<WhatsAppGlobalSettings> = repository.globalSettings

    private val _rawAgents = repository.allAgents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedStatusFilter = MutableStateFlow<AgentOperationalStatus?>(null)
    val selectedStatusFilter: StateFlow<AgentOperationalStatus?> = _selectedStatusFilter.asStateFlow()

    val filteredAgents: StateFlow<List<WhatsAppAgentEntity>> = combine(
        _rawAgents,
        _selectedStatusFilter
    ) { agents, filter ->
        if (filter == null) agents else agents.filter { it.operationalStatus == filter }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allAgents: StateFlow<List<WhatsAppAgentEntity>> = _rawAgents

    val activeOnlineCount: StateFlow<Int> = _rawAgents.combine(_rawAgents) { agents, _ ->
        agents.count { it.operationalStatus == AgentOperationalStatus.ACTIVE_ONLINE && it.isAutonomousEnabled }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalLiveChats: StateFlow<Int> = _rawAgents.combine(_rawAgents) { agents, _ ->
        agents.sumOf { it.currentActiveChats }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val avgSystemLatencyMs: StateFlow<Long> = _rawAgents.combine(_rawAgents) { agents, _ ->
        if (agents.isEmpty()) 750L else agents.map { it.avgResponseLatencyMs }.average().toLong()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 750L
    )

    val overallAutonomousRate: StateFlow<Int> = _rawAgents.combine(_rawAgents) { agents, _ ->
        if (agents.isEmpty()) 94 else {
            val avg = agents.map { it.confidenceThreshold }.average()
            (avg * 100).toInt().coerceIn(75, 99)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 94
    )

    val allEvents: StateFlow<List<WhatsAppAgentEventEntity>> = repository.allEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedEventCategory = MutableStateFlow(EventCategoryFilter.ALL)
    val selectedEventCategory: StateFlow<EventCategoryFilter> = _selectedEventCategory.asStateFlow()

    private val _selectedAgentLogFilter = MutableStateFlow<String?>(null)
    val selectedAgentLogFilter: StateFlow<String?> = _selectedAgentLogFilter.asStateFlow()

    private val _logSearchQuery = MutableStateFlow("")
    val logSearchQuery: StateFlow<String> = _logSearchQuery.asStateFlow()

    val filteredHistoryLogs: StateFlow<List<WhatsAppAgentEventEntity>> = combine(
        allEvents,
        _selectedEventCategory,
        _selectedAgentLogFilter,
        _logSearchQuery
    ) { events, category, agentFilter, query ->
        events.filter { event ->
            val matchesCategory = when (category) {
                EventCategoryFilter.ALL -> true
                EventCategoryFilter.MESSAGES_ONLY -> event.typedEventType.isMessage
                EventCategoryFilter.STATUS_EVENTS_ONLY -> !event.typedEventType.isMessage && event.typedEventType != WhatsAppAgentEventType.HANDOFF_TRIGGERED
                EventCategoryFilter.ESCALATIONS_ONLY -> event.typedEventType == WhatsAppAgentEventType.HANDOFF_TRIGGERED
            }
            val matchesAgent = agentFilter == null || event.agentId == agentFilter
            val matchesQuery = if (query.isBlank()) true else {
                event.detail.contains(query, ignoreCase = true) ||
                event.title.contains(query, ignoreCase = true) ||
                event.agentName.contains(query, ignoreCase = true) ||
                (event.customerPhone?.contains(query, ignoreCase = true) == true) ||
                (event.customerName?.contains(query, ignoreCase = true) == true)
            }
            matchesCategory && matchesAgent && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalEventCount: StateFlow<Int> = allEvents.combine(allEvents) { list, _ -> list.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMessagesCount: StateFlow<Int> = allEvents.combine(allEvents) { list, _ ->
        list.count { it.typedEventType.isMessage }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalEscalationCount: StateFlow<Int> = allEvents.combine(allEvents) { list, _ ->
        list.count { it.typedEventType == WhatsAppAgentEventType.HANDOFF_TRIGGERED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedChartAgentId = MutableStateFlow<String?>(null)
    val selectedChartAgentId: StateFlow<String?> = _selectedChartAgentId.asStateFlow()

    private val _selectedChartTimeframe = MutableStateFlow("Today")
    val selectedChartTimeframe: StateFlow<String> = _selectedChartTimeframe.asStateFlow()

    val hourlyMessageMetrics: StateFlow<WhatsAppAgentHourlyMetrics> = combine(
        allEvents,
        _rawAgents,
        _selectedChartAgentId,
        _selectedChartTimeframe
    ) { events, agents, agentId, timeframe ->
        WhatsAppAgentHourlyEngine.computeHourlyMetrics(
            events = events,
            agents = agents,
            selectedAgentId = agentId,
            timeframe = timeframe
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WhatsAppAgentHourlyEngine.computeHourlyMetrics(
            events = emptyList(),
            agents = emptyList()
        )
    )

    private val _editingAgent = MutableStateFlow<WhatsAppAgentEntity?>(null)
    val editingAgent: StateFlow<WhatsAppAgentEntity?> = _editingAgent.asStateFlow()

    private val _isSimulatingPing = MutableStateFlow(false)
    val isSimulatingPing: StateFlow<Boolean> = _isSimulatingPing.asStateFlow()

    private val _simulationFeedback = MutableStateFlow<String?>(null)
    val simulationFeedback: StateFlow<String?> = _simulationFeedback.asStateFlow()

    fun setFilter(status: AgentOperationalStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setEditingAgent(agent: WhatsAppAgentEntity?) {
        _editingAgent.value = agent
    }

    fun toggleAgentAutonomy(agentId: String, current: Boolean) {
        viewModelScope.launch {
            repository.toggleAgentAutonomy(agentId, !current)
        }
    }

    fun updateAgentStatus(agentId: String, newStatus: AgentOperationalStatus, activeChats: Int) {
        viewModelScope.launch {
            repository.updateAgentStatus(agentId, newStatus, activeChats)
        }
    }

    fun saveAgent(agent: WhatsAppAgentEntity) {
        viewModelScope.launch {
            repository.saveAgent(agent)
            _editingAgent.value = null
            _simulationFeedback.value = "Saved configuration for ${agent.name} (${agent.avgResponseLatencyMs}ms response delay)."
        }
    }

    fun deleteAgent(agent: WhatsAppAgentEntity) {
        viewModelScope.launch {
            repository.deleteAgent(agent)
            if (_editingAgent.value?.id == agent.id) {
                _editingAgent.value = null
            }
            _simulationFeedback.value = "Decommissioned autonomous agent ${agent.name} from cluster."
        }
    }

    fun toggleMasterKillSwitch(enabled: Boolean) {
        repository.toggleMasterKillSwitch(enabled)
        _simulationFeedback.value = if (enabled) {
            "Master autonomous cluster resumed online serving."
        } else {
            "Emergency Kill-Switch active: All autonomous agents paused."
        }
    }

    fun updateGlobalSettings(settings: WhatsAppGlobalSettings) {
        repository.updateGlobalSettings(settings)
        _simulationFeedback.value = "Global WhatsApp channel settings saved (${settings.aiResponseDelayMs}ms delay)."
    }

    fun updateAiResponseDelayMs(delayMs: Long) {
        val clamped = delayMs.coerceIn(100L, 5000L)
        repository.updateGlobalAiResponseDelayMs(clamped)
        _simulationFeedback.value = "AI response delay updated to ${clamped}ms across agent cluster."
    }

    fun simulateCustomerWhatsAppPing(agentId: String) {
        if (_isSimulatingPing.value) return
        _isSimulatingPing.value = true
        viewModelScope.launch {
            try {
                repository.simulateIncomingCustomerPing(agentId) { stepText ->
                    _simulationFeedback.value = stepText
                }
            } finally {
                _isSimulatingPing.value = false
            }
        }
    }

    fun clearSimulationFeedback() {
        _simulationFeedback.value = null
    }

    fun setEventCategoryFilter(filter: EventCategoryFilter) {
        _selectedEventCategory.value = filter
    }

    fun setSelectedAgentLogFilter(agentId: String?) {
        _selectedAgentLogFilter.value = agentId
    }

    fun setLogSearchQuery(query: String) {
        _logSearchQuery.value = query
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAllEvents()
        }
    }

    fun triggerEscalationAlert(agentId: String, reason: String = "Customer requested human intervention") {
        viewModelScope.launch {
            repository.triggerEscalationEvent(agentId, reason)
        }
    }

    fun selectChartAgent(agentId: String?) {
        _selectedChartAgentId.value = agentId
    }

    fun selectChartTimeframe(timeframe: String) {
        _selectedChartTimeframe.value = timeframe
    }

    fun createNewAgent(
        name: String,
        specialization: AgentSpecialization,
        phoneLine: String = "+27 82 555 0199 [Line Extra]",
        maxChats: Int = 20
    ) {
        val newAgent = WhatsAppAgentEntity(
            id = "agent_${System.currentTimeMillis()}",
            name = name.ifBlank { specialization.displayName.split(" ").first() },
            specialization = specialization.name,
            avatarEmoji = specialization.avatarEmoji,
            phoneLine = phoneLine,
            webhookUrl = "https://api.zama.ai/v1/whatsapp/webhook/${specialization.name.lowercase()}",
            status = AgentOperationalStatus.ACTIVE_ONLINE.name,
            isAutonomousEnabled = true,
            currentActiveChats = 1,
            maxConcurrentChats = maxChats,
            totalChatsToday = 0,
            avgResponseLatencyMs = 850L,
            confidenceThreshold = 0.85f,
            simulatedTypingDelaySec = 1.5f,
            sentimentScore = 0.95f,
            uptimePercentage = 100.0f,
            allowVoiceNoteReplies = true,
            allowAutoCalendarSync = true,
            supportedLanguages = "English, isiZulu",
            handoffTrigger = HandoffTriggerMode.NEGATIVE_SENTIMENT.name,
            systemPromptDirective = specialization.description,
            lastActiveEpochMillis = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveAgent(newAgent)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WhatsAppAgentViewModel::class.java)) {
                return WhatsAppAgentViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
