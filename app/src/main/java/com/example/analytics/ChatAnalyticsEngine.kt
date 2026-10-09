package com.example.analytics

import com.example.data.local.ChatLogEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Sentiment classification for chat messages.
 */
enum class SentimentCategory(val label: String, val colorHex: String) {
    POSITIVE("Positive", "#00E676"),
    INQUISITIVE("Inquisitive", "#00E5FF"),
    NEUTRAL("Neutral", "#9CA3AF"),
    CONCERNED("Concerned", "#FF5252")
}

/**
 * Hourly bucket data point for activity frequency visualization.
 */
data class HourlyActivityPoint(
    val hourOfDay: Int,          // 0..23
    val hourLabel: String,       // e.g. "14:00"
    val customerCount: Int,
    val aiCount: Int,
    val voiceCount: Int,
    val totalCount: Int,
    val averageSentiment: Float  // -1.0f .. +1.0f
)

/**
 * Chronological sentiment trend data point.
 */
data class SentimentTrendPoint(
    val index: Int,
    val messageId: String,
    val timeFormatted: String,
    val timestampMillis: Long,
    val isCustomer: Boolean,
    val textSnippet: String,
    val sentimentScore: Float,   // -1.0f .. +1.0f
    val category: SentimentCategory,
    val intentTag: String
)

/**
 * Aggregated analytics results computed from Room Database chat logs.
 */
data class ChatAnalyticsSummary(
    val totalMessages: Int,
    val customerMessages: Int,
    val aiMessages: Int,
    val voiceNotes: Int,
    val averageSentiment: Float,         // -1.0 .. +1.0
    val positivePercentage: Int,         // 0 .. 100
    val inquisitivePercentage: Int,
    val neutralPercentage: Int,
    val concernedPercentage: Int,
    val peakActivityHour: String,
    val bookingIntentCount: Int,
    val hourlyFrequency: List<HourlyActivityPoint>,
    val sentimentTrend: List<SentimentTrendPoint>
)

/**
 * Analytics engine that calculates activity frequency histograms,
 * sentiment trajectories, and KPI metrics directly from Room database chat logs.
 */
object ChatAnalyticsEngine {

    private val POSITIVE_KEYWORDS = listOf(
        "love", "thanks", "thank", "great", "perfect", "yes", "please",
        "book", "reserve", "awesome", "excited", "happy", "confirmed",
        "available", "wonderful", "cool", "super", "ready", "sweet", "good"
    )

    private val NEGATIVE_KEYWORDS = listOf(
        "cancel", "bad", "expensive", "wait", "late", "terrible", "problem",
        "angry", "disappointed", "complaint", "fail", "no", "wrong", "delay",
        "unhappy", "costly", "nevermind", "stop"
    )

    private val INQUISITIVE_KEYWORDS = listOf(
        "how much", "what time", "where", "price", "pricing", "can i", "do you",
        "available", "when", "cost", "slot", "hours", "duration", "which"
    )

    /**
     * Computes a sentiment score between -1.0 and +1.0 for a given chat entity.
     */
    fun analyzeMessageSentiment(entity: ChatLogEntity): Float {
        val lowerText = entity.text.lowercase(Locale.ROOT)
        val trace = entity.aiTrace?.lowercase(Locale.ROOT) ?: ""

        var score = 0.2f // baseline slightly positive for polite inquiries

        // Check positive keywords
        var posMatches = 0
        for (keyword in POSITIVE_KEYWORDS) {
            if (lowerText.contains(keyword)) posMatches++
        }

        // Check negative keywords
        var negMatches = 0
        for (keyword in NEGATIVE_KEYWORDS) {
            if (lowerText.contains(keyword)) negMatches++
        }

        // Check inquisitive cues
        var inqMatches = 0
        for (keyword in INQUISITIVE_KEYWORDS) {
            if (lowerText.contains(keyword)) inqMatches++
        }

        if (posMatches > 0) {
            score += (posMatches * 0.35f).coerceAtMost(0.7f)
        }
        if (negMatches > 0) {
            score -= (negMatches * 0.45f).coerceAtMost(0.9f)
        }
        if (inqMatches > 0 && negMatches == 0) {
            score += 0.15f
        }

        // Boost for confirmed booking or slot reservation traces
        if (trace.contains("reserved") || trace.contains("confirmed") || trace.contains("bookingrequest")) {
            score = (score + 0.35f).coerceAtMost(0.98f)
        }
        if (entity.isActionCard) {
            score = (score + 0.3f).coerceAtMost(0.95f)
        }

        return score.coerceIn(-1.0f, 1.0f)
    }

