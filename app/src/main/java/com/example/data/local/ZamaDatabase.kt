package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ChatMessage::class,
        WhatsAppInteractionEntity::class,
        Conversation::class,
        Message::class,
        BookingEventEntity::class,
        ChatLogEntity::class,
        AutonomousAgentLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class ZamaDatabase : RoomDatabase() {

    abstract fun whatsAppInteractionDao(): WhatsAppInteractionDao
    abstract fun chatDao(): ChatDao
    abstract fun conversationDao(): ConversationDao
    abstract fun bookingEventDao(): BookingEventDao
    abstract fun chatLogDao(): ChatLogDao
    abstract fun autonomousAgentLogDao(): AutonomousAgentLogDao

    companion object {
        @Volatile
        private var INSTANCE: ZamaDatabase? = null

        fun getDatabase(context: Context): ZamaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZamaDatabase::class.java,
                    "zama_whatsapp_agent_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
