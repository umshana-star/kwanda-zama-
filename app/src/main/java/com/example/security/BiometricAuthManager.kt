package com.example.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor

/**
 * Hardware capability status for biometric authentication on the current device.
 */
enum class BiometricCapabilityStatus {
    AVAILABLE,
    NOT_ENROLLED,
    HARDWARE_UNAVAILABLE,
    NO_HARDWARE,
    UNSUPPORTED_SDK
}

/**
 * Callback interface to receive authentication lifecycle events.
 */
interface BiometricAuthCallback {
    fun onAuthenticationStarted() {}
    fun onAuthenticationSuccess(resultDescription: String = "Biometric identity verified") {}
    fun onAuthenticationError(errorCode: Int, errString: CharSequence) {}
    fun onAuthenticationFailed() {}
}

/**
 * Manager responsible for checking biometric hardware capabilities and invoking
 * the native Android [BiometricPrompt] for secure app access.
 */
class BiometricAuthManager(private val context: Context) {

    private var currentCancellationSignal: CancellationSignal? = null

    /**
     * Inspects device hardware and security settings to determine biometric availability.
     */
    fun canAuthenticate(): BiometricCapabilityStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return BiometricCapabilityStatus.UNSUPPORTED_SDK
        }

        // On Android 10+ (API 29+), use BiometricManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
            if (biometricManager != null) {
                return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapabilityStatus.AVAILABLE
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapabilityStatus.NOT_ENROLLED
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapabilityStatus.NO_HARDWARE
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapabilityStatus.HARDWARE_UNAVAILABLE
                    else -> BiometricCapabilityStatus.HARDWARE_UNAVAILABLE
                }
            }
        }

        // On Android 9 (API 28), check PackageManager and Keyguard
        val packageManager = context.packageManager
        val hasFingerprintHardware = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        if (!hasFingerprintHardware) {
            return BiometricCapabilityStatus.NO_HARDWARE
        }

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return if (keyguardManager?.isKeyguardSecure == true) {
            BiometricCapabilityStatus.AVAILABLE
        } else {
            BiometricCapabilityStatus.NOT_ENROLLED
        }
    }

    /**
     * Launches the system Android [BiometricPrompt] on the provided [Activity].
     */
    fun authenticate(
        activity: Activity,
        title: String = "Biometric Verification",
        subtitle: String = "Unlock Zama Salon Concierge",
        description: String = "Scan your fingerprint or face to access encrypted records",
        negativeButtonText: String = "Use PIN Fallback",
        callback: BiometricAuthCallback
    ): CancellationSignal? {
        cancelAuthentication()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            callback.onAuthenticationError(
                -1,
                "BiometricPrompt requires Android 9.0 (API 28) or higher"
            )
            return null
        }

        callback.onAuthenticationStarted()

        val cancellationSignal = CancellationSignal().also {
            currentCancellationSignal = it
        }

        val executor: Executor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity.mainExecutor
        } else {
            val handler = Handler(Looper.getMainLooper())
            Executor { command -> handler.post(command) }
        }

        try {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButton(negativeButtonText, executor) { _, _ ->
                    callback.onAuthenticationError(
                        BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED,
                        "PIN fallback selected"
                    )
                }
                .build()

            val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    currentCancellationSignal = null
                    callback.onAuthenticationSuccess("Biometric identity verified successfully")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    currentCancellationSignal = null
                    callback.onAuthenticationError(
                        errorCode,
                        errString ?: "Biometric authentication encountered an error"
                    )
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback.onAuthenticationFailed()
                }
            }

            prompt.authenticate(cancellationSignal, executor, authenticationCallback)
            return cancellationSignal
        } catch (e: Exception) {
            currentCancellationSignal = null
            callback.onAuthenticationError(-2, e.localizedMessage ?: "Failed to initialize BiometricPrompt")
            return null
        }
    }

    /**
     * Cancels any active BiometricPrompt session.
     */
    fun cancelAuthentication() {
        currentCancellationSignal?.let {
            if (!it.isCanceled) {
                it.cancel()
            }
        }
        currentCancellationSignal = null
    }

    /**
     * Simulator method for unit tests or emulators lacking physical biometric hardware.
     */
    fun simulateSuccess(callback: BiometricAuthCallback) {
        callback.onAuthenticationStarted()
        Handler(Looper.getMainLooper()).postDelayed({
            callback.onAuthenticationSuccess("Simulated sensor touch confirmed")
        }, 300L)
    }

    /**
     * Simulator method for simulating failed sensor match.
     */
    fun simulateFailure(callback: BiometricAuthCallback) {
        callback.onAuthenticationStarted()
        Handler(Looper.getMainLooper()).postDelayed({
            callback.onAuthenticationFailed()
        }, 300L)
    }

    /**
     * Simulator method for simulating user cancellation or error.
     */
    fun simulateError(errorCode: Int = 10, message: String = "Biometric sensor timeout", callback: BiometricAuthCallback) {
        callback.onAuthenticationStarted()
        Handler(Looper.getMainLooper()).postDelayed({
            callback.onAuthenticationError(errorCode, message)
        }, 300L)
    }
}
