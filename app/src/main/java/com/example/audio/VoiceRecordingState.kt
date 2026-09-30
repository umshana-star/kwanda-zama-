package com.example.audio

import com.example.triage.TriageAnalysis
import java.io.File

/**
 * State representing the voice recording, Gemini API transcription, and automatic triage classification lifecycle.
 */
sealed class VoiceRecordingState {
    object Idle : VoiceRecordingState()

    data class Recording(
        val durationSeconds: Int,
        val normalizedAmplitude: Float
    ) : VoiceRecordingState()

    data class Transcribing(
        val durationSeconds: Int,
        val audioFile: File?
    ) : VoiceRecordingState()

    data class Triaging(
        val transcribedText: String,
        val audioFile: File?,
        val modelUsed: String = "gemini-3.5-flash"
    ) : VoiceRecordingState()

    data class Transcribed(
        val text: String,
        val audioFile: File?,
        val modelUsed: String = "gemini-3.5-flash",
        val triageAnalysis: TriageAnalysis? = null,
        val isFallback: Boolean = false
    ) : VoiceRecordingState()

    data class Error(
        val message: String,
        val isApiKeyMissing: Boolean = false
    ) : VoiceRecordingState()
}
