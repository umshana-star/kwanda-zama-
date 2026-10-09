package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.ChatLogEntity
import com.example.data.local.ChatMessage
import com.example.data.local.Conversation
import com.example.data.local.ConversationDatabase
import com.example.data.local.ConversationWithMessages
import com.example.data.local.Message
import com.example.data.local.WhatsAppInteractionEntity
import com.example.data.local.ZamaDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportScope(val label: String) {
    FULL_CHAT("Full Chat History"),
    FILTERED_VIEW("Filtered View")
}

object ChatExportManager {

    /**
     * Generates a human-readable text transcript of the chat messages.
     */
    fun generateTextTranscript(
        messages: List<com.example.model.ChatMessage>,
        scope: ExportScope = ExportScope.FULL_CHAT
    ): String {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("   ZAMA AI SALON - WHATSAPP AGENT CHAT   \n")
        sb.append("   Scope: ${scope.label}\n")
        sb.append("   Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("=========================================\n\n")

        messages.forEach { msg ->
            val sender = if (msg.isFromCustomer) "CUSTOMER" else "ZAMA AI AGENT"
            sb.append("[${msg.timestamp}] $sender: ${msg.text}\n")
            if (!msg.actionDetail.isNullOrBlank()) {
                sb.append("   ↳ Action: ${msg.actionDetail}\n")
            }
        }
        return sb.toString()
    }

    /**
     * Generates a structured JSON backup string from messages and optional Room entities.
     */
    fun generateJsonBackup(
        messages: List<com.example.model.ChatMessage>,
        scope: ExportScope = ExportScope.FULL_CHAT,
        filterQuery: String = "",
        roomChatLogs: List<ChatLogEntity> = emptyList(),
        roomChatMessages: List<ChatMessage> = emptyList()
    ): String {
        val root = JSONObject()
        val meta = JSONObject().apply {
            put("app", "Zama AI")
            put("exportFormat", "JSON")
            put("exportScope", scope.name)
            put("filterQuery", filterQuery)
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("messagesCount", messages.size)
            put("roomChatLogsCount", roomChatLogs.size)
            put("roomChatMessagesCount", roomChatMessages.size)
        }
        root.put("backupMetadata", meta)

        val messagesArray = JSONArray()
        messages.forEach { msg ->
            val item = JSONObject().apply {
                put("id", msg.id)
                put("text", msg.text)
                put("isFromCustomer", msg.isFromCustomer)
                put("timestamp", msg.timestamp)
                put("isActionCard", msg.isActionCard)
                msg.actionDetail?.let { put("actionDetail", it) }
            }
            messagesArray.put(item)
        }
        root.put("messages", messagesArray)

        val logsArray = JSONArray()
        roomChatLogs.forEach { log ->
            val item = JSONObject().apply {
                put("id", log.id)
                put("messageId", log.messageId)
                put("text", log.text)
                put("isFromCustomer", log.isFromCustomer)
                put("senderRole", log.senderRole)
                put("timestampMillis", log.timestampMillis)
                put("timestampFormatted", log.timestampFormatted)
                put("statusTicks", log.statusTicks)
            }
            logsArray.put(item)
        }
        root.put("roomChatLogs", logsArray)

        val chatMessagesArray = JSONArray()
        roomChatMessages.forEach { msg ->
            val item = JSONObject().apply {
                put("id", msg.id)
                put("messageId", msg.messageId)
                put("content", msg.content)
                put("isFromUser", msg.isFromUser)
                put("senderRole", msg.senderRole)
                put("timestamp", msg.timestamp)
                put("timestampMillis", msg.timestampMillis)
                put("statusTicks", msg.statusTicks)
            }
            chatMessagesArray.put(item)
        }
        root.put("roomChatMessages", chatMessagesArray)

        return root.toString(2)
    }

    /**
     * Creates a JSON file on disk in cacheDir.
     */
    fun createJsonFile(
        context: Context,
        messages: List<com.example.model.ChatMessage>,
        scope: ExportScope = ExportScope.FULL_CHAT,
        filterQuery: String = "",
        roomChatLogs: List<ChatLogEntity> = emptyList(),
        roomChatMessages: List<ChatMessage> = emptyList()
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "zama_chat_export_${System.currentTimeMillis()}.json")
        val jsonString = generateJsonBackup(messages, scope, filterQuery, roomChatLogs, roomChatMessages)
        FileOutputStream(file).use { it.write(jsonString.toByteArray(Charsets.UTF_8)) }
        return file
    }

