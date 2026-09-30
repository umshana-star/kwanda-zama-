package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the reactive authentication flow states (Idle, Authenticating, Success, Error)
 * emitted by [BiometricAuthManager] and rendered by `BiometricPromptStatusView`.
 */
sealed class BiometricAuthFlowState {
    data class Idle(
        val message: String = "Biometric sensor ready. Tap to authenticate."
    ) : BiometricAuthFlowState() {
        companion object : BiometricAuthFlowState()
    }

    data class Authenticating(
        val message: String = "Waiting for user biometric verification..."
    ) : BiometricAuthFlowState() {
        companion object : BiometricAuthFlowState()
    }

    data class Success(
        val message: String = "Biometric identity verified."
    ) : BiometricAuthFlowState() {
        companion object : BiometricAuthFlowState()
    }

    data class Error(
        val message: String = "Biometric authentication failed. Please try again or use Owner PIN.",
        val errorCode: Int = -1
    ) : BiometricAuthFlowState() {
        companion object : BiometricAuthFlowState()
    }
}

typealias BiometricAuthState = BiometricAuthFlowState

/**
 * Hardware capability status for biometric authentication on the device.
 */
sealed class BiometricCapability {
    data class Available(
        val canUseBiometric: Boolean,
        val canUseDeviceCredential: Boolean,
        val sensorTypes: List<String>
    ) : BiometricCapability()

    object NoneEnrolled : BiometricCapability()
    object HardwareUnavailable : BiometricCapability()
    object NotSupported : BiometricCapability()
}

/**
 * Result of a biometric authentication attempt.
 */
sealed class BiometricAuthResult {
    data class Success(val message: String = "Biometric authentication successful") : BiometricAuthResult()
    object Failed : BiometricAuthResult()
    data class Error(val errorCode: Int, val message: String) : BiometricAuthResult()
    object Cancelled : BiometricAuthResult()
}

/**
 * Manager class handling hardware readiness checks and [BiometricPrompt] execution
 * via `androidx.biometric` to authenticate the user and trigger success/error callbacks
 * for the application's secure entry flow.
 */
