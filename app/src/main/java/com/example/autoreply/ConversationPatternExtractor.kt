package com.example.autoreply

import com.example.model.ChatMessage
import java.util.Locale

/**
 * Machine Learning Pattern Extractor that analyzes multi-turn WhatsApp conversation history.
 * Discovers context variables, stage progression, and user preferences to synthesize
 * personalized auto-reply suggestions.
 */
object ConversationPatternExtractor {

    private val NAME_REGEX = Regex(
        "(?:my name is|i am|i'm|this is|it's|hi it is)\\s+([a-zA-Z]{2,20})",
        RegexOption.IGNORE_CASE
    )

    private val SERVICE_CATALOG = listOf(
        "Knotless Braids" to listOf("knotless", "knotless braids", "braids", "box braids", "goddess"),
        "Silk Press & Treatment" to listOf("silk press", "press", "thermal", "deep treatment", "argan", "blow dry", "straightening"),
        "Goddess Box Braids" to listOf("goddess", "boho", "curly tendrils", "human hair braids"),
        "Bridal Luxury Updo" to listOf("bridal", "wedding", "updo", "veil", "glamour", "trial"),
        "Lash Extensions" to listOf("lashes", "lash extensions", "russian volume", "classic lash", "volume set"),
        "Wig Installation & Customization" to listOf("wig", "frontal", "closure", "plucking", "bleached knots", "melt")
    )

    private val TIME_PATTERNS = listOf(
        "Saturday at 2:00 PM" to listOf("saturday 2pm", "saturday at 2", "sat 2pm", "saturday afternoon", "sat at 2"),
        "Saturday morning (10:00 AM)" to listOf("saturday morning", "sat 10am", "sat morning", "saturday 10am"),
        "Tomorrow at 11:00 AM" to listOf("tomorrow", "tomorrow 11am", "tomorrow morning"),
        "Friday afternoon (3:00 PM)" to listOf("friday", "friday afternoon", "fri 3pm", "friday 3"),
        "Sunday afternoon" to listOf("sunday", "sun afternoon")
    )

    fun extractContext(history: List<ChatMessage>): ExtractedConversationContext {
        if (history.isEmpty()) {
            return ExtractedConversationContext(
                currentStage = ConversationStage.INITIAL_GREETING,
                lastCustomerQuery = ""
            )
        }

        var detectedName: String? = null
        var detectedService: String? = null
        var detectedPriceEstimate: String? = null
        var detectedDayTime: String? = null
        var isUrgent = false
        val signals = mutableListOf<String>()

        val customerMessages = history.filter { it.isFromCustomer }
        val allText = history.joinToString("\n") { it.text }
        val latestCustomerMsg = customerMessages.lastOrNull()?.text ?: ""

        // 1. Detect Customer Name from greetings or self-introductions
        for (msg in customerMessages) {
            val match = NAME_REGEX.find(msg.text)
            if (match != null) {
                val candidateName = match.groupValues[1].replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                if (candidateName.lowercase() !in listOf("booking", "interested", "looking", "here", "wondering", "asking")) {
                    detectedName = candidateName
                    signals.add("Customer Identity: $detectedName")
                    break
                }
            }
        }

        // 2. Identify Discussed Salon Service across conversation history
        for ((serviceName, keywords) in SERVICE_CATALOG) {
            val matched = keywords.any { kw -> allText.contains(kw, ignoreCase = true) }
            if (matched) {
                detectedService = serviceName
                signals.add("Discussed Service: $serviceName")
                detectedPriceEstimate = when (serviceName) {
                    "Knotless Braids" -> "R650"
                    "Goddess Box Braids" -> "R850"
                    "Silk Press & Treatment" -> "R500"
                    "Bridal Luxury Updo" -> "R1,400"
                    "Lash Extensions" -> "R450"
                    "Wig Installation & Customization" -> "R750"
                    else -> "R600"
                }
                break
            }
        }

        // 3. Identify Temporal Day/Time Slot mentions
        for ((timeSlot, keywords) in TIME_PATTERNS) {
            val matched = keywords.any { kw -> allText.contains(kw, ignoreCase = true) }
            if (matched) {
                detectedDayTime = timeSlot
                signals.add("Slot Interest: $timeSlot")
                break
            }
        }

        // Heuristic fallback for simple day/time matching
        if (detectedDayTime == null) {
            val lowerLatest = latestCustomerMsg.lowercase()
            when {
                lowerLatest.contains("saturday") -> detectedDayTime = "Saturday at 2:00 PM"
                lowerLatest.contains("tomorrow") -> detectedDayTime = "Tomorrow at 11:00 AM"
                lowerLatest.contains("friday") -> detectedDayTime = "Friday at 3:00 PM"
                lowerLatest.contains("weekend") -> detectedDayTime = "This Weekend"
            }
        }

        // 4. Check Urgency / Escalation Signals
        val lowerAll = allText.lowercase()
        val lowerLatest = latestCustomerMsg.lowercase()
        val urgentWords = listOf("urgent", "cancel", "refund", "waiting", "late", "angry", "disappointed", "manager", "emergency", "terrible")
        if (urgentWords.any { lowerLatest.contains(it) }) {
            isUrgent = true
            signals.add("Urgency Flagged: High Priority Escalation")
        }

        // 5. Determine Current Conversation Stage
        val currentStage = when {
            isUrgent -> ConversationStage.COMPLAINT_ESCALATION
            lowerLatest.contains("deposit") || lowerLatest.contains("bank") || lowerLatest.contains("pay") || lowerLatest.contains("proof") || lowerLatest.contains("account") -> {
                signals.add("Stage: Deposit & Banking Verification")
                ConversationStage.DEPOSIT_PENDING
            }
            lowerLatest.contains("confirm") || lowerLatest.contains("book it") || lowerLatest.contains("perfect") || lowerLatest.contains("deal") -> {
                signals.add("Stage: Booking Confirmation")
                ConversationStage.CONFIRMED
            }
            detectedDayTime != null || lowerLatest.contains("available") || lowerLatest.contains("free") || lowerLatest.contains("time") -> {
                signals.add("Stage: Scheduling Slot Exploration")
                ConversationStage.SCHEDULING
            }
            detectedService != null || lowerLatest.contains("price") || lowerLatest.contains("how much") || lowerLatest.contains("cost") -> {
                signals.add("Stage: Service Catalog & Pricing Exploration")
                ConversationStage.SERVICE_INQUIRY
            }
            customerMessages.size <= 1 -> {
                signals.add("Stage: Initial Contact")
                ConversationStage.INITIAL_GREETING
            }
            else -> ConversationStage.SERVICE_INQUIRY
        }

        val hasUnansweredQuestion = latestCustomerMsg.contains("?") ||
                latestCustomerMsg.startsWith("can ", ignoreCase = true) ||
                latestCustomerMsg.startsWith("how ", ignoreCase = true) ||
                latestCustomerMsg.startsWith("is ", ignoreCase = true) ||
                latestCustomerMsg.startsWith("do ", ignoreCase = true)

        return ExtractedConversationContext(
            customerName = detectedName,
            detectedService = detectedService,
            detectedPriceEstimate = detectedPriceEstimate,
            detectedDayOrTime = detectedDayTime,
            currentStage = currentStage,
            isUrgent = isUrgent,
            hasUnansweredQuestion = hasUnansweredQuestion,
            lastCustomerQuery = latestCustomerMsg,
            turnCount = history.size,
            keySignalsFound = signals
        )
    }
}
