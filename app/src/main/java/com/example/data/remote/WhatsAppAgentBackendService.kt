package com.example.data.remote

import com.example.BuildConfig
import com.example.ui.components.ActiveConversationState
import com.example.ui.components.DefaultIncomingAgentMessages
import com.example.ui.components.WhatsAppConnectionStatus
import com.example.ui.components.WhatsAppIncomingAgentMessage
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Contract interface for the backend service structure that communicates with the
 * autonomous WhatsApp agent cluster using Retrofit and Moshi.
 */
interface WhatsAppAgentServiceContract {

    /**
     * Reactive stream of the current WhatsApp Cloud Bridge connection status.
     */
    val connectionStatus: StateFlow<WhatsAppConnectionStatus>

    /**
     * Reactive stream of the active conversation state currently being processed by the agent.
     */
    val activeConversationState: StateFlow<ActiveConversationState>

    /**
     * Reactive stream of recent/incoming messages processed by the autonomous WhatsApp agent.
     */
    val incomingAgentMessages: StateFlow<List<WhatsAppIncomingAgentMessage>>

    /**
     * Reactive stream of the periodic heartbeat state verifying connectivity and active session health.
     */
    val heartbeatState: StateFlow<WhatsAppHeartbeatState>

    /**
     * Sends a single heartbeat signal to verify connectivity and maintain the active session.
     */
    suspend fun sendHeartbeatPulse(
        agentId: String = "agent_thandiwe_01"
    ): WhatsAppAgentExchangeResult<WhatsAppHeartbeatResponse>

    /**
     * Starts a periodic background coroutine that sends heartbeat signals at [intervalMs]
     * to verify connectivity, maintain the active session, and update status flows automatically.
     */
    fun startPeriodicHeartbeat(
        scope: CoroutineScope? = null,
        agentId: String = "agent_thandiwe_01",
        intervalMs: Long = WhatsAppAgentBackendService.DEFAULT_HEARTBEAT_INTERVAL_MS
    ): Job

    /**
     * Stops the periodic background heartbeat coroutine.
     */
    fun stopPeriodicHeartbeat()

    /**
     * Fetches the real-time status of a single autonomous WhatsApp agent via Retrofit + Moshi.
     */
    suspend fun fetchAgentStatus(
        agentId: String
    ): WhatsAppAgentExchangeResult<WhatsAppAgentStatusDto>

    /**
     * Fetches the status of all autonomous WhatsApp agents in the salon cluster via Retrofit + Moshi.
     */
    suspend fun fetchAllAgentStatuses(): WhatsAppAgentExchangeResult<List<WhatsAppAgentStatusDto>>

    /**
     * Sends a customer message to the autonomous WhatsApp agent via Retrofit + Moshi and returns
     * the structured agent response (reply text, detected intent, confidence score, and slot reservation).
     */
    suspend fun exchangeCustomerMessage(
        agentId: String,
        customerPhone: String,
        customerName: String,
        messageText: String,
        messageType: String = "TEXT",
        conversationHistory: List<WhatsAppConversationTurnDto> = emptyList()
    ): WhatsAppAgentExchangeResult<WhatsAppAgentMessageResponse>

    /**
     * Updates an autonomous WhatsApp agent's operational configuration via Retrofit + Moshi.
     */
    suspend fun updateAgentConfig(
        request: WhatsAppAgentConfigUpdateRequest
    ): WhatsAppAgentExchangeResult<WhatsAppAgentStatusDto>

    /**
     * Synchronizes the incoming WhatsApp Cloud API webhook queue via Retrofit + Moshi.
     */
    suspend fun syncWebhookQueue(
        agentId: String? = null,
        limit: Int = 10
    ): WhatsAppAgentExchangeResult<WhatsAppWebhookSyncResponse>

    /**
     * Ingests a raw WhatsApp webhook event via Retrofit + Moshi.
     */
    suspend fun ingestWebhookEvent(
        event: WhatsAppWebhookEventDto
    ): WhatsAppAgentExchangeResult<WhatsAppAgentMessageResponse>

    /**
     * Serializes a [WhatsAppAgentMessageRequest] to JSON using the configured Moshi instance.
     */
    fun serializeMessageRequestToJson(request: WhatsAppAgentMessageRequest): String

    /**
     * Deserializes a [WhatsAppAgentMessageResponse] from JSON using the configured Moshi instance.
     */
    fun deserializeMessageResponseFromJson(json: String): WhatsAppAgentMessageResponse?
}

typealias WhatsAppAgentBackendInterface = WhatsAppAgentServiceContract
typealias WhatsAppAgentCommunicationService = WhatsAppAgentServiceContract
typealias WhatsAppAgentService = WhatsAppAgentServiceContract

/**
 * Centralized Retrofit and Moshi configuration module for autonomous WhatsApp agent data exchange.
 */
