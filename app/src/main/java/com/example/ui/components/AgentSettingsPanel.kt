package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChatThemePalette
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen

/**
 * Settings Card for configuring WhatsApp Autonomous Agent automation,
 * with a primary toggle for the Automated 'Away' response and message customizer.
 */
@Composable
fun AgentSettingsPanel(
    palette: ChatThemePalette,
    isAwayModeActive: Boolean,
    awayMessage: String,
    isAutoReplyActive: Boolean,
    onToggleAwayMode: (Boolean) -> Unit,
    onUpdateAwayMessage: (String) -> Unit,
    onResetAwayMessage: () -> Unit,
    onToggleAutoReply: (Boolean) -> Unit,
    onSimulateAwayTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editedAwayMessage by remember { mutableStateOf(awayMessage) }
    var isDirty by remember { mutableStateOf(false) }

    LaunchedEffect(awayMessage) {
        editedAwayMessage = awayMessage
        isDirty = false
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = palette.headerBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.headerBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("agent_settings_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (isAwayModeActive) Color(0x33FFB300) else Color(0x3300E5FF),
                                CircleShape
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isAwayModeActive) Color(0xFFFFB300) else ZamaElectricCyan
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAwayModeActive) Icons.Default.Bedtime else Icons.Default.Settings,
                            contentDescription = "Agent Settings",
                            tint = if (isAwayModeActive) Color(0xFFFFB300) else ZamaElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Agent Settings & Automation",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "WhatsApp Dispatch Rules • Away Mode",
                            color = palette.textSecondary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Active Mode Badge
                Surface(
                    color = if (isAwayModeActive) Color(0x33FFB300) else Color(0x3300E676),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        0.5.dp,
                        if (isAwayModeActive) Color(0xFFFFB300) else ZamaNeonGreen
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isAwayModeActive) Color(0xFFFFB300) else ZamaNeonGreen,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isAwayModeActive) "AWAY ACTIVE" else "AUTONOMOUS",
                            color = if (isAwayModeActive) Color(0xFFFFB300) else ZamaNeonGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Feature: Automated 'Away' Response Toggle
            Surface(
                color = if (isAwayModeActive) Color(0x26FFB300) else Color(0x14FFFFFF),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (isAwayModeActive) Color(0x80FFB300) else palette.headerBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = if (isAwayModeActive) Color(0xFFFFB300) else palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Automated 'Away' Response",
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Instantly sends an automated away notice for all incoming WhatsApp messages via the agent service layer.",
                                color = palette.textSecondary,
                                fontSize = 10.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = isAwayModeActive,
                            onCheckedChange = { onToggleAwayMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFB300),
                                checkedTrackColor = Color(0x66FFB300),
                                uncheckedThumbColor = Color(0xFF71717A),
                                uncheckedTrackColor = Color(0xFF27272A)
                            ),
                            modifier = Modifier.testTag("away_mode_toggle_switch")
                        )
                    }

                    // Expandable Configuration for Away Message
                    AnimatedVisibility(
                        visible = isAwayModeActive,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            // Active status banner
                            Surface(
                                color = Color(0x33FFB300),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFFFB300)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("away_mode_active_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Incoming inquiries will bypass standard AI reasoning and receive the automated away response below.",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "CUSTOM AWAY NOTICE (Tip: use {name} for sender's name):",
                                color = palette.textSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = editedAwayMessage,
                                onValueChange = {
                                    editedAwayMessage = it
                                    isDirty = (it != awayMessage)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("away_message_input_field"),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = palette.textPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                ),
                                minLines = 3,
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFB300),
                                    unfocusedBorderColor = palette.headerBorder,
                                    focusedContainerColor = palette.inputFieldFocusedContainer,
                                    unfocusedContainerColor = palette.inputFieldUnfocusedContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons: Save, Reset, Test
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            onUpdateAwayMessage(editedAwayMessage)
                                            isDirty = false
                                        },
                                        enabled = isDirty && editedAwayMessage.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFB300),
                                            contentColor = Color(0xFF1A1A1A),
                                            disabledContainerColor = Color(0x33FFB300),
                                            disabledContentColor = Color(0x66FFB300)
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("save_away_message_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "SAVE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onResetAwayMessage()
                                            editedAwayMessage = com.example.service.whatsapp.WhatsAppAgentService.DEFAULT_AWAY_MESSAGE
                                            isDirty = false
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = palette.textSecondary
                                        ),
                                        border = BorderStroke(0.5.dp, palette.headerBorder),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("reset_away_message_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "RESET DEFAULT", fontSize = 10.sp)
                                    }
                                }

                                // Quick Test Away Dispatch
                                Button(
                                    onClick = onSimulateAwayTest,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0x3300E5FF),
                                        contentColor = ZamaElectricCyan
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, ZamaElectricCyan),
                                    modifier = Modifier.testTag("test_away_auto_reply_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TEST AWAY",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Setting: Master Auto-Reply Pipeline
            Surface(
                color = Color(0x0DFFFFFF),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, palette.headerBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = if (isAutoReplyActive) ZamaNeonGreen else palette.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Master Auto-Reply Pipeline",
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (isAutoReplyActive) "Active • Outbound replies dispatched via API" else "Paused • Logs interactions without sending outbound replies",
                            color = palette.textSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = isAutoReplyActive,
                        onCheckedChange = { onToggleAutoReply(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ZamaNeonGreen,
                            checkedTrackColor = Color(0x3300E676),
                            uncheckedThumbColor = Color(0xFF71717A),
                            uncheckedTrackColor = Color(0xFF27272A)
                        ),
                        modifier = Modifier.testTag("master_auto_reply_switch")
                    )
                }
            }
        }
    }
}
