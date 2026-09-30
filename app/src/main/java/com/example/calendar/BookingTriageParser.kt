package com.example.calendar

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Data representation of a parsed booking extracted from customer triage messages.
 */
data class ParsedBookingInfo(
    val clientName: String,
    val serviceName: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val durationMinutes: Int,
    val quotedPrice: String,
    val notes: String,
    val confidence: Float,
    val formattedDateTime: String
)

/**
 * Intelligent parser that extracts structured appointment booking details
 * from incoming customer WhatsApp / triage messages.
 */
object BookingTriageParser {

    private val SERVICE_CATALOG = listOf(
        ServiceDefinition(
            serviceName = "Medium Knotless Braids",
            keywords = listOf("knotless", "knotless braids", "braids", "box braids", "cornrows", "plaits"),
            durationMinutes = 180,
            defaultPrice = "R650"
        ),
        ServiceDefinition(
            serviceName = "Silk Press & Deep Treatment",
            keywords = listOf("silk press", "press", "blow out", "blowdry", "flat iron", "straighten"),
            durationMinutes = 90,
            defaultPrice = "R450"
        ),
        ServiceDefinition(
            serviceName = "Custom Wig Install & Styling",
            keywords = listOf("wig install", "wig", "lace frontal", "closure install", "glue down", "frontal"),
            durationMinutes = 120,
            defaultPrice = "R850"
        ),
        ServiceDefinition(
            serviceName = "Precision Haircut & Lineup",
            keywords = listOf("haircut", "cut", "trim", "fade", "lineup", "taper", "scissor cut"),
            durationMinutes = 45,
            defaultPrice = "R250"
        ),
        ServiceDefinition(
            serviceName = "Hydration Wash & Scalp Treatment",
            keywords = listOf("wash", "deep condition", "scalp treatment", "treatment", "detangle", "steam"),
            durationMinutes = 60,
            defaultPrice = "R300"
        )
    )

    private val WEEKDAYS = mapOf(
        "sunday" to Calendar.SUNDAY,
        "monday" to Calendar.MONDAY,
        "tuesday" to Calendar.TUESDAY,
        "wednesday" to Calendar.WEDNESDAY,
        "thursday" to Calendar.THURSDAY,
        "friday" to Calendar.FRIDAY,
        "saturday" to Calendar.SATURDAY
    )

    /**
     * Determines whether the given text contains booking signals.
     */
    fun isBookingRelated(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val bookingTriggers = listOf(
            "book", "booking", "appointment", "reserve", "slot", "available", "schedule",
            "saturday", "sunday", "tomorrow", "friday", "next week", "can i get"
        )
        return bookingTriggers.any { lower.contains(it) }
    }

    /**
     * Parses the message text into structured booking information.
     */
    fun parse(
        messageText: String,
        referenceTimeMillis: Long = System.currentTimeMillis(),
        fallbackClientName: String = "Valued Customer"
    ): ParsedBookingInfo? {
        val lower = messageText.lowercase(Locale.ROOT)

        // 1. Detect Requested Service
        var matchedService: ServiceDefinition? = null
        for (service in SERVICE_CATALOG) {
            if (service.keywords.any { lower.contains(it) }) {
                matchedService = service
                break
            }
        }

        val serviceName = matchedService?.serviceName ?: "Salon Styling Appointment"
        val durationMinutes = matchedService?.durationMinutes ?: 120

        // 2. Extract Quoted / Mentioned Price
        val priceRegex = Pattern.compile("(?:r|zar|rand)\\s*(\\d+[,\\d]*)", Pattern.CASE_INSENSITIVE)
        val priceMatcher = priceRegex.matcher(messageText)
        val quotedPrice = if (priceMatcher.find()) {
            "R${priceMatcher.group(1)}"
        } else {
            matchedService?.defaultPrice ?: "R500"
        }

        // 3. Extract Client Name (if present)
        var clientName = fallbackClientName
        val nameRegex = Pattern.compile("(?:name is|i am|from|this is)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)", Pattern.CASE_INSENSITIVE)
        val nameMatcher = nameRegex.matcher(messageText)
        if (nameMatcher.find()) {
            clientName = nameMatcher.group(1)?.trim() ?: fallbackClientName
        }

        // 4. Extract Date and Time
        val calendar = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
        }