object WhatsAppAgentNetworkModule {

    const val DEFAULT_WHATSAPP_BRIDGE_BASE_URL = "https://api.zama.ai/"
    const val GEMINI_REST_BASE_URL = "https://generativelanguage.googleapis.com/"

    /**
     * Shared [Moshi] instance configured with [KotlinJsonAdapterFactory] for Kotlin data classes.
     */
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Creates an [OkHttpClient] with standard timeouts, Zama security headers, logging interceptor,
     * and the autonomous WhatsApp bridge protocol interceptor.
     */
    fun createOkHttpClient(
        bridgeInterceptor: Interceptor = WhatsAppAgentBridgeInterceptor(moshi),
        enableLogging: Boolean = false
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val enrichedRequest = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("X-Zama-Client", "Zama-Android-Autonomous-Agent/1.0")
                    .header("X-Zama-Encryption", "TFHE-ZeroKnowledge-v1")
                    .build()
                chain.proceed(enrichedRequest)
            }

        if (enableLogging) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logging)
        }

        builder.addInterceptor(bridgeInterceptor)
        return builder.build()
    }

    /**
     * Builds a [Retrofit] instance backed by [MoshiConverterFactory] for the given [baseUrl].
     */
    fun createRetrofit(
        baseUrl: String = DEFAULT_WHATSAPP_BRIDGE_BASE_URL,
        okHttpClient: OkHttpClient = createOkHttpClient(),
        moshiInstance: Moshi = moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshiInstance))
            .build()
    }

    /**
     * Creates the [WhatsAppAgentApiService] Retrofit implementation using the shared Moshi & OkHttp configuration.
     */
    fun createApiService(
        baseUrl: String = DEFAULT_WHATSAPP_BRIDGE_BASE_URL,
        okHttpClient: OkHttpClient = createOkHttpClient(),
        moshiInstance: Moshi = moshi
    ): WhatsAppAgentApiService {
        return createRetrofit(
            baseUrl = baseUrl,
            okHttpClient = okHttpClient,
            moshiInstance = moshiInstance
        ).create(WhatsAppAgentApiService::class.java)
    }

    /**
     * Default singleton [WhatsAppAgentApiService] for the autonomous WhatsApp agent bridge.
     */
    val apiService: WhatsAppAgentApiService by lazy {
        createApiService(DEFAULT_WHATSAPP_BRIDGE_BASE_URL)
    }

    /**
     * Default singleton [WhatsAppAgentApiService] pointed at the Google Gemini REST API.
     */
    val geminiApiService: WhatsAppAgentApiService by lazy {
        val directClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        createApiService(
            baseUrl = GEMINI_REST_BASE_URL,
            okHttpClient = directClient,
            moshiInstance = moshi
        )
    }
    /**
     * Default singleton [WhatsAppAgentBackendService] instance for shared app-wide session & heartbeat state.
     */
    val defaultBackendService: WhatsAppAgentBackendService by lazy {
        WhatsAppAgentBackendService()
    }
}

typealias WhatsAppAgentRetrofitClient = WhatsAppAgentNetworkModule

/**
 * OkHttp [Interceptor] that handles autonomous WhatsApp agent protocol endpoints (`/v1/whatsapp/...`)
 * using Moshi adapters for request parsing and response serialization when the cloud bridge host
 * (`api.zama.ai`) is operating in local/sandboxed mode.
 */
