package com.example.service.whatsapp.proxy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Server-Side Proxy Client for executing Gemini inference without storing
 * API keys on client devices or inside APK packages.
 *
 * In production environments, requests are proxied through a trusted backend service
 * (e.g. Firebase Cloud Functions, Cloud Run, or custom API gateway) that injects
 * backend-held credentials and enforces rate limiting, quota controls, and authentication.
 */
class GeminiServerProxyClient(
    var proxyEndpointUrl: String = "https://api.zama.ai/v1/gemini/proxy",
    var isProxyEnabled: Boolean = false,
    private val authTokenProvider: () -> String? = { null }
) {

    /**
     * Dispatches a message prompt to the server-side proxy endpoint.
     * Returns the synthesized response text if successful, or null on error / when disabled.
     */
    suspend fun queryProxy(
        prompt: String,
        senderName: String = "Client",
        history: List<Pair<String, Boolean>> = emptyList()
    ): String? = withContext(Dispatchers.IO) {
        if (!isProxyEnabled) return@withContext null

        try {
            val url = URL(proxyEndpointUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Client-App", "Zama-WhatsApp-Agent-Android")

            val token = authTokenProvider()
            if (!token.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }

            connection.connectTimeout = 12000
            connection.readTimeout = 18000
            connection.doOutput = true

            val payload = JSONObject().apply {
                put("message", prompt)
                put("senderName", senderName)
                put("historyCount", history.size)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { it.write(payload.toString()) }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
                val json = JSONObject(responseText)
                json.optString("replyText", null) ?: json.optString("reply", null)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
