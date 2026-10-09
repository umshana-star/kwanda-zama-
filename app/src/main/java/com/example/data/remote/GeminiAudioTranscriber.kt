package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.triage.MessageTriageEngine
import com.example.triage.TriageAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

sealed class VoiceTranscriptionResult {
    data class Success(
        val transcribedText: String,
        val modelUsed: String = "gemini-2.5-flash"
    ) : VoiceTranscriptionResult()

    data class Error(
        val message: String,
        val isApiKeyIssue: Boolean = false,
        val rawResponse: String? = null
    ) : VoiceTranscriptionResult()
}

/**
 * Result representing automatic voice transcription coupled directly with triage analysis categorization.
 */
sealed class VoiceTriageResult {
    data class Success(
        val transcribedText: String,
        val triageAnalysis: TriageAnalysis,
        val modelUsed: String = "gemini-2.5-flash"
    ) : VoiceTriageResult()

    data class Error(
        val message: String,
        val isApiKeyIssue: Boolean = false,
        val rawResponse: String? = null
    ) : VoiceTriageResult()
}

/**
 * Service that communicates with the Google Gemini API (model: gemini-2.5-flash)
 * to transcribe recorded microphone audio (MPEG-4/AAC) into text for autonomous chat.
 */
class GeminiAudioTranscriber(
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY }
) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiAudioTranscriber"
        const val MODEL_NAME = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    /**
     * Transcribes an audio file by sending it to Gemini 3.5 Flash multimodal endpoint.
     */
    suspend fun transcribeAudio(
        audioFile: File,
        mimeType: String = "audio/mp4"
    ): VoiceTranscriptionResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext VoiceTranscriptionResult.Error(
                message = "Gemini API key is missing or set to placeholder. Configure GEMINI_API_KEY in the AI Studio Secrets panel.",
                isApiKeyIssue = true
            )
        }

        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext VoiceTranscriptionResult.Error(
                message = "Audio file is empty or missing. Please record again."
            )
        }

        try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            // Build request payload for Gemini API
            val promptText = "You are an autonomous AI employee's voice-to-text transcriber for a WhatsApp business chat. " +
                    "Listen to the user's spoken audio and transcribe it verbatim into text. " +
                    "Accurately recognize South African English, local salon and beauty service names " +
                    "(e.g., Knotless Braids, Box Braids, Cornrows, Silk Press, Fade, Rand/R pricing, dates, and times like Saturday at 2pm). " +
                    "Return ONLY the verbatim transcription without extra quotes, timestamps, markdown, or greetings."

            val promptPart = JSONObject().apply {
                put("text", promptText)
            }

            val inlineDataObj = JSONObject().apply {
                put("mimeType", mimeType)
                put("data", base64Audio)
            }

            val audioPart = JSONObject().apply {
                put("inlineData", inlineDataObj)
            }

            val partsArray = JSONArray().apply {
                put(promptPart)
                put(audioPart)
            }

            val contentObj = JSONObject().apply {
                put("parts", partsArray)
            }

            val contentsArray = JSONArray().apply {
                put(contentObj)
            }

            val generationConfig = JSONObject().apply {
                put("temperature", 0.1)
            }

            val rootJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", generationConfig)
            }

            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val url = "$BASE_URL/$MODEL_NAME:generateContent"

            val httpRequest = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val isKeyIssue = response.code == 400 || response.code == 403
                val errorMsg = if (isKeyIssue) {
                    "Invalid or unauthorized Gemini API key (HTTP ${response.code}). Check your key in AI Studio Secrets panel."
                } else {
                    "Gemini API request failed with HTTP ${response.code}"
                }
                return@withContext VoiceTranscriptionResult.Error(
                    message = errorMsg,
                    isApiKeyIssue = isKeyIssue,
                    rawResponse = responseBodyString
                )
            }

            val responseJson = JSONObject(responseBodyString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext VoiceTranscriptionResult.Error(
                    message = "Gemini returned no candidates for transcription.",
                    rawResponse = responseBodyString
                )
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val candidateParts = content?.optJSONArray("parts")

            val transcribedTextBuilder = StringBuilder()
            if (candidateParts != null) {
                for (i in 0 until candidateParts.length()) {
                    val part = candidateParts.getJSONObject(i)
                    val txt = part.optString("text", "")
                    transcribedTextBuilder.append(txt)
                }
            }

            val rawResult = transcribedTextBuilder.toString().trim()
            if (rawResult.isBlank()) {
                VoiceTranscriptionResult.Error(
                    message = "Could not decipher speech. Please speak clearly into the microphone."
                )
            } else {
                VoiceTranscriptionResult.Success(
                    transcribedText = rawResult,
                    modelUsed = MODEL_NAME
                )
            }
        } catch (e: Exception) {
            VoiceTranscriptionResult.Error(
                message = "Network or transcription error: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Transcribes recorded audio and immediately routes the resulting text through the
     * MessageTriageEngine to categorize its intent, urgency, priority level, and escalation needs.
     */
    suspend fun transcribeAndTriageAudio(
        audioFile: File,
        mimeType: String = "audio/mp4",
        isCustomer: Boolean = true
    ): VoiceTriageResult = withContext(Dispatchers.IO) {
        val transResult = transcribeAudio(audioFile, mimeType)
        when (transResult) {
            is VoiceTranscriptionResult.Success -> {
                val triage = MessageTriageEngine.analyze(transResult.transcribedText, isCustomer = isCustomer)
                VoiceTriageResult.Success(
                    transcribedText = transResult.transcribedText,
                    triageAnalysis = triage,
                    modelUsed = transResult.modelUsed
                )
            }
            is VoiceTranscriptionResult.Error -> {
                VoiceTriageResult.Error(
                    message = transResult.message,
                    isApiKeyIssue = transResult.isApiKeyIssue,
                    rawResponse = transResult.rawResponse
                )
            }
        }
    }
}