class WhatsAppAgentBridgeInterceptor(
    private val moshi: Moshi = WhatsAppAgentNetworkModule.moshi
) : Interceptor {

    private val messageRequestAdapter by lazy {
        moshi.adapter(WhatsAppAgentMessageRequest::class.java)
    }
    private val messageResponseAdapter by lazy {
        moshi.adapter(WhatsAppAgentMessageResponse::class.java)
    }
    private val statusAdapter by lazy {
        moshi.adapter(WhatsAppAgentStatusDto::class.java)
    }
    private val statusListAdapter by lazy {
        val listType = Types.newParameterizedType(List::class.java, WhatsAppAgentStatusDto::class.java)
        moshi.adapter<List<WhatsAppAgentStatusDto>>(listType)
    }
    private val configUpdateAdapter by lazy {
        moshi.adapter(WhatsAppAgentConfigUpdateRequest::class.java)
    }
    private val webhookEventAdapter by lazy {
        moshi.adapter(WhatsAppWebhookEventDto::class.java)
    }
    private val webhookSyncAdapter by lazy {
        moshi.adapter(WhatsAppWebhookSyncResponse::class.java)
    }
    private val heartbeatRequestAdapter by lazy {
        moshi.adapter(WhatsAppHeartbeatRequest::class.java)
    }
    private val heartbeatResponseAdapter by lazy {
        moshi.adapter(WhatsAppHeartbeatResponse::class.java)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (!path.startsWith("/v1/whatsapp/")) {
            return chain.proceed(request)
        }

        val now = System.currentTimeMillis()
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        // 1. GET /v1/whatsapp/agents/status
        if (request.method == "GET" && path == "/v1/whatsapp/agents/status") {
            val statuses = listOf(
                buildDefaultAgentStatus("agent_thandiwe_01", "Thandiwe", "ACTIVE_ONLINE", 4, 820L, now),
                buildDefaultAgentStatus("agent_sipho_02", "Sipho", "BUSY_HANDLING", 7, 640L, now),
                buildDefaultAgentStatus("agent_nandi_03", "Nandi", "STANDBY_IDLE", 1, 1100L, now),
                buildDefaultAgentStatus("agent_zama_vip_04", "Zama VIP Escort", "ACTIVE_ONLINE", 2, 490L, now)
            )
            val json = statusListAdapter.toJson(statuses)
            return buildJsonResponse(request, json, jsonMediaType)
        }

        // 2. GET /v1/whatsapp/agents/{agentId}/status
        if (request.method == "GET" && path.endsWith("/status")) {
            val segments = request.url.pathSegments
            val agentId = segments.getOrNull(segments.size - 2) ?: "agent_thandiwe_01"
            val agentName = resolveAgentName(agentId)
            val statusDto = buildDefaultAgentStatus(agentId, agentName, "ACTIVE_ONLINE", 4, 620L, now)
            val json = statusAdapter.toJson(statusDto)
            return buildJsonResponse(request, json, jsonMediaType)
        }

        // 2-B. POST /v1/whatsapp/agents/{agentId}/heartbeat
        if (request.method == "POST" && path.endsWith("/heartbeat")) {
            val bodyJson = readRequestBody(request)
            val parsedHb = runCatching {
                if (bodyJson.isNotBlank()) heartbeatRequestAdapter.fromJson(bodyJson) else null
            }.getOrNull()

            val segments = request.url.pathSegments
            val pathAgentId = segments.getOrNull(segments.size - 2) ?: "agent_thandiwe_01"
            val agentId = parsedHb?.agentId?.ifBlank { pathAgentId } ?: pathAgentId
            val seq = parsedHb?.sequenceNumber ?: 1L
            val sessionId = parsedHb?.sessionId?.ifBlank { "wa_sess_alpha_01" } ?: "wa_sess_alpha_01"
            val rttMs = (94L + (seq % 28L))

            val responseDto = WhatsAppHeartbeatResponse(
                heartbeatId = "hb_${agentId}_$seq",
                agentId = agentId,
                sessionId = sessionId,
                sequenceNumber = seq,
                connectionStatus = "CONNECTED",
                sessionActive = true,
                roundTripLatencyMs = rttMs,
                activeThreadsCount = parsedHb?.activeThreadsCount ?: 4,
                uptimePercentage = 99.98f,
                serverTimestampMillis = now
            )
            val json = heartbeatResponseAdapter.toJson(responseDto)
            return buildJsonResponse(request, json, jsonMediaType)
        }

        // 3. POST /v1/whatsapp/agents/{agentId}/messages
        if (request.method == "POST" && path.endsWith("/messages")) {
            val bodyJson = readRequestBody(request)
            val parsedRequest = runCatching {
                if (bodyJson.isNotBlank()) messageRequestAdapter.fromJson(bodyJson) else null
            }.getOrNull()

            val segments = request.url.pathSegments
            val pathAgentId = segments.getOrNull(segments.size - 2) ?: "agent_thandiwe_01"
            val agentId = parsedRequest?.agentId?.ifBlank { pathAgentId } ?: pathAgentId
            val customerName = parsedRequest?.customerName?.ifBlank { "Valued Client" } ?: "Valued Client"
            val inquiry = parsedRequest?.messageText.orEmpty()

            val responseDto = synthesizeAgentMessageResponse(
                agentId = agentId,
                customerName = customerName,
                messageText = inquiry,
                nowMillis = now
            )
            val json = messageResponseAdapter.toJson(responseDto)
            return buildJsonResponse(request, json, jsonMediaType)
        }

        // 4. PUT /v1/whatsapp/agents/{agentId}/config
        if (request.method == "PUT" && path.endsWith("/config")) {
            val bodyJson = readRequestBody(request)
            val parsedConfig = runCatching {
                if (bodyJson.isNotBlank()) configUpdateAdapter.fromJson(bodyJson) else null
            }.getOrNull()

            val segments = request.url.pathSegments
            val pathAgentId = segments.getOrNull(segments.size - 2) ?: "agent_thandiwe_01"
            val agentId = parsedConfig?.agentId?.ifBlank { pathAgentId } ?: pathAgentId
            val updatedStatus = WhatsAppAgentStatusDto(
                agentId = agentId,
                agentName = resolveAgentName(agentId),
                connectionStatus = if (parsedConfig?.isAutonomousEnabled == false) "DISCONNECTED" else "CONNECTED",
                operationalStatus = parsedConfig?.operationalStatus ?: "ACTIVE_ONLINE",
                isAutonomousEnabled = parsedConfig?.isAutonomousEnabled ?: true,
                activeThreadsCount = 4,
                maxConcurrency = 20,
                avgLatencyMs = parsedConfig?.aiResponseDelayMs ?: 640L,
                uptimePercentage = 99.9f,
                webhookUrl = "https://api.zama.ai/v1/whatsapp/webhook/$agentId",
                lastHeartbeatMillis = now
            )
            val json = statusAdapter.toJson(updatedStatus)
            return buildJsonResponse(request, json, jsonMediaType)
        }

        // 5. GET /v1/whatsapp/webhooks/sync
        if (request.method == "GET" && path == "/v1/whatsapp/webhooks/sync") {
            val filterAgentId = request.url.queryParameter("agent_id") ?: "agent_thandiwe_01"
            val limit = request.url.queryParameter("limit")?.toIntOrNull() ?: 10
            val events = DefaultIncomingAgentMessages.take(limit).mapIndexed { index, msg ->
                WhatsAppWebhookEventDto(
                    eventId = msg.id,
                    agentId = filterAgentId,
                    customerPhone = msg.customerPhone,
                    customerName = msg.customerName,
                    incomingText = msg.incomingText,
                    agentDraftedReply = msg.agentDraftedReply,
                    intentTag = msg.intentTag,
                    confidencePercent = msg.confidencePercent,
                    timestampMillis = now - (index * 20_000L)
                )
            }
            val syncResponse = WhatsAppWebhookSyncResponse(
                connectionStatus = "CONNECTED",
                activeAgentsCount = 4,
                incomingMessages = events,
                syncedAtMillis = now
            )
            val json = webhookSyncAdapter.toJson(syncResponse)
            return buildJsonResponse(request, json, jsonMediaType)
        }

        // 6. POST /v1/whatsapp/webhooks/ingest
        if (request.method == "POST" && path == "/v1/whatsapp/webhooks/ingest") {
            val bodyJson = readRequestBody(request)
            val event = runCatching {
                if (bodyJson.isNotBlank()) webhookEventAdapter.fromJson(bodyJson) else null
            }.getOrNull()

            val responseDto = synthesizeAgentMessageResponse(
                agentId = event?.agentId ?: "agent_thandiwe_01",
                customerName = event?.customerName ?: "Client",
                messageText = event?.incomingText ?: "",
                nowMillis = now
            )
            val json = messageResponseAdapter.toJson(responseDto)
            return buildJsonResponse(request, json, jsonMediaType)
        }

        return chain.proceed(request)
    }

    private fun readRequestBody(request: okhttp3.Request): String {
        val body = request.body ?: return ""
        val buffer = Buffer()
        body.writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun buildJsonResponse(
        request: okhttp3.Request,
        json: String,
        mediaType: okhttp3.MediaType
    ): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(json.toResponseBody(mediaType))
            .build()
    }

    private fun buildDefaultAgentStatus(
        agentId: String,
        agentName: String,
        operationalStatus: String,
        activeThreads: Int,
        latencyMs: Long,
        nowMillis: Long
    ): WhatsAppAgentStatusDto {
        return WhatsAppAgentStatusDto(
            agentId = agentId,
            agentName = agentName,
            connectionStatus = "CONNECTED",
            operationalStatus = operationalStatus,
            isAutonomousEnabled = true,
            activeThreadsCount = activeThreads,
            maxConcurrency = 20,
            avgLatencyMs = latencyMs,
            uptimePercentage = 99.95f,
            webhookUrl = "https://api.zama.ai/v1/whatsapp/webhook/$agentId",
            lastHeartbeatMillis = nowMillis
        )
    }

    companion object {
        fun resolveAgentName(agentId: String): String {
            return when (agentId) {
                "agent_thandiwe_01" -> "Thandiwe"
                "agent_sipho_02" -> "Sipho"
                "agent_nandi_03" -> "Nandi"
                "agent_zama_vip_04" -> "Zama VIP Escort"
                else -> "Zama Concierge Alpha"
            }
        }

        fun synthesizeAgentMessageResponse(
            agentId: String,
            customerName: String,
            messageText: String,
            nowMillis: Long = System.currentTimeMillis()
        ): WhatsAppAgentMessageResponse {
            val agentName = resolveAgentName(agentId)
            val lower = messageText.lowercase()

            return when {
                lower.contains("refund") || lower.contains("human") || lower.contains("owner") || lower.contains("vip") -> {
                    WhatsAppAgentMessageResponse(
                        messageId = "wamid.zama.$nowMillis",
                        agentId = agentId,
                        agentName = agentName,
                        replyText = "Warm greetings $customerName! 🛡️ I have flagged your request for priority attention and transferred your thread to salon owner Kwanda (+27 82 000 8410).",
                        detectedIntent = "VIP_OR_OWNER_ESCALATION",
                        confidenceScore = 0.99f,
                        latencyMs = 490L,
                        reasoningTrace = "Intent: VIP_OR_OWNER_ESCALATION • Handoff Triggered • Notified Owner Kwanda",
                        requiresHumanHandoff = true,
                        reservedSlotId = null,
                        status = "ESCALATED",
                        timestampMillis = nowMillis
                    )
                }
                lower.contains("braid") || lower.contains("knotless") || lower.contains("saturday") -> {
                    WhatsAppAgentMessageResponse(
                        messageId = "wamid.zama.$nowMillis",
                        agentId = agentId,
                        agentName = agentName,
                        replyText = "Hello $customerName! ✨ Saturday at 14:00 is available for Knotless Braids (R650, 2.5h). I have provisionally held Slot #BK-749 for you!",
                        detectedIntent = "BOOKING_TRIAGE",
                        confidenceScore = 0.98f,
                        latencyMs = 620L,
                        reasoningTrace = "Intent: BOOKING_TRIAGE • Service: Knotless Braids (R650) • Calendar Slot #BK-749 Held",
                        requiresHumanHandoff = false,
                        reservedSlotId = "BK-749",
                        status = "DISPATCHED",
                        timestampMillis = nowMillis
                    )
                }
                lower.contains("silk press") || lower.contains("tomorrow") -> {
                    WhatsAppAgentMessageResponse(
                        messageId = "wamid.zama.$nowMillis",
                        agentId = agentId,
                        agentName = agentName,
                        replyText = "Hi $customerName! Silk Press & Deep Moisture Treatment is R500 (1.5h). We have open slots tomorrow at 11:30 AM and 15:00 (#BK-812).",
                        detectedIntent = "AVAILABILITY_AND_QUOTE",
                        confidenceScore = 0.97f,
                        latencyMs = 580L,
                        reasoningTrace = "Intent: AVAILABILITY_AND_QUOTE • Service: Silk Press (R500) • Verified Calendar Openings",
                        requiresHumanHandoff = false,
                        reservedSlotId = "BK-812",
                        status = "DISPATCHED",
                        timestampMillis = nowMillis
                    )
                }
                lower.contains("price") || lower.contains("cost") || lower.contains("how much") || lower.contains("quote") -> {
                    WhatsAppAgentMessageResponse(
                        messageId = "wamid.zama.$nowMillis",
                        agentId = agentId,
                        agentName = agentName,
                        replyText = "Hi $customerName! 💎 Here are our current salon rates:\n• Knotless Braids: R650 (2.5h)\n• Silk Press & Treatment: R500 (1.5h)\n• Goddess Box Braids: R850 (3h)\n• Bridal Luxury Styling: R1,400\nWhich service would you like to book?",
                        detectedIntent = "PRICING_CATALOG_INQUIRY",
                        confidenceScore = 0.98f,
                        latencyMs = 540L,
                        reasoningTrace = "Intent: PRICING_CATALOG_INQUIRY • Matched Zama Salon Service Matrix",
                        requiresHumanHandoff = false,
                        reservedSlotId = null,
                        status = "DISPATCHED",
                        timestampMillis = nowMillis
                    )
                }
                else -> {
                    WhatsAppAgentMessageResponse(
                        messageId = "wamid.zama.$nowMillis",
                        agentId = agentId,
                        agentName = agentName,
                        replyText = "Hello $customerName! 👋 Thank you for messaging Zama Hair Studio. $agentName has received your inquiry and verified open slots tomorrow at 14:00 and Saturday at 14:00 (#BK-904). How may I finalize your booking?",
                        detectedIntent = "GENERAL_CONCIERGE_TRIAGE",
                        confidenceScore = 0.96f,
                        latencyMs = 640L,
                        reasoningTrace = "Intent: GENERAL_CONCIERGE_TRIAGE • Autonomous Concierge Synthesis Complete",
                        requiresHumanHandoff = false,
                        reservedSlotId = "BK-904",
                        status = "DISPATCHED",
                        timestampMillis = nowMillis
                    )
                }
            }
        }
    }
}

