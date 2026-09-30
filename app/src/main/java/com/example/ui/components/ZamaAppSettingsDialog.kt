package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.security.BiometricSecurityUiState
import com.example.security.PinSetupResult
import com.example.ui.theme.LocalZamaPalette
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaThemeMode

/**
 * Global Settings Modal for Zama AI Business Hub.
 * Manages Display Themes (Futuristic Dark vs High-Contrast Daylight), Biometric Security Enclave,
 * and Salted PBKDF2-SHA256 Owner PIN management.
 */
@Composable
fun ZamaAppSettingsDialog(
    isOpen: Boolean,
    currentThemeMode: ZamaThemeMode,
    onSelectThemeMode: (ZamaThemeMode) -> Unit,
    securityState: BiometricSecurityUiState,
    onToggleBiometrics: (Boolean) -> Unit,
    onUpdateAutoLock: (Int) -> Unit,
    onSetupOwnerPin: (newPin: String, confirmPin: String) -> PinSetupResult = { _, _ -> PinSetupResult.Success },
    onChangeOwnerPin: (currentPin: String, newPin: String, confirmPin: String) -> PinSetupResult = { _, _, _ -> PinSetupResult.Success },
    onResetOwnerPin: () -> Boolean = { true },
    onOpenAuditLogs: () -> Unit,
    onManageAgents: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val palette = LocalZamaPalette.current
    val scrollState = rememberScrollState()
    var showPinEditor by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var pinFeedbackIsError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("app_settings_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = palette.cyanAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = palette.cyanAccent,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "ZAMA AI SETTINGS",
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Display Themes & Salon Enclave Security",
                                color = palette.textMuted,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_close_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = palette.border, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Theme Selection Section (Futuristic Dark vs High-Contrast Daylight)
                    ThemeSettingsSection(
                        currentMode = currentThemeMode,
                        onSelectMode = onSelectThemeMode
                    )

                    HorizontalDivider(color = palette.border, thickness = 0.8.dp)

                    // 2. Biometric Security Controls
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = palette.greenAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "BIOMETRIC ENCLAVE SECURITY",
                                color = palette.greenAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Toggle Biometric Lock
                        Surface(
                            color = palette.cardSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Biometric Lock Required",
                                        color = palette.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Requires fingerprint or face unlock before accessing private customer triage & analytics",
                                        color = palette.textMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Switch(
                                    checked = securityState.isBiometricProtectionEnabled,
                                    onCheckedChange = onToggleBiometrics,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = palette.greenAccent,
                                        checkedTrackColor = palette.greenAccent.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.testTag("switch_settings_biometric")
                                )
                            }
                        }

                        // Auto-Lock Timer
                        Text(
                            text = "AUTO-LOCK TIMEOUT",
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        val durations = listOf(1 to "1 Min", 5 to "5 Mins", 15 to "15 Mins", 30 to "30 Mins")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            durations.forEach { (mins, label) ->
                                val isSelected = securityState.autoLockDurationMinutes == mins
                                Surface(
                                    color = if (isSelected) palette.cyanAccent.copy(alpha = 0.2f) else palette.cardSurface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) palette.cyanAccent else palette.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_timeout_$mins")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onUpdateAutoLock(mins) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) palette.cyanAccent else palette.textPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Salted PBKDF2-SHA256 Owner PIN Management Card
                        Surface(
                            color = palette.cyanAccent.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_pin_management_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = palette.cyanAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (securityState.isPinConfigured) {
                                                    "OWNER PIN VERIFIER: CONFIGURED"
                                                } else {
                                                    "OWNER PIN VERIFIER: NOT CONFIGURED"
                                                },
                                                color = palette.cyanAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "Salted PBKDF2-HMAC-SHA256 verifier (65,536 iterations) with 5-attempt brute-force lockout protection.",
                                                color = palette.textSecondary,
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            showPinEditor = !showPinEditor
                                            pinFeedbackMessage = null
                                        },
                                        border = BorderStroke(1.dp, palette.cyanAccent),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("btn_toggle_pin_editor")
                                    ) {
                                        Text(
                                            text = if (showPinEditor) {
                                                "CANCEL"
                                            } else if (securityState.isPinConfigured) {
                                                "CHANGE PIN"
                                            } else {
                                                "SET UP PIN"
                                            },
                                            color = palette.cyanAccent,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                if (pinFeedbackMessage != null) {
                                    Text(
                                        text = pinFeedbackMessage.orEmpty(),
                                        color = if (pinFeedbackIsError) Color(0xFFFF5252) else palette.greenAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.testTag("pin_settings_feedback_text")
                                    )
                                }

                                if (showPinEditor) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (securityState.isPinConfigured) {
                                            OutlinedTextField(
                                                value = currentPinInput,
                                                onValueChange = { input ->
                                                    if (input.length <= 8 && input.all { it.isDigit() }) {
                                                        currentPinInput = input
                                                    }
                                                },
                                                label = { Text("Current Owner PIN", fontSize = 10.sp) },
                                                visualTransformation = PasswordVisualTransformation(),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = palette.cyanAccent,
                                                    unfocusedBorderColor = palette.border,
                                                    focusedTextColor = palette.textPrimary,
                                                    unfocusedTextColor = palette.textPrimary
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("input_current_owner_pin")
                                            )
                                        }

                                        OutlinedTextField(
                                            value = newPinInput,
                                            onValueChange = { input ->
                                                if (input.length <= 8 && input.all { it.isDigit() }) {
                                                    newPinInput = input
                                                }
                                            },
                                            label = { Text("New 4-digit Owner PIN", fontSize = 10.sp) },
                                            visualTransformation = PasswordVisualTransformation(),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = palette.cyanAccent,
                                                unfocusedBorderColor = palette.border,
                                                focusedTextColor = palette.textPrimary,
                                                unfocusedTextColor = palette.textPrimary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("input_new_owner_pin")
                                        )

                                        OutlinedTextField(
                                            value = confirmPinInput,
                                            onValueChange = { input ->
                                                if (input.length <= 8 && input.all { it.isDigit() }) {
                                                    confirmPinInput = input
                                                }
                                            },
                                            label = { Text("Confirm New Owner PIN", fontSize = 10.sp) },
                                            visualTransformation = PasswordVisualTransformation(),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = palette.cyanAccent,
                                                unfocusedBorderColor = palette.border,
                                                focusedTextColor = palette.textPrimary,
                                                unfocusedTextColor = palette.textPrimary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("input_confirm_owner_pin")
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val result = if (securityState.isPinConfigured) {
                                                        onChangeOwnerPin(currentPinInput, newPinInput, confirmPinInput)
                                                    } else {
                                                        onSetupOwnerPin(newPinInput, confirmPinInput)
                                                    }
                                                    when (result) {
                                                        is PinSetupResult.Success -> {
                                                            pinFeedbackIsError = false
                                                            pinFeedbackMessage = "Owner PIN saved with salted PBKDF2-SHA256 verifier."
                                                            currentPinInput = ""
                                                            newPinInput = ""
                                                            confirmPinInput = ""
                                                            showPinEditor = false
                                                        }
                                                        is PinSetupResult.ValidationError -> {
                                                            pinFeedbackIsError = true
                                                            pinFeedbackMessage = result.message
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = palette.greenAccent,
                                                    contentColor = Color(0xFF040A06)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("btn_save_owner_pin")
                                            ) {
                                                Text(
                                                    text = "SAVE OWNER PIN",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            if (securityState.isPinConfigured && securityState.isUnlocked) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val resetOk = onResetOwnerPin()
                                                        if (resetOk) {
                                                            pinFeedbackIsError = false
                                                            pinFeedbackMessage = "Owner PIN verifier cleared."
                                                            currentPinInput = ""
                                                            newPinInput = ""
                                                            confirmPinInput = ""
                                                            showPinEditor = false
                                                        }
                                                    },
                                                    border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.testTag("btn_reset_owner_pin")
                                                ) {
                                                    Text(
                                                        text = "CLEAR PIN",
                                                        color = Color(0xFFFF5252),
                                                        fontSize = 10.sp,
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

                        // View Security Audit Logs Card
                        Surface(
                            color = palette.cardSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenAuditLogs()
                                }
                                .testTag("btn_settings_open_audit_logs")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Biometric Security Audit Trail",
                                        color = palette.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Inspect ${securityState.auditLogs.size} recorded authentication & session lock events",
                                        color = palette.textMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Surface(
                                    color = palette.greenAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(1.dp, palette.greenAccent.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "AUDIT LOGS",
                                        color = palette.greenAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Autonomous WhatsApp Agent Cluster
                    if (onManageAgents != null) {
                        HorizontalDivider(color = palette.border, thickness = 0.8.dp)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = palette.cyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "AUTONOMOUS WHATSAPP AGENTS",
                                    color = palette.cyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Surface(
                                color = palette.cardSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onManageAgents()
                                    }
                                    .testTag("btn_settings_open_agents")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Manage Active Agents & Status",
                                            color = palette.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Configure 4 active autonomous bots (Concierge, Appointments, Quotes, VIP)",
                                            color = palette.textMuted,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Surface(
                                        color = palette.cyanAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(100.dp),
                                        border = BorderStroke(1.dp, palette.cyanAccent.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "OPEN DASHBOARD",
                                            color = palette.cyanAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
