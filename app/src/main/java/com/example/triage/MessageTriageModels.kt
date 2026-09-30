package com.example.triage

import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * Message Intent categories for triage analysis.
 */
enum class TriageIntent(
    val displayName: String,
    val iconEmoji: String,
    val colorHex: Long,
    val description: String
) {
    BOOKING(
        displayName = "Booking",
        iconEmoji = "📅",
        colorHex = 0xFF25D366,
        description = "Appointment scheduling, slot availability, or date queries"
    ),
    INQUIRY(
        displayName = "Inquiry",
        iconEmoji = "💬",
        colorHex = 0xFF00E5FF,
        description = "Pricing, salon services catalog, location, or styling details"
    ),
    COMPLAINT(
        displayName = "Complaint",
        iconEmoji = "⚠️",
        colorHex = 0xFFFF5252,
        description = "Customer dissatisfaction, delays, service issues, or refund requests"
    ),
    HUMAN_ESCALATION(
        displayName = "Human Escalation",
        iconEmoji = "🚨",
        colorHex = 0xFFFF1744,
        description = "Direct request to speak to the salon owner/manager or dispute"
    ),
    FEEDBACK(
        displayName = "Praise",
        iconEmoji = "⭐",
        colorHex = 0xFFFFD700,
        description = "Positive feedback, compliment, or styling appreciation"
    ),
    GENERAL(
        displayName = "General",
        iconEmoji = "👋",
        colorHex = 0xFF8696A0,
        description = "Standard greeting or general assistance"
    )
}

/**
 * Message Urgency levels to assist business owners in prioritizing response times.
 */
enum class TriageUrgency(
    val label: String,
    val priorityLevel: Int,
    val colorHex: Long
) {
    CRITICAL("CRITICAL", 4, 0xFFFF1744),
    HIGH("HIGH", 3, 0xFFFF9100),
    MEDIUM("MEDIUM", 2, 0xFF00E5FF),
    LOW("LOW", 1, 0xFF8696A0)
}

/**
 * Pre-composed quick reply suggestion that business owners or the AI assistant
 * can dispatch with a single tap.
 */
data class QuickReplySuggestion(
    val id: String,
    val title: String,
    val fullText: String,
    val intent: TriageIntent,
    val iconEmoji: String = intent.iconEmoji,
    val isPrimaryAction: Boolean = false
)

/**
 * High-visibility Priority Classification for customer messages.
 * Allows instant visual distinction between Critical/Urgent requests and General routine messages.
 */
enum class TriagePriorityLevel(
    val label: String,
    val tagText: String,
    val iconEmoji: String,
    val colorHex: Long,
    val backgroundAlpha: Float = 0.32f,
    val borderAlpha: Float = 0.90f
) {
    URGENT(
        label = "Urgent",
        tagText = "URGENT",
        iconEmoji = "🚨",
        colorHex = 0xFFFF1744, // Vibrant high-visibility electric red/crimson
        backgroundAlpha = 0.35f,
        borderAlpha = 0.95f
    ),
    GENERAL(
        label = "General",
        tagText = "GENERAL",
        iconEmoji = "💬",
        colorHex = 0xFF00E5FF, // High-visibility electric cyan
        backgroundAlpha = 0.20f,
        borderAlpha = 0.70f
    )
}

/**
 * Result of the AI triage analysis for an incoming customer message.
 */
data class TriageAnalysis(
    val intent: TriageIntent,
    val urgency: TriageUrgency,
    val requiresImmediateHumanAttention: Boolean,
    val escalationReason: String?,
    val tags: List<String>,
    val suggestedReplies: List<QuickReplySuggestion>
) {
    /**
     * Determines whether the message demands urgent prioritization based on triage metadata:
     * Critical/High urgency, immediate human escalation request, or severe complaint.
     */
    val isUrgent: Boolean
        get() = urgency == TriageUrgency.CRITICAL ||
                urgency == TriageUrgency.HIGH ||
                requiresImmediateHumanAttention ||
                intent == TriageIntent.HUMAN_ESCALATION ||
                intent == TriageIntent.COMPLAINT

    /**
     * Maps to high-visibility binary priority level (URGENT vs GENERAL).
     */
    val priorityLevel: TriagePriorityLevel
        get() = if (isUrgent) TriagePriorityLevel.URGENT else TriagePriorityLevel.GENERAL
}

/**
 * AI-powered Customer Message Triage Engine.
 * Analyzes incoming messages, categorizes intent and urgency, flags urgent items
 * for immediate human intervention, and generates contextual 1-tap quick replies.
 */
