package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.BiometricVisualState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BiometricAuthManagerTest {

    private lateinit var context: Context
    private lateinit var manager: BiometricAuthManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = BiometricAuthManager(context)
    }

    @Test
    fun testCanAuthenticate_returnsValidStatus() {
        val status = manager.canAuthenticate()
        assertNotNull(status)
        // In Robolectric without enrolled biometrics, expect NO_HARDWARE or NOT_ENROLLED or AVAILABLE
        assertTrue(
            status in listOf(
                BiometricCapabilityStatus.AVAILABLE,
                BiometricCapabilityStatus.NOT_ENROLLED,
                BiometricCapabilityStatus.NO_HARDWARE,
                BiometricCapabilityStatus.HARDWARE_UNAVAILABLE
            )
        )
    }

    @Test
    fun testSimulateSuccess_triggersSuccessCallback() {
        var startedCalled = false
        var successCalled = false
        var resultMsg = ""

        manager.simulateSuccess(object : BiometricAuthCallback {
            override fun onAuthenticationStarted() {
                startedCalled = true
            }

            override fun onAuthenticationSuccess(resultDescription: String) {
                successCalled = true
                resultMsg = resultDescription
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {}
            override fun onAuthenticationFailed() {}
        })

        assertTrue(startedCalled)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue(successCalled)
        assertEquals("Simulated sensor touch confirmed", resultMsg)
    }

    @Test
    fun testSimulateFailure_triggersFailedCallback() {
        var startedCalled = false
        var failedCalled = false

        manager.simulateFailure(object : BiometricAuthCallback {
            override fun onAuthenticationStarted() {
                startedCalled = true
            }

            override fun onAuthenticationSuccess(resultDescription: String) {}
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {}
            override fun onAuthenticationFailed() {
                failedCalled = true
            }
        })

        assertTrue(startedCalled)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue(failedCalled)
    }

    @Test
    fun testSimulateError_triggersErrorCallback() {
        var startedCalled = false
        var errorCalled = false
        var errorCodeCaptured = 0
        var errorMessageCaptured = ""

        manager.simulateError(
            errorCode = 10,
            message = "Biometric sensor timeout",
            callback = object : BiometricAuthCallback {
                override fun onAuthenticationStarted() {
                    startedCalled = true
                }

                override fun onAuthenticationSuccess(resultDescription: String) {}
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    errorCalled = true
                    errorCodeCaptured = errorCode
                    errorMessageCaptured = errString.toString()
                }

                override fun onAuthenticationFailed() {}
            }
        )

        assertTrue(startedCalled)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue(errorCalled)
        assertEquals(10, errorCodeCaptured)
        assertEquals("Biometric sensor timeout", errorMessageCaptured)
    }

    @Test
    fun testVisualStates_representation() {
        val idle = BiometricVisualState.Idle
        assertNotNull(idle)

        val authenticating = BiometricVisualState.Authenticating("Sensor active")
        assertEquals("Sensor active", authenticating.message)

        val success = BiometricVisualState.Success("Verified")
        assertEquals("Verified", success.message)

        val error = BiometricVisualState.Error("Fingerprint rejected", true)
        assertEquals("Fingerprint rejected", error.errorMessage)
        assertTrue(error.canRetry)
    }
}