        // Check for specific weekday
        var weekdayFound = false
        for ((dayName, dayConstant) in WEEKDAYS) {
            if (lower.contains(dayName)) {
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                var daysToAdd = dayConstant - currentDayOfWeek
                if (daysToAdd <= 0) {
                    daysToAdd += 7 // Schedule for upcoming weekday
                }
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                weekdayFound = true
                break
            }
        }

        if (!weekdayFound) {
            if (lower.contains("tomorrow")) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            } else if (lower.contains("today")) {
                // Today remains
            } else if (lower.contains("next week")) {
                calendar.add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        // Parse Time of Day (e.g., "2pm", "2:30pm", "14:00", "10am")
        var hour = 10 // default 10:00 AM
        var minute = 0

        val time12hRegex = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)", Pattern.CASE_INSENSITIVE)
        val time12hMatcher = time12hRegex.matcher(messageText)

        val time24hRegex = Pattern.compile("(?:at\\s+)?([01]?\\d|2[0-3]):([0-5]\\d)")
        val time24hMatcher = time24hRegex.matcher(messageText)

        if (time12hMatcher.find()) {
            val rawHour = time12hMatcher.group(1)?.toIntOrNull() ?: 10
            val rawMinute = time12hMatcher.group(2)?.toIntOrNull() ?: 0
            val amPm = time12hMatcher.group(3)?.lowercase(Locale.ROOT) ?: "am"

            hour = when {
                amPm == "pm" && rawHour < 12 -> rawHour + 12
                amPm == "am" && rawHour == 12 -> 0
                else -> rawHour
            }
            minute = rawMinute
        } else if (time24hMatcher.find()) {
            hour = time24hMatcher.group(1)?.toIntOrNull() ?: 10
            minute = time24hMatcher.group(2)?.toIntOrNull() ?: 0
        } else {
            // General time indicators
            if (lower.contains("morning")) hour = 9
            else if (lower.contains("afternoon")) hour = 14
            else if (lower.contains("evening")) hour = 17
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startEpochMillis = calendar.timeInMillis
        val endEpochMillis = startEpochMillis + (durationMinutes * 60 * 1000L)

        // 5. Extract Notes / Style specifics
        val notesList = mutableListOf<String>()
        if (lower.contains("waist length") || lower.contains("waist")) notesList.add("Waist length requested")
        if (lower.contains("chest length")) notesList.add("Chest length")
        if (lower.contains("butt length") || lower.contains("bum length")) notesList.add("Bum length")
        if (lower.contains("with wash") || lower.contains("wash included")) notesList.add("Wash included")
        if (lower.contains("deposit")) notesList.add("Deposit discussed")
        val notes = if (notesList.isNotEmpty()) notesList.joinToString(" • ") else "Auto-parsed from WhatsApp inquiry"

        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault())
        val formattedDateTime = dateFormat.format(Date(startEpochMillis))

        // Confidence estimation
        val hasDirectKeywords = matchedService != null && (weekdayFound || lower.contains("tomorrow") || time12hMatcher.reset().find() || time24hMatcher.reset().find())
        val confidence = if (hasDirectKeywords) 0.95f else 0.70f

        return ParsedBookingInfo(
            clientName = clientName,
            serviceName = serviceName,
            startEpochMillis = startEpochMillis,
            endEpochMillis = endEpochMillis,
            durationMinutes = durationMinutes,
            quotedPrice = quotedPrice,
            notes = notes,
            confidence = confidence,
            formattedDateTime = formattedDateTime
        )
    }

    private data class ServiceDefinition(
        val serviceName: String,
        val keywords: List<String>,
        val durationMinutes: Int,
        val defaultPrice: String
    )
}
