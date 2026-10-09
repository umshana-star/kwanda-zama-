package com.example.security

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatMessage
import com.example.data.local.ConversationDatabase
import com.example.data.local.WhatsAppAgentRepository
import com.example.data.local.ZamaDatabase
import com.example.data.remote.GeminiAgentService
import com.example.data.remote.GeminiAudioTranscriber
import com.example.model.WhatsAppAgentSettingsManager
import com.example.service.whatsapp.WhatsAppAgentService
import com.example.service.whatsapp.api.SandboxWhatsAppCommunicationApi
import com.example.service.whatsapp.intelligence.GeminiAgentIntelligence
import com.example.service.whatsapp.router.WhatsAppInteractionRouter
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductionSecurityAuditTest {

    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var db: ZamaDatabase
    private lateinit var repository: WhatsAppAgentRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var keyStoreManager: EncryptedKeyStoreManager
    private lateinit var settingsManager: WhatsAppAgentSettingsManager
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

        keyStoreManager = EncryptedKeyStoreManager(context)
        keyStoreManager.clearApiKey()

        settingsManager = WhatsAppAgentSettingsManager(context)
        settingsManager.clearAllSettings()

        val commApi = SandboxWhatsAppCommunicationApi()
        val intelligence = GeminiAgentIntelligence(SecureApiKeyProvider(context))
        val agentService = WhatsAppAgentService(
            repository = repository,
            communicationApi = commApi,
            intelligence = intelligence
        )
        val router = WhatsAppInteractionRouter(agentService)

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
    fun testProductionGeminiModelAlignment() {
        assertEquals("Production Gemini model must be gemini-2.5-flash", "gemini-2.5-flash", GeminiAgentService.MODEL_NAME)
        assertEquals("Production audio transcription model must be gemini-2.5-flash", "gemini-2.5-flash", GeminiAudioTranscriber.MODEL_NAME)
    }

    @Test
    fun testDataWipe_erasesAllDatabasesAndEncryptedKeyStore() {
        runBlocking {
            // 1. Populate Room database
            repository.saveChatMessage(
                ChatMessage(
                    messageId = "audit_wipe_msg_01",
                    content = "Customer salon booking inquiry",
                    isFromUser = true,
                    timestamp = "14:20"
                )
            )
            val msgsBefore = repository.allChatMessages.first()
            assertTrue(msgsBefore.isNotEmpty())

            // 2. Establish PIN in auth vault
            val pinResult = authRepository.createPin("951753")
            assertTrue(pinResult is PinSetupResult.Success)
            assertTrue(authRepository.isPinConfigured())

            // 3. Store hardware-encrypted key
            keyStoreManager.storeApiKey("AIzaSyTestProductionAuditKey_987654321")
            assertTrue(keyStoreManager.hasStoredKey())
            assertNotNull(keyStoreManager.retrieveApiKey())

            // 4. Configure agent away mode
            viewModel.toggleAwayMode(true)
            assertTrue(viewModel.isAwayModeActive.value)

            // 5. Execute production data wipe
            viewModel.purgeAllDataSync()

            // 6. Verify total zero-leakage erasure
            val msgsAfter = repository.allChatMessages.first()
            assertEquals(0, msgsAfter.size)
            assertFalse("Security vault PIN must be purged", authRepository.isPinConfigured())
            assertFalse("Encrypted KeyStore API key must be purged", keyStoreManager.hasStoredKey())
            assertNull("Decrypted KeyStore API key must return null", keyStoreManager.retrieveApiKey())
            assertFalse("Away mode must reset to default false", viewModel.isAwayModeActive.value)
        }
    }

    @Test
    fun testAuthentication_bruteForceLockoutEnforcedSynchronously() {
        authRepository.createPin("830192")

        // Fail 5 times to trigger lockout
        for (i in 1..AuthRepository.MAX_FAILED_ATTEMPTS) {
            val result = authRepository.verifyPin("000000")
            if (i < AuthRepository.MAX_FAILED_ATTEMPTS) {
                assertTrue(result is PinVerificationResult.Incorrect)
            } else {
                assertTrue(result is PinVerificationResult.LockedOut)
            }
        }

        assertTrue("User must be locked out after 5 consecutive failures", authRepository.isLockedOut())
        val remainingSecs = authRepository.getLockoutRemainingSeconds()
        assertTrue(remainingSecs > 0)

        // Re-verifying while locked out immediately returns LockedOut without trying hash
        val lockedAttempt = authRepository.verifyPin("830192")
        assertTrue(lockedAttempt is PinVerificationResult.LockedOut)

        // Reset security state resets failed attempts and lockout
        authRepository.resetFailedAttempts()
        assertFalse(authRepository.isLockedOut())
        val okResult = authRepository.verifyPin("830192")
        assertTrue(okResult is PinVerificationResult.Success)
    }

    @Test
    fun testNetworkSecurityConfig_enforcesNoCleartextTraffic() {
        val netConfigXml = listOf(
            File("src/main/res/xml/network_security_config.xml"),
            File("app/src/main/res/xml/network_security_config.xml")
        ).firstOrNull { it.exists() }
        assertNotNull("network_security_config.xml must exist", netConfigXml)
        val content = netConfigXml!!.readText()
        assertTrue("cleartextTrafficPermitted must be false", content.contains("cleartextTrafficPermitted=\"false\""))
    }

    @Test
    fun testDataExtractionAndBackupRules_excludeSensitiveVaults() {
        val dataExtractionRules = listOf(
            File("src/main/res/xml/data_extraction_rules.xml"),
            File("app/src/main/res/xml/data_extraction_rules.xml")
        ).firstOrNull { it.exists() }
        assertNotNull("data_extraction_rules.xml must exist", dataExtractionRules)
        val extractContent = dataExtractionRules!!.readText()
        assertTrue("data_extraction_rules must exclude auth security vault", extractContent.contains("zama_auth_security_vault.xml"))
        assertTrue("data_extraction_rules must exclude credentials vault", extractContent.contains("zama_secure_credentials_vault.xml"))

        val backupRules = listOf(
            File("src/main/res/xml/backup_rules.xml"),
            File("app/src/main/res/xml/backup_rules.xml")
        ).firstOrNull { it.exists() }
        assertNotNull("backup_rules.xml must exist", backupRules)
        val backupContent = backupRules!!.readText()
        assertTrue("backup_rules must exclude auth security vault", backupContent.contains("zama_auth_security_vault.xml"))
        assertTrue("backup_rules must exclude credentials vault", backupContent.contains("zama_secure_credentials_vault.xml"))
    }
}
