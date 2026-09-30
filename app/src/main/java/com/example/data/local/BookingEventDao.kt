package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for querying, inserting, and updating booking events in Room.
 */
@Dao
interface BookingEventDao {

    @Query("SELECT * FROM booking_events ORDER BY start_epoch_millis ASC")
    fun getAllBookingsFlow(): Flow<List<BookingEventEntity>>

    @Query("SELECT * FROM booking_events WHERE start_epoch_millis >= :nowMillis ORDER BY start_epoch_millis ASC")
    fun getUpcomingBookingsFlow(nowMillis: Long): Flow<List<BookingEventEntity>>

    @Query("SELECT * FROM booking_events WHERE id = :id LIMIT 1")
    suspend fun getBookingById(id: Long): BookingEventEntity?

    @Query("SELECT * FROM booking_events WHERE message_id = :messageId LIMIT 1")
    suspend fun getBookingByMessageId(messageId: String): BookingEventEntity?

    @Query("SELECT COUNT(*) FROM booking_events")
    fun getBookingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM booking_events WHERE synced_to_device = 1 OR status = 'SYNCED_TO_CALENDAR'")
    fun getSyncedBookingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BookingEventEntity>)

    @Update
    suspend fun updateBooking(booking: BookingEventEntity)

    @Query("UPDATE booking_events SET status = 'SYNCED_TO_CALENDAR', synced_to_device = 1, device_calendar_event_id = :deviceEventId WHERE id = :id")
    suspend fun markAsSynced(id: Long, deviceEventId: Long? = null)

    @Query("DELETE FROM booking_events WHERE id = :id")
    suspend fun deleteBookingById(id: Long)

    @Query("DELETE FROM booking_events")
    suspend fun clearAllBookings()
}
