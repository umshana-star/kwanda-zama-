package com.example.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.AgentOperationalStatus
import com.example.model.AgentSpecialization
import com.example.model.EventCategoryFilter
import com.example.model.HandoffTriggerMode
import com.example.model.WhatsAppAgentEntity
import com.example.model.WhatsAppAgentEventEntity
import com.example.model.WhatsAppAgentEventType
import com.example.model.WhatsAppEventSeverity
import com.example.model.WhatsAppGlobalSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class WhatsAppAgentTest {

    private lateinit var app: Application
    private lateinit var viewModel: WhatsAppAgentViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("zama_whatsapp_agent_prefs", 0).edit().clear().commit()
        viewModel = WhatsAppAgentViewModel(app)
    }

    @Test
    fun testDataModelCreationAndEnums() {
        val agent = WhatsAppAgentEntity(
            id = "agent_test_01",
            name = "Thandiwe",
            specialization = AgentSpecialization.SALON_CONCIERGE.name,
            avatarEmoji = "💇‍♀️",
            status = AgentOperationalStatus.ACTIVE_ONLINE.name,
            isAutonomousEnabled = true,
            currentActiveChats = 4,
            maxConcurrentChats = 20,
            avgResponseLatencyMs = 820L,
            confidenceThreshold = 0.88f,
            simulatedTypingDelaySec = 1.5f,
            sentimentScore = 0.96f
        )

        assertEquals("agent_test_01", agent.id)
        assertEquals("Thandiwe", agent.name)
        assertEquals(AgentSpecialization.SALON_CONCIERGE, agent.agentSpecialization)
        assertEquals(AgentOperationalStatus.ACTIVE_ONLINE, agent.operationalStatus)
        assertEquals(HandoffTriggerMode.NEGATIVE_SENTIMENT, agent.triggerMode)
        assertTrue(agent.isAutonomousEnabled)
        assertEquals(4, agent.currentActiveChats)
        assertEquals(20, agent.maxConcurrentChats)
    }

    @Test
    fun testGlobalSettingsDefaultsAndKillSwitch() = runTest {
        val initialSettings = viewModel.globalSettings.value
        assertTrue(initialSettings.masterAutonomousActive)
        assertEquals("08:00", initialSettings.businessHoursStart)
        assertEquals("20:00", initialSettings.businessHoursEnd)

        // Toggle Master Kill-Switch to pause all agents
        viewModel.toggleMasterKillSwitch(false)
        val updatedSettings = viewModel.globalSettings.value
        assertFalse(updatedSettings.masterAutonomousActive)

        // Re-enable
        viewModel.toggleMasterKillSwitch(true)
        assertTrue(viewModel.globalSettings.value.masterAutonomousActive)
    }

    @Test
    fun testStatusFilterToggling() {
        assertEquals(null, viewModel.selectedStatusFilter.value)

        viewModel.setFilter(AgentOperationalStatus.ACTIVE_ONLINE)
        assertEquals(AgentOperationalStatus.ACTIVE_ONLINE, viewModel.selectedStatusFilter.value)

        viewModel.setFilter(AgentOperationalStatus.BUSY_HANDLING)
        assertEquals(AgentOperationalStatus.BUSY_HANDLING, viewModel.selectedStatusFilter.value)

        viewModel.setFilter(null)
        assertEquals(null, viewModel.selectedStatusFilter.value)
    }

    @Test
    fun testCreateNewAgentPreset() = runTest {
        viewModel.createNewAgent(
            name = "Busisiwe",
            specialization = AgentSpecialization.AFTERCARE_SUPPORT,
            phoneLine = "+27 82 555 0198",
            maxChats = 15
        )

        assertNotNull(viewModel.allAgents.value)
    }

    @Test
    fun testFactoryInstantiation() {
        val factory = WhatsAppAgentViewModel.Factory(app)
        val vm = factory.create(WhatsAppAgentViewModel::class.java)
        assertNotNull(vm)
    }

    @Test
    fun testEventEntityCreationAndEnums() {
        val event = WhatsAppAgentEventEntity(
            id = "evt_test_01",
            agentId = "agent_thandiwe_01",
            agentName = "Thandiwe",
            eventType = WhatsAppAgentEventType.MESSAGE_INBOUND.name,
            title = "Inbound Customer Inquiry",
            detail = "Do you have Saturday at 2pm available?",
            customerPhone = "+27 82 491 8820",
            customerName = "Lerato K.",
            formattedTime = "14:10",
            severity = WhatsAppEventSeverity.INFO.name,
            latencyMs = 720L,
            confidence = 0.95f,
            aiTrace = "Intent: BookingSlotRequest",
            metadataBadge = "Inbound"
        )

        assertEquals("evt_test_01", event.id)
        assertEquals(WhatsAppAgentEventType.MESSAGE_INBOUND, event.typedEventType)
        assertEquals(WhatsAppEventSeverity.INFO, event.typedSeverity)
        assertTrue(event.typedEventType.isMessage)
        assertEquals("+27 82 491 8820", event.customerPhone)
        assertEquals("Lerato K.", event.customerName)
        assertEquals(720L, event.latencyMs)
        assertEquals(0.95f, event.confidence)
    }

    @Test
    fun testEventCategoryFilterOptions() {
        assertEquals(EventCategoryFilter.ALL, viewModel.selectedEventCategory.value)

        viewModel.setEventCategoryFilter(EventCategoryFilter.MESSAGES_ONLY)
        assertEquals(EventCategoryFilter.MESSAGES_ONLY, viewModel.selectedEventCategory.value)

        viewModel.setEventCategoryFilter(EventCategoryFilter.STATUS_EVENTS_ONLY)
        assertEquals(EventCategoryFilter.STATUS_EVENTS_ONLY, viewModel.selectedEventCategory.value)

        viewModel.setEventCategoryFilter(EventCategoryFilter.ESCALATIONS_ONLY)
        assertEquals(EventCategoryFilter.ESCALATIONS_ONLY, viewModel.selectedEventCategory.value)
    }

    @Test
    fun testAgentLogFilterAndSearchQuery() {
        assertEquals(null, viewModel.selectedAgentLogFilter.value)
        assertEquals("", viewModel.logSearchQuery.value)

        viewModel.setSelectedAgentLogFilter("agent_sipho_02")
        assertEquals("agent_sipho_02", viewModel.selectedAgentLogFilter.value)

        viewModel.setLogSearchQuery("knotless")
        assertEquals("knotless", viewModel.logSearchQuery.value)

        viewModel.setSelectedAgentLogFilter(null)
        assertEquals(null, viewModel.selectedAgentLogFilter.value)
    }

    @Test
    fun testTriggerEscalationAlertAndClear() = runTest {
        viewModel.triggerEscalationAlert("agent_zama_vip_04", "VIP Client requested direct consultation")
        assertNotNull(viewModel.filteredHistoryLogs.value)

        viewModel.clearAllLogs()
        assertNotNull(viewModel.filteredHistoryLogs.value)
    }

    @Test
    fun testHourlyMessageMetricsState() {
        val hourlyMetrics = viewModel.hourlyMessageMetrics.value
        assertNotNull(hourlyMetrics)
        assertTrue(hourlyMetrics.hourlyPoints.isNotEmpty())
        assertEquals(null, viewModel.selectedChartAgentId.value)
        assertEquals("Today", viewModel.selectedChartTimeframe.value)

        viewModel.selectChartAgent("agent_thandiwe_01")
        assertEquals("agent_thandiwe_01", viewModel.selectedChartAgentId.value)

        viewModel.selectChartTimeframe("Past 12 Hours")
        assertEquals("Past 12 Hours", viewModel.selectedChartTimeframe.value)
    }

    @Test
    fun testAiResponseDelayMsUpdate() {
        assertEquals(850L, viewModel.globalSettings.value.aiResponseDelayMs)

        viewModel.updateAiResponseDelayMs(1250L)
        assertEquals(1250L, viewModel.globalSettings.value.aiResponseDelayMs)

        // Verify clamping to valid range (100ms - 5000ms)
        viewModel.updateAiResponseDelayMs(50L)
        assertEquals(100L, viewModel.globalSettings.value.aiResponseDelayMs)

        viewModel.updateAiResponseDelayMs(7500L)
        assertEquals(5000L, viewModel.globalSettings.value.aiResponseDelayMs)
    }
}