    /**
     * Determines the categorical classification from a raw sentiment score.
     */
    fun classifySentiment(score: Float, text: String): SentimentCategory {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            score <= -0.15f -> SentimentCategory.CONCERNED
            score >= 0.45f -> SentimentCategory.POSITIVE
            INQUISITIVE_KEYWORDS.any { lower.contains(it) } -> SentimentCategory.INQUISITIVE
            else -> SentimentCategory.NEUTRAL
        }
    }

    /**
     * Extracts a concise intent tag from the message or AI neural trace.
     */
    fun extractIntentTag(entity: ChatLogEntity): String {
        val trace = entity.aiTrace ?: ""
        return when {
            trace.contains("VoiceAppointmentBooking") -> "Voice Booking"
            trace.contains("RelativeDateQuery") -> "Date Query"
            trace.contains("PricingInquiry") -> "Pricing"
            trace.contains("BookingRequest") -> "Booking"
            trace.contains("GeneralInquiry") -> "Catalog"
            entity.text.contains("braid", ignoreCase = true) -> "Braids Service"
            entity.text.contains("silk", ignoreCase = true) -> "Silk Press"
            entity.actionDetail?.startsWith("VOICE_INPUT") == true -> "Voice Note"
            entity.isActionCard -> "Action Card"
            else -> if (entity.isFromCustomer) "Customer Inquiry" else "AI Assistance"
        }
    }

    /**
     * Aggregates a list of Room ChatLogEntity items into complete analytics.
     */
    fun computeAnalytics(entities: List<ChatLogEntity>): ChatAnalyticsSummary {
        if (entities.isEmpty()) {
            return ChatAnalyticsSummary(
                totalMessages = 0,
                customerMessages = 0,
                aiMessages = 0,
                voiceNotes = 0,
                averageSentiment = 0.5f,
                positivePercentage = 100,
                inquisitivePercentage = 0,
                neutralPercentage = 0,
                concernedPercentage = 0,
                peakActivityHour = "14:00 - 15:00",
                bookingIntentCount = 0,
                hourlyFrequency = defaultHourlyBuckets(),
                sentimentTrend = emptyList()
            )
        }

        val total = entities.size
        var customerCount = 0
        var aiCount = 0
        var voiceCount = 0
        var bookingCount = 0

        var sumSentiment = 0f
        var positiveCount = 0
        var inquisitiveCount = 0
        var neutralCount = 0
        var concernedCount = 0

        // 24-hour frequency counters
        val hourlyCustomer = IntArray(24)
        val hourlyAi = IntArray(24)
        val hourlyVoice = IntArray(24)
        val hourlySentimentSum = FloatArray(24)
        val hourlyTotal = IntArray(24)

        val trendList = mutableListOf<SentimentTrendPoint>()
        val calendar = Calendar.getInstance()

        entities.forEachIndexed { index, entity ->
            if (entity.isFromCustomer) customerCount++ else aiCount++
            if (entity.actionDetail?.startsWith("VOICE_INPUT") == true) voiceCount++

            val score = analyzeMessageSentiment(entity)
            sumSentiment += score

            val category = classifySentiment(score, entity.text)
            when (category) {
                SentimentCategory.POSITIVE -> positiveCount++
                SentimentCategory.INQUISITIVE -> inquisitiveCount++
                SentimentCategory.NEUTRAL -> neutralCount++
                SentimentCategory.CONCERNED -> concernedCount++
            }

            val intent = extractIntentTag(entity)
            if (intent.contains("Booking") || entity.text.contains("book", ignoreCase = true)) {
                bookingCount++
            }

            // Map timestamp to hour of day (fallback to formatted time string parsing)
            calendar.timeInMillis = entity.timestampMillis
            var hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (hour !in 0..23) {
                val parsed = entity.timestampFormatted.split(":").firstOrNull()?.toIntOrNull()
                hour = parsed?.coerceIn(0, 23) ?: 14
            }

            if (entity.isFromCustomer) hourlyCustomer[hour]++ else hourlyAi[hour]++
            if (entity.actionDetail?.startsWith("VOICE_INPUT") == true) hourlyVoice[hour]++
            hourlyTotal[hour]++
            hourlySentimentSum[hour] += score

            trendList.add(
                SentimentTrendPoint(
                    index = index + 1,
                    messageId = entity.messageId,
                    timeFormatted = entity.timestampFormatted,
                    timestampMillis = entity.timestampMillis,
                    isCustomer = entity.isFromCustomer,
                    textSnippet = if (entity.text.length > 50) entity.text.take(47) + "..." else entity.text,
                    sentimentScore = score,
                    category = category,
                    intentTag = intent
                )
            )
        }

        // Find peak activity hour
        var maxHour = 14
        var maxCount = 0
        for (h in 0 until 24) {
            if (hourlyTotal[h] > maxCount) {
                maxCount = hourlyTotal[h]
                maxHour = h
            }
        }
        val peakHourStr = String.format(Locale.ROOT, "%02d:00 - %02d:00", maxHour, (maxHour + 1) % 24)

        // Generate full 24-hour histogram points
        val hourlyPoints = (0 until 24).map { h ->
            val count = hourlyTotal[h]
            val avgSent = if (count > 0) hourlySentimentSum[h] / count else 0f
            HourlyActivityPoint(
                hourOfDay = h,
                hourLabel = String.format(Locale.ROOT, "%02d:00", h),
                customerCount = hourlyCustomer[h],
                aiCount = hourlyAi[h],
                voiceCount = hourlyVoice[h],
                totalCount = count,
                averageSentiment = avgSent
            )
        }

        val avgSentiment = sumSentiment / total
        val posPct = ((positiveCount.toFloat() / total) * 100).toInt()
        val inqPct = ((inquisitiveCount.toFloat() / total) * 100).toInt()
        val neuPct = ((neutralCount.toFloat() / total) * 100).toInt()
        val conPct = max(0, 100 - posPct - inqPct - neuPct)

        return ChatAnalyticsSummary(
            totalMessages = total,
            customerMessages = customerCount,
            aiMessages = aiCount,
            voiceNotes = voiceCount,
            averageSentiment = avgSentiment,
            positivePercentage = posPct,
            inquisitivePercentage = inqPct,
            neutralPercentage = neuPct,
            concernedPercentage = conPct,
            peakActivityHour = peakHourStr,
            bookingIntentCount = bookingCount,
            hourlyFrequency = hourlyPoints,
            sentimentTrend = trendList
        )
    }

    private fun defaultHourlyBuckets(): List<HourlyActivityPoint> {
        return (0 until 24).map { h ->
            HourlyActivityPoint(
                hourOfDay = h,
                hourLabel = String.format(Locale.ROOT, "%02d:00", h),
                customerCount = 0,
                aiCount = 0,
                voiceCount = 0,
                totalCount = 0,
                averageSentiment = 0.5f
            )
        }
    }

    /**
     * Generates a sample simulated 24-hour chat history dataset for testing.
     */
    fun createSimulatedTraffic(baseTimeMillis: Long = System.currentTimeMillis()): List<ChatLogEntity> {
        val list = mutableListOf<ChatLogEntity>()
        val cal = Calendar.getInstance()
        cal.timeInMillis = baseTimeMillis

        // Set to start of current day 09:00
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 15)

        fun timeStr(cal: Calendar): String =
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(cal.timeInMillis))

        val dialogs = listOf(
            Triple("Hi there, are you open today?", false, "Hello! Yes, Zama Salon is open 09:00 - 19:00. How can I assist?"),
            Triple("How much for knotless braids waist length?", false, "Knotless Braids (waist length) are R850 including premium wash & blow. Would you like to reserve a slot?"),
            Triple("Yes please! What do you have available this Saturday?", false, "We have openings this Saturday at 11:30 AM and 2:00 PM with master stylist Zama. Which works best?"),
            Triple("Saturday at 2:00 PM is perfect, thanks!", true, "Slot #BK-749 reserved for Saturday 14:00. A reminder will be sent Friday. See you then! ✨"),
            Triple("Can I also add a silk press for my sister tomorrow morning?", false, "Certainly! Silk Press is R500. We have tomorrow at 10:00 AM available. Shall I book her in?"),
            Triple("Awesome! Yes please book her in.", true, "All booked! Booking #BK-750 confirmed for tomorrow at 10:00 AM. Thank you Sarah!")
        )

        var msgIdx = 1
        for ((custText, isBooking, aiText) in dialogs) {
            // Customer msg
            val custTime = cal.timeInMillis
            val formattedCust = timeStr(cal)
            list.add(
                ChatLogEntity(
                    messageId = "sim_${msgIdx++}",
                    sessionId = "simulated_traffic",
                    isFromCustomer = true,
                    senderRole = "CUSTOMER",
                    text = custText,
                    timestampMillis = custTime,
                    timestampFormatted = formattedCust,
                    statusTicks = "✓✓",
                    isActionCard = false,
                    actionDetail = if (custText.contains("Saturday at 2:00 PM")) "VOICE_INPUT:gemini-2.5-flash" else null
                )
            )

            // AI reply 1 min later
            cal.add(Calendar.MINUTE, 1)
            val aiTime = cal.timeInMillis
            val formattedAi = timeStr(cal)
            list.add(
                ChatLogEntity(
                    messageId = "sim_${msgIdx++}",
                    sessionId = "simulated_traffic",
                    isFromCustomer = false,
                    senderRole = "AI_AGENT",
                    text = aiText,
                    timestampMillis = aiTime,
                    timestampFormatted = formattedAi,
                    statusTicks = "✓✓",
                    isActionCard = isBooking,
                    actionDetail = if (isBooking) "RESERVATION_CONFIRMED" else null,
                    aiTrace = "Intent: ${if (isBooking) "SlotConfirmed" else "CatalogInquiry"}\nConfidence: 99.8%"
                )
            )

            // Advance time for next interaction
            cal.add(Calendar.HOUR_OF_DAY, 1)
            cal.add(Calendar.MINUTE, 20)
        }

        return list
    }
}
