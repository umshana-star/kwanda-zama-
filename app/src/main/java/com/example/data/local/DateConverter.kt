package com.example.data.local

import androidx.annotation.Keep
import androidx.room.TypeConverter
import java.util.Date

/**
 * Room TypeConverter for mapping java.util.Date objects to Long timestamps and vice versa
 * to ensure proper database persistence for entities with date fields.
 * Preserved for R8/ProGuard shrinking and minification.
 */
@Keep
class DateConverter {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
