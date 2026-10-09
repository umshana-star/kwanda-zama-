package com.example.ui

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatMessage
import com.example.data.local.WhatsAppAgentRepository
import com.example.data.local.ZamaDatabase
import com.example.model.WhatsAppAgentSettingsManager
import com.example.security.AuthRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrivacySettingsScreenTest {

    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var db: ZamaDatabase
    private lateinit var repository: WhatsAppAgentRepository
    private lateinit var authRepository: AuthRepository
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

        settingsManager = WhatsAppAgentSettingsManager(context)
        settingsManager.clearAllSettings()

        val commApi = SandboxWhatsAppCommunicationApi()
        val intelligence = GeminiAgentIntelligence(com.example.security.SecureApiKeyProvider(context))
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
    fun testWipeAllLocalData_clearsDatabaseAndSecurityVault() {
        runBlocking {
            // Seed sample chat message
            repository.saveChatMessage(
                ChatMessage(
                    messageId = "test_privacy_001",
                    content = "Customer inquiry message",
                    isFromUser = true,
                    timestamp = "10:30"
                )
            )
            val historyBefore = repository.allChatMessages.first()
            assertTrue(historyBefore.isNotEmpty())

            // Seed PIN
            authRepository.createPin("859203")
            assertTrue(authRepository.isPinConfigured())

            // Seed Away mode
            viewModel.toggleAwayMode(true)
            assertTrue(viewModel.isAwayModeActive.value)

            // Trigger Wipe All Local Data
            viewModel.purgeAllDataSync()

            // Verify complete erasure
            val historyAfter = repository.allChatMessages.first()
            assertEquals(0, historyAfter.size)
            assertFalse(authRepository.isPinConfigured())
            assertFalse(viewModel.isAwayModeActive.value)
        }
    }
}
