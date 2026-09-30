package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Database @Entity representing a parsed booking event extracted from customer triage messages.
 * Stores appointment scheduling details, client contact, service metadata, and calendar sync state.
 */
@Entity(
    tableName = "booking_events",
    indices = [
        Index(value = ["start_epoch_millis"]),
        Index(value = ["status"]),
        Index(value = ["message_id"])
    ]
)
data class BookingEventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "client_name")
    val clientName: String,

    @ColumnInfo(name = "client_phone")
    val clientPhone: String = "+27 82 555 0192",

    @ColumnInfo(name = "service_name")
    val serviceName: String,

    @ColumnInfo(name = "start_epoch_millis")
    val startEpochMillis: Long,

    @ColumnInfo(name = "end_epoch_millis")
    val endEpochMillis: Long,

    @ColumnInfo(name = "quoted_price")
    val quotedPrice: String,

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int = 180,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "raw_message_text")
    val rawMessageText: String = "",

    @ColumnInfo(name = "status")
    val status: String = STATUS_CONFIRMED,

    @ColumnInfo(name = "synced_to_device")
    val syncedToDevice: Boolean = false,

    @ColumnInfo(name = "device_calendar_event_id")
    val deviceCalendarEventId: Long? = null,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_CONFIRMED = "CONFIRMED"
        const val STATUS_SYNCED = "SYNCED_TO_CALENDAR"
        const val STATUS_PENDING = "PENDING_CONFIRMATION"
        const val STATUS_CANCELLED = "CANCELLED"
    }

    val isSynced: Boolean
        get() = syncedToDevice || status == STATUS_SYNCED
}
