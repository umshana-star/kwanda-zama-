package com.example.service.whatsapp.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Direct HTTP Client for communicating with the official Meta WhatsApp Business Cloud API.
 * Encapsulates network transport, authentication headers, error serialization,
 * and JSON payload construction.
 */
class WhatsAppBusinessApiClient(
    val phoneNumberIdProvider: () -> String = { "109847291823901" },
    val accessTokenProvider: () -> String = { System.getenv("WHATSAPP_BUSINESS_API_TOKEN") ?: "" },
    val baseUrl: String = "https://graph.facebook.com/v21.0/"
) {

    /**
     * Sends an outbound text message to a customer's WhatsApp number.
     */
    suspend fun sendTextMessage(
        recipientPhoneNumber: String,
        bodyText: String,
        replyToMessageId: String? = null
    ): WhatsAppSendResult = withContext(Dispatchers.IO) {
        val phoneNumberId = phoneNumberIdProvider()
        val accessToken = accessTokenProvider()

        if (accessToken.isBlank()) {
            return@withContext WhatsAppSendResult.Error(
                errorMessage = "WhatsApp Business API Access Token is not configured. Configure WHATSAPP_BUSINESS_API_TOKEN in Secrets.",
                errorCode = 401
            )
        }

        try {
            val endpoint = "${baseUrl.trimEnd('/')}/$phoneNumberId/messages"
            val payload = JSONObject().apply {
                put("messaging_product", "whatsapp")
                put("recipient_type", "individual")
                put("to", sanitizePhoneNumber(recipientPhoneNumber))
                put("type", "text")
                put("text", JSONObject().apply {
                    put("preview_url", false)
                    put("body", bodyText)
                })

                if (!replyToMessageId.isNullOrBlank()) {
                    put("context", JSONObject().apply {
                        put("message_id", replyToMessageId)
                    })
                }
            }

            executePostRequest(endpoint, payload.toString(), accessToken)
        } catch (e: Exception) {
            WhatsAppSendResult.Error(
                errorMessage = "Failed to send WhatsApp message: ${e.message}",
                isTransient = true
            )
        }
    }

    /**
     * Sends an interactive button message (up to 3 quick action buttons) via WhatsApp Business API.
     */
    suspend fun sendInteractiveButtonMessage(
        recipientPhoneNumber: String,
        bodyText: String,
        buttons: List<String>,
        replyToMessageId: String? = null
    ): WhatsAppSendResult = withContext(Dispatchers.IO) {
        val phoneNumberId = phoneNumberIdProvider()
        val accessToken = accessTokenProvider()

        if (accessToken.isBlank()) {
            return@withContext WhatsAppSendResult.Error(
                errorMessage = "WhatsApp Business API Access Token is missing.",
                errorCode = 401
            )
        }

        try {
            val endpoint = "${baseUrl.trimEnd('/')}/$phoneNumberId/messages"
            val actionButtonsArray = JSONArray()

            // WhatsApp Business API allows a maximum of 3 quick reply buttons
            buttons.take(3).forEachIndexed { index, buttonTitle ->
                actionButtonsArray.put(
                    JSONObject().apply {
                        put("type", "reply")
                        put("reply", JSONObject().apply {
                            put("id", "btn_action_${index + 1}_${UUID.randomUUID().toString().take(6)}")
                            put("title", buttonTitle.take(20)) // 20 character maximum for button titles
                        })
                    }
                )
            }

            val payload = JSONObject().apply {
                put("messaging_product", "whatsapp")
                put("recipient_type", "individual")
                put("to", sanitizePhoneNumber(recipientPhoneNumber))
                put("type", "interactive")
                put("interactive", JSONObject().apply {
                    put("type", "button")
                    put("body", JSONObject().apply {
                        put("text", bodyText)
                    })
                    put("action", JSONObject().apply {
                        put("buttons", actionButtonsArray)
                    })
                })

                if (!replyToMessageId.isNullOrBlank()) {
                    put("context", JSONObject().apply {
                        put("message_id", replyToMessageId)
                    })
                }
            }

            executePostRequest(endpoint, payload.toString(), accessToken)
        } catch (e: Exception) {
            WhatsAppSendResult.Error(
                errorMessage = "Failed to dispatch interactive buttons: ${e.message}",
                isTransient = true
            )
        }
    }

    /**
     * Sends a status update to mark an incoming customer message as read.
     */
    suspend fun markMessageAsRead(messageId: String): Boolean = withContext(Dispatchers.IO) {
        val phoneNumberId = phoneNumberIdProvider()
        val accessToken = accessTokenProvider()

        if (accessToken.isBlank()) return@withContext false

        try {
            val endpoint = "${baseUrl.trimEnd('/')}/$phoneNumberId/messages"
            val payload = JSONObject().apply {
                put("messaging_product", "whatsapp")
                put("status", "read")
                put("message_id", messageId)
            }

            val connection = openConnection(endpoint, accessToken)
            OutputStreamWriter(connection.outputStream, "UTF-8").use { it.write(payload.toString()) }

            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }

    private fun executePostRequest(
        endpoint: String,
        jsonBody: String,
        accessToken: String
    ): WhatsAppSendResult {
        val connection = openConnection(endpoint, accessToken)
        OutputStreamWriter(connection.outputStream, "UTF-8").use { it.write(jsonBody) }

        val responseCode = connection.responseCode
        return if (responseCode in 200..299) {
            val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
            val responseJson = JSONObject(responseText)
            val messageId = responseJson.optJSONArray("messages")
                ?.optJSONObject(0)
                ?.optString("id") ?: "wamid_generated_${UUID.randomUUID().toString().take(12)}"

            WhatsAppSendResult.Success(
                messageId = messageId,
                deliveryStatus = WhatsAppDeliveryStatus.SENT
            )
        } else {
            val errorStream = connection.errorStream
            val errorBody = if (errorStream != null) {
                BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
            } else {
                "HTTP $responseCode"
            }

            val parsedError = parseGraphApiError(errorBody)
            WhatsAppSendResult.Error(
                errorMessage = parsedError ?: "WhatsApp API returned HTTP $responseCode: $errorBody",
                errorCode = responseCode
            )
        }
    }

    private fun openConnection(endpoint: String, accessToken: String): HttpURLConnection {
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("User-Agent", "Zama-Autonomous-WhatsApp-Agent/2.0")
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.doOutput = true
        return connection
    }

    private fun sanitizePhoneNumber(raw: String): String {
        return raw.filter { it.isDigit() }
    }

    private fun parseGraphApiError(json: String): String? {
        return try {
            val root = JSONObject(json)
            val errObj = root.optJSONObject("error")
            errObj?.optString("message", null)
        } catch (_: Exception) {
            null
        }
    }
}