class BiometricAuthManager(
    private val appContext: Context? = null
) {

    private val boundActivity: FragmentActivity? = appContext as? FragmentActivity

    private val _authFlowState = MutableStateFlow<BiometricAuthFlowState>(BiometricAuthFlowState.Idle())

    /**
     * Reactive stream of the current biometric authentication flow state
     * ([BiometricAuthFlowState.Idle], [BiometricAuthFlowState.Authenticating],
     * [BiometricAuthFlowState.Success], [BiometricAuthFlowState.Error]).
     */
    val authFlowState: StateFlow<BiometricAuthFlowState> = _authFlowState.asStateFlow()

    /**
     * Updates the current [authFlowState] directly.
     */
    fun setFlowState(state: BiometricAuthFlowState) {
        _authFlowState.value = state
    }

    /**
     * Resets the current [authFlowState] back to [BiometricAuthFlowState.Idle].
     */
    fun resetFlowState(message: String = "Biometric sensor ready. Tap to authenticate.") {
        _authFlowState.value = BiometricAuthFlowState.Idle(message)
    }

    /**
     * Checks whether biometric hardware or device credentials are available using the bound [appContext]
     * or a supplied [context].
     */
    fun checkBiometricCapability(context: Context? = appContext): BiometricCapability {
        val targetContext = context ?: appContext ?: return BiometricCapability.NotSupported
        return Companion.checkBiometricCapability(targetContext)
    }

    /**
     * Returns true if `androidx.biometric.BiometricManager` reports that authentication is ready.
     */
    fun canAuthenticate(context: Context? = appContext): Boolean {
        return checkBiometricCapability(context) is BiometricCapability.Available
    }

    /**
     * Launches `androidx.biometric.BiometricPrompt` and invokes dedicated success, error,
     * failed, and cancellation callbacks for the application's secure entry flow.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = DEFAULT_TITLE,
        subtitle: String = DEFAULT_SUBTITLE,
        description: String = DEFAULT_DESCRIPTION,
        negativeButtonText: String = DEFAULT_NEGATIVE_BUTTON,
        onSuccess: (BiometricPrompt.AuthenticationResult?) -> Unit = {},
        onError: (errorCode: Int, errString: String) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {},
        onCancelled: () -> Unit = {
            onError(BiometricPrompt.ERROR_USER_CANCELED, "Authentication cancelled")
        }
    ) {
        _authFlowState.value = BiometricAuthFlowState.Authenticating()
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = createAuthenticationCallback(
            onSuccess = onSuccess,
            onError = onError,
            onFailed = onFailed,
            onCancelled = onCancelled
        )

        try {
            val promptInfo = buildPromptInfo(
                context = activity,
                title = title,
                subtitle = subtitle,
                description = description,
                negativeButtonText = negativeButtonText
            )
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: "Failed to initialize biometric prompt"
            _authFlowState.value = BiometricAuthFlowState.Error(errorCode = -1, message = errMsg)
            onError(-1, errMsg)
        }
    }

    /**
     * Convenience overload that launches `androidx.biometric.BiometricPrompt` on the [boundActivity]
     * supplied at construction time.
     */
    fun authenticate(
        title: String = DEFAULT_TITLE,
        subtitle: String = DEFAULT_SUBTITLE,
        description: String = DEFAULT_DESCRIPTION,
        negativeButtonText: String = DEFAULT_NEGATIVE_BUTTON,
        onSuccess: (BiometricPrompt.AuthenticationResult?) -> Unit = {},
        onError: (errorCode: Int, errString: String) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {},
        onCancelled: () -> Unit = {
            onError(BiometricPrompt.ERROR_USER_CANCELED, "Authentication cancelled")
        }
    ) {
        val activity = boundActivity
        if (activity == null) {
            val msg = "FragmentActivity is required to display BiometricPrompt"
            _authFlowState.value = BiometricAuthFlowState.Error(errorCode = -1, message = msg)
            onError(-1, msg)
            return
        }
        authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            description = description,
            negativeButtonText = negativeButtonText,
            onSuccess = onSuccess,
            onError = onError,
            onFailed = onFailed,
            onCancelled = onCancelled
        )
    }

    /**
     * Triggers the native Android [BiometricPrompt] dialog and emits a unified [BiometricAuthResult].
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = DEFAULT_TITLE,
        subtitle: String = DEFAULT_SUBTITLE,
        description: String = DEFAULT_DESCRIPTION,
        onResult: (BiometricAuthResult) -> Unit = {}
    ) {
        authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            description = description,
            onSuccess = {
                onResult(BiometricAuthResult.Success("Authenticated successfully"))
            },
            onError = { errorCode, errString ->
                onResult(BiometricAuthResult.Error(errorCode, errString))
            },
            onFailed = {
                onResult(BiometricAuthResult.Failed)
            },
            onCancelled = {
                onResult(BiometricAuthResult.Cancelled)
            }
        )
    }

    /**
     * Creates a [BiometricPrompt.AuthenticationCallback] routing `androidx.biometric` events
     * to the provided success/error/failed/cancelled lambdas and updating [authFlowState].
     */
    fun createAuthenticationCallback(
        onSuccess: (BiometricPrompt.AuthenticationResult?) -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit,
        onFailed: () -> Unit = {},
        onCancelled: () -> Unit = {
            onError(BiometricPrompt.ERROR_USER_CANCELED, "Authentication cancelled")
        }
    ): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _authFlowState.value = BiometricAuthFlowState.Success("Biometric identity verified.")
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val message = errString.toString()
                if (
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    _authFlowState.value = BiometricAuthFlowState.Idle("Authentication paused. Tap to resume.")
                    onCancelled()
                } else {
                    _authFlowState.value = BiometricAuthFlowState.Error(errorCode = errorCode, message = message)
                    onError(errorCode, message)
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                _authFlowState.value = BiometricAuthFlowState.Error(
                    errorCode = -2,
                    message = "Biometric not recognized. Please try again or use Owner PIN."
                )
                onFailed()
            }
        }
    }

    /**
     * Builds a [BiometricPrompt.PromptInfo] configured with strong/weak biometrics
     * and device credential or Owner PIN fallback.
     */
    fun buildPromptInfo(
        context: Context,
        title: String = DEFAULT_TITLE,
        subtitle: String = DEFAULT_SUBTITLE,
        description: String = DEFAULT_DESCRIPTION,
        negativeButtonText: String = DEFAULT_NEGATIVE_BUTTON
    ): BiometricPrompt.PromptInfo {
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        val biometricManager = BiometricManager.from(context)
        val canDeviceCredential = runCatching {
            biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
        }.getOrDefault(false)

        if (canDeviceCredential) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            promptInfoBuilder.setNegativeButtonText(negativeButtonText)
        }

        return promptInfoBuilder.build()
    }

    companion object {
        const val DEFAULT_TITLE = "Unlock Zama Business Hub"
        const val DEFAULT_SUBTITLE = "Kwanda Zama Salon Owner Authentication"
        const val DEFAULT_DESCRIPTION =
            "Verify your fingerprint, face unlock, or device credential to access private customer triage logs and WhatsApp threads."
        const val DEFAULT_NEGATIVE_BUTTON = "Use Owner PIN"

        /**
         * Checks whether biometric hardware and device credentials are available on [context].
         */
        fun checkBiometricCapability(context: Context): BiometricCapability {
            return try {
                val biometricManager = BiometricManager.from(context)
                val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
                when (biometricManager.canAuthenticate(authenticators)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.Available(
                        canUseBiometric = true,
                        canUseDeviceCredential = true,
                        sensorTypes = listOf("Fingerprint", "Face Unlock")
                    )
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NoneEnrolled
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.HardwareUnavailable
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NotSupported
                    BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapability.HardwareUnavailable
                    else -> BiometricCapability.NotSupported
                }
            } catch (_: Throwable) {
                BiometricCapability.NotSupported
            }
        }

        /**
         * Static helper to display [BiometricPrompt] and return a [BiometricAuthResult].
         */
        fun showBiometricPrompt(
            activity: FragmentActivity,
            title: String = DEFAULT_TITLE,
            subtitle: String = DEFAULT_SUBTITLE,
            description: String = DEFAULT_DESCRIPTION,
            onResult: (BiometricAuthResult) -> Unit
        ) {
            BiometricAuthManager(activity).showBiometricPrompt(
                activity = activity,
                title = title,
                subtitle = subtitle,
                description = description,
                onResult = onResult
            )
        }
    }
}
