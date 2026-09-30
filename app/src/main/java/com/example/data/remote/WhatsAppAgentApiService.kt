package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit HTTP API interface for communicating with the autonomous WhatsApp agent
 * backend cluster (`v1/whatsapp/...`) and the Gemini neural inference endpoint.
 *
 * Uses `MoshiConverterFactory` and `@JsonClass` models in [WhatsAppAgentApiModels.kt]
 * to handle all JSON serialization and deserialization.
 */
interface WhatsAppAgentApiService {

    /**
     * Retrieves the real-time connection and operational status of a specific autonomous WhatsApp agent.
     */
    @GET("v1/whatsapp/agents/{agentId}/status")
    suspend fun getAgentStatus(
        @Path("agentId") agentId: String
    ): WhatsAppAgentStatusDto

    /**
     * Sends a periodic heartbeat signal to verify WhatsApp Cloud Bridge connectivity
     * and maintain the autonomous agent's active session.
     */
    @POST("v1/whatsapp/agents/{agentId}/heartbeat")
    suspend fun sendHeartbeat(
        @Path("agentId") agentId: String,
        @Body request: WhatsAppHeartbeatRequest
    ): WhatsAppHeartbeatResponse

    /**
     * Retrieves the status telemetry of all autonomous WhatsApp agents in the salon cluster.
     */
    @GET("v1/whatsapp/agents/status")
    suspend fun getAllAgentStatuses(): List<WhatsAppAgentStatusDto>

    /**
     * Exchanges a customer message with the autonomous WhatsApp agent and returns the synthesized reply,
     * detected intent, confidence score, and calendar slot reservation details.
     */
    @POST("v1/whatsapp/agents/{agentId}/messages")
    suspend fun sendCustomerMessage(
        @Path("agentId") agentId: String,
        @Body request: WhatsAppAgentMessageRequest
    ): WhatsAppAgentMessageResponse

    /**
     * Updates an autonomous WhatsApp agent's configuration (autonomy toggle, confidence threshold,
     * response delay, voice note & calendar sync flags) on the backend.
     */
    @PUT("v1/whatsapp/agents/{agentId}/config")
    suspend fun updateAgentConfiguration(
        @Path("agentId") agentId: String,
        @Body request: WhatsAppAgentConfigUpdateRequest
    ): WhatsAppAgentStatusDto

    /**
     * Synchronizes the incoming WhatsApp Cloud API webhook queue and returns pending/recent messages.
     */
    @GET("v1/whatsapp/webhooks/sync")
    suspend fun syncIncomingWebhooks(
        @Query("agent_id") agentId: String? = null,
        @Query("limit") limit: Int = 10
    ): WhatsAppWebhookSyncResponse

    /**
     * Ingests a raw WhatsApp Cloud API webhook event for autonomous triage and response synthesis.
     */
    @POST("v1/whatsapp/webhooks/ingest")
    suspend fun ingestWebhookEvent(
        @Body event: WhatsAppWebhookEventDto
    ): WhatsAppAgentMessageResponse

    /**
     * Executes neural inference against the Google Gemini REST API (`gemini-3.5-flash`)
     * using Retrofit and Moshi serialization.
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateAutonomousAgentReply(
        @Path("model") model: String = GeminiAgentService.MODEL_NAME,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequestDto
    ): GeminiGenerateContentResponseDto
}

typealias WhatsAppAgentApi = WhatsAppAgentApiService
typealias WhatsAppAgentRetrofitService = WhatsAppAgentApiService