object MessageTriageEngine {

    private val HUMAN_ESCALATION_KEYWORDS = listOf(
        "human", "manager", "owner", "supervisor", "speak to someone",
        "talk to a person", "not a bot", "real person", "urgent call",
        "speak to the owner", "call me now", "dispute", "lawyer"
    )

    private val COMPLAINT_KEYWORDS = listOf(
        "terrible", "worst", "unhappy", "angry", "disappointed",
        "waiting for", "late", "delay", "damaged", "ruined",
        "pain", "hurts", "refund", "horrible", "awful",
        "bad job", "crooked", "too tight", "cancel my appointment"
    )

    private val BOOKING_KEYWORDS = listOf(
        "book", "booking", "appointment", "slot", "available", "schedule",
        "saturday", "sunday", "tomorrow", "friday", "braids", "silk press",
        "knotless", "haircut", "install", "wash", "date", "time", "reserve"
    )

    private val INQUIRY_KEYWORDS = listOf(
        "price", "pricing", "cost", "how much", "quote", "where are you",
        "location", "address", "directions", "hours", "open", "catalog",
        "duration", "how long", "services", "deposit", "specials"
    )

    private val FEEDBACK_KEYWORDS = listOf(
        "thank you", "thanks", "loved", "great", "amazing", "beautiful",
        "perfect", "good job", "love it", "appreciate", "wonderful"
    )

    /**
     * Performs triage classification on a message.
     */
    fun analyze(messageText: String, isCustomer: Boolean = true): TriageAnalysis {
        val normalized = messageText.trim().lowercase(Locale.ROOT)

        // 1. Check for Human Escalation
        val isExplicitHumanRequest = HUMAN_ESCALATION_KEYWORDS.any { normalized.contains(it) }
        val isAggressiveComplaint = COMPLAINT_KEYWORDS.any { normalized.contains(it) }

        if (isExplicitHumanRequest) {
            val tags = listOf("#UrgentEscalation", "#HumanAttentionRequired", "#OwnerAlert")
            return TriageAnalysis(
                intent = TriageIntent.HUMAN_ESCALATION,
                urgency = TriageUrgency.CRITICAL,
                requiresImmediateHumanAttention = true,
                escalationReason = "Customer explicitly requested human manager/owner intervention",
                tags = tags,
                suggestedReplies = getSuggestionsForIntent(TriageIntent.HUMAN_ESCALATION)
            )
        }

        if (isAggressiveComplaint) {
            val isSevere = normalized.contains("refund") || normalized.contains("ruined") ||
                    normalized.contains("hurts") || normalized.contains("pain") || normalized.contains("worst")
            val urgency = if (isSevere) TriageUrgency.CRITICAL else TriageUrgency.HIGH
            val tags = listOf("#CustomerComplaint", "#ServiceIssue", if (isSevere) "#ImmediateReview" else "#FollowUp")
            return TriageAnalysis(
                intent = TriageIntent.COMPLAINT,
                urgency = urgency,
                requiresImmediateHumanAttention = isSevere,
                escalationReason = if (isSevere) "Customer reported critical dissatisfaction/service failure" else null,
                tags = tags,
                suggestedReplies = getSuggestionsForIntent(TriageIntent.COMPLAINT)
            )
        }

        // 2. Check for Pricing / Location / Service Inquiries (prioritize if asking about price, quote, cost, or info)
        val hasInquiryTrigger = INQUIRY_KEYWORDS.any { normalized.contains(it) }
        val hasDirectBookingTrigger = normalized.contains("book") || normalized.contains("appointment") ||
                normalized.contains("reserve") || normalized.contains("schedule")

        if (hasInquiryTrigger && (!hasDirectBookingTrigger || normalized.contains("price") || normalized.contains("cost") || normalized.contains("how much"))) {
            val tags = listOf("#Inquiry", "#CatalogPricing", "#SalonInfo")
            return TriageAnalysis(
                intent = TriageIntent.INQUIRY,
                urgency = TriageUrgency.MEDIUM,
                requiresImmediateHumanAttention = false,
                escalationReason = null,
                tags = tags,
                suggestedReplies = getSuggestionsForIntent(TriageIntent.INQUIRY)
            )
        }

        // 3. Check for Booking Intent
        if (hasDirectBookingTrigger || BOOKING_KEYWORDS.any { normalized.contains(it) }) {
            val isUrgentSlot = normalized.contains("today") || normalized.contains("tomorrow") || normalized.contains("asap")
            val urgency = if (isUrgentSlot) TriageUrgency.HIGH else TriageUrgency.MEDIUM
            val tags = listOf("#BookingRequest", if (isUrgentSlot) "#UrgentSlot" else "#ScheduledAppointment", "#SalonCalendar")
            return TriageAnalysis(
                intent = TriageIntent.BOOKING,
                urgency = urgency,
                requiresImmediateHumanAttention = false,
                escalationReason = null,
                tags = tags,
                suggestedReplies = getSuggestionsForIntent(TriageIntent.BOOKING)
            )
        }

        // 4. Check for Feedback / Compliments
        if (FEEDBACK_KEYWORDS.any { normalized.contains(it) }) {
            val tags = listOf("#PositiveReview", "#ClientPraise", "#Retention")
            return TriageAnalysis(
                intent = TriageIntent.FEEDBACK,
                urgency = TriageUrgency.LOW,
                requiresImmediateHumanAttention = false,
                escalationReason = null,
                tags = tags,
                suggestedReplies = getSuggestionsForIntent(TriageIntent.FEEDBACK)
            )
        }

        // 5. Default General category
        return TriageAnalysis(
            intent = TriageIntent.GENERAL,
            urgency = TriageUrgency.LOW,
            requiresImmediateHumanAttention = false,
            escalationReason = null,
            tags = listOf("#GeneralInquiry", "#WhatsAppLead"),
            suggestedReplies = getSuggestionsForIntent(TriageIntent.GENERAL)
        )
    }

