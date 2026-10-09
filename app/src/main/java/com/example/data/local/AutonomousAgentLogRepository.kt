package com.example.data.local

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Repository to manage autonomous agent message logs and state for the WhatsApp integration.
 */
class AutonomousAgentLogRepository(
    private val logDao: AutonomousAgentLogDao
) {

    val allLogs: Flow<List<AutonomousAgentLogEntity>> = logDao.getAllLogs()
    val escalatedLogs: Flow<List<AutonomousAgentLogEntity>> = logDao.getEscalatedLogs()
    val logCount: Flow<Int> = logDao.getLogCount()

    fun getRecentLogs(limit: Int = 20): Flow<List<AutonomousAgentLogEntity>> {
        return logDao.getRecentLogs(limit)
    }

    fun getLogsByThread(threadId: String): Flow<List<AutonomousAgentLogEntity>> {
        return logDao.getLogsByThread(threadId)
    }

    fun getLogsByCustomerPhone(phone: String): Flow<List<AutonomousAgentLogEntity>> {
        return logDao.getLogsByCustomerPhone(phone)
    }

    fun getLogsByAgentState(state: String): Flow<List<AutonomousAgentLogEntity>> {
        return logDao.getLogsByAgentState(state)
    }

    fun getLogsByState(state: String): Flow<List<AutonomousAgentLogEntity>> {
        return getLogsByAgentState(state)
    }

    fun searchLogs(query: String): Flow<List<AutonomousAgentLogEntity>> {
        return logDao.searchLogs(query)
    }

    suspend fun recordLog(log: AutonomousAgentLogEntity) {
        logDao.insertLog(log)
    }

    suspend fun logAgentInteraction(
        threadId: String,
        customerPhone: String,
        customerName: String,
        messageText: String,
        agentReply: String,
        detectedIntent: String,
        confidenceScore: Float = 0.95f,
        reasoningTrace: String? = null,
        latencyMs: Long = 0L,
        deliveryStatus: String = "DELIVERED",
        agentState: String = "AUTONOMOUS",
        isEscalatedToHuman: Boolean = false,
        escalationReason: String? = null,
        quickActions: List<String>? = null
    ): AutonomousAgentLogEntity {
        val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val entity = AutonomousAgentLogEntity(
            logId = "log_${UUID.randomUUID().toString().take(12)}",
            threadId = threadId,
            customerPhone = customerPhone,
            customerName = customerName,
            messageText = messageText,
            agentReply = agentReply,
            detectedIntent = detectedIntent,
            confidenceScore = confidenceScore,
            reasoningTrace = reasoningTrace,
            latencyMs = latencyMs,
            deliveryStatus = deliveryStatus,
            agentState = agentState,
            isEscalatedToHuman = isEscalatedToHuman,
            escalationReason = escalationReason,
            quickActionsJson = quickActions?.joinToString(prefix = "[", postfix = "]") { "\"$it\"" },
            timestampMillis = System.currentTimeMillis(),
            formattedTime = formattedTime
        )
        logDao.insertLog(entity)
        return entity
    }

    suspend fun updateDeliveryStatus(logId: String, status: String) {
        logDao.updateDeliveryStatus(logId, status)
    }

    suspend fun updateAgentState(logId: String, state: String) {
        logDao.updateAgentState(logId, state)
    }

    suspend fun deleteLogById(logId: String) {
        logDao.deleteLogById(logId)
    }

    suspend fun clearAllLogs(): Int {
        return logDao.clearAllLogs()
    }
}