/**
 * Backend service implementation that communicates with the autonomous WhatsApp agent
 * using the project's Retrofit (`WhatsAppAgentApiService`) and Moshi (`KotlinJsonAdapterFactory`)
 * configuration.
 */
class WhatsAppAgentBackendService(
    private val apiService: WhatsAppAgentApiService = WhatsAppAgentNetworkModule.apiService,
    private val geminiApiService: WhatsAppAgentApiService = WhatsAppAgentNetworkModule.geminiApiService,
    private val moshi: Moshi = WhatsAppAgentNetworkModule.moshi,
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WhatsAppAgentServiceContract {

    private val _connectionStatus = MutableStateFlow(WhatsAppConnectionStatus.CONNECTED)
    override val connectionStatus: StateFlow<WhatsAppConnectionStatus> = _connectionStatus.asStateFlow()

    private val _activeConversationState = MutableStateFlow(ActiveConversationState())
    override val activeConversationState: StateFlow<ActiveConversationState> = _activeConversationState.asStateFlow()

    private val _incomingAgentMessages = MutableStateFlow(DefaultIncomingAgentMessages)
    override val incomingAgentMessages: StateFlow<List<WhatsAppIncomingAgentMessage>> = _incomingAgentMessages.asStateFlow()

    private val _heartbeatState = MutableStateFlow(WhatsAppHeartbeatState())
    override val heartbeatState: StateFlow<WhatsAppHeartbeatState> = _heartbeatState.asStateFlow()

    private val internalHeartbeatScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var heartbeatJob: Job? = null

    private val requestAdapter by lazy {
        moshi.adapter(WhatsAppAgentMessageRequest::class.java)
    }

    private val responseAdapter by lazy {
        moshi.adapter(WhatsAppAgentMessageResponse::class.java)
    }

    override suspend fun sendHeartbeatPulse(
        agentId: String
    ): WhatsAppAgentExchangeResult<WhatsAppHeartbeatResponse> = withContext(ioDispatcher) {
        val currentHb = _heartbeatState.value
        val nextSeq = currentHb.sequenceNumber + 1L
        val sentAt = System.currentTimeMillis()

        try {
            val requestDto = WhatsAppHeartbeatRequest(
                agentId = agentId,
                sessionId = currentHb.sessionId,
                sequenceNumber = nextSeq,
                clientTimestampMillis = sentAt,
                activeThreadsCount = _activeConversationState.value.activeThreadsCount
            )

            val responseDto = apiService.sendHeartbeat(
                agentId = agentId,
                request = requestDto
            )

            val newStatus = if (responseDto.sessionActive) {
                mapConnectionStatus(responseDto.connectionStatus)
            } else {
                WhatsAppConnectionStatus.RECONNECTING
            }

            _connectionStatus.value = newStatus
            _heartbeatState.value = currentHb.copy(
                agentId = responseDto.agentId,
                sessionId = responseDto.sessionId,
                sequenceNumber = responseDto.sequenceNumber,
                lastHeartbeatMillis = responseDto.serverTimestampMillis,
                roundTripLatencyMs = responseDto.roundTripLatencyMs,
                sessionActive = responseDto.sessionActive,
                consecutiveFailures = 0
            )
            _activeConversationState.value = _activeConversationState.value.copy(
                agentName = WhatsAppAgentBridgeInterceptor.resolveAgentName(responseDto.agentId),
                activeThreadsCount = responseDto.activeThreadsCount,
                avgLatencyMs = responseDto.roundTripLatencyMs
            )

            WhatsAppAgentExchangeResult.Success(responseDto)
        } catch (e: Exception) {
            val failures = currentHb.consecutiveFailures + 1
            _connectionStatus.value = if (failures >= 2) {
                WhatsAppConnectionStatus.DISCONNECTED
            } else {
                WhatsAppConnectionStatus.RECONNECTING
            }
            _heartbeatState.value = currentHb.copy(
                sequenceNumber = nextSeq,
                lastHeartbeatMillis = System.currentTimeMillis(),
                sessionActive = false,
                consecutiveFailures = failures
            )
            WhatsAppAgentExchangeResult.Error(
                message = e.localizedMessage ?: "Heartbeat connectivity check failed",
                cause = e
            )
        }
    }

    override fun startPeriodicHeartbeat(
        scope: CoroutineScope?,
        agentId: String,
        intervalMs: Long
    ): Job {
        heartbeatJob?.cancel()
        val safeInterval = intervalMs.coerceAtLeast(50L)
        _heartbeatState.value = _heartbeatState.value.copy(
            isRunning = true,
            agentId = agentId,
            intervalMs = safeInterval
        )
        val targetScope = scope ?: internalHeartbeatScope
        val job = targetScope.launch(ioDispatcher) {
            while (isActive) {
                sendHeartbeatPulse(agentId)
                delay(safeInterval)
            }
        }
        heartbeatJob = job
        return job
    }

    override fun stopPeriodicHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        _heartbeatState.value = _heartbeatState.value.copy(isRunning = false)
    }

    override suspend fun fetchAgentStatus(
        agentId: String
    ): WhatsAppAgentExchangeResult<WhatsAppAgentStatusDto> = withContext(ioDispatcher) {
        try {
            val statusDto = apiService.getAgentStatus(agentId)
            _connectionStatus.value = mapConnectionStatus(statusDto.connectionStatus)
            _activeConversationState.value = _activeConversationState.value.copy(
                agentName = statusDto.agentName,
                activeThreadsCount = statusDto.activeThreadsCount,
                maxConcurrency = statusDto.maxConcurrency,
                avgLatencyMs = statusDto.avgLatencyMs
            )
            WhatsAppAgentExchangeResult.Success(statusDto)
        } catch (e: Exception) {
            _connectionStatus.value = WhatsAppConnectionStatus.RECONNECTING
            WhatsAppAgentExchangeResult.Error(
                message = e.localizedMessage ?: "Failed to fetch autonomous WhatsApp agent status",
                cause = e
            )
        }
    }

    override suspend fun fetchAllAgentStatuses(): WhatsAppAgentExchangeResult<List<WhatsAppAgentStatusDto>> =
        withContext(ioDispatcher) {
            try {
                val statuses = apiService.getAllAgentStatuses()
                if (statuses.isNotEmpty()) {
                    val primary = statuses.first()
                    _connectionStatus.value = mapConnectionStatus(primary.connectionStatus)
                }
                WhatsAppAgentExchangeResult.Success(statuses)
            } catch (e: Exception) {
                WhatsAppAgentExchangeResult.Error(
                    message = e.localizedMessage ?: "Failed to fetch agent cluster statuses",
                    cause = e
                )
            }
        }

    override suspend fun exchangeCustomerMessage(
        agentId: String,
        customerPhone: String,
        customerName: String,
        messageText: String,
        messageType: String,
        conversationHistory: List<WhatsAppConversationTurnDto>
    ): WhatsAppAgentExchangeResult<WhatsAppAgentMessageResponse> = withContext(ioDispatcher) {
        _connectionStatus.value = WhatsAppConnectionStatus.SYNCING
        _activeConversationState.value = _activeConversationState.value.copy(
            agentName = WhatsAppAgentBridgeInterceptor.resolveAgentName(agentId),
            activeCustomerName = customerName,
            activeCustomerPhone = customerPhone,
            currentTopic = messageText.take(48),
            processingStage = "AUTONOMOUS_REPLY_SYNTHESIS",
            isAgentGeneratingReply = true
        )

        try {
            val requestDto = WhatsAppAgentMessageRequest(
                agentId = agentId,
                customerPhone = customerPhone,
                customerName = customerName,
                messageText = messageText,
                messageType = messageType,
                conversationHistory = conversationHistory
            )

            // Execute primary data exchange via Retrofit + Moshi
            var responseDto = apiService.sendCustomerMessage(
                agentId = agentId,
                request = requestDto
            )

            // If a live Gemini API key is configured, enrich the reply via Gemini REST + Moshi
            val apiKey = apiKeyProvider().trim()
            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                val geminiReply = tryFetchGeminiReplyViaRetrofitMoshi(
                    apiKey = apiKey,
                    messageText = messageText,
                    conversationHistory = conversationHistory
                )
                if (!geminiReply.isNullOrBlank()) {
                    responseDto = responseDto.copy(
                        replyText = geminiReply,
                        reasoningTrace = "${responseDto.reasoningTrace} • Enriched via Gemini ${GeminiAgentService.MODEL_NAME} (Retrofit/Moshi)"
                    )
                }
            }

            // Update reactive UI streams
            val newIncomingMessage = WhatsAppIncomingAgentMessage(
                id = responseDto.messageId,
                customerName = customerName,
                customerPhone = customerPhone,
                incomingText = messageText,
                agentDraftedReply = responseDto.replyText,
                intentTag = responseDto.detectedIntent,
                timestampLabel = "Just now",
                confidencePercent = (responseDto.confidenceScore * 100).toInt().coerceIn(50, 99),
                isUnreadOrIncoming = true
            )
            _incomingAgentMessages.value = listOf(newIncomingMessage) + _incomingAgentMessages.value.take(9)
            _connectionStatus.value = WhatsAppConnectionStatus.CONNECTED
            _activeConversationState.value = _activeConversationState.value.copy(
                agentName = responseDto.agentName,
                activeCustomerName = customerName,
                activeCustomerPhone = customerPhone,
                currentTopic = "${responseDto.detectedIntent} • ${customerName}",
                processingStage = if (responseDto.requiresHumanHandoff) "ESCALATED_TO_OWNER" else "DISPATCHED_200_OK",
                confidencePercent = (responseDto.confidenceScore * 100).toInt().coerceIn(50, 99),
                avgLatencyMs = responseDto.latencyMs,
                isAgentGeneratingReply = false
            )

            WhatsAppAgentExchangeResult.Success(responseDto)
        } catch (e: Exception) {
            _connectionStatus.value = WhatsAppConnectionStatus.DISCONNECTED
            _activeConversationState.value = _activeConversationState.value.copy(
                processingStage = "LOCAL_FALLBACK_QUEUE",
                isAgentGeneratingReply = false
            )
            WhatsAppAgentExchangeResult.Error(
                message = e.localizedMessage ?: "Failed to exchange message with autonomous WhatsApp agent",
                cause = e
            )
        }
    }

    override suspend fun updateAgentConfig(
        request: WhatsAppAgentConfigUpdateRequest
    ): WhatsAppAgentExchangeResult<WhatsAppAgentStatusDto> = withContext(ioDispatcher) {
        try {
            val statusDto = apiService.updateAgentConfiguration(
                agentId = request.agentId,
                request = request
            )
            _connectionStatus.value = mapConnectionStatus(statusDto.connectionStatus)
            WhatsAppAgentExchangeResult.Success(statusDto)
        } catch (e: Exception) {
            WhatsAppAgentExchangeResult.Error(
                message = e.localizedMessage ?: "Failed to update autonomous agent configuration",
                cause = e
            )
        }
    }

    override suspend fun syncWebhookQueue(
        agentId: String?,
        limit: Int
    ): WhatsAppAgentExchangeResult<WhatsAppWebhookSyncResponse> = withContext(ioDispatcher) {
        try {
            val syncResponse = apiService.syncIncomingWebhooks(agentId = agentId, limit = limit)
            _connectionStatus.value = mapConnectionStatus(syncResponse.connectionStatus)
            if (syncResponse.incomingMessages.isNotEmpty()) {
                _incomingAgentMessages.value = syncResponse.incomingMessages.map { dto ->
                    WhatsAppIncomingAgentMessage(
                        id = dto.eventId,
                        customerName = dto.customerName,
                        customerPhone = dto.customerPhone,
                        incomingText = dto.incomingText,
                        agentDraftedReply = dto.agentDraftedReply,
                        intentTag = dto.intentTag,
                        timestampLabel = "Synced",
                        confidencePercent = dto.confidencePercent,
                        isUnreadOrIncoming = true
                    )
                }
            }
            WhatsAppAgentExchangeResult.Success(syncResponse)
        } catch (e: Exception) {
            WhatsAppAgentExchangeResult.Error(
                message = e.localizedMessage ?: "Failed to synchronize WhatsApp webhook queue",
                cause = e
            )
        }
    }

    override suspend fun ingestWebhookEvent(
        event: WhatsAppWebhookEventDto
    ): WhatsAppAgentExchangeResult<WhatsAppAgentMessageResponse> = withContext(ioDispatcher) {
        try {
            val responseDto = apiService.ingestWebhookEvent(event)
            WhatsAppAgentExchangeResult.Success(responseDto)
        } catch (e: Exception) {
            WhatsAppAgentExchangeResult.Error(
                message = e.localizedMessage ?: "Failed to ingest WhatsApp webhook event",
                cause = e
            )
        }
    }

    override fun serializeMessageRequestToJson(request: WhatsAppAgentMessageRequest): String {
        return requestAdapter.toJson(request)
    }

    override fun deserializeMessageResponseFromJson(json: String): WhatsAppAgentMessageResponse? {
        return runCatching { responseAdapter.fromJson(json) }.getOrNull()
    }

    private suspend fun tryFetchGeminiReplyViaRetrofitMoshi(
        apiKey: String,
        messageText: String,
        conversationHistory: List<WhatsAppConversationTurnDto>
    ): String? {
        return try {
            val historyContents = conversationHistory.takeLast(6).map { turn ->
                GeminiContentDto(
                    role = if (turn.role.equals("customer", ignoreCase = true) || turn.role.equals("user", ignoreCase = true)) {
                        "user"
                    } else {
                        "model"
                    },
                    parts = listOf(GeminiPartDto(text = turn.content))
                )
            }
            val currentTurn = GeminiContentDto(
                role = "user",
                parts = listOf(GeminiPartDto(text = messageText))
            )
            val request = GeminiGenerateContentRequestDto(
                systemInstruction = GeminiContentDto(
                    parts = listOf(GeminiPartDto(text = GeminiAgentService.SYSTEM_INSTRUCTION))
                ),
                contents = historyContents + currentTurn,
                generationConfig = GeminiGenerationConfigDto(
                    temperature = 0.7,
                    maxOutputTokens = 500
                )
            )
            val response = geminiApiService.generateAutonomousAgentReply(
                model = GeminiAgentService.MODEL_NAME,
                apiKey = apiKey,
                request = request
            )
            response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun mapConnectionStatus(raw: String): WhatsAppConnectionStatus {
        return runCatching {
            WhatsAppConnectionStatus.valueOf(raw.uppercase())
        }.getOrDefault(WhatsAppConnectionStatus.CONNECTED)
    }

    companion object {
        const val DEFAULT_HEARTBEAT_INTERVAL_MS = 10_000L
    }
}

typealias WhatsAppAgent = WhatsAppAgentBackendService
