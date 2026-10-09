package com.example.audio

sealed class VoiceRecordingState {
    object Idle : VoiceRecordingState()

    data class Recording(
        val durationSeconds: Int,
        val normalizedAmplitude: Float,
        val liveDictatedText: String = "",
        val isDictatingToInput: Boolean = true
    ) : VoiceRecordingState()

    data class Transcribing(
        val durationSeconds: Int
    ) : VoiceRecordingState()

    data class Transcribed(
        val text: String,
        val modelUsed: String = "Voice-to-Text"
    ) : VoiceRecordingState()

    data class Error(
        val message: String
    ) : VoiceRecordingState()
}
