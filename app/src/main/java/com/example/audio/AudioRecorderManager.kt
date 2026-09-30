package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Manages audio recording from the device microphone using MediaRecorder.
 * Outputs MPEG-4 / AAC audio formatted for Gemini API multimodal audio input.
 */
class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    var isRecording: Boolean = false
        private set

    /**
     * Starts recording audio from the microphone to a temporary cache file.
     */
    fun startRecording(): Result<File> {
        return try {
            // Cancel any ongoing recording before starting a new one
            cancelRecording()

            val audioFile = File(
                context.cacheDir,
                "zama_voice_${System.currentTimeMillis()}.m4a"
            )

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            currentOutputFile = audioFile
            isRecording = true
            Result.success(audioFile)
        } catch (e: Exception) {
            cleanUp()
            Result.failure(e)
        }
    }

    /**
     * Returns the peak amplitude since the last call, normalized to 0.0f..1.0f.
     */
    fun getNormalizedAmplitude(): Float {
        return try {
            if (isRecording && mediaRecorder != null) {
                val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                // MediaRecorder max amplitude is typically up to 32767
                (maxAmp / 32767f).coerceIn(0.05f, 1.0f)
            } else {
                0.05f
            }
        } catch (e: Exception) {
            0.05f
        }
    }

    /**
     * Stops the active recording and returns the completed audio file.
     */
    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {
                }
                release()
            }
            mediaRecorder = null
            isRecording = false
            val file = currentOutputFile
            currentOutputFile = null
            file
        } catch (_: Exception) {
            cleanUp()
            null
        }
    }

    /**
     * Cancels the active recording and purges temporary files.
     */
    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (_: Exception) {}
                    release()
                }
            }
        } catch (_: Exception) {
        } finally {
            cleanUp()
        }
    }

    private fun cleanUp() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
        try {
            currentOutputFile?.delete()
        } catch (_: Exception) {}
        currentOutputFile = null
    }

    companion object {
        private const val TAG = "AudioRecorderManager"
    }
}
