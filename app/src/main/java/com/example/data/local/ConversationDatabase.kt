package com.example.data.local

import android.content.Context
import androidx.annotation.Keep
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Dedicated Room database instance for managing Conversation and Message entities,
 * configured with [DateConverter] to ensure proper persistence of java.util.Date objects.
 * Preserved for R8/ProGuard shrinking and minification.
 */
@Keep
@Database(
    entities = [
        Conversation::class,
        Message::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class ConversationDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: ConversationDatabase? = null

        /**
         * Returns the singleton instance of [ConversationDatabase].
         */
        fun getInstance(context: Context): ConversationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConversationDatabase::class.java,
                    "conversation_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
