package com.example.data.local

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for autonomous agent message logs and state
 * in the WhatsApp integration.
 */
@Keep
@Dao
interface AutonomousAgentLogDao {

    @Query("SELECT * FROM autonomous_agent_logs ORDER BY timestamp_millis DESC")
    fun getAllLogs(): Flow<List<AutonomousAgentLogEntity>>

    @Query("SELECT * FROM autonomous_agent_logs WHERE thread_id = :threadId ORDER BY timestamp_millis DESC")
    fun getLogsByThread(threadId: String): Flow<List<AutonomousAgentLogEntity>>

    @Query("SELECT * FROM autonomous_agent_logs WHERE customer_phone = :phone ORDER BY timestamp_millis DESC")
    fun getLogsByCustomerPhone(phone: String): Flow<List<AutonomousAgentLogEntity>>

    @Query("SELECT * FROM autonomous_agent_logs WHERE agent_state = :state ORDER BY timestamp_millis DESC")
    fun getLogsByAgentState(state: String): Flow<List<AutonomousAgentLogEntity>>

    @Query("SELECT * FROM autonomous_agent_logs WHERE is_escalated_to_human = 1 ORDER BY timestamp_millis DESC")
    fun getEscalatedLogs(): Flow<List<AutonomousAgentLogEntity>>

    @Query("SELECT * FROM autonomous_agent_logs ORDER BY timestamp_millis DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<AutonomousAgentLogEntity>>

    @Query("SELECT * FROM autonomous_agent_logs WHERE log_id = :logId LIMIT 1")
    fun getLogById(logId: String): Flow<AutonomousAgentLogEntity?>

    @Query("SELECT * FROM autonomous_agent_logs WHERE log_id = :logId LIMIT 1")
    suspend fun getLogByIdSync(logId: String): AutonomousAgentLogEntity?

    @Query("SELECT COUNT(*) FROM autonomous_agent_logs")
    fun getLogCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM autonomous_agent_logs")
    suspend fun getLogCountSync(): Int

    @Query(
        """
        SELECT * FROM autonomous_agent_logs 
        WHERE message_text LIKE '%' || :query || '%' 
           OR agent_reply LIKE '%' || :query || '%' 
           OR customer_name LIKE '%' || :query || '%' 
           OR customer_phone LIKE '%' || :query || '%'
           OR detected_intent LIKE '%' || :query || '%'
        ORDER BY timestamp_millis DESC
        """
    )
    fun searchLogs(query: String): Flow<List<AutonomousAgentLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AutonomousAgentLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AutonomousAgentLogEntity>)

    @Update
    suspend fun updateLog(log: AutonomousAgentLogEntity)

    @Query("UPDATE autonomous_agent_logs SET delivery_status = :status WHERE log_id = :logId")
    suspend fun updateDeliveryStatus(logId: String, status: String)

    @Query("UPDATE autonomous_agent_logs SET agent_state = :state WHERE log_id = :logId")
    suspend fun updateAgentState(logId: String, state: String)

    @Delete
    suspend fun deleteLog(log: AutonomousAgentLogEntity)

    @Query("DELETE FROM autonomous_agent_logs WHERE log_id = :logId")
    suspend fun deleteLogById(logId: String)

    @Query("DELETE FROM autonomous_agent_logs")
    suspend fun clearAllLogs(): Int
}
