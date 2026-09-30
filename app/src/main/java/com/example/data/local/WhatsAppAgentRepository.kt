package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.remote.WhatsAppAgentBackendService
import com.example.data.remote.WhatsAppAgentExchangeResult
import com.example.data.remote.WhatsAppAgentMessageResponse
import com.example.data.remote.WhatsAppAgentServiceContract
import com.example.data.remote.WhatsAppAgentStatusDto
import com.example.model.AgentOperationalStatus
import com.example.model.AgentSpecialization
import com.example.model.HandoffTriggerMode
import com.example.model.WhatsAppAgentEntity
import com.example.model.WhatsAppAgentEventEntity
import com.example.model.WhatsAppAgentEventType
import com.example.model.WhatsAppEventSeverity
import com.example.model.WhatsAppGlobalSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository abstracting data access for Autonomous WhatsApp Agents and Global Business Settings.
 * Manages Room DAO queries, default agent seeding, SharedPreferences persistence, and remote
 * data exchange via [WhatsAppAgentServiceContract] (Retrofit + Moshi).
 */
class WhatsAppAgentRepository(
    private val agentDao: WhatsAppAgentDao,
    context: Context,
    private val eventDao: WhatsAppAgentEventDao = ZamaDatabase.getDatabase(context).whatsAppAgentEventDao(),
    val chatHistoryDao: WhatsAppChatHistoryDao = ZamaDatabase.getDatabase(context).whatsAppChatHistoryDao(),
    val backendService: WhatsAppAgentServiceContract = WhatsAppAgentBackendService(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val appScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("zama_whatsapp_agent_prefs", Context.MODE_PRIVATE)

    private val _globalSettings = MutableStateFlow(loadGlobalSettings())
    val globalSettings: StateFlow<WhatsAppGlobalSettings> = _globalSettings.asStateFlow()

    val allAgents: Flow<List<WhatsAppAgentEntity>> = agentDao.getAllAgents()
    val allEvents: Flow<List<WhatsAppAgentEventEntity>> = eventDao.getAllEvents()
    val allChatThreads: Flow<List<WhatsAppChatThreadEntity>> = chatHistoryDao.getAllThreads()
    val allThreadsWithMessages: Flow<List<WhatsAppThreadWithMessages>> = chatHistoryDao.observeAllThreadsWithMessages()

    init {
        appScope.launch {
            seedDefaultAgentsIfEmpty()
            seedDefaultEventsIfEmpty()
            seedDefaultChatThreadsIfEmpty()
        }
    }

    private suspend fun seedDefaultAgentsIfEmpty() = withContext(ioDispatcher) {
        val count = agentDao.getAgentCount()
        if (count == 0) {
            val defaults = listOf(
                WhatsAppAgentEntity(
                    id = "agent_thandiwe_01",
                    name = "Thandiwe",
                    specialization = AgentSpecialization.SALON_CONCIERGE.name,
                    avatarEmoji = "💇‍♀️",
                    phoneLine = "+27 82 555 0192 (Line 1 - Front Desk)",
                    webhookUrl = "https://api.zama.ai/v1/whatsapp/webhook/concierge",
                    status = AgentOperationalStatus.ACTIVE_ONLINE.name,
                    isAutonomousEnabled = true,
                    currentActiveChats = 4,
                    maxConcurrentChats = 20,
                    totalChatsToday = 48,
                    avgResponseLatencyMs = 820L,
                    confidenceThreshold = 0.88f,
                    simulatedTypingDelaySec = 1.5f,
                    sentimentScore = 0.96f,
                    uptimePercentage = 99.9f,
                    allowVoiceNoteReplies = true,
                    allowAutoCalendarSync = true,
                    supportedLanguages = "English, isiZulu, Sesotho",
                    handoffTrigger = HandoffTriggerMode.NEGATIVE_SENTIMENT.name,
                    systemPromptDirective = "Warm, high-energy salon concierge. Welcome clients, recommend trending knotless braids and luxury silk press treatments.",
                    lastActiveEpochMillis = System.currentTimeMillis() - 15_000
                ),
                WhatsAppAgentEntity(
                    id = "agent_sipho_02",
                    name = "Sipho",
                    specialization = AgentSpecialization.APPOINTMENT_SCHEDULER.name,
                    avatarEmoji = "📅",
                    phoneLine = "+27 82 555 0193 (Line 2 - Appointments)",
                    webhookUrl = "https://api.zama.ai/v1/whatsapp/webhook/calendar",
                    status = AgentOperationalStatus.BUSY_HANDLING.name,
                    isAutonomousEnabled = true,
                    currentActiveChats = 7,
                    maxConcurrentChats = 25,
                    totalChatsToday = 62,
                    avgResponseLatencyMs = 640L,
                    confidenceThreshold = 0.92f,
                    simulatedTypingDelaySec = 1.2f,
                    sentimentScore = 0.98f,
                    uptimePercentage = 99.7f,
                    allowVoiceNoteReplies = false,
                    allowAutoCalendarSync = true,
                    supportedLanguages = "English, isiZulu, Afrikaans",
                    handoffTrigger = HandoffTriggerMode.EXPLICIT_HUMAN_REQUEST.name,
                    systemPromptDirective = "Deterministic calendar slot dispatcher. Checks slot conflicts and commits instant appointment reservations.",
                    lastActiveEpochMillis = System.currentTimeMillis() - 5_000
                ),
                WhatsAppAgentEntity(
                    id = "agent_nandi_03",
                    name = "Nandi",
                    specialization = AgentSpecialization.PRICING_QUOTE_SPECIALIST.name,
                    avatarEmoji = "💎",
                    phoneLine = "+27 82 555 0194 (Line 3 - Quotes & Inquiries)",
                    webhookUrl = "https://api.zama.ai/v1/whatsapp/webhook/quotes",
                    status = AgentOperationalStatus.STANDBY_IDLE.name,
                    isAutonomousEnabled = true,
                    currentActiveChats = 1,
                    maxConcurrentChats = 15,
                    totalChatsToday = 29,
                    avgResponseLatencyMs = 1100L,
                    confidenceThreshold = 0.85f,
                    simulatedTypingDelaySec = 2.0f,
                    sentimentScore = 0.93f,
                    uptimePercentage = 99.5f,
                    allowVoiceNoteReplies = true,
                    allowAutoCalendarSync = false,
                    supportedLanguages = "English, isiXhosa, isiZulu",
                    handoffTrigger = HandoffTriggerMode.LOW_CONFIDENCE.name,
                    systemPromptDirective = "Calculates precise quotes based on hair length (Mid-Back vs Waist), human hair vs synthetic, and wash treatments.",
                    lastActiveEpochMillis = System.currentTimeMillis() - 120_000
                ),
                WhatsAppAgentEntity(
                    id = "agent_zama_vip_04",
                    name = "Zama VIP Escort",
                    specialization = AgentSpecialization.VIP_TRIAGE_ESCALATION.name,
                    avatarEmoji = "🛡️",
                    phoneLine = "+27 82 555 0195 (VIP White-Glove Desk)",
                    webhookUrl = "https://api.zama.ai/v1/whatsapp/webhook/vip",
                    status = AgentOperationalStatus.ACTIVE_ONLINE.name,
                    isAutonomousEnabled = true,
                    currentActiveChats = 2,
                    maxConcurrentChats = 10,
                    totalChatsToday = 18,
                    avgResponseLatencyMs = 490L,
                    confidenceThreshold = 0.95f,
                    simulatedTypingDelaySec = 1.0f,
                    sentimentScore = 0.99f,
                    uptimePercentage = 100.0f,
                    allowVoiceNoteReplies = true,
                    allowAutoCalendarSync = true,
                    supportedLanguages = "English, isiZulu, Sesotho, French",
                    handoffTrigger = HandoffTriggerMode.VIP_HIGH_VALUE.name,
                    systemPromptDirective = "Executive white-glove treatment. Escalates directly to salon owner Kwanda for orders exceeding R1,500.",
                    lastActiveEpochMillis = System.currentTimeMillis() - 2_000
                )
            )
            agentDao.insertAgents(defaults)
        }
    }

    private suspend fun seedDefaultEventsIfEmpty() = withContext(ioDispatcher) {
        val count = eventDao.getEventCountSync()
        if (count == 0) {
            val now = System.currentTimeMillis()
            val initialEvents = listOf(
                WhatsAppAgentEventEntity(
                    id = "evt_seed_101",
                    agentId = "agent_zama_vip_04",
                    agentName = "Zama VIP Escort",
                    eventType = WhatsAppAgentEventType.HANDOFF_TRIGGERED.name,
                    title = "VIP Owner Escalation Triggered",
                    detail = "Client requesting R2,800 full bridal entourage package. Executed warm handoff to salon owner Kwanda (+27 82 000 8410).",
                    customerPhone = "+27 82 999 1010",
                    customerName = "Nomvula D. (VIP)",
                    timestampMillis = now - 35_000,
                    formattedTime = "14:18",
                    severity = WhatsAppEventSeverity.ALERT.name,
                    confidence = 0.99f,
                    aiTrace = "Trigger: VIP_HIGH_VALUE (>R1,500) • Package: Bridal Entourage Glam • Owner SMS Notification Dispatched",
                    metadataBadge = "VIP Handoff"
                ),
                WhatsAppAgentEventEntity(
                    id = "evt_seed_102",
                    agentId = "agent_thandiwe_01",
                    agentName = "Thandiwe",
                    eventType = WhatsAppAgentEventType.MESSAGE_OUTBOUND.name,
                    title = "Autonomous Reply Dispatched",
                    detail = "Hello Lerato! 👋 Yes, we have a slot this Saturday at 14:00 with Stylist Zinzi. Waist-length knotless braids are R850. Would you like me to hold this slot?",
                    customerPhone = "+27 82 491 8820",
                    customerName = "Lerato K.",
                    timestampMillis = now - 70_000,
                    formattedTime = "14:17",
                    severity = WhatsAppEventSeverity.SUCCESS.name,
                    latencyMs = 780L,
                    confidence = 0.96f,
                    aiTrace = "Intent: BookingSlotRequest • Catalog: Knotless Braids • Calendar: Slot #BK-892 Free • Auto-reply Latency: 780ms",
                    metadataBadge = "✓✓ Dispatched"
                ),
                WhatsAppAgentEventEntity(
                    id = "evt_seed_103",
                    agentId = "agent_thandiwe_01",
                    agentName = "Thandiwe",
                    eventType = WhatsAppAgentEventType.MESSAGE_INBOUND.name,
                    title = "Inbound Customer Inquiry",
                    detail = "Hi! Do you have availability for waist-length knotless braids this Saturday at 2pm?",
                    customerPhone = "+27 82 491 8820",
                    customerName = "Lerato K.",
                    timestampMillis = now - 72_000,
                    formattedTime = "14:17",
                    severity = WhatsAppEventSeverity.INFO.name,
                    metadataBadge = "Meta Cloud API"
                ),
                WhatsAppAgentEventEntity(
                    id = "evt_seed_104",
                    agentId = "agent_thandiwe_01",
                    agentName = "Thandiwe",
                    eventType = WhatsAppAgentEventType.SLOT_RESERVED.name,
                    title = "Tentative Slot Reserved",
                    detail = "Temporarily held slot #BK-892 (Sat 14:00 - 16:30) for Lerato K. Pending customer final confirmation within 15 mins.",
                    customerPhone = "+27 82 491 8820",
                    customerName = "Lerato K.",
                    timestampMillis = now - 69_000,
                    formattedTime = "14:17",
                    severity = WhatsAppEventSeverity.SUCCESS.name,
                    metadataBadge = "Slot #BK-892"
                ),
                WhatsAppAgentEventEntity(
                    id = "evt_seed_105",
                    agentId = "agent_sipho_02",
                    agentName = "Sipho",
                    eventType = WhatsAppAgentEventType.MESSAGE_OUTBOUND.name,
                    title = "Autonomous Reply Dispatched",
                    detail = "Reschedule confirmed! 📅 Your Fade & Beard appointment is now booked for tomorrow Friday at 11:30 with Stylist Jabulani.",
                    customerPhone = "+27 83 220 1944",
                    customerName = "Sipho M.",
                    timestampMillis = now - 180_000,
                    formattedTime = "14:15",
                    severity = WhatsAppEventSeverity.SUCCESS.name,
                    latencyMs = 610L,
                    confidence = 0.98f,
                    aiTrace = "Conflict Resolution: Shifted #BK-774 from 10:00 -> 11:30 • Google Calendar Synced",
                    metadataBadge = "✓✓ Dispatched"
                ),
                WhatsAppAgentEventEntity(
                    id = "evt_seed_106",
                    agentId = "agent_sipho_02",
                    agentName = "Sipho",
                    eventType = WhatsAppAgentEventType.MESSAGE_INBOUND.name,
                    title = "Inbound Customer Inquiry",
                    detail = "Can I move my appointment from 10:00 to 11:30 tomorrow?",
                    customerPhone = "+27 83 220 1944",
                    customerName = "Sipho M.",
                    timestampMillis = now - 182_000,
                    formattedTime = "14:15",
                    severity = WhatsAppEventSeverity.INFO.name,
                    metadataBadge = "Meta Cloud API"
                ),
                WhatsAppAgentEventEntity(
                    id = "evt_seed_107",
                    agentId = "agent_nandi_03",
                    agentName = "Nandi",
                    eventType = WhatsAppAgentEventType.PRICING_QUOTE.name,
                    title = "Smart Catalog Quote Synthesized",
                    detail = "Calculated dual quote: Synthetic Goddess Braids (R750) vs Bohemian Human Hair Blend (R1,250).",
                    customerPhone = "+27 71 884 1002",
                    customerName = "Nomsa T.",
                    timestampMillis = now - 300_000,
                    formattedTime = "14:12",
                    severity = WhatsAppEventSeverity.INFO.name,
                    latencyMs = 920L,
                    confidence = 0.94f,
                    metadataBadge = "Quote #QT-419"
                ),
                WhatsAppAgentEventEntity(
                    id = "evt_seed_108",
                    agentId = "agent_nandi_03",
                    agentName = "Nandi",
                    eventType = WhatsAppAgentEventType.MESSAGE_OUTBOUND.name,
                    title = "Autonomous Reply Dispatched",
                    detail = "Hi Nomsa! ✨ Synthetic fiber goddess braids are R750 (curls lasting ~3 weeks), while Human Hair Bohemian curl blend is R1,250 (tangle-free for 8+ weeks). Which style do you prefer?",
                    customerPhone = "+27 71 884 1002",
                    customerName = "Nomsa T.",
                    timestampMillis = now - 301_000,
                    formattedTime = "14:12",
                    severity = WhatsAppEventSeverity.SUCCESS.name,
                    latencyMs = 920L,
                    confidence = 0.94f,
                    aiTrace = "Intent: PricingComparison • Add-ons: Wash & Blow Included • Hair Grade: 100% Virgin Fiber vs Kanekalon",
                    metadataBadge = "✓✓ Dispatched"
                ),
                WhatsAppAgentEventEntity(
                    id = "evt_seed_109",
                    agentId = "system_gateway",
                    agentName = "Gateway Core",
                    eventType = WhatsAppAgentEventType.WEBHOOK_RECEIVED.name,
                    title = "Encrypted Webhook Ingested",
                    detail = "Meta Cloud API v19.0 webhook payload verified. SHA-256 HMAC signature matched. Ingestion latency 124ms.",
                    customerPhone = "+27 82 555 0192",
                    timestampMillis = now - 450_000,
                    formattedTime = "14:10",
                    severity = WhatsAppEventSeverity.INFO.name,
                    latencyMs = 124L,
                    metadataBadge = "HTTP 200 OK"
                ),
                WhatsAppAgentEventEntity(
                    id = "evt_seed_110",
                    agentId = "agent_thandiwe_01",
                    agentName = "Thandiwe",
                    eventType = WhatsAppAgentEventType.STATUS_CHANGED.name,
                    title = "Agent Status Transition",
                    detail = "Thandiwe transitioned to Online & Serving. Auto-reply concurrency at 4/20 chats.",
                    timestampMillis = now - 600_000,
                    formattedTime = "14:05",
                    severity = WhatsAppEventSeverity.SUCCESS.name,
                    metadataBadge = "Status: ONLINE"
                )
            )
            eventDao.insertEvents(initialEvents)
        }
    }

    suspend fun saveAgent(agent: WhatsAppAgentEntity) = withContext(ioDispatcher) {
        agentDao.insertAgent(agent)
        logEvent(
            WhatsAppAgentEventEntity(
                agentId = agent.id,
                agentName = agent.name,
                eventType = WhatsAppAgentEventType.CONFIG_UPDATED.name,
                title = "Agent Configuration Tuned",
                detail = "Updated parameters: Max chats=${agent.maxConcurrentChats}, Confidence=${(agent.confidenceThreshold * 100).toInt()}%, Delay=${agent.simulatedTypingDelaySec}s.",
                timestampMillis = System.currentTimeMillis(),
                formattedTime = formatEpochToTime(System.currentTimeMillis()),
                severity = WhatsAppEventSeverity.INFO.name,
                metadataBadge = "Config Saved"
            )
        )
    }

    suspend fun deleteAgent(agent: WhatsAppAgentEntity) = withContext(ioDispatcher) {
        agentDao.deleteAgent(agent)
        logEvent(
            WhatsAppAgentEventEntity(
                agentId = agent.id,
                agentName = agent.name,
                eventType = WhatsAppAgentEventType.STATUS_CHANGED.name,
                title = "Agent Node Removed",
                detail = "Autonomous agent ${agent.name} [${agent.agentSpecialization.displayName}] decommissioned from cluster.",
                timestampMillis = System.currentTimeMillis(),
                formattedTime = formatEpochToTime(System.currentTimeMillis()),
                severity = WhatsAppEventSeverity.WARNING.name,
                metadataBadge = "Node Deleted"
            )
        )
    }

    suspend fun toggleAgentAutonomy(id: String, enabled: Boolean) = withContext(ioDispatcher) {
        agentDao.updateAgentAutonomy(id, enabled)
        val statusText = if (enabled) "ENABLED (Autonomous Mode)" else "DISABLED (Manual Intervention)"
        logEvent(
            WhatsAppAgentEventEntity(
                agentId = id,
                agentName = id.replace("agent_", "").capitalizeName(),
                eventType = WhatsAppAgentEventType.STATUS_CHANGED.name,
                title = "Agent Autonomy Toggled",
                detail = "Autonomous message processing $statusText for node $id.",
                timestampMillis = System.currentTimeMillis(),
                formattedTime = formatEpochToTime(System.currentTimeMillis()),
                severity = if (enabled) WhatsAppEventSeverity.SUCCESS.name else WhatsAppEventSeverity.WARNING.name,
                metadataBadge = if (enabled) "Autonomy ON" else "Autonomy OFF"
            )
        )
    }

    suspend fun updateAgentStatus(id: String, status: AgentOperationalStatus, activeChats: Int) = withContext(ioDispatcher) {
        agentDao.updateAgentStatus(id, status.name, activeChats, System.currentTimeMillis())
        logEvent(
            WhatsAppAgentEventEntity(
                agentId = id,
                agentName = id.replace("agent_", "").capitalizeName(),
                eventType = WhatsAppAgentEventType.STATUS_CHANGED.name,
                title = "Operational State Changed",
                detail = "Agent transitioned to ${status.label} ($activeChats active conversations).",
                timestampMillis = System.currentTimeMillis(),
                formattedTime = formatEpochToTime(System.currentTimeMillis()),
                severity = when (status) {
                    AgentOperationalStatus.ACTIVE_ONLINE -> WhatsAppEventSeverity.SUCCESS.name
                    AgentOperationalStatus.ESCALATION_ALERT -> WhatsAppEventSeverity.ALERT.name
                    AgentOperationalStatus.OFFLINE_PAUSED -> WhatsAppEventSeverity.WARNING.name
                    else -> WhatsAppEventSeverity.INFO.name
                },
                metadataBadge = status.name
            )
        )
    }

    fun toggleMasterKillSwitch(enabled: Boolean) {
        val updated = _globalSettings.value.copy(masterAutonomousActive = enabled)
        saveGlobalSettings(updated)
        appScope.launch(ioDispatcher) {
            agentDao.setMasterAutonomyForAll(enabled)
            logEvent(
                WhatsAppAgentEventEntity(
                    agentId = "system_master",
                    agentName = "Master Cluster",
                    eventType = WhatsAppAgentEventType.KILL_SWITCH_TOGGLED.name,
                    title = if (enabled) "Master Autonomy Activated" else "Master Kill-Switch Activated",
                    detail = if (enabled) {
                        "All autonomous WhatsApp agent nodes resumed online operation."
                    } else {
                        "EMERGENCY KILL-SWITCH: All autonomous agents paused. Inbound messages redirected to owner phone."
                    },
                    timestampMillis = System.currentTimeMillis(),
                    formattedTime = formatEpochToTime(System.currentTimeMillis()),
                    severity = if (enabled) WhatsAppEventSeverity.SUCCESS.name else WhatsAppEventSeverity.ALERT.name,
                    metadataBadge = if (enabled) "Cluster ON" else "KILL-SWITCH ON"
                )
            )
        }
    }

    fun updateGlobalSettings(settings: WhatsAppGlobalSettings) {
        saveGlobalSettings(settings)
        appScope.launch(ioDispatcher) {
            logEvent(
                WhatsAppAgentEventEntity(
                    agentId = "system_global",
                    agentName = "Global System",
                    eventType = WhatsAppAgentEventType.CONFIG_UPDATED.name,
                    title = "Global Settings Updated",
                    detail = "AI Response Delay: ${settings.aiResponseDelayMs}ms, Hours: ${settings.businessHoursStart}-${settings.businessHoursEnd}, AutoSync Calendar=${settings.autoSyncGoogleCalendar}.",
                    timestampMillis = System.currentTimeMillis(),
                    formattedTime = formatEpochToTime(System.currentTimeMillis()),
                    severity = WhatsAppEventSeverity.INFO.name,
                    latencyMs = settings.aiResponseDelayMs,
                    metadataBadge = "${settings.aiResponseDelayMs}ms Delay"
                )
            )
        }
    }

    fun updateGlobalAiResponseDelayMs(delayMs: Long) {
        val clamped = delayMs.coerceIn(100L, 5000L)
        val updated = _globalSettings.value.copy(aiResponseDelayMs = clamped)
        saveGlobalSettings(updated)
        appScope.launch(ioDispatcher) {
            logEvent(
                WhatsAppAgentEventEntity(
                    agentId = "system_global",
                    agentName = "Global System",
                    eventType = WhatsAppAgentEventType.CONFIG_UPDATED.name,
                    title = "AI Response Delay Updated",
                    detail = "Autonomous AI response delay tuned to ${clamped}ms across the WhatsApp agent cluster.",
                    timestampMillis = System.currentTimeMillis(),
                    formattedTime = formatEpochToTime(System.currentTimeMillis()),
                    severity = WhatsAppEventSeverity.INFO.name,
                    latencyMs = clamped,
                    metadataBadge = "${clamped}ms Delay"
                )
            )
        }
    }

    /**
     * Simulates an incoming customer message arriving through the WhatsApp webhook.
     * Transitions the target agent through live operational states and records real history logs.
     */
    suspend fun simulateIncomingCustomerPing(
        agentId: String,
        onStep: (String) -> Unit
    ) = withContext(ioDispatcher) {
        val agent = agentDao.getAgentById(agentId)
        val agentName = when (agentId) {
            "agent_thandiwe_01" -> "Thandiwe"
            "agent_sipho_02" -> "Sipho"
            "agent_nandi_03" -> "Nandi"
            "agent_zama_vip_04" -> "Zama VIP Escort"
            else -> "Autonomous Agent"
        }

        val testCustomerPhone = "+27 82 ${(100..999).random()} ${(1000..9999).random()}"
        val testCustomerName = listOf("Amanda M.", "Thabo S.", "Kagiso N.", "Zintle B.", "Bontle P.").random()
        val customerInquiry = when (agentId) {
            "agent_sipho_02" -> "Hi Sipho! Can I book a fade haircut and wash for Friday at 15:30?"
            "agent_nandi_03" -> "How much for medium knotless braids reaching mid-back with human hair curly ends?"
            "agent_zama_vip_04" -> "I want to book an exclusive VIP private suite session for our bridal shower."
            else -> "Hello! Do you have any slots free tomorrow afternoon for luxury wash and blow?"
        }

        val agentReply = when (agentId) {
            "agent_sipho_02" -> "Hello $testCustomerName! 📅 Friday at 15:30 is open with Stylist Sipho. I have provisionally locked this slot for you (#BK-918)."
            "agent_nandi_03" -> "Hi $testCustomerName! 💎 Mid-back knotless braids with human hair French curls is R1,150. Duration is approx 3 hours. Shall we lock a slot?"
            "agent_zama_vip_04" -> "Warm greetings $testCustomerName! 🛡️ Our VIP bridal suite includes champagne service and 3 dedicated stylists. Escalate to owner Kwanda for executive booking."
            else -> "Hello $testCustomerName! ✨ Yes, tomorrow at 14:00 and 16:30 are both available. Luxury Wash & Silk Press is R550. Which time suits you?"
        }

        onStep("Receiving encrypted WhatsApp webhook packet...")
        logEvent(
            WhatsAppAgentEventEntity(
                agentId = agentId,
                agentName = agentName,
                eventType = WhatsAppAgentEventType.WEBHOOK_RECEIVED.name,
                title = "Webhook Payload Ingested",
                detail = "Meta Cloud API webhook received. Message ID: wamid.HBgM${System.currentTimeMillis()}.",
                customerPhone = testCustomerPhone,
                customerName = testCustomerName,
                timestampMillis = System.currentTimeMillis(),
                formattedTime = formatEpochToTime(System.currentTimeMillis()),
                severity = WhatsAppEventSeverity.INFO.name,
                latencyMs = 118L,
                metadataBadge = "Webhook 200"
            )
        )
        delay(500)

        // Customer Message Inbound
        logEvent(
            WhatsAppAgentEventEntity(
                agentId = agentId,
                agentName = agentName,
                eventType = WhatsAppAgentEventType.MESSAGE_INBOUND.name,
                title = "Inbound WhatsApp Message",
                detail = customerInquiry,
                customerPhone = testCustomerPhone,
                customerName = testCustomerName,
                timestampMillis = System.currentTimeMillis(),
                formattedTime = formatEpochToTime(System.currentTimeMillis()),
                severity = WhatsAppEventSeverity.INFO.name,
                metadataBadge = "WhatsApp Inbound"
            )
        )

        agentDao.updateAgentStatus(agentId, AgentOperationalStatus.BUSY_HANDLING.name, 5, System.currentTimeMillis())
        logEvent(
            WhatsAppAgentEventEntity(
                agentId = agentId,
                agentName = agentName,
                eventType = WhatsAppAgentEventType.STATUS_CHANGED.name,
                title = "Status: In Triage",
                detail = "$agentName synthesizing contextual reply and verifying slot availability.",
                customerPhone = testCustomerPhone,
                customerName = testCustomerName,
                timestampMillis = System.currentTimeMillis(),
                formattedTime = formatEpochToTime(System.currentTimeMillis()),
                severity = WhatsAppEventSeverity.INFO.name,
                metadataBadge = "IN TRIAGE"
            )
        )

        onStep("Autonomous agent reasoning over catalog & availability...")
        val exchangeResult = backendService.exchangeCustomerMessage(
            agentId = agentId,
            customerPhone = testCustomerPhone,
            customerName = testCustomerName,
            messageText = customerInquiry
        )
        val synthesizedReply = when (exchangeResult) {
            is WhatsAppAgentExchangeResult.Success -> exchangeResult.data.replyText.ifBlank { agentReply }
            is WhatsAppAgentExchangeResult.Error -> agentReply
        }
        val synthesizedConfidence = when (exchangeResult) {
            is WhatsAppAgentExchangeResult.Success -> exchangeResult.data.confidenceScore
            is WhatsAppAgentExchangeResult.Error -> (92..99).random() / 100f
        }
        val synthesizedTrace = when (exchangeResult) {
            is WhatsAppAgentExchangeResult.Success -> exchangeResult.data.reasoningTrace
            is WhatsAppAgentExchangeResult.Error ->
                "Inference: Intent detected • Catalog Matched • Zero-Knowledge Verification OK"
        }
        delay(900)

        // Slot or Quote Event
        if (agentId == "agent_sipho_02" || agentId == "agent_thandiwe_01") {
            logEvent(
                WhatsAppAgentEventEntity(
                    agentId = agentId,
                    agentName = agentName,
                    eventType = WhatsAppAgentEventType.SLOT_RESERVED.name,
                    title = "Calendar Slot Provisionally Held",
                    detail = "Reserved tentative calendar slot #BK-${(700..999).random()} for $testCustomerName.",
                    customerPhone = testCustomerPhone,
                    customerName = testCustomerName,
                    timestampMillis = System.currentTimeMillis(),
                    formattedTime = formatEpochToTime(System.currentTimeMillis()),
                    severity = WhatsAppEventSeverity.SUCCESS.name,
                    metadataBadge = "Slot Held"
                )
            )
        }

        onStep("Synthesized personalized response with zero-knowledge verification.")
        val configuredDelayMs = _globalSettings.value.aiResponseDelayMs.coerceIn(100L, 5000L)
        delay(configuredDelayMs.coerceAtMost(1500L))

        // Agent Reply Outbound
        val latency = configuredDelayMs
        logEvent(
            WhatsAppAgentEventEntity(
                agentId = agentId,
                agentName = agentName,
                eventType = WhatsAppAgentEventType.MESSAGE_OUTBOUND.name,
                title = "Autonomous Reply Dispatched",
                detail = synthesizedReply,
                customerPhone = testCustomerPhone,
                customerName = testCustomerName,
                timestampMillis = System.currentTimeMillis(),
                formattedTime = formatEpochToTime(System.currentTimeMillis()),
                severity = WhatsAppEventSeverity.SUCCESS.name,
                latencyMs = latency,
                confidence = synthesizedConfidence,
                aiTrace = "$synthesizedTrace • Sent in ${latency}ms",
                metadataBadge = "✓✓ Dispatched"
            )
        )

        agentDao.updateAgentStatus(agentId, AgentOperationalStatus.ACTIVE_ONLINE.name, 4, System.currentTimeMillis())
        logEvent(
            WhatsAppAgentEventEntity(
                agentId = agentId,
                agentName = agentName,
                eventType = WhatsAppAgentEventType.STATUS_CHANGED.name,
                title = "Status: Online & Serving",
                detail = "$agentName completed transaction and resumed standby listener.",
                timestampMillis = System.currentTimeMillis(),
                formattedTime = formatEpochToTime(System.currentTimeMillis()),
                severity = WhatsAppEventSeverity.SUCCESS.name,
                metadataBadge = "ONLINE"
            )
        )

        val simThreadId = "thread_${agentId}_${testCustomerPhone.filter { it.isDigit() }}"
        val userTime = System.currentTimeMillis() - 1500L
        chatHistoryDao.appendMessageToThread(
            WhatsAppChatMessageEntity(
                messageId = "wamid.user.$userTime",
                threadId = simThreadId,
                agentId = agentId,
                agentName = agentName,
                customerPhone = testCustomerPhone,
                customerName = testCustomerName,
                isFromUser = true,
                senderRole = "USER",
                content = customerInquiry,
                timestampMillis = userTime,
                formattedTime = formatEpochToTime(userTime)
            )
        )
        val replyTime = System.currentTimeMillis()
        chatHistoryDao.appendMessageToThread(
            WhatsAppChatMessageEntity(
                messageId = "wamid.agent.$replyTime",
                threadId = simThreadId,
                agentId = agentId,
                agentName = agentName,
                customerPhone = testCustomerPhone,
                customerName = testCustomerName,
                isFromUser = false,
                senderRole = "WHATSAPP_AGENT",
                content = synthesizedReply,
                timestampMillis = replyTime,
                formattedTime = formatEpochToTime(replyTime),
                confidenceScore = synthesizedConfidence,
                latencyMs = latency,
                aiReasoningTrace = synthesizedTrace
            )
        )

        onStep("Dispatched via WhatsApp Cloud API. Slot held.")
    }

    /**
     * Exchanges a customer message with the autonomous WhatsApp agent via the Retrofit + Moshi
     * [backendService] and persists the inbound/outbound events and thread messages in Room.
     */
    suspend fun exchangeMessageWithAgent(
        agentId: String,
        customerPhone: String,
        customerName: String,
        messageText: String,
        threadId: String = "thread_${agentId}_${customerPhone.filter { it.isDigit() }.ifEmpty { "default" }}"
    ): WhatsAppAgentExchangeResult<WhatsAppAgentMessageResponse> = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val priorHistory = chatHistoryDao.getMessagesForThreadSync(threadId)
            .takeLast(10)
            .map { it.toConversationTurnDto() }

        chatHistoryDao.appendMessageToThread(
            WhatsAppChatMessageEntity(
                messageId = "wamid.user.$now",
                threadId = threadId,
                agentId = agentId,
                agentName = resolveAgentName(agentId),
                customerPhone = customerPhone,
                customerName = customerName,
                isFromUser = true,
                senderRole = "USER",
                content = messageText,
                timestampMillis = now,
                formattedTime = formatEpochToTime(now)
            )
        )

        val result = backendService.exchangeCustomerMessage(
            agentId = agentId,
            customerPhone = customerPhone,
            customerName = customerName,
            messageText = messageText,
            conversationHistory = priorHistory
        )
        if (result is WhatsAppAgentExchangeResult.Success) {
            val data = result.data
            chatHistoryDao.appendMessageToThread(
                message = WhatsAppChatMessageEntity(
                    messageId = data.messageId,
                    threadId = threadId,
                    agentId = agentId,
                    agentName = data.agentName,
                    customerPhone = customerPhone,
                    customerName = customerName,
                    isFromUser = false,
                    senderRole = "WHATSAPP_AGENT",
                    content = data.replyText,
                    timestampMillis = data.timestampMillis,
                    formattedTime = formatEpochToTime(data.timestampMillis),
                    detectedIntent = data.detectedIntent,
                    confidenceScore = data.confidenceScore,
                    latencyMs = data.latencyMs,
                    aiReasoningTrace = data.reasoningTrace,
                    reservedSlotId = data.reservedSlotId
                ),
                topicSummary = data.detectedIntent
            )
            logEvent(
                WhatsAppAgentEventEntity(
                    agentId = agentId,
                    agentName = data.agentName,
                    eventType = WhatsAppAgentEventType.MESSAGE_OUTBOUND.name,
                    title = "Autonomous Reply Dispatched",
                    detail = data.replyText,
                    customerPhone = customerPhone,
                    customerName = customerName,
                    timestampMillis = data.timestampMillis,
                    formattedTime = formatEpochToTime(data.timestampMillis),
                    severity = WhatsAppEventSeverity.SUCCESS.name,
                    latencyMs = data.latencyMs,
                    confidence = data.confidenceScore,
                    aiTrace = data.reasoningTrace,
                    metadataBadge = "✓✓ Dispatched"
                )
            )
        }
        result
    }

    /**
     * Observes the chronological chat history for a specific [threadId].
     */
    fun observeThreadMessages(threadId: String): Flow<List<WhatsAppChatMessageEntity>> {
        return chatHistoryDao.getMessagesForThread(threadId)
    }

    /**
     * Observes a single [WhatsAppThreadWithMessages] (thread metadata + message history).
     */
    fun observeThreadWithMessages(threadId: String): Flow<WhatsAppThreadWithMessages?> {
        return chatHistoryDao.observeThreadWithMessages(threadId)
    }

    /**
     * Observes all message threads assigned to a specific [agentId].
     */
    fun observeThreadsForAgent(agentId: String): Flow<List<WhatsAppChatThreadEntity>> {
        return chatHistoryDao.getThreadsByAgent(agentId)
    }

    /**
     * Persists a message into a conversation thread via [WhatsAppChatHistoryDao].
     */
    suspend fun persistMessageInThread(
        message: WhatsAppChatMessageEntity,
        topicSummary: String? = null
    ): Long = withContext(ioDispatcher) {
        chatHistoryDao.appendMessageToThread(message, topicSummary)
    }

    /**
     * Marks all messages in a thread as read.
     */
    suspend fun markThreadAsRead(threadId: String) = withContext(ioDispatcher) {
        chatHistoryDao.markThreadAsRead(threadId)
    }

    /**
     * Deletes a thread and all of its persisted messages from Room.
     */
    suspend fun deleteThread(threadId: String) = withContext(ioDispatcher) {
        chatHistoryDao.deleteThreadAndMessages(threadId)
    }

    private suspend fun seedDefaultChatThreadsIfEmpty() = withContext(ioDispatcher) {
        if (chatHistoryDao.getThreadCountSync() == 0) {
            val now = System.currentTimeMillis()
            val thread1Id = "thread_thandiwe_lerato_01"
            chatHistoryDao.appendMessageToThread(
                message = WhatsAppChatMessageEntity(
                    messageId = "wamid.seed.t1.m1",
                    threadId = thread1Id,
                    agentId = "agent_thandiwe_01",
                    agentName = "Thandiwe",
                    customerPhone = "+27 82 491 8820",
                    customerName = "Lerato K.",
                    isFromUser = true,
                    senderRole = "USER",
                    content = "Hi! Do you have availability for waist-length knotless braids this Saturday at 2pm?",
                    timestampMillis = now - 72_000,
                    formattedTime = "14:17",
                    detectedIntent = "BOOKING_TRIAGE"
                ),
                topicSummary = "Knotless Braids Saturday Booking"
            )
            chatHistoryDao.appendMessageToThread(
                message = WhatsAppChatMessageEntity(
                    messageId = "wamid.seed.t1.m2",
                    threadId = thread1Id,
                    agentId = "agent_thandiwe_01",
                    agentName = "Thandiwe",
                    customerPhone = "+27 82 491 8820",
                    customerName = "Lerato K.",
                    isFromUser = false,
                    senderRole = "WHATSAPP_AGENT",
                    content = "Hello Lerato! 👋 Yes, we have a slot this Saturday at 14:00 with Stylist Zinzi. Waist-length knotless braids are R850. Would you like me to hold this slot?",
                    timestampMillis = now - 70_000,
                    formattedTime = "14:17",
                    detectedIntent = "BOOKING_TRIAGE",
                    confidenceScore = 0.96f,
                    latencyMs = 780L,
                    aiReasoningTrace = "Intent: BookingSlotRequest • Catalog: Knotless Braids • Calendar: Slot #BK-892 Free",
                    reservedSlotId = "BK-892"
                ),
                topicSummary = "Knotless Braids Saturday Booking"
            )

            val thread2Id = "thread_sipho_reschedule_02"
            chatHistoryDao.appendMessageToThread(
                message = WhatsAppChatMessageEntity(
                    messageId = "wamid.seed.t2.m1",
                    threadId = thread2Id,
                    agentId = "agent_sipho_02",
                    agentName = "Sipho",
                    customerPhone = "+27 83 220 1944",
                    customerName = "Sipho M.",
                    isFromUser = true,
                    senderRole = "USER",
                    content = "Can I move my appointment from 10:00 to 11:30 tomorrow?",
                    timestampMillis = now - 182_000,
                    formattedTime = "14:15",
                    detectedIntent = "RESCHEDULE_REQUEST"
                ),
                topicSummary = "Fade & Beard Appointment Reschedule"
            )
            chatHistoryDao.appendMessageToThread(
                message = WhatsAppChatMessageEntity(
                    messageId = "wamid.seed.t2.m2",
                    threadId = thread2Id,
                    agentId = "agent_sipho_02",
                    agentName = "Sipho",
                    customerPhone = "+27 83 220 1944",
                    customerName = "Sipho M.",
                    isFromUser = false,
                    senderRole = "WHATSAPP_AGENT",
                    content = "Reschedule confirmed! 📅 Your Fade & Beard appointment is now booked for tomorrow Friday at 11:30 with Stylist Jabulani.",
                    timestampMillis = now - 180_000,
                    formattedTime = "14:15",
                    detectedIntent = "RESCHEDULE_REQUEST",
                    confidenceScore = 0.98f,
                    latencyMs = 610L,
                    aiReasoningTrace = "Conflict Resolution: Shifted #BK-774 from 10:00 -> 11:30 • Google Calendar Synced",
                    reservedSlotId = "BK-774"
                ),
                topicSummary = "Fade & Beard Appointment Reschedule"
            )
        }
    }

    private fun resolveAgentName(agentId: String): String = when (agentId) {
        "agent_thandiwe_01" -> "Thandiwe"
        "agent_sipho_02" -> "Sipho"
        "agent_nandi_03" -> "Nandi"
        "agent_zama_vip_04" -> "Zama VIP Escort"
        else -> "Autonomous Agent"
    }

    /**
     * Synchronizes the remote status of an autonomous WhatsApp agent via Retrofit + Moshi.
     */
    suspend fun syncRemoteAgentStatus(
        agentId: String
    ): WhatsAppAgentExchangeResult<WhatsAppAgentStatusDto> = withContext(ioDispatcher) {
        backendService.fetchAgentStatus(agentId)
    }

    suspend fun triggerEscalationEvent(agentId: String, reason: String) = withContext(ioDispatcher) {
        val agentName = when (agentId) {
            "agent_thandiwe_01" -> "Thandiwe"
            "agent_sipho_02" -> "Sipho"
            "agent_nandi_03" -> "Nandi"
            "agent_zama_vip_04" -> "Zama VIP Escort"
            else -> "Autonomous Agent"
        }
        val phone = "+27 82 ${(100..999).random()} ${(1000..9999).random()}"
        logEvent(
            WhatsAppAgentEventEntity(
                agentId = agentId,
                agentName = agentName,
                eventType = WhatsAppAgentEventType.HANDOFF_TRIGGERED.name,
                title = "Escalation Alert: Human Intervention",
                detail = "$reason. Autonomous triage halted; conversation transferred to salon owner Kwanda (+27 82 000 8410).",
                customerPhone = phone,
                customerName = "Client Escalation",
                timestampMillis = System.currentTimeMillis(),
                formattedTime = formatEpochToTime(System.currentTimeMillis()),
                severity = WhatsAppEventSeverity.ALERT.name,
                confidence = 0.52f,
                aiTrace = "Escalation Rule Triggered: Sentiment Dip / Low Confidence. Forwarded to Kwanda.",
                metadataBadge = "OWNER HANDOFF"
            )
        )
        agentDao.updateAgentStatus(agentId, AgentOperationalStatus.ESCALATION_ALERT.name, 3, System.currentTimeMillis())
    }

    suspend fun logEvent(event: WhatsAppAgentEventEntity) = withContext(ioDispatcher) {
        eventDao.insertEvent(event)
    }

    suspend fun clearAllEvents() = withContext(ioDispatcher) {
        eventDao.clearAllEvents()
    }

    private fun formatEpochToTime(epoch: Long): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = epoch
        val hours = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(java.util.Calendar.MINUTE)
        return String.format(java.util.Locale.US, "%02d:%02d", hours, minutes)
    }

    private fun String.capitalizeName(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
    }

    private fun loadGlobalSettings(): WhatsAppGlobalSettings {
        return WhatsAppGlobalSettings(
            masterAutonomousActive = prefs.getBoolean("master_autonomous_active", true),
            businessHoursOnly = prefs.getBoolean("business_hours_only", false),
            businessHoursStart = prefs.getString("business_hours_start", "08:00") ?: "08:00",
            businessHoursEnd = prefs.getString("business_hours_end", "20:00") ?: "20:00",
            autoSyncGoogleCalendar = prefs.getBoolean("auto_sync_calendar", true),
            requireOwnerApprovalForRefunds = prefs.getBoolean("require_owner_refund_approval", true),
            typingIndicatorSimulation = prefs.getBoolean("typing_indicator_sim", true),
            zeroKnowledgeEncryption = prefs.getBoolean("zk_encryption", true),
            emergencyEscalationPhone = prefs.getString("emergency_escalation_phone", "+27 82 000 8410") ?: "+27 82 000 8410",
            dailyChatBudgetCap = prefs.getInt("daily_chat_budget_cap", 500),
            aiResponseDelayMs = prefs.getLong("ai_response_delay_ms", 850L)
        )
    }

    private fun saveGlobalSettings(settings: WhatsAppGlobalSettings) {
        prefs.edit()
            .putBoolean("master_autonomous_active", settings.masterAutonomousActive)
            .putBoolean("business_hours_only", settings.businessHoursOnly)
            .putString("business_hours_start", settings.businessHoursStart)
            .putString("business_hours_end", settings.businessHoursEnd)
            .putBoolean("auto_sync_calendar", settings.autoSyncGoogleCalendar)
            .putBoolean("require_owner_refund_approval", settings.requireOwnerApprovalForRefunds)
            .putBoolean("typing_indicator_sim", settings.typingIndicatorSimulation)
            .putBoolean("zk_encryption", settings.zeroKnowledgeEncryption)
            .putString("emergency_escalation_phone", settings.emergencyEscalationPhone)
            .putInt("daily_chat_budget_cap", settings.dailyChatBudgetCap)
            .putLong("ai_response_delay_ms", settings.aiResponseDelayMs)
            .apply()
        _globalSettings.value = settings
    }
}