    /**
     * Exports full Room database tables and creates an ACTION_SEND Intent.
     */
    suspend fun exportRoomDatabaseAndCreateShareIntent(
        context: Context,
        database: ZamaDatabase
    ): Pair<File, Intent> {
        val chatLogs = database.chatLogDao().getAllChatLogsSync()
        val chatMessages = database.chatDao().getAllMessagesList()
        val interactions = database.whatsAppInteractionDao().getAllInteractionsSync()

        val root = JSONObject()
        val meta = JSONObject().apply {
            put("app", "Zama AI")
            put("exportFormat", "JSON")
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("roomChatLogsCount", chatLogs.size)
            put("roomChatMessagesCount", chatMessages.size)
            put("roomInteractionsCount", interactions.size)
        }
        root.put("backupMetadata", meta)

        val logsArr = JSONArray()
        chatLogs.forEach { log ->
            logsArr.put(JSONObject().apply {
                put("id", log.id)
                put("messageId", log.messageId)
                put("text", log.text)
                put("isFromCustomer", log.isFromCustomer)
                put("senderRole", log.senderRole)
                put("timestampMillis", log.timestampMillis)
                put("timestampFormatted", log.timestampFormatted)
            })
        }
        root.put("chatLogs", logsArr)

        val messagesArr = JSONArray()
        chatMessages.forEach { msg ->
            messagesArr.put(JSONObject().apply {
                put("id", msg.id)
                put("messageId", msg.messageId)
                put("content", msg.content)
                put("isFromUser", msg.isFromUser)
                put("senderRole", msg.senderRole)
                put("timestamp", msg.timestamp)
                put("timestampMillis", msg.timestampMillis)
            })
        }
        root.put("chatMessages", messagesArr)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val exportFile = File(exportDir, "zama_db_backup_${System.currentTimeMillis()}.json")
        FileOutputStream(exportFile).use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }

        val uri: Uri? = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
        } catch (_: Exception) {
            try {
                FileProvider.getUriForFile(context, "${context.applicationContext.packageName}.fileprovider", exportFile)
            } catch (_: Exception) {
                null
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            if (uri != null) {
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            putExtra(Intent.EXTRA_SUBJECT, "Zama AI Full Database Export")
        }

        return Pair(exportFile, intent)
    }

    /**
     * Exports chat messages and interactions for the ChatViewModel interactive export button.
     */
    fun exportToJson(
        context: Context,
        messages: List<ChatMessage>,
        interactions: List<WhatsAppInteractionEntity> = emptyList()
    ): Pair<File, Intent> {
        val rootJson = JSONObject()
        rootJson.put("exportTimestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        rootJson.put("appVersion", "1.0")
        rootJson.put("totalMessages", messages.size)
        rootJson.put("totalInteractions", interactions.size)

        val messagesArray = JSONArray()
        messages.forEach { msg ->
            val obj = JSONObject().apply {
                put("messageId", msg.messageId)
                put("content", msg.content)
                put("isFromUser", msg.isFromUser)
                put("senderRole", msg.senderRole)
                put("timestamp", msg.timestamp)
                put("isVoiceNote", msg.isVoiceNote)
                msg.audioModelUsed?.let { put("audioModelUsed", it) }
                msg.aiTrace?.let { put("aiTrace", it) }
                msg.intentTag?.let { put("intentTag", it) }
            }
            messagesArray.put(obj)
        }
        rootJson.put("messages", messagesArray)

        val interactionsArray = JSONArray()
        interactions.forEach { inter ->
            val obj = JSONObject().apply {
                put("interactionId", inter.interactionId)
                put("senderPhoneNumber", inter.senderPhoneNumber)
                put("customerDisplayName", inter.customerDisplayName)
                put("resolvedIntent", inter.resolvedIntent)
                put("agentConfidence", inter.agentConfidence.toDouble())
                put("deliveryStatus", inter.deliveryStatus)
                put("latencyMs", inter.latencyMs)
                put("isEscalatedToHuman", inter.isEscalatedToHuman)
                inter.replyText?.let { put("replyText", it) }
            }
            interactionsArray.put(obj)
        }
        rootJson.put("interactions", interactionsArray)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val exportFile = File(exportDir, "zama_whatsapp_chat_export_${System.currentTimeMillis()}.json")
        FileOutputStream(exportFile).use { it.write(rootJson.toString(2).toByteArray(Charsets.UTF_8)) }

        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
        } catch (_: Exception) {
            try {
                FileProvider.getUriForFile(context, "${context.applicationContext.packageName}.fileprovider", exportFile)
            } catch (_: Exception) {
                null
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            if (uri != null) {
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            putExtra(Intent.EXTRA_SUBJECT, "Zama Autonomous WhatsApp Agent Chat Export")
            putExtra(Intent.EXTRA_TEXT, "Exported chat history and interaction logs from Zama AI (${messages.size} messages).")
        }

        return Pair(exportFile, shareIntent)
    }
}
