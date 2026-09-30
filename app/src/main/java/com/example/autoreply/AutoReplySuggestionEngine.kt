package com.example.autoreply

import android.util.Log
import com.example.BuildConfig
import com.example.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Machine Learning-based Auto-Reply Suggestion Engine.
 * Formulates personalized, context-aware responses based on extracted
 * multi-turn conversation patterns, customer history, and optional Gemini neural inference.
 */
object AutoReplySuggestionEngine {

    private const val TAG = "AutoReplyEngine"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    /**
     * Instantly synthesizes 3-4 personalized auto-reply suggestions tailored
     * directly to the multi-turn conversation history.
     */
    fun generatePatternSuggestions(history: List<ChatMessage>): List<AutoReplySuggestion> {
        val context = ConversationPatternExtractor.extractContext(history)
        val nameGreeting = if (!context.customerName.isNullOrBlank()) "Hi ${context.customerName}" else "Hi there"
        val serviceName = context.detectedService ?: "Knotless Braids"
        val price = context.detectedPriceEstimate ?: "R650"
        val slot = context.detectedDayOrTime ?: "Saturday at 2:00 PM"

        val suggestions = mutableListOf<AutoReplySuggestion>()

        // 1. Primary contextual suggestion based on the active conversation stage
        when (context.currentStage) {
            ConversationStage.COMPLAINT_ESCALATION -> {
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_escalation_immediate",
                        suggestedText = "$nameGreeting, I sincerely apologize for the delay. Your experience is our top priority—I am flagging this to our salon manager right now and having them call you directly in 5 minutes.",
                        shortLabel = "🚨 Immediate Manager Call",
                        confidenceScore = 0.98f,
                        patternType = HistoryPatternType.URGENT_EMPATHY_DEESCALATION,
                        patternRationale = "Pattern: Detected urgent complaint or dissatisfaction; prioritized immediate human escalation",
                        contextTags = listOf("Urgent De-escalation", "Manager Callback")
                    )
                )
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_escalation_solution",
                        suggestedText = "$nameGreeting, thank you for your patience. We want to make this right immediately—would you like us to reschedule you to a VIP priority slot or process an instant refund for your deposit?",
                        shortLabel = "⚡ Priority Resolution Offer",
                        confidenceScore = 0.94f,
                        patternType = HistoryPatternType.URGENT_EMPATHY_DEESCALATION,
                        patternRationale = "Pattern: Offered proactive resolution pathways to minimize customer friction",
                        contextTags = listOf("Resolution Offer", "VIP Slot")
                    )
                )
            }

            ConversationStage.DEPOSIT_PENDING -> {
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_deposit_banking",
                        suggestedText = "$nameGreeting! To secure your $slot slot for $serviceName, please send a R200 deposit to: Capitec Bank • Acc: 154892019 • Ref: ${context.customerName ?: "Hair"}-$slot. Reply with your POP and you are all set! ✨",
                        shortLabel = "💳 Banking & Deposit Lock",
                        confidenceScore = 0.97f,
                        patternType = HistoryPatternType.DEPOSIT_BANKING_LOCK,
                        patternRationale = "Pattern: Customer confirmed interest in $serviceName; provided Capitec banking details and reference",
                        contextTags = listOf("Capitec Banking", "R200 Deposit", serviceName)
                    )
                )
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_deposit_card_link",
                        suggestedText = "You can also pay securely with card or Apple Pay using our instant Zapper link: pay.zama.co.za/deposit?ref=${context.customerName ?: "Client"}. Your slot is held for 30 minutes! 🔒",
                        shortLabel = "🔗 Instant Card Pay Link",
                        confidenceScore = 0.92f,
                        patternType = HistoryPatternType.DEPOSIT_BANKING_LOCK,
                        patternRationale = "Pattern: Alternative zero-friction mobile card payment link",
                        contextTags = listOf("Instant Checkout", "Held for 30 mins")
                    )
                )
            }

            ConversationStage.SCHEDULING -> {
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_slot_confirm",
                        suggestedText = "$nameGreeting! Great news—$slot is open for your $serviceName. Our senior stylist is available. Would you like me to reserve this for you?",
                        shortLabel = "📅 Confirm $slot",
                        confidenceScore = 0.96f,
                        patternType = HistoryPatternType.SLOT_TIME_OFFER,
                        patternRationale = "Pattern: Correlated customer's requested time ($slot) with catalog item ($serviceName)",
                        contextTags = listOf(slot, serviceName, "Senior Stylist")
                    )
                )
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_slot_alternative",
                        suggestedText = "$nameGreeting, we can do $slot, or if you prefer an earlier time, we also have 10:30 AM open on that day! Which time suits you best?",
                        shortLabel = "🕒 Slot Options (Morning/Afternoon)",
                        confidenceScore = 0.93f,
                        patternType = HistoryPatternType.SLOT_TIME_OFFER,
                        patternRationale = "Pattern: Offered prime time alongside morning alternative to expedite booking",
                        contextTags = listOf("Flexible Slots", "Morning Opening")
                    )
                )
            }

            ConversationStage.SERVICE_INQUIRY -> {
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_quote_personalized",
                        suggestedText = "$nameGreeting! Our $serviceName starts at $price and includes our complimentary scalp treatment and thermal seal. Duration is about 2.5 to 3 hours. Would you like to check weekend openings?",
                        shortLabel = "✂️ $serviceName Quote ($price)",
                        confidenceScore = 0.95f,
                        patternType = HistoryPatternType.SERVICE_PRICING_QUOTE,
                        patternRationale = "Pattern: History mentions $serviceName; provided full pricing ($price) and time estimate",
                        contextTags = listOf(serviceName, price, "Includes Treatment")
                    )
                )
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_combo_recommendation",
                        suggestedText = "$nameGreeting, for $serviceName we also have a popular combo including deep hydration treatment and lash touch-up for just R150 extra. Would you like me to add that to your quote?",
                        shortLabel = "✨ Popular Treatment Combo",
                        confidenceScore = 0.90f,
                        patternType = HistoryPatternType.SERVICE_PRICING_QUOTE,
                        patternRationale = "Pattern: Recommended relevant hydration add-on based on service category",
                        contextTags = listOf("Add-on Offer", "Hydration Combo")
                    )
                )
            }

            ConversationStage.CONFIRMED -> {
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_confirmed_welcome",
                        suggestedText = "$nameGreeting, your booking for $serviceName on $slot is officially confirmed! 📍 Location: 45 Rosebank Central. Please arrive 10 minutes early with your hair washed and detangled. See you soon! 💕",
                        shortLabel = "✅ Arrival & Prep Details",
                        confidenceScore = 0.96f,
                        patternType = HistoryPatternType.CONFIRMATION_REMINDER,
                        patternRationale = "Pattern: Booking finalized; shared address and hair prep guidelines",
                        contextTags = listOf("Confirmed", "Rosebank", "Prep Tips")
                    )
                )
            }

            ConversationStage.AFTERCARE_RETENTION -> {
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_aftercare_routine",
                        suggestedText = "$nameGreeting, here is your quick care tip: mist your scalp with rosewater daily and wear a silk bonnet at night to keep your $serviceName looking fresh for up to 6 weeks! ✨",
                        shortLabel = "💆 Maintenance & Silk Bonnet Tip",
                        confidenceScore = 0.93f,
                        patternType = HistoryPatternType.MULTITURN_CONTINUATION,
                        patternRationale = "Pattern: Post-appointment care pattern to boost retention",
                        contextTags = listOf("Aftercare", "Maintenance", "Retention")
                    )
                )
            }

            ConversationStage.INITIAL_GREETING -> {
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_warm_welcome",
                        suggestedText = "Hi there! Welcome to Zama Luxury Hair & Beauty Concierge. How can we pamper you today? We specialize in Knotless Braids, Silk Press, Goddess Locs, and Wig Installations. ✨",
                        shortLabel = "👋 Welcome & Catalog",
                        confidenceScore = 0.94f,
                        patternType = HistoryPatternType.GREETING_NEW_CLIENT,
                        patternRationale = "Pattern: New contact interaction; shared signature services overview",
                        contextTags = listOf("Welcome", "Menu Highlights")
                    )
                )
                suggestions.add(
                    AutoReplySuggestion(
                        id = "reply_weekend_special",
                        suggestedText = "Hello! We currently have a few prime slots left for this upcoming weekend. Are you looking to book an appointment or explore our service pricing?",
                        shortLabel = "📅 Inquire Weekend Openings",
                        confidenceScore = 0.91f,
                        patternType = HistoryPatternType.SLOT_TIME_OFFER,
                        patternRationale = "Pattern: Prompted user to specify appointment vs inquiry",
                        contextTags = listOf("Weekend Availability")
                    )
                )
            }
        }

        // 2. Add an auxiliary multi-turn continuation reply if not already present
        if (suggestions.size < 3) {
            suggestions.add(
                AutoReplySuggestion(
                    id = "reply_context_clarification",
                    suggestedText = "$nameGreeting, would you like me to hold that for you, or do you have any questions about hair extensions, styling length, or stylists?",
                    shortLabel = "💬 Clarify Hair Details",
                    confidenceScore = 0.88f,
                    patternType = HistoryPatternType.MULTITURN_CONTINUATION,
                    patternRationale = "Pattern: General conversational progression to assist customer decision",
                    contextTags = listOf("Assistance", "Hair Details")
                )
            )
        }

        return suggestions
    }

    /**
     * Asynchronously leverages the Google Gemini API to produce advanced,
     * hyper-personalized neural auto-replies based on full conversation context.
     * Gracefully falls back to pattern suggestions if network is offline or API key is absent.
     */
    suspend fun generateAiPersonalizedReplies(
        history: List<ChatMessage>,
        apiKey: String = BuildConfig.GEMINI_API_KEY
    ): List<AutoReplySuggestion> = withContext(Dispatchers.IO) {
        val fallbackSuggestions = generatePatternSuggestions(history)
        if (apiKey.isBlank() || history.isEmpty()) {
            return@withContext fallbackSuggestions
        }

        try {
            val transcript = history.takeLast(10).joinToString("\n") { msg ->
                val speaker = if (msg.isFromCustomer) "CUSTOMER" else "SALON_AGENT"
                "$speaker: ${msg.text}"
            }

            val systemPrompt = """
                You are a machine learning Auto-Reply assistant for a high-end hair salon WhatsApp business.
                Analyze the conversation transcript and identify key patterns (customer name, service interest, slot preference, stage).
                Generate 3 short, personalized, highly engaging WhatsApp responses the agent can send next.
                Output ONLY a JSON array of objects with the following schema:
                [
                  {
                    "label": "Short button label (e.g. Slot Confirmation)",
                    "text": "Personalized WhatsApp response message ready to send",
                    "confidence": 0.95,
                    "pattern": "Brief description of the pattern recognized in history"
                  }
                ]
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemPrompt\n\nCONVERSATION TRANSCRIPT:\n$transcript")
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 800)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext fallbackSuggestions
            }

            val parsedJson = JSONObject(responseString)
            val candidates = parsedJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text") ?: ""

            val cleanJson = textOutput.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val jsonArray = JSONArray(cleanJson)

            val aiSuggestions = mutableListOf<AutoReplySuggestion>()
            for (i in 0 until jsonArray.length().coerceAtMost(4)) {
                val item = jsonArray.getJSONObject(i)
                val label = item.optString("label", "Suggested Reply")
                val text = item.optString("text", "")
                val conf = item.optDouble("confidence", 0.95).toFloat()
                val pattern = item.optString("pattern", "Neural Context Match")

                if (text.isNotBlank()) {
                    aiSuggestions.add(
                        AutoReplySuggestion(
                            id = "ai_reply_$i",
                            suggestedText = text,
                            shortLabel = label,
                            confidenceScore = conf,
                            patternType = HistoryPatternType.MULTITURN_CONTINUATION,
                            patternRationale = "Gemini Neural ML: $pattern",
                            contextTags = listOf("Neural Auto-Reply", "Gemini 2.5 Flash"),
                            isAiGenerated = true
                        )
                    )
                }
            }

            if (aiSuggestions.isNotEmpty()) {
                aiSuggestions
            } else {
                fallbackSuggestions
            }
        } catch (_: Exception) {
            fallbackSuggestions
        }
    }
}
