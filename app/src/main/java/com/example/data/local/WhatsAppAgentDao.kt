package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.WhatsAppAgentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for managing autonomous WhatsApp agents.
 */
@Dao
interface WhatsAppAgentDao {

    @Query("SELECT * FROM whatsapp_agents ORDER BY last_active_epoch_millis DESC")
    fun getAllAgents(): Flow<List<WhatsAppAgentEntity>>

    @Query("SELECT * FROM whatsapp_agents WHERE id = :id LIMIT 1")
    fun getAgentById(id: String): Flow<WhatsAppAgentEntity?>

    @Query("SELECT * FROM whatsapp_agents WHERE status = :status ORDER BY name ASC")
    fun getAgentsByStatus(status: String): Flow<List<WhatsAppAgentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: WhatsAppAgentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgents(agents: List<WhatsAppAgentEntity>)

    @Update
    suspend fun updateAgent(agent: WhatsAppAgentEntity)

    @Delete
    suspend fun deleteAgent(agent: WhatsAppAgentEntity)

    @Query("DELETE FROM whatsapp_agents WHERE id = :id")
    suspend fun deleteAgentById(id: String)

    @Query("UPDATE whatsapp_agents SET is_autonomous_enabled = :enabled WHERE id = :id")
    suspend fun updateAgentAutonomy(id: String, enabled: Boolean)

    @Query("UPDATE whatsapp_agents SET status = :status, current_active_chats = :activeChats, last_active_epoch_millis = :timestamp WHERE id = :id")
    suspend fun updateAgentStatus(id: String, status: String, activeChats: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE whatsapp_agents SET is_autonomous_enabled = :enabled")
    suspend fun setMasterAutonomyForAll(enabled: Boolean)

    @Query("SELECT COUNT(*) FROM whatsapp_agents")
    suspend fun getAgentCount(): Int
}
