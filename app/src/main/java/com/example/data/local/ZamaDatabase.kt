package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.WhatsAppAgentEntity
import com.example.model.WhatsAppAgentEventEntity

/**
 * Main Room Database for the Zama AI application.
 * Persists chat history logs, neural agent traces, and interaction events.
 */
@Database(
    entities = [
        ChatLogEntity::class,
        ChatMessage::class,
        BookingEventEntity::class,
        WhatsAppAgentEntity::class,
        WhatsAppAgentEventEntity::class,
        WhatsAppChatThreadEntity::class,
        WhatsAppChatMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class ZamaDatabase : RoomDatabase() {

    abstract fun chatLogDao(): ChatLogDao
    abstract fun chatDao(): ChatDao
    abstract fun bookingEventDao(): BookingEventDao
    abstract fun whatsAppAgentDao(): WhatsAppAgentDao
    abstract fun whatsAppAgentEventDao(): WhatsAppAgentEventDao
    abstract fun whatsAppChatHistoryDao(): WhatsAppChatHistoryDao

    fun chatThreadDao(): WhatsAppChatHistoryDao = whatsAppChatHistoryDao()

    companion object {
        @Volatile
        private var INSTANCE: ZamaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `booking_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `message_id` TEXT NOT NULL, `client_name` TEXT NOT NULL, `client_phone` TEXT NOT NULL, `service_name` TEXT NOT NULL, `start_epoch_millis` INTEGER NOT NULL, `end_epoch_millis` INTEGER NOT NULL, `quoted_price` TEXT NOT NULL, `duration_minutes` INTEGER NOT NULL, `notes` TEXT NOT NULL, `raw_message_text` TEXT NOT NULL, `status` TEXT NOT NULL, `synced_to_device` INTEGER NOT NULL, `device_calendar_event_id` INTEGER, `created_at_millis` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_booking_events_start_epoch_millis` ON `booking_events` (`start_epoch_millis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_booking_events_status` ON `booking_events` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_booking_events_message_id` ON `booking_events` (`message_id`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `whatsapp_agents` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `specialization` TEXT NOT NULL, `avatar_emoji` TEXT NOT NULL, `phone_line` TEXT NOT NULL, `webhook_url` TEXT NOT NULL, `status` TEXT NOT NULL, `is_autonomous_enabled` INTEGER NOT NULL, `current_active_chats` INTEGER NOT NULL, `max_concurrent_chats` INTEGER NOT NULL, `total_chats_today` INTEGER NOT NULL, `avg_response_latency_ms` INTEGER NOT NULL, `confidence_threshold` REAL NOT NULL, `simulated_typing_delay_sec` REAL NOT NULL, `sentiment_score` REAL NOT NULL, `uptime_percentage` REAL NOT NULL, `allow_voice_note_replies` INTEGER NOT NULL, `allow_auto_calendar_sync` INTEGER NOT NULL, `supported_languages` TEXT NOT NULL, `handoff_trigger` TEXT NOT NULL, `system_prompt_directive` TEXT NOT NULL, `last_active_epoch_millis` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `whatsapp_agent_events` (`id` TEXT NOT NULL, `agent_id` TEXT NOT NULL, `agent_name` TEXT NOT NULL, `event_type` TEXT NOT NULL, `title` TEXT NOT NULL, `detail` TEXT NOT NULL, `customer_phone` TEXT, `customer_name` TEXT, `timestamp_millis` INTEGER NOT NULL, `formatted_time` TEXT NOT NULL, `severity` TEXT NOT NULL, `latency_ms` INTEGER, `confidence` REAL, `ai_trace` TEXT, `metadata_badge` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_agent_events_agent_id` ON `whatsapp_agent_events` (`agent_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_agent_events_event_type` ON `whatsapp_agent_events` (`event_type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_agent_events_timestamp_millis` ON `whatsapp_agent_events` (`timestamp_millis`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `whatsapp_chat_threads` (`thread_id` TEXT NOT NULL, `agent_id` TEXT NOT NULL, `agent_name` TEXT NOT NULL, `customer_phone` TEXT NOT NULL, `customer_name` TEXT NOT NULL, `topic_summary` TEXT NOT NULL, `last_message_preview` TEXT NOT NULL, `last_message_sender` TEXT NOT NULL, `last_message_timestamp_millis` INTEGER NOT NULL, `unread_count` INTEGER NOT NULL, `message_count` INTEGER NOT NULL, `thread_status` TEXT NOT NULL, `reserved_slot_id` TEXT, `is_pinned` INTEGER NOT NULL, `created_at_millis` INTEGER NOT NULL, PRIMARY KEY(`thread_id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_chat_threads_agent_id` ON `whatsapp_chat_threads` (`agent_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_chat_threads_customer_phone` ON `whatsapp_chat_threads` (`customer_phone`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_chat_threads_last_message_timestamp_millis` ON `whatsapp_chat_threads` (`last_message_timestamp_millis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_chat_threads_thread_status` ON `whatsapp_chat_threads` (`thread_status`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `whatsapp_chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `message_id` TEXT NOT NULL, `thread_id` TEXT NOT NULL, `agent_id` TEXT NOT NULL, `agent_name` TEXT NOT NULL, `customer_phone` TEXT NOT NULL, `customer_name` TEXT NOT NULL, `is_from_user` INTEGER NOT NULL, `sender_role` TEXT NOT NULL, `content` TEXT NOT NULL, `message_type` TEXT NOT NULL, `timestamp_millis` INTEGER NOT NULL, `formatted_time` TEXT NOT NULL, `delivery_status` TEXT NOT NULL, `detected_intent` TEXT, `confidence_score` REAL, `latency_ms` INTEGER, `ai_reasoning_trace` TEXT, `reserved_slot_id` TEXT)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_chat_messages_thread_id` ON `whatsapp_chat_messages` (`thread_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_chat_messages_agent_id` ON `whatsapp_chat_messages` (`agent_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_chat_messages_timestamp_millis` ON `whatsapp_chat_messages` (`timestamp_millis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_whatsapp_chat_messages_sender_role` ON `whatsapp_chat_messages` (`sender_role`)")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

        fun getDatabase(context: Context): ZamaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZamaDatabase::class.java,
                    "zama_ai_chat.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

typealias WhatsAppChatDatabase = ZamaDatabase
