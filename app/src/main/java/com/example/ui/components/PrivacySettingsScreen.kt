package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalChatThemePalette
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaCardSurface
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.viewmodel.ChatViewModel

/**
 * PrivacySettingsScreen composable that provides comprehensive information
 * about data retention, zero-broker storage policies, cryptographic safeguards,
 * and an explicit 'Wipe All Local Data' button to fulfill app data safety and reset requirements.
 */
@Composable
fun PrivacySettingsScreen(
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
    onDataWiped: () -> Unit = {},
    onNavigateToDataSafety: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalChatThemePalette.current
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var isWiping by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            palette.canvasGradientTop,
            palette.canvasGradientMiddle,
            palette.canvasGradientBottom
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .testTag("privacy_settings_screen")
    ) {
        // App Bar / Top Header
        Surface(
            color = palette.headerBackground,
            border = BorderStroke(1.dp, palette.headerBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("privacy_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "PRIVACY & DATA SAFETY",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Data Retention Policy & Local Storage Controls",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ZamaCardSurface),
                    border = BorderStroke(1.dp, if (palette.isNeonGlow) ZamaElectricCyan.copy(alpha = 0.4f) else ZamaBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = ZamaNeonGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = ZamaNeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Zero-Broker Privacy Guarantee",
                                    color = ZamaChromeLight,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Data remains strictly on-device in your private sandbox",
                                    color = ZamaChromeMid,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Data Retention Information Section
            item {
                Text(
                    text = "DATA RETENTION & LIFECYCLE",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                RetentionPolicyCard(
                    icon = Icons.Default.Storage,
                    title = "Customer Chat History & Room Database",
                    retentionPeriod = "Until manually cleared by user or app uninstalled",
                    details = "Stored securely in an encrypted local SQLite sandbox on your device. Never synced to third-party ad brokers or monetization engines."
                )
            }

            item {
                RetentionPolicyCard(
                    icon = Icons.Default.SyncAlt,
                    title = "WhatsApp Inbound/Outbound Interactions",
                    retentionPeriod = "Retained locally for business audit & multi-turn context",
                    details = "Messages, contact phone numbers, and AI reasoning traces are logged locally for auditing salon appointments and customer inquiries."
                )
            }

            item {
                RetentionPolicyCard(
                    icon = Icons.Default.Mic,
                    title = "Voice Dictation Audio",
                    retentionPeriod = "Ephemeral (0 seconds retention)",
                    details = "Microphone input is captured only during explicit user push-to-talk. Audio streams are immediately discarded upon text transcription."
                )
            }

            item {
                RetentionPolicyCard(
                    icon = Icons.Default.Lock,
                    title = "Authentication Credentials & PIN Vault",
                    retentionPeriod = "Permanent local storage (PBKDF2 Salted Hash)",
                    details = "Plaintext PINs are never stored or logged. Salted verification hashes are kept in hardware-backed KeyStore until reset."
                )
            }

            item {
                RetentionPolicyCard(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Samples",
                    retentionPeriod = "Never stored or accessed by app",
                    details = "Authentication is executed directly by the Android Secure Enclave (TEE). Biometric data never leaves the operating system."
                )
            }

            // Compliance & Security Practices
            item {
                Text(
                    text = "SECURITY SAFEGUARDS",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ZamaDarkSurface),
                    border = BorderStroke(1.dp, ZamaBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecurityBullet(text = "Encrypted in Transit: Enforced TLS 1.3 / HTTPS for all network requests")
                        SecurityBullet(text = "No Sensitive Logs: PINs, tokens, and private conversations are never dumped to logcat")
                        SecurityBullet(text = "Hardware Security: KeyStore cryptographic isolation for secure vault operations")
                        SecurityBullet(text = "Data Portability: One-tap export of interaction history to JSON via Android share sheet")
                    }
                }
            }

            // Wipe All Local Data Section
            if (onNavigateToDataSafety != null) {
                item {
                    OutlinedButton(
                        onClick = onNavigateToDataSafety,
                        border = BorderStroke(1.dp, if (palette.isNeonGlow) ZamaElectricCyan else ZamaBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("nav_to_data_safety_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = null,
                            tint = if (palette.isNeonGlow) ZamaElectricCyan else palette.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GOOGLE PLAY DATA SAFETY DETAILS",
                            color = if (palette.isNeonGlow) ZamaElectricCyan else palette.textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "DATA RESET & SAFETY CONTROLS",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1111)),
                    border = BorderStroke(1.dp, Color(0x66FF5252)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Wipe All Local Data",
                                color = Color(0xFFFF8A80),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Fulfill complete data deletion requirements. Instantly purges all Room SQLite database records (chat messages, WhatsApp interaction logs, audit history), resets security vault credentials and PIN salt, and restores app to factory initial state.",
                            color = ZamaChromeMid,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showWipeConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("wipe_all_local_data_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WIPE ALL LOCAL DATA",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isWiping) showWipeConfirmDialog = false },
            containerColor = ZamaDarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Wipe All Local Data?",
                        color = ZamaChromeLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all local data? This will permanently erase your chat history, WhatsApp customer interaction logs, booking records, and PIN credentials. This action cannot be undone.",
                    color = ZamaChromeMid,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isWiping = true
                        chatViewModel.deleteAllLocalDataAndReset {
                            isWiping = false
                            showWipeConfirmDialog = false
                            onDataWiped()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_wipe_data_button")
                ) {
                    Text(
                        text = if (isWiping) "WIPING..." else "CONFIRM WIPE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showWipeConfirmDialog = false },
                    border = BorderStroke(1.dp, ZamaBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("cancel_wipe_data_button")
                ) {
                    Text(
                        text = "CANCEL",
                        color = ZamaChromeLight,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        )
    }
}

@Composable
private fun RetentionPolicyCard(
    icon: ImageVector,
    title: String,
    retentionPeriod: String,
    details: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ZamaCardSurface),
        border = BorderStroke(1.dp, ZamaBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = ZamaChromeLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Retention: ",
                    color = ZamaNeonGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = retentionPeriod,
                    color = ZamaChromeLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = details,
                color = ZamaChromeMid,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SecurityBullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = ZamaNeonGreen,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = ZamaChromeMid,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
