package com.example.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceToTextManager(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<VoiceRecordingState>(VoiceRecordingState.Idle)
    val state: StateFlow<VoiceRecordingState> = _state.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _finalTranscript = MutableStateFlow("")
    val finalTranscript: StateFlow<String> = _finalTranscript.asStateFlow()

    private val _rmsAmplitude = MutableStateFlow(0f)
    val rmsAmplitude: StateFlow<Float> = _rmsAmplitude.asStateFlow()

    private var durationSeconds = 0
    private var durationRunnable: Runnable? = null
    private var onTextUpdatedCallback: ((String, Boolean) -> Unit)? = null
    private var baseText = ""

    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
            _rmsAmplitude.value = normalized
            if (_state.value is VoiceRecordingState.Recording) {
                _state.value = VoiceRecordingState.Recording(
                    durationSeconds = durationSeconds,
                    normalizedAmplitude = normalized,
                    liveDictatedText = _partialTranscript.value
                )
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
        }

        override fun onError(error: Int) {
            _isListening.value = false
            stopTimer()
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                else -> "Speech recognition error ($error)"
            }
            if (_partialTranscript.value.isNotBlank()) {
                _state.value = VoiceRecordingState.Transcribed(_partialTranscript.value)
            } else {
                _state.value = VoiceRecordingState.Error(errorMessage)
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            stopTimer()
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognized = matches?.firstOrNull() ?: _partialTranscript.value
            val fullText = if (baseText.isBlank()) recognized else "$baseText $recognized"
            _finalTranscript.value = fullText
            _partialTranscript.value = fullText
            _state.value = VoiceRecordingState.Transcribed(fullText)
            onTextUpdatedCallback?.invoke(fullText, true)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: return
            val fullText = if (baseText.isBlank()) partial else "$baseText $partial"
            _partialTranscript.value = fullText
            _state.value = VoiceRecordingState.Recording(
                durationSeconds = durationSeconds,
                normalizedAmplitude = _rmsAmplitude.value,
                liveDictatedText = fullText
            )
            onTextUpdatedCallback?.invoke(fullText, false)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun onRecognizedText(text: String, isFinal: Boolean) {
        if (isFinal) {
            _finalTranscript.value = text
            _partialTranscript.value = text
            _state.value = VoiceRecordingState.Transcribed(text)
        } else {
            _partialTranscript.value = text
        }
    }

    fun startDictation(
        existingText: String = "",
        onTextUpdated: ((String, Boolean) -> Unit)? = null
    ): Result<Unit> {
        if (!hasMicrophonePermission()) {
            _state.value = VoiceRecordingState.Error("Microphone permission required for Voice-to-Text")
            return Result.failure(SecurityException("RECORD_AUDIO permission not granted"))
        }

        baseText = existingText.trim()
        onTextUpdatedCallback = onTextUpdated
        durationSeconds = 0

        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                    _state.value = VoiceRecordingState.Error("Speech recognition is not available on this device")
                    return@post
                }

                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                        setRecognitionListener(recognitionListener)
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
                _state.value = VoiceRecordingState.Recording(
                    durationSeconds = 0,
                    normalizedAmplitude = 0.1f,
                    liveDictatedText = baseText
                )
                startTimer()
            } catch (e: Exception) {
                _state.value = VoiceRecordingState.Error(e.message ?: "Failed to start speech recognizer")
            }
        }
        return Result.success(Unit)
    }

    fun stopDictation() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            stopTimer()
            _isListening.value = false
        }
    }

    fun cancelDictation() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            stopTimer()
            _isListening.value = false
            _state.value = VoiceRecordingState.Idle
        }
    }

    fun destroy() {
        mainHandler.post {
            stopTimer()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    private fun startTimer() {
        stopTimer()
        durationRunnable = object : Runnable {
            override fun run() {
                durationSeconds++
                if (_state.value is VoiceRecordingState.Recording) {
                    _state.value = VoiceRecordingState.Recording(
                        durationSeconds = durationSeconds,
                        normalizedAmplitude = _rmsAmplitude.value,
                        liveDictatedText = _partialTranscript.value
                    )
                }
                mainHandler.postDelayed(this, 1000)
            }
        }
        mainHandler.postDelayed(durationRunnable!!, 1000)
    }

    private fun stopTimer() {
        durationRunnable?.let { mainHandler.removeCallbacks(it) }
        durationRunnable = null
    }
}
