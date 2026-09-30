package com.example.analytics

import com.example.data.local.ChatLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAnalyticsEngineTest {

    @Test
    fun analyzeMessageSentiment_positiveKeywords_returnsHighSentiment() {
        val entity = ChatLogEntity(
            messageId = "test_1",
            sessionId = "test_session",
            isFromCustomer = true,
            senderRole = "CUSTOMER",
            text = "I love this! Yes please book and reserve it, thanks so much!",
            timestampMillis = System.currentTimeMillis(),
            timestampFormatted = "14:00"
        )

        val score = ChatAnalyticsEngine.analyzeMessageSentiment(entity)
        assertTrue("Positive score should be greater than 0.5, was $score", score > 0.5f)
        val category = ChatAnalyticsEngine.classifySentiment(score, entity.text)
        assertEquals(SentimentCategory.POSITIVE, category)
    }

    @Test
    fun analyzeMessageSentiment_negativeKeywords_returnsLowSentiment() {
        val entity = ChatLogEntity(
            messageId = "test_2",
            sessionId = "test_session",
            isFromCustomer = true,
            senderRole = "CUSTOMER",
            text = "Too expensive and late, please cancel this bad appointment immediately",
            timestampMillis = System.currentTimeMillis(),
            timestampFormatted = "15:00"
        )

        val score = ChatAnalyticsEngine.analyzeMessageSentiment(entity)
        assertTrue("Negative score should be negative, was $score", score < 0.0f)
        val category = ChatAnalyticsEngine.classifySentiment(score, entity.text)
        assertEquals(SentimentCategory.CONCERNED, category)
    }

    @Test
    fun computeAnalytics_aggregatesFrequencyAndSummaryCorrectly() {
        val traffic = ChatAnalyticsEngine.createSimulatedTraffic()
        assertTrue("Simulated traffic should have items", traffic.isNotEmpty())

        val summary = ChatAnalyticsEngine.computeAnalytics(traffic)
        assertEquals(traffic.size, summary.totalMessages)
        assertTrue(summary.customerMessages > 0)
        assertTrue(summary.aiMessages > 0)
        assertTrue(summary.hourlyFrequency.size == 24)
        assertNotNull(summary.peakActivityHour)
    }

    @Test
    fun buildD3Html_generatesValidHtmlString() {
        val traffic = ChatAnalyticsEngine.createSimulatedTraffic()
        val summary = ChatAnalyticsEngine.computeAnalytics(traffic)
        val html = D3ChartHtmlGenerator.buildD3Html(summary)

        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("CHAT ACTIVITY FREQUENCY (D3.js)"))
        assertTrue(html.contains("SENTIMENT TRAJECTORY (D3.js)"))
        assertTrue(html.contains("svg viewBox"))
    }
}
