package com.example.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.model.ChatMessage
import com.example.triage.MessageTriageEngine
import com.example.triage.TriageAnalysis
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Supported Export file formats.
 */
enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    PDF("pdf", "application/pdf", "PDF Document (.pdf)"),
    TEXT("txt", "text/plain", "Text File (.txt)")
}

/**
 * Scope of conversation messages to export.
 */
enum class ExportScope(val title: String, val description: String) {
    FULL_CHAT("Entire Conversation", "All logged interactions and messages in chronological order"),
    FILTERED_VIEW("Current Filtered View", "Only messages currently matching your search query & sender filter"),
    TRIAGE_SUMMARY("Triage & Escalation Report", "Executive summary of customer inquiries, bookings, complaints, and flagged escalations"),
    SPECIFIC_INCIDENT("Specific Incident Dossier", "Official standalone record for a selected customer message and triage decision")
}

/**
 * Manages document generation, formatting, PDF creation, file storage, and sharing for chat logs.
 */
object ChatExportManager {

    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val MARGIN_Y = 40f
    private const val CONTENT_WIDTH = A4_WIDTH - (MARGIN_X * 2)

    /**
     * Formats conversation or triage records into a structured plain text transcript.
     */
    fun generateTextTranscript(
        messages: List<ChatMessage>,
        scope: ExportScope,
        filterQuery: String = ""
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val generatedAt = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("                 ZAMA AI SALON — CLIENT INTERACTION RECORD                      ")
        sb.appendLine("================================================================================")
        sb.appendLine("Export Type    : ${scope.title}")
        sb.appendLine("Export Date    : $generatedAt")
        sb.appendLine("Total Messages : ${messages.size}")
        if (filterQuery.isNotBlank()) {
            sb.appendLine("Filter Applied : \"$filterQuery\"")
        }
        sb.appendLine("================================================================================")
        sb.appendLine()

        if (scope == ExportScope.TRIAGE_SUMMARY) {
            // Executive Triage & Escalation Analysis
            val customerMessages = messages.filter { it.isFromCustomer }
            val triageAnalyses = customerMessages.map { msg ->
                msg to MessageTriageEngine.analyze(msg.text, isCustomer = true)
            }
            val criticalAttention = triageAnalyses.filter { it.second.requiresImmediateHumanAttention }
            val bookingRequests = triageAnalyses.filter { it.second.intent == com.example.triage.TriageIntent.BOOKING }
            val complaints = triageAnalyses.filter { it.second.intent == com.example.triage.TriageIntent.COMPLAINT }

            sb.appendLine(">>> EXECUTIVE SALON TRIAGE BREAKDOWN <<<")
            sb.appendLine("Customer Messages Analyzed : ${customerMessages.size}")
            sb.appendLine("Bookings & Appointments   : ${bookingRequests.size}")
            sb.appendLine("Customer Inquiries        : ${triageAnalyses.count { it.second.intent == com.example.triage.TriageIntent.INQUIRY }}")
            sb.appendLine("Complaints & Friction     : ${complaints.size}")
            sb.appendLine("Flagged for Human Attention: ${criticalAttention.size}")
            sb.appendLine("--------------------------------------------------------------------------------")
            sb.appendLine()

            if (criticalAttention.isNotEmpty()) {
                sb.appendLine("🚨 HIGH-PRIORITY FLAGGED ESCALATIONS (REQUIRES IMMEDIATE OWNER/MANAGER REVIEW):")
                criticalAttention.forEachIndexed { idx, (msg, triage) ->
                    sb.appendLine("[$idx] Timestamp: ${msg.timestamp}")
                    sb.appendLine("    Urgency  : ${triage.urgency}")
                    sb.appendLine("    Intent   : ${triage.intent.displayName}")
                    sb.appendLine("    Reason   : ${triage.escalationReason ?: "Customer dissatisfaction or explicit manager request"}")
                    sb.appendLine("    Tags     : ${triage.tags.joinToString(", ")}")
                    sb.appendLine("    Message  : \"${msg.text}\"")
                    sb.appendLine()
                }
                sb.appendLine("--------------------------------------------------------------------------------")
                sb.appendLine()
            }

            sb.appendLine("DETAILED TRIAGE LOG:")
            triageAnalyses.forEachIndexed { idx, (msg, triage) ->
                val attentionLabel = if (triage.requiresImmediateHumanAttention) "[🚨 ATTENTION FLAGGED]" else ""
                sb.appendLine("${idx + 1}. [${msg.timestamp}] ${triage.intent.displayName.uppercase()} (${triage.urgency}) $attentionLabel")
                sb.appendLine("   Customer: \"${msg.text}\"")
                sb.appendLine("   Category Tags: ${triage.tags.joinToString(" ")}")
                sb.appendLine()
            }
        } else {
            // Sequential Chat Log
            messages.forEachIndexed { idx, msg ->
                val sender = if (msg.isFromCustomer) "CUSTOMER (WhatsApp)" else "ZAMA AI AGENT"
                val triage = if (msg.isFromCustomer) {
                    val analysis = MessageTriageEngine.analyze(msg.text, isCustomer = true)
                    val flag = if (analysis.requiresImmediateHumanAttention) " | 🚨 ATTENTION" else ""
                    " [${analysis.intent.displayName.uppercase()} | ${analysis.urgency}$flag]"
                } else ""

                sb.appendLine("[$idx] $sender $triage — ${msg.timestamp}")
                sb.appendLine(msg.text)
                if (!msg.actionDetail.isNullOrBlank()) {
                    sb.appendLine("   ↳ Action: ${msg.actionDetail}")
                }
                sb.appendLine("--------------------------------------------------------------------------------")
            }
        }

        sb.appendLine()
        sb.appendLine("================================================================================")
        sb.appendLine("End of Transcript • Zama AI Salon Record System")
        sb.appendLine("================================================================================")
        return sb.toString()
    }

