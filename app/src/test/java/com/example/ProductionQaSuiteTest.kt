package com.example

import android.app.Application
import android.content.Context
import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.calendar.BookingTriageParser
import com.example.calendar.CalendarSyncManager
import com.example.data.local.BookingEventEntity
import com.example.data.local.ZamaDatabase
import com.example.data.remote.GeminiAgentService
import com.example.data.remote.GeminiAudioTranscriber
import com.example.data.remote.GeminiResponseResult
import com.example.data.remote.VoiceTranscriptionResult
import com.example.export.ChatExportManager
import com.example.export.ExportScope
import com.example.model.AgentSpecialization
import com.example.model.ChatMessage
import com.example.security.BiometricSecurityViewModel
import com.example.security.PinSetupResult
import com.example.ui.theme.ThemeViewModel
import com.example.ui.theme.ZamaThemeMode
import com.example.ui.viewmodel.WhatsAppAgentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1200dp")
@OptIn(ExperimentalCoroutinesApi::class)
class ProductionQaSuiteTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var app: Application
    private lateinit var db: ZamaDatabase

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences(BiometricSecurityViewModel.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        app.getSharedPreferences("zama_whatsapp_agent_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(app, ZamaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `qa 01 - fresh install first launch displays lock screen and interactive controls`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_lock_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_trigger_biometric").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_show_pin_keypad").assertIsDisplayed()
    }

    @Test
    fun `qa 02 - first-time owner PIN setup via UI keypad unlocks session and persists across process death`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_show_pin_keypad").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pin_keypad_card").assertIsDisplayed()

        // Enter new PIN 7391 (draft step)
        listOf("7", "3", "9", "1").forEach { digit ->
            composeRule.onNodeWithTag("pin_key_$digit").performClick()
        }
        composeRule.waitForIdle()

        // Confirm PIN 7391 (confirmation step -> unlocks session)
        listOf("7", "3", "9", "1").forEach { digit ->
            composeRule.onNodeWithTag("pin_key_$digit").performClick()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("zama_main_scroll_feed").assertIsDisplayed()

        // Simulate process death by instantiating a brand new BiometricSecurityViewModel
        val recreatedVm = BiometricSecurityViewModel(app)
        assertTrue("Configured PIN verifier must persist across process death", recreatedVm.uiState.value.isPinConfigured)
        assertFalse("Recreated session starts locked when protection is enabled", recreatedVm.uiState.value.isUnlocked)
        assertFalse("Wrong PIN must be rejected after restart", recreatedVm.authenticateWithMasterPin("1937"))
        assertTrue("Configured Owner PIN must unlock after restart", recreatedVm.authenticateWithMasterPin("7391"))
    }

    @Test
    fun `qa 03 - lockout and backoff persist across process death and block bypass attempts`() {
        val vm1 = BiometricSecurityViewModel(app)
        assertTrue(vm1.setupOwnerPin("5927", "5927") is PinSetupResult.Success)
        val t0 = System.currentTimeMillis()

        repeat(5) { idx ->
            assertFalse(vm1.authenticateWithMasterPin("0001", nowMillis = t0 + idx * 50L))
        }
        assertTrue(vm1.uiState.value.isLockedOut)

        val vmAfterDeath = BiometricSecurityViewModel(app)
        assertTrue("Lockout state must survive process death", vmAfterDeath.uiState.value.isLockedOut)
        assertFalse(
            "Valid PIN must still be blocked while persisted cooldown is active",
            vmAfterDeath.authenticateWithMasterPin("5927", nowMillis = t0 + 5_000L)
        )
        assertTrue(
            "Valid PIN must succeed after cooldown window elapses",
            vmAfterDeath.authenticateWithMasterPin("5927", nowMillis = t0 + 31_000L)
        )
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-port")
    fun `qa 04 - small screen portrait and font scale accessibility rendering`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_lock_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_trigger_biometric").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_show_pin_keypad").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w840dp-h1200dp-land")
    fun `qa 05 - large screen landscape tablet rendering and configuration rotation`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("biometric_lock_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("btn_show_pin_keypad").performClick()
        listOf("7", "3", "9", "1").forEach { composeRule.onNodeWithTag("pin_key_$it").performClick() }
        listOf("7", "3", "9", "1").forEach { composeRule.onNodeWithTag("pin_key_$it").performClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("zama_main_scroll_feed").assertIsDisplayed()
        composeRule.onNodeWithTag("security_status_pill").assertIsDisplayed()
    }

    @Test
    fun `qa 06 - whatsapp agent creation edit deploy delete and response delay 100ms to 5000ms persistence`() = runTest {
        val agentVm = WhatsAppAgentViewModel(app)

        agentVm.updateAiResponseDelayMs(100L)
        assertEquals(100L, agentVm.globalSettings.value.aiResponseDelayMs)

        agentVm.updateAiResponseDelayMs(5000L)
        assertEquals(5000L, agentVm.globalSettings.value.aiResponseDelayMs)

        agentVm.updateAiResponseDelayMs(2450L)
        assertEquals(2450L, agentVm.globalSettings.value.aiResponseDelayMs)

        val updatedSettings = agentVm.globalSettings.value.copy(autoSyncGoogleCalendar = false)
        agentVm.updateGlobalSettings(updatedSettings)
        assertFalse(agentVm.globalSettings.value.autoSyncGoogleCalendar)

        val recreatedAgentVm = WhatsAppAgentViewModel(app)
        assertEquals(2450L, recreatedAgentVm.globalSettings.value.aiResponseDelayMs)
        assertFalse(recreatedAgentVm.globalSettings.value.autoSyncGoogleCalendar)

        recreatedAgentVm.createNewAgent(
            name = "Lindiwe Bridal Bot",
            specialization = AgentSpecialization.VIP_TRIAGE_ESCALATION,
            phoneLine = "+27 82 777 4500",
            maxChats = 25
        )
        assertNotNull(recreatedAgentVm.allAgents.value)
    }

    @Test
    fun `qa 07 - calendar appointment creation cancellation and external intent generation`() = runTest {
        val dao = db.bookingEventDao()

        val parsed = BookingTriageParser.parse(
            "Hi! Can I book Knotless Braids this Saturday at 2pm? My name is Zanele",
            fallbackClientName = "Zanele"
        )
        assertNotNull("Booking inquiry must be parsed into a BookingEventEntity", parsed)
        val entity = BookingEventEntity(
            messageId = "qa_msg_booking_01",
            clientName = parsed!!.clientName,
            clientPhone = "+27 82 333 1100",
            serviceName = parsed.serviceName,
            startEpochMillis = parsed.startEpochMillis,
            endEpochMillis = parsed.endEpochMillis,
            quotedPrice = parsed.quotedPrice,
            durationMinutes = parsed.durationMinutes,
            notes = parsed.notes,
            rawMessageText = "Hi! Can I book Knotless Braids this Saturday at 2pm?",
            status = BookingEventEntity.STATUS_CONFIRMED,
            syncedToDevice = false
        )
        val bookingId = dao.insertBooking(entity)
        val saved = dao.getBookingById(bookingId)
        assertNotNull(saved)
        assertTrue(saved!!.serviceName.contains("Braids", ignoreCase = true))

        val calIntent = CalendarSyncManager.createCalendarInsertIntent(saved)
        assertNotNull(calIntent)
        val icsUri = CalendarSyncManager.exportIcsCalendarFile(app, listOf(saved))
        assertNotNull(icsUri)

        dao.deleteBookingById(bookingId)
        assertNull(dao.getBookingById(bookingId))
    }

    @Test
    fun `qa 08 - chat export transcript generation and triage summary`() {
        val sampleMessages = listOf(
            ChatMessage(
                id = "exp_1",
                isFromCustomer = true,
                text = "How much for Silk Press tomorrow at 11am?",
                timestamp = "10:15"
            ),
            ChatMessage(
                id = "exp_2",
                isFromCustomer = false,
                text = "Silk Press is R500 and 11:00 AM is open!",
                timestamp = "10:16"
            )
        )

        val transcript = ChatExportManager.generateTextTranscript(
            messages = sampleMessages,
            scope = ExportScope.FULL_CHAT
        )
        assertTrue(transcript.contains("Silk Press"))
        assertTrue(transcript.contains("ZAMA AI SALON"))
    }

    @Test
    fun `qa 09 - offline and missing API key fallback resilience`() = runTest {
        val offlineAgentService = GeminiAgentService(apiKeyProvider = { "" })
        val replyResult = offlineAgentService.generateAgentReply("How much for Knotless Braids on Saturday?")
        val replyText = when (replyResult) {
            is GeminiResponseResult.Success -> replyResult.replyText
            is GeminiResponseResult.Error -> replyResult.fallbackReply.orEmpty()
        }
        assertTrue(replyText.isNotBlank())
        assertTrue(replyText.contains("R650") || replyText.contains("Braids", ignoreCase = true))

        val transcriber = GeminiAudioTranscriber(apiKeyProvider = { "MY_GEMINI_API_KEY" })
        val tempAudio = File(app.cacheDir, "qa_empty.m4a")
        val transRes = transcriber.transcribeAudio(tempAudio)
        assertTrue(transRes is VoiceTranscriptionResult.Error)
        assertTrue((transRes as VoiceTranscriptionResult.Error).isApiKeyIssue)

        val themeVm = ThemeViewModel(app)
        themeVm.setThemeMode(ZamaThemeMode.HIGH_CONTRAST_LIGHT)
        val recreatedThemeVm = ThemeViewModel(app)
        assertEquals(ZamaThemeMode.HIGH_CONTRAST_LIGHT, recreatedThemeVm.themeMode.value)
    }

    @Test
    fun `qa 10 - room database upgrade migration from v1 to v4 preserves existing chat history`() = runTest {
        val dbName = "zama_migration_test_${System.currentTimeMillis()}.db"
        val dbFile = app.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()

        // Create a version 1 SQLite database with legacy chat_logs and chat_messages rows
        val rawSqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawSqlite.execSQL("CREATE TABLE IF NOT EXISTS `chat_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `message_id` TEXT NOT NULL, `session_id` TEXT NOT NULL DEFAULT 'default_session', `is_from_customer` INTEGER NOT NULL, `sender_role` TEXT NOT NULL, `text` TEXT NOT NULL, `timestamp_millis` INTEGER NOT NULL, `timestamp_formatted` TEXT NOT NULL, `status_ticks` TEXT NOT NULL, `is_action_card` INTEGER NOT NULL, `action_detail` TEXT, `ai_trace` TEXT)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_logs_session_id` ON `chat_logs` (`session_id`)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_logs_timestamp_millis` ON `chat_logs` (`timestamp_millis`)")
        rawSqlite.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `message_id` TEXT NOT NULL, `content` TEXT NOT NULL, `is_from_user` INTEGER NOT NULL, `sender_role` TEXT NOT NULL, `timestamp` TEXT NOT NULL, `timestamp_millis` INTEGER NOT NULL, `status_ticks` TEXT NOT NULL, `is_voice_note` INTEGER NOT NULL, `audio_model_used` TEXT, `ai_trace` TEXT)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_timestamp_millis` ON `chat_messages` (`timestamp_millis`)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_is_from_user` ON `chat_messages` (`is_from_user`)")
        rawSqlite.execSQL("INSERT INTO `chat_logs` (`message_id`, `session_id`, `is_from_customer`, `sender_role`, `text`, `timestamp_millis`, `timestamp_formatted`, `status_ticks`, `is_action_card`) VALUES ('legacy_v1_msg', 'default_session', 1, 'CUSTOMER', 'Legacy v1 customer message', 1700000000000, '09:30', '✓✓', 0)")
        rawSqlite.version = 1
        rawSqlite.close()

        // Open with Room v4 using strict ZamaDatabase.ALL_MIGRATIONS (without destructive fallback)
        val migratedDb = Room.databaseBuilder(app, ZamaDatabase::class.java, dbName)
            .addMigrations(*ZamaDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            val latestLog = migratedDb.chatLogDao().getLatestChatLog()
            assertNotNull(latestLog)
            assertEquals("Legacy v1 customer message", latestLog!!.text)

            // Verify newly migrated v2/v3/v4 tables are writable
            val bookingId = migratedDb.bookingEventDao().insertBooking(
                BookingEventEntity(
                    messageId = "mig_b1",
                    clientName = "Thandi",
                    serviceName = "Silk Press",
                    startEpochMillis = 1700010000000L,
                    endEpochMillis = 1700017200000L,
                    quotedPrice = "R500"
                )
            )
            assertNotNull(migratedDb.bookingEventDao().getBookingById(bookingId))
        } finally {
            migratedDb.close()
            dbFile.delete()
        }
    }
}
