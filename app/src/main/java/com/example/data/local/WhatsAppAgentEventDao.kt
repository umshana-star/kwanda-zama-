package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.WhatsAppAgentEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for storing and querying WhatsApp Agent history logs,
 * recent customer/agent messages, and real-time operational event updates.
 */
@Dao
interface WhatsAppAgentEventDao {

    @Query("SELECT * FROM whatsapp_agent_events ORDER BY timestamp_millis DESC")
    fun getAllEvents(): Flow<List<WhatsAppAgentEventEntity>>

    @Query("SELECT * FROM whatsapp_agent_events WHERE agent_id = :agentId ORDER BY timestamp_millis DESC")
    fun getEventsByAgent(agentId: String): Flow<List<WhatsAppAgentEventEntity>>

    @Query("SELECT * FROM whatsapp_agent_events WHERE event_type = :eventType ORDER BY timestamp_millis DESC")
    fun getEventsByType(eventType: String): Flow<List<WhatsAppAgentEventEntity>>

    @Query("SELECT * FROM whatsapp_agent_events ORDER BY timestamp_millis DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<WhatsAppAgentEventEntity>>

    @Query("SELECT COUNT(*) FROM whatsapp_agent_events")
    fun getEventCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM whatsapp_agent_events")
    suspend fun getEventCountSync(): Int

    @Query("""
        SELECT * FROM whatsapp_agent_events 
        WHERE detail LIKE '%' || :query || '%' 
           OR title LIKE '%' || :query || '%' 
           OR agent_name LIKE '%' || :query || '%' 
           OR customer_phone LIKE '%' || :query || '%'
        ORDER BY timestamp_millis DESC
    """)
    fun searchEvents(query: String): Flow<List<WhatsAppAgentEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: WhatsAppAgentEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<WhatsAppAgentEventEntity>)

    @Query("DELETE FROM whatsapp_agent_events")
    suspend fun clearAllEvents(): Int

    @Query("DELETE FROM whatsapp_agent_events WHERE agent_id = :agentId")
    suspend fun clearEventsByAgent(agentId: String): Int
}
