package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.export.ChatExportManager
import com.example.export.ExportFormat
import com.example.export.ExportScope
import com.example.model.ChatMessage
import com.example.triage.MessageTriageEngine
import com.example.triage.TriageAnalysis
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-craft Dialog for exporting conversation or triage reports as PDF or Text files.
 */
@Composable
fun ChatExportDialog(
    allMessages: List<ChatMessage>,
    filteredMessages: List<ChatMessage>,
    activeFilterQuery: String,
    onDismiss: () -> Unit,
    singleTriageTarget: Pair<ChatMessage, TriageAnalysis>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedScope by remember {
        mutableStateOf(
            when {
                singleTriageTarget != null -> ExportScope.SPECIFIC_INCIDENT
                filteredMessages.size < allMessages.size -> ExportScope.FILTERED_VIEW
                else -> ExportScope.FULL_CHAT
            }
        )
    }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var isExporting by remember { mutableStateOf(false) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val messagesToExport = when (selectedScope) {
        ExportScope.FULL_CHAT -> allMessages
        ExportScope.FILTERED_VIEW -> filteredMessages
        ExportScope.TRIAGE_SUMMARY -> allMessages
        ExportScope.SPECIFIC_INCIDENT -> {
            if (singleTriageTarget != null) listOf(singleTriageTarget.first) else allMessages
        }
    }

    val customerCount = remember(messagesToExport) { messagesToExport.count { it.isFromCustomer } }
    val criticalCount = remember(messagesToExport) {
        messagesToExport.filter { it.isFromCustomer }
            .count { MessageTriageEngine.analyze(it.text, isCustomer = true).requiresImmediateHumanAttention }
    }

    Dialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF0F151F),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0x4D00E5FF)),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .testTag("chat_export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x2600E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = ZamaElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "EXPORT CHAT RECORDS",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Official Salon Record Keeping & Triage",
                                color = ZamaChromeMid,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isExporting,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close export dialog",
                            tint = ZamaChromeMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0x268696A0), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Choose Export Scope
                Text(
                    text = "1. SELECT EXPORT CONTENT",
                    color = ZamaElectricCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (singleTriageTarget != null) {
                        val (targetMsg, targetTriage) = singleTriageTarget
                        ScopeOptionItem(
                            title = "Specific Triage Dossier (Case #TR-${targetMsg.id.take(8).uppercase()})",
                            description = "${targetTriage.intent.displayName} • ${targetTriage.urgency} • ${if (targetTriage.requiresImmediateHumanAttention) "🚨 ATTENTION FLAGGED" else "Standard"}",
                            isSelected = selectedScope == ExportScope.SPECIFIC_INCIDENT,
                            onClick = { selectedScope = ExportScope.SPECIFIC_INCIDENT },
                            tag = "scope_option_specific_incident"
                        )
                    }

                    ScopeOptionItem(
                        title = "Full Conversation (${allMessages.size} messages)",
                        description = "Complete history of all customer inquiries and AI actions",
                        isSelected = selectedScope == ExportScope.FULL_CHAT,
                        onClick = { selectedScope = ExportScope.FULL_CHAT },
                        tag = "scope_option_full"
                    )

                    ScopeOptionItem(
                        title = "Current Filtered Results (${filteredMessages.size} messages)",
                        description = if (activeFilterQuery.isNotBlank()) "Matches for \"$activeFilterQuery\"" else "Filtered view",
                        isSelected = selectedScope == ExportScope.FILTERED_VIEW,
                        onClick = { selectedScope = ExportScope.FILTERED_VIEW },
                        tag = "scope_option_filtered"
                    )

                    ScopeOptionItem(
                        title = "Executive Triage & Escalation Report",
                        description = "$customerCount customer requests analyzed • $criticalCount flagged for attention",
                        isSelected = selectedScope == ExportScope.TRIAGE_SUMMARY,
                        onClick = { selectedScope = ExportScope.TRIAGE_SUMMARY },
                        tag = "scope_option_triage"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Choose File Format
                Text(
                    text = "2. SELECT FILE FORMAT",
                    color = ZamaElectricCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FormatOptionCard(
                        title = "PDF Document",
                        subtitle = "Professional formatted A4 document with Zama branding & status badges",
                        icon = Icons.Default.PictureAsPdf,
                        isSelected = selectedFormat == ExportFormat.PDF,
                        color = Color(0xFFFF5252),
                        onClick = { selectedFormat = ExportFormat.PDF },
                        modifier = Modifier.weight(1f),
                        tag = "format_pdf_btn"
                    )

                    FormatOptionCard(
                        title = "Text File (.txt)",
                        subtitle = "Lightweight structured plain text log for archiving or quick sharing",
                        icon = Icons.AutoMirrored.Filled.TextSnippet,
                        isSelected = selectedFormat == ExportFormat.TEXT,
                        color = ZamaElectricCyan,
                        onClick = { selectedFormat = ExportFormat.TEXT },
                        modifier = Modifier.weight(1f),
                        tag = "format_txt_btn"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Pill
                Surface(
                    color = Color(0xFF161E2C),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0x338696A0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "File: Zama_Chat_${selectedScope.name.lowercase()}.${selectedFormat.extension}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${messagesToExport.size} entries included • Ready for audit, print, or WhatsApp sharing",
                                color = ZamaChromeMid,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (exportSuccessMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x2600FF88), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ZamaNeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = exportSuccessMessage ?: "",
                            color = ZamaNeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33FF5252), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Copy Transcript Action
                    Surface(
                        color = Color(0xFF182230),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x338696A0)),
                        modifier = Modifier
                            .clickable(enabled = !isExporting) {
                                val textContent = if (selectedScope == ExportScope.SPECIFIC_INCIDENT && singleTriageTarget != null) {
                                    ChatExportManager.generateSpecificTriageTranscript(
                                        message = singleTriageTarget.first,
                                        analysis = singleTriageTarget.second
                                    )
                                } else {
                                    ChatExportManager.generateTextTranscript(
                                        messages = messagesToExport,
                                        scope = selectedScope,
                                        filterQuery = activeFilterQuery
                                    )
                                }
                                ChatExportManager.copyToClipboard(context, textContent)
                                Toast.makeText(context, "Transcript copied to clipboard!", Toast.LENGTH_SHORT).show()
                                exportSuccessMessage = "Transcript copied to clipboard!"
                            }
                            .testTag("export_copy_clipboard_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy text transcript",
                                tint = ZamaChromeLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Copy Text",
                                color = ZamaChromeLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Main Export & Share Action
                    Surface(
                        color = ZamaElectricCyan,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !isExporting) {
                                isExporting = true
                                errorMessage = null
                                exportSuccessMessage = null

                                scope.launch {
                                    try {
                                        val exportedFile: File = withContext(Dispatchers.IO) {
                                            if (selectedScope == ExportScope.SPECIFIC_INCIDENT && singleTriageTarget != null) {
                                                when (selectedFormat) {
                                                    ExportFormat.PDF -> {
                                                        ChatExportManager.createSpecificTriagePdf(
                                                            context = context,
                                                            message = singleTriageTarget.first,
                                                            analysis = singleTriageTarget.second
                                                        )
                                                    }
                                                    ExportFormat.TEXT -> {
                                                        ChatExportManager.createSpecificTriageTextFile(
                                                            context = context,
                                                            message = singleTriageTarget.first,
                                                            analysis = singleTriageTarget.second
                                                        )
                                                    }
                                                }
                                            } else {
                                                when (selectedFormat) {
                                                    ExportFormat.PDF -> {
                                                        ChatExportManager.createPdfDocument(
                                                            context = context,
                                                            messages = messagesToExport,
                                                            scope = selectedScope,
                                                            filterQuery = activeFilterQuery
                                                        )
                                                    }
                                                    ExportFormat.TEXT -> {
                                                        ChatExportManager.createTextFile(
                                                            context = context,
                                                            messages = messagesToExport,
                                                            scope = selectedScope,
                                                            filterQuery = activeFilterQuery
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        val shareIntent = ChatExportManager.createShareIntent(
                                            context = context,
                                            file = exportedFile,
                                            mimeType = selectedFormat.mimeType
                                        )
                                        val chooser = Intent.createChooser(shareIntent, "Save or Share Zama Chat Records")
                                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(chooser)

                                        exportSuccessMessage = "${selectedFormat.displayName} created successfully!"
                                    } catch (e: Exception) {
                                        errorMessage = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            }
                            .testTag("export_confirm_share_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generating...",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Export & Share",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Radio / Check item for selecting export scope.
 */
@Composable
private fun ScopeOptionItem(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        color = if (isSelected) Color(0x1F00E5FF) else Color(0xFF131B26),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) ZamaElectricCyan else Color(0x338696A0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(
                        1.5.dp,
                        if (isSelected) ZamaElectricCyan else ZamaChromeMid,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ZamaElectricCyan)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else ZamaChromeLight,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = description,
                    color = ZamaChromeMid,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Card for selecting file format (PDF vs TXT).
 */
@Composable
private fun FormatOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.15f) else Color(0xFF131B26),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) color else Color(0x338696A0)
        ),
        modifier = modifier
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) color else ZamaChromeMid,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    color = if (isSelected) Color.White else ZamaChromeLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = ZamaChromeMid,
                fontSize = 9.5.sp,
                lineHeight = 12.sp
            )
        }
    }
}
