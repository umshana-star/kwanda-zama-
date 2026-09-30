package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Enterprise haptic feedback controller providing crisp tactile feedback
 * for voice recording and interactive slider gestures.
 */
class HapticFeedbackManager(
    private val context: Context,
    private val composeHaptic: HapticFeedback
) {
    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Triggered when the user taps or holds the voice recording button.
     * Produces a distinct double-burst confirmation.
     */
    fun recordingStarted() {
        composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (_: Exception) {
            // Graceful fallback on devices without vibration motor
        }
    }

    /**
     * Triggered when the user finishes recording and submits the voice note to Gemini.
     */
    fun recordingStopped() {
        composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(25, 180))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    /**
     * Triggered when voice recording is cancelled or discarded.
     */
    fun recordingCancelled() {
        composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(15, 120))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15)
            }
        } catch (_: Exception) {}
    }

    /**
     * Triggered rapidly as the slider thumb moves across discrete steps or thresholds.
     * Provides subtle, tactile ticking sensations.
     */
    fun sliderTick() {
        composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(10, 80))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10)
            }
        } catch (_: Exception) {}
    }

    /**
     * Triggered when a slider drag gesture concludes or snaps into a preset.
     */
    fun sliderValueConfirmed() {
        composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(20, 200))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20)
            }
        } catch (_: Exception) {}
    }

    /**
     * Triggered when a personality preset chip is tapped.
     */
    fun presetSelected() {
        sliderValueConfirmed()
    }

    /**
     * General button click feedback.
     */
    fun click() {
        composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

@Composable
fun rememberHapticFeedbackManager(): HapticFeedbackManager {
    val context = LocalContext.current
    val composeHaptic = LocalHapticFeedback.current
    return remember(context, composeHaptic) {
        HapticFeedbackManager(context, composeHaptic)
    }
}