    /**
     * Writes plain text transcript to a file in the app's cache directory.
     */
    fun createTextFile(
        context: Context,
        messages: List<ChatMessage>,
        scope: ExportScope,
        filterQuery: String = ""
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Zama_Chat_${scope.name.lowercase()}_$timeStamp.txt"
        val file = File(exportDir, fileName)

        val content = generateTextTranscript(messages, scope, filterQuery)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    /**
     * Generates a multi-page styled PDF document using Android's native PdfDocument.
     */
    fun createPdfDocument(
        context: Context,
        messages: List<ChatMessage>,
        scope: ExportScope,
        filterQuery: String = ""
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Zama_Chat_${scope.name.lowercase()}_$timeStamp.pdf"
        val file = File(exportDir, fileName)

        val pdfDocument = PdfDocument()
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 25, 47) // Navy / Cyan dark tone
            textSize = 15f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(80, 90, 105)
            textSize = 9f
        }
        val headerBarPaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Dark sleek background
        }
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f
            isFakeBoldText = true
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 229, 255) // Electric cyan
            textSize = 8.5f
            isFakeBoldText = true
        }
        val customerBadgePaint = Paint().apply {
            color = Color.rgb(230, 248, 240) // Light green bg
        }
        val customerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 120, 70)
            textSize = 8.5f
            isFakeBoldText = true
        }
        val aiBadgePaint = Paint().apply {
            color = Color.rgb(235, 245, 255) // Light cyan bg
        }
        val aiTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 90, 160)
            textSize = 8.5f
            isFakeBoldText = true
        }
        val urgentBadgePaint = Paint().apply {
            color = Color.rgb(255, 235, 238)
        }
        val urgentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(213, 0, 0)
            textSize = 8f
            isFakeBoldText = true
        }
        val timestampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(130, 140, 155)
            textSize = 8f
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8f
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Helper to draw common page header
        fun drawPageHeader(c: Canvas, pNum: Int) {
            // Header Bar
            c.drawRect(0f, 0f, A4_WIDTH.toFloat(), 56f, headerBarPaint)
            c.drawText("ZAMA AI SALON • CONVERSATION TRANSCRIPT", MARGIN_X, 26f, headerTitlePaint)
            c.drawText(
                "Scope: ${scope.title.uppercase()} | Exported: $generatedDate",
                MARGIN_X,
                42f,
                headerSubPaint
            )

            // Top metadata line below bar
            c.drawLine(MARGIN_X, 64f, A4_WIDTH - MARGIN_X, 64f, dividerPaint)
        }

        // Helper to draw common page footer
        fun drawPageFooter(c: Canvas, pNum: Int) {
            val footerY = A4_HEIGHT - 20f
            c.drawLine(MARGIN_X, footerY - 8f, A4_WIDTH - MARGIN_X, footerY - 8f, dividerPaint)
            c.drawText("Zama AI Autonomous Salon Assistant • Confidential Record", MARGIN_X, footerY, footerPaint)
            val pageStr = "Page $pNum"
            val pWidth = footerPaint.measureText(pageStr)
            c.drawText(pageStr, A4_WIDTH - MARGIN_X - pWidth, footerY, footerPaint)
        }

        drawPageHeader(canvas, pageNumber)
        drawPageFooter(canvas, pageNumber)

        var currentY = 80f

        // Overview Box on First Page
        val metaBoxHeight = if (scope == ExportScope.TRIAGE_SUMMARY) 76f else 54f
        val metaBgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
        val metaStrokePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val metaRect = RectF(MARGIN_X, currentY, A4_WIDTH - MARGIN_X, currentY + metaBoxHeight)
        canvas.drawRoundRect(metaRect, 6f, 6f, metaBgPaint)
        canvas.drawRoundRect(metaRect, 6f, 6f, metaStrokePaint)

        canvas.drawText("TRANSCRIPT OVERVIEW", MARGIN_X + 12f, currentY + 16f, titlePaint.apply { textSize = 10f })
        canvas.drawText(
            "Total Messages Logged: ${messages.size}  |  Customers: ${messages.count { it.isFromCustomer }}  |  Zama AI: ${messages.count { !it.isFromCustomer }}",
            MARGIN_X + 12f,
            currentY + 30f,
            subtitlePaint
        )
        if (filterQuery.isNotBlank()) {
            canvas.drawText(
                "Active Keyword Filter: \"$filterQuery\"",
                MARGIN_X + 12f,
                currentY + 44f,
                subtitlePaint
            )
        }

        if (scope == ExportScope.TRIAGE_SUMMARY) {
            val customerMsgs = messages.filter { it.isFromCustomer }
            val triageList = customerMsgs.map { MessageTriageEngine.analyze(it.text, isCustomer = true) }
            val urgentCount = triageList.count { it.requiresImmediateHumanAttention }
            val bookingCount = triageList.count { it.intent == com.example.triage.TriageIntent.BOOKING }
            val inquiryCount = triageList.count { it.intent == com.example.triage.TriageIntent.INQUIRY }
            canvas.drawText(
                "Triage: $bookingCount Bookings, $inquiryCount Inquiries, $urgentCount Flagged for Urgent Attention",
                MARGIN_X + 12f,
                currentY + 58f,
                subtitlePaint.apply { isFakeBoldText = true }
            )
        }

        currentY += metaBoxHeight + 18f

        // Draw Message Items
        val maxAvailableY = A4_HEIGHT - 45f // Leave room for footer

        for (msg in messages) {
            val isCustomer = msg.isFromCustomer
            val triage = if (isCustomer) MessageTriageEngine.analyze(msg.text, isCustomer = true) else null

            // Estimate text layout height
            textPaint.textSize = 9.5f
            val staticLayout = createStaticLayout(msg.text, textPaint, CONTENT_WIDTH.toInt() - 20)
            val messageHeight = staticLayout.height + 34f // header, tags, text, margins

            // Check if item fits on current page
            if (currentY + messageHeight > maxAvailableY) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawPageHeader(canvas, pageNumber)
                drawPageFooter(canvas, pageNumber)
                currentY = 75f
            }

            // Draw Item Container
            val itemRect = RectF(MARGIN_X, currentY, A4_WIDTH - MARGIN_X, currentY + messageHeight)
            val itemBgPaint = Paint().apply {
                color = if (isCustomer) Color.rgb(255, 255, 255) else Color.rgb(246, 249, 252)
            }
            canvas.drawRoundRect(itemRect, 4f, 4f, itemBgPaint)
            canvas.drawRoundRect(itemRect, 4f, 4f, dividerPaint)

            // Header line inside item: Sender pill + Timestamp
            val badgeX = MARGIN_X + 8f
            val badgeY = currentY + 7f
            val senderLabel = if (isCustomer) "CUSTOMER" else "ZAMA AI"
            val badgePaint = if (isCustomer) customerBadgePaint else aiBadgePaint
            val labelPaint = if (isCustomer) customerTextPaint else aiTextPaint
            val badgeWidth = labelPaint.measureText(senderLabel) + 12f

            canvas.drawRoundRect(RectF(badgeX, badgeY, badgeX + badgeWidth, badgeY + 14f), 3f, 3f, badgePaint)
            canvas.drawText(senderLabel, badgeX + 6f, badgeY + 10.5f, labelPaint)

            var nextTagX = badgeX + badgeWidth + 6f

            if (triage != null) {
                val triageLabel = "${triage.intent.displayName.uppercase()} • ${triage.urgency}"
                val triageWidth = customerTextPaint.measureText(triageLabel) + 12f
                canvas.drawRoundRect(RectF(nextTagX, badgeY, nextTagX + triageWidth, badgeY + 14f), 3f, 3f, aiBadgePaint)
                canvas.drawText(triageLabel, nextTagX + 6f, badgeY + 10.5f, customerTextPaint)
                nextTagX += triageWidth + 6f

                if (triage.requiresImmediateHumanAttention) {
                    val alertLabel = "🚨 ATTENTION FLAGGED"
                    val alertWidth = urgentTextPaint.measureText(alertLabel) + 10f
                    canvas.drawRoundRect(RectF(nextTagX, badgeY, nextTagX + alertWidth, badgeY + 14f), 3f, 3f, urgentBadgePaint)
                    canvas.drawText(alertLabel, nextTagX + 5f, badgeY + 10.5f, urgentTextPaint)
                }
            }

            // Timestamp on the right
            val timeText = msg.timestamp
            val timeWidth = timestampPaint.measureText(timeText)
            canvas.drawText(timeText, A4_WIDTH - MARGIN_X - 10f - timeWidth, currentY + 17f, timestampPaint)

            // Message text body
            canvas.save()
            canvas.translate(MARGIN_X + 10f, currentY + 26f)
            staticLayout.draw(canvas)
            canvas.restore()

            currentY += messageHeight + 8f
        }

        pdfDocument.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    /**
     * Creates an Intent to share or save the exported file externally.
     */
    fun createShareIntent(context: Context, file: File, mimeType: String): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, "Exported salon conversation from Zama AI (${file.name})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Formats a single customer message and its AI triage analysis into a formal incident/triage report.
     */
    fun generateSpecificTriageTranscript(message: ChatMessage, analysis: TriageAnalysis): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val generatedAt = dateFormat.format(Date())

        return buildString {
            appendLine("================================================================================")
            appendLine("              ZAMA AI SALON — SPECIFIC TRIAGE INCIDENT DOSSIER                  ")
            appendLine("================================================================================")
            appendLine("Dossier ID        : TR-${message.id.take(8).uppercase()}")
            appendLine("Interaction Time  : ${message.timestamp}")
            appendLine("Exported At       : $generatedAt")
            appendLine("Triage Category   : ${analysis.intent.displayName.uppercase()} (${analysis.intent.iconEmoji})")
            appendLine("Urgency Rating    : ${analysis.urgency.name}")
            appendLine("Escalation Status : ${if (analysis.requiresImmediateHumanAttention) "🚨 ACTION REQUIRED BY SALON STAFF" else "Standard Autonomous Resolution"}")
            if (analysis.escalationReason != null) {
                appendLine("Escalation Reason : ${analysis.escalationReason}")
            }
            appendLine("Category Tags     : ${analysis.tags.joinToString(", ")}")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("CUSTOMER MESSAGE BODY:")
            appendLine("\"${message.text}\"")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("RECOMMENDED ACTION PROTOCOL:")
            when {
                analysis.requiresImmediateHumanAttention -> {
                    appendLine("1. Salon manager or Kwanda Zama should initiate immediate direct contact.")
                    appendLine("2. Verify customer profile and past appointment history in salon records.")
                    appendLine("3. Provide empathetic reassurance or offer priority re-booking / compensation.")
                }
                analysis.intent == com.example.triage.TriageIntent.BOOKING -> {
                    appendLine("1. Confirm availability in salon calendar.")
                    appendLine("2. Send automated deposit confirmation link and address details.")
                }
                else -> {
                    appendLine("1. Answer verified autonomously via salon knowledge base.")
                    appendLine("2. Retain for routine operational audit.")
                }
            }
            appendLine("================================================================================")
            appendLine("Official Record • Zama AI Autonomous Salon Systems")
            appendLine("================================================================================")
        }
    }

    /**
     * Generates a single triage incident report as a plain text file.
     */
    fun createSpecificTriageTextFile(
        context: Context,
        message: ChatMessage,
        analysis: TriageAnalysis
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Zama_Triage_${analysis.intent.name.lowercase()}_$timeStamp.txt"
        val file = File(exportDir, fileName)

        val content = generateSpecificTriageTranscript(message, analysis)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    /**
     * Generates a single triage incident report as a styled PDF document.
     */
    fun createSpecificTriagePdf(
        context: Context,
        message: ChatMessage,
        analysis: TriageAnalysis
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Zama_Triage_${analysis.intent.name.lowercase()}_$timeStamp.pdf"
        val file = File(exportDir, fileName)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paints
        val headerBarPaint = Paint().apply { color = Color.rgb(15, 23, 42) }
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f
            isFakeBoldText = true
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 229, 255)
            textSize = 9f
            isFakeBoldText = true
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8f
        }
        val cardBgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
        val cardBorderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            isFakeBoldText = true
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            isFakeBoldText = true
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
        }

        // Header Bar
        canvas.drawRect(0f, 0f, A4_WIDTH.toFloat(), 56f, headerBarPaint)
        canvas.drawText("ZAMA AI SALON • SPECIFIC TRIAGE DOSSIER", MARGIN_X, 26f, headerTitlePaint)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Case ID: TR-${message.id.take(8).uppercase()} | Generated: $dateStr", MARGIN_X, 42f, headerSubPaint)
        canvas.drawLine(MARGIN_X, 64f, A4_WIDTH - MARGIN_X, 64f, dividerPaint)

        var y = 84f

        // Status Banner Card
        val isAttention = analysis.requiresImmediateHumanAttention
        val statusBgPaint = Paint().apply {
            color = if (isAttention) Color.rgb(254, 242, 242) else Color.rgb(240, 253, 244)
        }
        val statusBorderPaint = Paint().apply {
            color = if (isAttention) Color.rgb(239, 68, 68) else Color.rgb(34, 197, 94)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val statusTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isAttention) Color.rgb(185, 28, 28) else Color.rgb(21, 128, 61)
            textSize = 12f
            isFakeBoldText = true
        }

        val statusRect = RectF(MARGIN_X, y, A4_WIDTH - MARGIN_X, y + 50f)
        canvas.drawRoundRect(statusRect, 6f, 6f, statusBgPaint)
        canvas.drawRoundRect(statusRect, 6f, 6f, statusBorderPaint)

        val bannerTitle = if (isAttention) "🚨 HUMAN ATTENTION REQUIRED • ESCALATION FLAGGED" else "✅ ROUTINE INQUIRY • AUTONOMOUS HANDLING"
        canvas.drawText(bannerTitle, MARGIN_X + 14f, y + 22f, statusTitlePaint)
        canvas.drawText(
            "Category: ${analysis.intent.displayName.uppercase()} | Urgency: ${analysis.urgency.name} | Interaction Time: ${message.timestamp}",
            MARGIN_X + 14f,
            y + 38f,
            labelPaint
        )

        y += 66f

        // Metadata Table Card
        val metaRect = RectF(MARGIN_X, y, A4_WIDTH - MARGIN_X, y + 80f)
        canvas.drawRoundRect(metaRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(metaRect, 6f, 6f, cardBorderPaint)

        canvas.drawText("TRIAGE CLASSIFICATION DETAILS", MARGIN_X + 14f, y + 20f, valuePaint)
        canvas.drawLine(MARGIN_X + 14f, y + 26f, A4_WIDTH - MARGIN_X - 14f, y + 26f, dividerPaint)

        canvas.drawText("Intent Category:", MARGIN_X + 14f, y + 44f, labelPaint)
        canvas.drawText(analysis.intent.displayName, MARGIN_X + 120f, y + 44f, valuePaint)

        canvas.drawText("Urgency Level:", MARGIN_X + 14f, y + 62f, labelPaint)
        canvas.drawText(analysis.urgency.name, MARGIN_X + 120f, y + 62f, valuePaint)

        canvas.drawText("Category Tags:", MARGIN_X + 280f, y + 44f, labelPaint)
        canvas.drawText(analysis.tags.joinToString(", ").ifEmpty { "General" }, MARGIN_X + 370f, y + 44f, valuePaint)

        canvas.drawText("Escalation Reason:", MARGIN_X + 280f, y + 62f, labelPaint)
        canvas.drawText(analysis.escalationReason ?: "None", MARGIN_X + 370f, y + 62f, valuePaint)

        y += 98f

        // Customer Message Body Card
        canvas.drawText("CUSTOMER MESSAGE TRANSCRIPT", MARGIN_X, y + 14f, labelPaint)
        y += 22f

        val textLayout = createStaticLayout(message.text, textPaint, CONTENT_WIDTH.toInt() - 28)
        val bodyHeight = textLayout.height + 28f
        val bodyRect = RectF(MARGIN_X, y, A4_WIDTH - MARGIN_X, y + bodyHeight)
        canvas.drawRoundRect(bodyRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(bodyRect, 6f, 6f, cardBorderPaint)

        canvas.save()
        canvas.translate(MARGIN_X + 14f, y + 14f)
        textLayout.draw(canvas)
        canvas.restore()

        y += bodyHeight + 18f

        // Recommended Operational Protocol Card
        canvas.drawText("RECOMMENDED OPERATIONAL ACTION PROTOCOL", MARGIN_X, y + 14f, labelPaint)
        y += 22f

        val protocolText = when {
            analysis.requiresImmediateHumanAttention ->
                "1. Salon manager or Kwanda Zama should initiate immediate direct contact via phone/WhatsApp.\n" +
                "2. Verify client appointment history and service notes in salon records.\n" +
                "3. Offer priority slot reschedule or client courtesy resolution."
            analysis.intent == com.example.triage.TriageIntent.BOOKING ->
                "1. Confirm calendar slot availability with assigned stylist.\n" +
                "2. Send WhatsApp automated booking confirmation and salon address instructions."
            else ->
                "1. Standard query answered autonomously by Zama AI.\n" +
                "2. No manual intervention required at this stage."
        }

        val protoLayout = createStaticLayout(protocolText, textPaint, CONTENT_WIDTH.toInt() - 28)
        val protoHeight = protoLayout.height + 28f
        val protoRect = RectF(MARGIN_X, y, A4_WIDTH - MARGIN_X, y + protoHeight)
        canvas.drawRoundRect(protoRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(protoRect, 6f, 6f, cardBorderPaint)

        canvas.save()
        canvas.translate(MARGIN_X + 14f, y + 14f)
        protoLayout.draw(canvas)
        canvas.restore()

        // Footer
        val footerY = A4_HEIGHT - 20f
        canvas.drawLine(MARGIN_X, footerY - 8f, A4_WIDTH - MARGIN_X, footerY - 8f, dividerPaint)
        canvas.drawText("Zama AI Autonomous Salon Systems • Official Audit Dossier", MARGIN_X, footerY, footerPaint)
        val pageStr = "Page 1 of 1"
        val pWidth = footerPaint.measureText(pageStr)
        canvas.drawText(pageStr, A4_WIDTH - MARGIN_X - pWidth, footerY, footerPaint)

        pdfDocument.finishPage(page)
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    /**
     * Copies a string directly to the device clipboard.
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Zama Chat Transcript") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    @Suppress("DEPRECATION")
    private fun createStaticLayout(text: String, textPaint: TextPaint, width: Int): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(true)
                .build()
        } else {
            StaticLayout(
                text,
                textPaint,
                width,
                Layout.Alignment.ALIGN_NORMAL,
                1.15f,
                0f,
                true
            )
        }
    }
}
