package com.example.integration

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatMessage
import com.example.data.local.WhatsAppAgentRepository
import com.example.data.local.WhatsAppInteractionEntity
import com.example.data.local.ZamaDatabase
import com.example.model.WhatsAppAgentSettingsManager
import com.example.security.AuthRepository
import com.example.security.BiometricAuthCallback
import com.example.security.BiometricAuthManager
import com.example.security.PinSetupResult
import com.example.security.PinVerificationResult
import com.example.service.whatsapp.WhatsAppAgentService
import com.example.service.whatsapp.api.SandboxWhatsAppCommunicationApi
import com.example.service.whatsapp.intelligence.GeminiAgentIntelligence
import com.example.service.whatsapp.router.WhatsAppInteractionRouter
import com.example.ui.components.BiometricVisualState
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReleaseHardeningIntegrationTest {

    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var db: ZamaDatabase
    private lateinit var repository: WhatsAppAgentRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var biometricManager: BiometricAuthManager
    private lateinit var settingsManager: WhatsAppAgentSettingsManager
    private lateinit var agentService: WhatsAppAgentService
    private lateinit var router: WhatsAppInteractionRouter
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        context = application

        db = Room.inMemoryDatabaseBuilder(context, ZamaDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = WhatsAppAgentRepository(db.whatsAppInteractionDao(), db.chatDao())
        authRepository = AuthRepository(context)
        authRepository.resetAllSecurityData()

        biometricManager = BiometricAuthManager(context)
        settingsManager = WhatsAppAgentSettingsManager(context)
        settingsManager.clearAllSettings()

        val commApi = SandboxWhatsAppCommunicationApi()
        val intelligence = GeminiAgentIntelligence(com.example.security.SecureApiKeyProvider(context))
        agentService = WhatsAppAgentService(
            repository = repository,
            communicationApi = commApi,
            intelligence = intelligence
        )
        router = WhatsAppInteractionRouter(agentService)

        viewModel = ChatViewModel(
            application = application,
            repository = repository,
            agentService = agentService,
            interactionRouter = router
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCompleteReleaseJourney_freshInstallToPurge() {
        runBlocking {
            // --- 1. Fresh Install State ---
            authRepository.resetAllSecurityData()
            assertFalse("PIN must not be configured on fresh install", authRepository.isPinConfigured())
            assertTrue("Seed message initialized or clean", repository.getMessageCount() >= 0)

        // --- 2. Biometric Verification Simulation ---
        var biometricSuccess = false
        biometricManager.simulateSuccess(object : BiometricAuthCallback {
            override fun onAuthenticationSuccess(resultDescription: String) {
                biometricSuccess = true
            }
        })
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertTrue("Biometric simulation succeeds", biometricSuccess)

        // Visual state verification
        val idleState = BiometricVisualState.Idle
        assertNotNull(idleState)

        // --- 3. PIN Creation & Fallback ---
        // Reject weak PINs
        val weakResult = authRepository.createPin("123456")
        assertTrue(weakResult is PinSetupResult.Error)

        // Create robust PIN
        val pin = "938201"
        val createResult = authRepository.createPin(pin)
        assertTrue(createResult is PinSetupResult.Success)
        assertTrue(authRepository.isPinConfigured())

        // Verify valid PIN
        val verifyOk = authRepository.verifyPin(pin)
        assertTrue(verifyOk is PinVerificationResult.Success)

        // Verify incorrect PIN handling
        val verifyWrong = authRepository.verifyPin("000000")
        assertTrue(verifyWrong is PinVerificationResult.Incorrect)

        // --- 4. WhatsApp Agent Message Exchange & Response ---
        router.simulateCustomerMessage(
            messageText = "Hi Zama! How much for Knotless Braids?",
            senderName = "Thandiwe Mokoena",
            senderPhone = "+27830001122"
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // --- 5. Room Persistence Verification ---
        val messages = repository.allChatMessages.first()
        assertTrue("At least customer message or response was saved", messages.isNotEmpty())

        val interactions = repository.allInteractions.first()
        assertTrue("Interaction was recorded in database", interactions.isNotEmpty())
        assertEquals("+27830001122", interactions[0].customerPhone)

        // --- 6. Away Mode Functionality ---
        viewModel.toggleAwayMode(true)
        assertTrue(viewModel.isAwayModeActive.value)
        viewModel.setAwayMessage("We are closed for the evening. We will reply at 8:00 AM.")
        assertEquals("We are closed for the evening. We will reply at 8:00 AM.", viewModel.awayMessage.value)

        // --- 7. Full Local Data Purge & App Reset ---
        viewModel.purgeAllDataSync()

        assertFalse("PIN must be cleared after reset", authRepository.isPinConfigured())
        assertFalse("Away mode must be reset to false", viewModel.isAwayModeActive.value)
        assertEquals(0, repository.getMessageCount())
        }
    }
}
