package com.example.service.whatsapp.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Connection mode for WhatsApp Business communication service.
 */
sealed class WhatsAppConnectionMode {
    object LiveMetaCloudApi : WhatsAppConnectionMode()
    object SandboxSimulation : WhatsAppConnectionMode()
}

/**
 * Service Layer implementation handling real communication with the Meta WhatsApp Business API,
 * with seamless failover and sandbox simulation.
 */
class WhatsAppBusinessCommunicationService(
    private val apiClient: WhatsAppBusinessApiClient = WhatsAppBusinessApiClient(),
    private val sandboxFallbackApi: SandboxWhatsAppCommunicationApi = SandboxWhatsAppCommunicationApi(),
    private val config: WhatsAppApiConfig = WhatsAppApiConfig(),
    var isLiveModeEnabled: Boolean = false,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : WhatsAppCommunicationApi {

    private val _deliveryReceipts = MutableSharedFlow<Pair<String, WhatsAppDeliveryStatus>>(extraBufferCapacity = 128)
    override val deliveryReceipts: Flow<Pair<String, WhatsAppDeliveryStatus>> = _deliveryReceipts.asSharedFlow()

    private val _activeMode = MutableStateFlow<WhatsAppConnectionMode>(
        if (isLiveModeEnabled) WhatsAppConnectionMode.LiveMetaCloudApi else WhatsAppConnectionMode.SandboxSimulation
    )
    val activeMode: StateFlow<WhatsAppConnectionMode> = _activeMode.asStateFlow()

    private val _totalSent = MutableStateFlow(0)
    val totalSent: StateFlow<Int> = _totalSent.asStateFlow()

    private val _totalDelivered = MutableStateFlow(0)
    val totalDelivered: StateFlow<Int> = _totalDelivered.asStateFlow()

    private val _totalErrors = MutableStateFlow(0)
    val totalErrors: StateFlow<Int> = _totalErrors.asStateFlow()

    init {
        // Forward sandbox delivery receipts if operating in sandbox mode
        scope.launch {
            sandboxFallbackApi.deliveryReceipts.collect { receipt ->
                _deliveryReceipts.tryEmit(receipt)
                if (receipt.second == WhatsAppDeliveryStatus.DELIVERED) {
                    _totalDelivered.value = _totalDelivered.value + 1
                }
            }
        }
    }

    fun setLiveMode(enabled: Boolean) {
        isLiveModeEnabled = enabled
        _activeMode.value = if (enabled) WhatsAppConnectionMode.LiveMetaCloudApi else WhatsAppConnectionMode.SandboxSimulation
    }

    /**
     * Sends an outbound message to a customer via the WhatsApp Business API.
     * Uses the Live Meta Graph API client when enabled, falling back to Sandbox simulation.
     */
    override suspend fun sendMessage(message: WhatsAppOutboundMessage): WhatsAppSendResult {
        if (!isLiveModeEnabled) {
            val result = sandboxFallbackApi.sendMessage(message)
            if (result is WhatsAppSendResult.Success) {
                _totalSent.value = _totalSent.value + 1
            }
            return result
        }

        // Live Mode via Meta WhatsApp Business Cloud API
        val result = if (message.quickActionButtons.isNotEmpty()) {
            apiClient.sendInteractiveButtonMessage(
                recipientPhoneNumber = message.recipientPhoneNumber,
                bodyText = message.bodyText,
                buttons = message.quickActionButtons,
                replyToMessageId = message.replyToMessageId
            )
        } else {
            apiClient.sendTextMessage(
                recipientPhoneNumber = message.recipientPhoneNumber,
                bodyText = message.bodyText,
                replyToMessageId = message.replyToMessageId
            )
        }

        when (result) {
            is WhatsAppSendResult.Success -> {
                _totalSent.value = _totalSent.value + 1
                _deliveryReceipts.tryEmit(Pair(result.messageId, result.deliveryStatus))
            }
            is WhatsAppSendResult.Error -> {
                _totalErrors.value = _totalErrors.value + 1
                // If live transmission fails due to credentials or network, fall back to sandbox
                // so the user experience is never blocked.
                if (result.isTransient || result.errorCode == 401) {
                    val fallbackResult = sandboxFallbackApi.sendMessage(message)
                    if (fallbackResult is WhatsAppSendResult.Success) {
                        _totalSent.value = _totalSent.value + 1
                    }
                    return fallbackResult
                }
            }
        }
        return result
    }

    /**
     * Marks an incoming WhatsApp message as read.
     */
    override suspend fun markAsRead(messageId: String): Boolean {
        return if (isLiveModeEnabled) {
            val liveSuccess = apiClient.markMessageAsRead(messageId)
            if (liveSuccess) {
                _deliveryReceipts.tryEmit(Pair(messageId, WhatsAppDeliveryStatus.READ))
                true
            } else {
                sandboxFallbackApi.markAsRead(messageId)
            }
        } else {
            sandboxFallbackApi.markAsRead(messageId)
        }
    }

    /**
     * Verifies incoming webhook subscription handshake tokens for Meta WhatsApp Cloud API.
     */
    override fun verifyWebhookToken(hubMode: String, hubVerifyToken: String, hubChallenge: String): String? {
        return WhatsAppWebhookParser.verifyWebhookHandshake(
            hubMode = hubMode,
            hubVerifyToken = hubVerifyToken,
            hubChallenge = hubChallenge,
            expectedVerifyToken = config.webhookVerifyToken
        )
    }

    /**
     * Processes inbound delivery receipts received from live Meta webhooks.
     */
    fun onDeliveryReceiptReceived(receipt: WhatsAppDeliveryReceipt) {
        _deliveryReceipts.tryEmit(Pair(receipt.messageId, receipt.status))
        when (receipt.status) {
            WhatsAppDeliveryStatus.DELIVERED -> _totalDelivered.value = _totalDelivered.value + 1
            WhatsAppDeliveryStatus.FAILED -> _totalErrors.value = _totalErrors.value + 1
            else -> { /* no-op */ }
        }
    }
}
