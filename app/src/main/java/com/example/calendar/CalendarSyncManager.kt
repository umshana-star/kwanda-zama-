package com.example.calendar

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.local.BookingEventDao
import com.example.data.local.BookingEventEntity
import com.example.data.local.ZamaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Manager handling business calendar synchronization:
 * - Parsing incoming triage messages into structured booking records
 * - Dispatching native Android CalendarContract insertion intents
 * - Direct Calendar Provider synchronization (when permission is granted)
 * - RFC 5545 iCalendar (.ics) export & sharing
 */
object CalendarSyncManager {

    const val SALON_DEFAULT_LOCATION = "Zama Hair & Beauty Studio, 14 Florida Rd, Durban"

    /**
     * Creates an Intent to launch the device's default Calendar application
     * (e.g., Google Calendar, Samsung Calendar) pre-filled with all parsed appointment details.
     */
    fun createCalendarInsertIntent(booking: BookingEventEntity): Intent {
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "${booking.serviceName} — ${booking.clientName}")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                buildString {
                    append("💈 Service: ${booking.serviceName}\n")
                    append("👤 Client: ${booking.clientName} (${booking.clientPhone})\n")
                    append("💰 Quoted Price: ${booking.quotedPrice}\n")
                    append("⏱️ Duration: ${booking.durationMinutes} min\n")
                    if (booking.notes.isNotBlank()) append("📝 Notes: ${booking.notes}\n")
                    if (booking.rawMessageText.isNotBlank()) append("💬 WhatsApp Inquiry: \"${booking.rawMessageText}\"\n")
                    append("🤖 Synced by Zama AI Autonomous Employee")
                }
            )
            putExtra(CalendarContract.Events.EVENT_LOCATION, SALON_DEFAULT_LOCATION)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, booking.startEpochMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, booking.endEpochMillis)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            putExtra(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Attempts direct background insertion into Android Calendar Provider
     * if WRITE_CALENDAR runtime permission has been granted.
     */
    fun insertDirectlyToCalendarProvider(context: Context, booking: BookingEventEntity): Long? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return null

        return try {
            val contentResolver = context.contentResolver

            // 1. Locate primary calendar ID
            var calendarId = 1L
            val cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val idCol = it.getColumnIndex(CalendarContract.Calendars._ID)
                    if (idCol != -1) {
                        calendarId = it.getLong(idCol)
                    }
                }
            }

            // 2. Insert Event
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "${booking.serviceName} — ${booking.clientName}")
                put(CalendarContract.Events.DESCRIPTION, "Zama AI Synced: ${booking.quotedPrice} • ${booking.notes}")
                put(CalendarContract.Events.EVENT_LOCATION, SALON_DEFAULT_LOCATION)
                put(CalendarContract.Events.DTSTART, booking.startEpochMillis)
                put(CalendarContract.Events.DTEND, booking.endEpochMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            }

            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a standard RFC 5545 iCalendar (.ics) file containing the booking appointments.
     */
    fun exportIcsCalendarFile(context: Context, bookings: List<BookingEventEntity>): Uri? {
        return try {
            val icsDateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val icsBuilder = StringBuilder().apply {
                append("BEGIN:VCALENDAR\r\n")
                append("VERSION:2.0\r\n")
                append("PRODID:-//Zama AI//Salon Calendar Sync//EN\r\n")
                append("CALSCALE:GREGORIAN\r\n")
                append("METHOD:PUBLISH\r\n")
                append("X-WR-CALNAME:Zama AI Business Bookings\r\n")

                for (booking in bookings) {
                    val startUtc = icsDateFormat.format(Date(booking.startEpochMillis))
                    val endUtc = icsDateFormat.format(Date(booking.endEpochMillis))
                    val nowUtc = icsDateFormat.format(Date())

                    append("BEGIN:VEVENT\r\n")
                    append("UID:zama-booking-${booking.id}-${booking.startEpochMillis}@zama.ai\r\n")
                    append("DTSTAMP:$nowUtc\r\n")
                    append("DTSTART:$startUtc\r\n")
                    append("DTEND:$endUtc\r\n")
                    append("SUMMARY:${escapeIcs(booking.serviceName + " — " + booking.clientName)}\r\n")
                    append("DESCRIPTION:${escapeIcs("Client: " + booking.clientName + "\\nPrice: " + booking.quotedPrice + "\\nNotes: " + booking.notes)}\r\n")
                    append("LOCATION:${escapeIcs(SALON_DEFAULT_LOCATION)}\r\n")
                    append("STATUS:CONFIRMED\r\n")
                    append("END:VEVENT\r\n")
                }

                append("END:VCALENDAR\r\n")
            }

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val icsFile = File(exportDir, "zama_salon_bookings.ics")
            FileOutputStream(icsFile).use { out ->
                out.write(icsBuilder.toString().toByteArray(Charsets.UTF_8))
            }

            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    icsFile
                )
            } catch (_: IllegalArgumentException) {
                Uri.fromFile(icsFile)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun escapeIcs(value: String): String {
        return value.replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n")
    }

    /**
     * Parses a chat message for booking signals and automatically inserts it into the database.
     */
    suspend fun parseAndIngestMessage(
        context: Context,
        messageId: String,
        messageText: String,
        clientName: String = "Valued Customer",
        clientPhone: String = "+27 82 555 0192"
    ): BookingEventEntity? = withContext(Dispatchers.IO) {
        val parsed = BookingTriageParser.parse(messageText, fallbackClientName = clientName) ?: return@withContext null
        val dao = ZamaDatabase.getDatabase(context).bookingEventDao()

        // Prevent duplicate creation for the same message
        val existing = dao.getBookingByMessageId(messageId)
        if (existing != null) {
            return@withContext existing
        }

        val newBooking = BookingEventEntity(
            messageId = messageId,
            clientName = parsed.clientName,
            clientPhone = clientPhone,
            serviceName = parsed.serviceName,
            startEpochMillis = parsed.startEpochMillis,
            endEpochMillis = parsed.endEpochMillis,
            quotedPrice = parsed.quotedPrice,
            durationMinutes = parsed.durationMinutes,
            notes = parsed.notes,
            rawMessageText = messageText,
            status = BookingEventEntity.STATUS_CONFIRMED,
            syncedToDevice = false
        )

        val insertedId = dao.insertBooking(newBooking)
        newBooking.copy(id = insertedId)
    }

    /**
     * Seeds initial demonstration booking appointments extracted from triage messages
     * to ensure the business owner immediately sees real, interactive scheduled events.
     */
    suspend fun seedInitialBookingsIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        val dao = ZamaDatabase.getDatabase(context).bookingEventDao()
        val now = System.currentTimeMillis()

        // Check if database already has bookings
        val initialList = listOf(
            createSampleBooking(
                messageId = "demo_msg_01",
                clientName = "Sarah Ndlovu",
                clientPhone = "+27 82 491 8234",
                serviceName = "Medium Knotless Braids",
                daysFromNow = 1,
                hourOfDay = 14,
                durationMin = 180,
                quotedPrice = "R650",
                notes = "Waist length • Black 1B hair provided",
                rawText = "Hi Zama! Can I book medium knotless braids for tomorrow at 2pm? Waist length please.",
                synced = true
            ),
            createSampleBooking(
                messageId = "demo_msg_02",
                clientName = "Thandiwe Khumalo",
                clientPhone = "+27 71 883 9102",
                serviceName = "Silk Press & Deep Treatment",
                daysFromNow = 2,
                hourOfDay = 10,
                durationMin = 90,
                quotedPrice = "R450",
                notes = "Includes hydration steam wash",
                rawText = "Good day, I'd like to book a silk press and deep condition treatment for Friday at 10am.",
                synced = true
            ),
            createSampleBooking(
                messageId = "demo_msg_03",
                clientName = "Lerato Mokoena",
                clientPhone = "+27 83 221 4455",
                serviceName = "Custom Wig Install & Styling",
                daysFromNow = 3,
                hourOfDay = 13,
                durationMin = 120,
                quotedPrice = "R850",
                notes = "HD Lace frontal • Body wave styling",
                rawText = "Hello! Please reserve a slot for a custom wig install this Saturday at 1pm.",
                synced = false
            ),
            createSampleBooking(
                messageId = "demo_msg_04",
                clientName = "Nomvula Cele",
                clientPhone = "+27 79 334 0019",
                serviceName = "Precision Haircut & Lineup",
                daysFromNow = 4,
                hourOfDay = 11,
                durationMin = 45,
                quotedPrice = "R250",
                notes = "Fade & sharp edge cleanup",
                rawText = "Can I book a haircut appointment for Sunday 11am?",
                synced = false
            )
        )

        for (booking in initialList) {
            val exists = dao.getBookingByMessageId(booking.messageId)
            if (exists == null) {
                dao.insertBooking(booking)
            }
        }
    }

    private fun createSampleBooking(
        messageId: String,
        clientName: String,
        clientPhone: String,
        serviceName: String,
        daysFromNow: Int,
        hourOfDay: Int,
        durationMin: Int,
        quotedPrice: String,
        notes: String,
        rawText: String,
        synced: Boolean
    ): BookingEventEntity {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysFromNow)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = cal.timeInMillis
        val endMillis = startMillis + (durationMin * 60 * 1000L)

        return BookingEventEntity(
            messageId = messageId,
            clientName = clientName,
            clientPhone = clientPhone,
            serviceName = serviceName,
            startEpochMillis = startMillis,
            endEpochMillis = endMillis,
            quotedPrice = quotedPrice,
            durationMinutes = durationMin,
            notes = notes,
            rawMessageText = rawText,
            status = if (synced) BookingEventEntity.STATUS_SYNCED else BookingEventEntity.STATUS_CONFIRMED,
            syncedToDevice = synced
        )
    }
}