    /**
     * Provides common pre-crafted responses based on message triage category,
     * allowing fast 1-tap replies for the business owner.
     */
    fun getSuggestionsForIntent(intent: TriageIntent): List<QuickReplySuggestion> {
        return when (intent) {
            TriageIntent.BOOKING -> listOf(
                QuickReplySuggestion(
                    id = "book_confirm_sat",
                    title = "Confirm Sat 2pm Slot",
                    fullText = "Yes, we have an opening this Saturday at 2:00 PM! Would you like me to reserve it for you?",
                    intent = TriageIntent.BOOKING,
                    iconEmoji = "📅",
                    isPrimaryAction = true
                ),
                QuickReplySuggestion(
                    id = "book_deposit_link",
                    title = "Send Deposit Link (R150)",
                    fullText = "To secure your slot, a R150 booking deposit is required. You can settle it securely here: pay.zama.co.za/deposit",
                    intent = TriageIntent.BOOKING,
                    iconEmoji = "💳"
                ),
                QuickReplySuggestion(
                    id = "book_style_length",
                    title = "Ask Style & Length",
                    fullText = "What hairstyle and length would you like? (e.g., Mid-back or Waist-length Knotless Braids)",
                    intent = TriageIntent.BOOKING,
                    iconEmoji = "✂️"
                ),
                QuickReplySuggestion(
                    id = "book_next_available",
                    title = "Next Available Opening",
                    fullText = "Our next available appointment is on Thursday at 10:00 AM or Friday at 3:00 PM. Which works best?",
                    intent = TriageIntent.BOOKING,
                    iconEmoji = "⏰"
                )
            )
            TriageIntent.INQUIRY -> listOf(
                QuickReplySuggestion(
                    id = "inquiry_silk_press",
                    title = "Silk Press: R450",
                    fullText = "Our Silk Press is R450. It includes a deep steam cleanse, hydrating treatment, trim, and silky straight finish!",
                    intent = TriageIntent.INQUIRY,
                    iconEmoji = "💇‍♀️",
                    isPrimaryAction = true
                ),
                QuickReplySuggestion(
                    id = "inquiry_braids_pricing",
                    title = "Braids Menu & Pricing",
                    fullText = "Medium Knotless Braids are R650, Small is R850 (hair extensions included). Free wash included this month!",
                    intent = TriageIntent.INQUIRY,
                    iconEmoji = "📋"
                ),
                QuickReplySuggestion(
                    id = "inquiry_location",
                    title = "Salon Location & Directions",
                    fullText = "We are located at 42 Victoria Embankment, Durban Central. Safe on-site parking is available!",
                    intent = TriageIntent.INQUIRY,
                    iconEmoji = "📍"
                ),
                QuickReplySuggestion(
                    id = "inquiry_hours",
                    title = "Operating Hours",
                    fullText = "We are open Tuesday to Saturday 08:30 – 18:00, and Sunday 09:00 – 15:00. Closed Mondays.",
                    intent = TriageIntent.INQUIRY,
                    iconEmoji = "🕒"
                )
            )
            TriageIntent.COMPLAINT -> listOf(
                QuickReplySuggestion(
                    id = "complaint_apology",
                    title = "Sincere Apology & Investigating",
                    fullText = "We sincerely apologize for this experience! This is not our standard. Let me check with the stylist right now.",
                    intent = TriageIntent.COMPLAINT,
                    iconEmoji = "🙏",
                    isPrimaryAction = true
                ),
                QuickReplySuggestion(
                    id = "complaint_call_back",
                    title = "Owner Direct Call",
                    fullText = "I have notified our salon owner, Kwanda. May we call you directly on this number in 5 minutes to resolve this?",
                    intent = TriageIntent.COMPLAINT,
                    iconEmoji = "📞"
                ),
                QuickReplySuggestion(
                    id = "complaint_reschedule",
                    title = "Complimentary Touch-Up",
                    fullText = "We would love to make this right. We invite you back for a complimentary touch-up with our head stylist.",
                    intent = TriageIntent.COMPLAINT,
                    iconEmoji = "🔄"
                ),
                QuickReplySuggestion(
                    id = "complaint_refund_review",
                    title = "Refund Review Ticket",
                    fullText = "I have logged a priority refund and review ticket #ZR-902 with salon management.",
                    intent = TriageIntent.COMPLAINT,
                    iconEmoji = "📋"
                )
            )
            TriageIntent.HUMAN_ESCALATION -> listOf(
                QuickReplySuggestion(
                    id = "esc_owner_taking_over",
                    title = "Manager Taking Over",
                    fullText = "Hello, this is Kwanda (Salon Owner). I am stepping in directly to assist you with this matter.",
                    intent = TriageIntent.HUMAN_ESCALATION,
                    iconEmoji = "👤",
                    isPrimaryAction = true
                ),
                QuickReplySuggestion(
                    id = "esc_direct_phone",
                    title = "Call Our Direct Line",
                    fullText = "Please ring our direct salon line at +27 82 123 4567, or reply here and a manager will call immediately.",
                    intent = TriageIntent.HUMAN_ESCALATION,
                    iconEmoji = "📞"
                ),
                QuickReplySuggestion(
                    id = "esc_pause_ai",
                    title = "Pause AI Assistant",
                    fullText = "AI auto-reply is paused for this chat. A human team member is reviewing your inquiry.",
                    intent = TriageIntent.HUMAN_ESCALATION,
                    iconEmoji = "⏸️"
                )
            )
            TriageIntent.FEEDBACK -> listOf(
                QuickReplySuggestion(
                    id = "fb_thank_you",
                    title = "Thank You & Warm Note",
                    fullText = "Thank you so much! It was an absolute pleasure styling your hair. We look forward to seeing you again!",
                    intent = TriageIntent.FEEDBACK,
                    iconEmoji = "💖",
                    isPrimaryAction = true
                ),
                QuickReplySuggestion(
                    id = "fb_leave_review",
                    title = "Send Google Review Link",
                    fullText = "We're thrilled you loved it! If you have a minute, we'd appreciate a quick 5-star review: zama.co.za/review",
                    intent = TriageIntent.FEEDBACK,
                    iconEmoji = "⭐"
                ),
                QuickReplySuggestion(
                    id = "fb_next_discount",
                    title = "10% VIP Return Voucher",
                    fullText = "As a token of appreciation, here is your 10% VIP discount code for your next booking: ZAMAVIP10",
                    intent = TriageIntent.FEEDBACK,
                    iconEmoji = "🎁"
                )
            )
            TriageIntent.GENERAL -> listOf(
                QuickReplySuggestion(
                    id = "gen_welcome",
                    title = "Welcome to Zama Salon",
                    fullText = "Hello! Welcome to Zama Hair & Beauty. How can we assist you today?",
                    intent = TriageIntent.GENERAL,
                    iconEmoji = "👋",
                    isPrimaryAction = true
                ),
                QuickReplySuggestion(
                    id = "gen_catalog",
                    title = "Browse Services Menu",
                    fullText = "Here is our current service menu: Knotless Braids, Silk Press, Cornrows, Locs Maintenance & Treatments.",
                    intent = TriageIntent.GENERAL,
                    iconEmoji = "📖"
                ),
                QuickReplySuggestion(
                    id = "gen_specials",
                    title = "Current Monthly Specials",
                    fullText = "This month get 15% off all Knotless Braids bookings made Monday through Thursday!",
                    intent = TriageIntent.GENERAL,
                    iconEmoji = "✨"
                )
            )
        }
    }
}
