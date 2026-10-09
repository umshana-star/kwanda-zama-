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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.ZamaVoid
import com.example.ui.viewmodel.ChatViewModel

/**
 * Data & Security Privacy Screen defining transparent data practices,
 * hardware security guarantees, and an explicit pathway to wipe all local data.
 */
@Composable
fun DataSecurityPrivacyScreen(
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
    onDataPurged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalChatThemePalette.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isPurging by remember { mutableStateOf(false) }

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
            .testTag("data_security_privacy_screen")
    ) {
        // Top Header
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
                        .size(44.dp)
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
                        text = "DATA & SECURITY VAULT",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Privacy Foundation • Zero-Trust Safeguards",
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
            // Overview banner
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
                                modifier = Modifier.size(36.dp)
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
                                    text = "Privacy-First Architecture",
                                    color = ZamaChromeLight,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Your business conversations remain on your device",
                                    color = ZamaChromeMid,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Data Foundation Section
            item {
                Text(
                    text = "WHAT DATA THIS APP STORES",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SecurityDataRow(
                    icon = Icons.Default.Storage,
                    title = "Chat History & Room Database",
                    description = "Customer messages and AI reasoning traces are stored locally in private SQLite sandbox. Never shared with 3rd-party ad brokers.",
                    badge = "On-Device SQLite"
                )
            }

            item {
                SecurityDataRow(
                    icon = Icons.Default.Lock,
                    title = "Authentication & PIN Metadata",
                    description = "PINs are hashed via PBKDF2WithHmacSHA256 with 32-byte cryptographic salt. Raw plaintext PINs are NEVER stored or logged.",
                    badge = "PBKDF2 Salted"
                )
            }

            item {
                SecurityDataRow(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Verification",
                    description = "Biometrics are processed strictly by the Android OS Secure Enclave (TEE). No fingerprint/facial images are ever accessed by the app.",
                    badge = "Hardware TEE"
                )
            }

            item {
                SecurityDataRow(
                    icon = Icons.Default.Mic,
                    title = "Microphone & Speech Dictation",
                    description = "Audio captured only when microphone button is held. Processed ephemerally for transcription; never stored as audio files.",
                    badge = "Ephemeral Only"
                )
            }

            // Network and Security Safeguards
            item {
                Text(
                    text = "NETWORK & LOGGING SECURITY",
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
                        SecurityCheckBullet(text = "Enforced TLS 1.3 / HTTPS on all WhatsApp Business endpoints")
                        SecurityCheckBullet(text = "No permanent API tokens in BuildConfig, source, or APK bytecode")
                        SecurityCheckBullet(text = "Sensitive logs sanitized: PINs, tokens, and payloads are never dumped")
                        SecurityCheckBullet(text = "Server proxy option routes Gemini AI logic without exposing client secrets")
                    }
                }
            }

            // Reset & Data Purge Pathway
            item {
                Text(
                    text = "DATA RETENTION & APP RESET",
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
                                text = "Delete All Data & Reset App",
                                color = Color(0xFFFF8A80),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Immediately purges all Room SQLite tables (messages, interactions, bookings, logs), wipes security vault PIN and salt, resets away settings, and restores app to fresh-install state.",
                            color = ZamaChromeMid,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("button_purge_all_data")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PURGE ALL LOCAL DATA",
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
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPurging) showDeleteConfirmDialog = false },
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
                        text = "Permanently Reset App?",
                        color = ZamaChromeLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Text(
                    text = "This action is irreversible. All client conversations, WhatsApp interaction logs, audit history, and security credentials will be permanently erased from this device.",
                    color = ZamaChromeMid,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isPurging = true
                        chatViewModel.deleteAllLocalDataAndReset {
                            isPurging = false
                            showDeleteConfirmDialog = false
                            onDataPurged()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_purge_button")
                ) {
                    Text(
                        text = if (isPurging) "PURGING..." else "CONFIRM PURGE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    border = BorderStroke(1.dp, ZamaBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("cancel_purge_button")
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
private fun SecurityDataRow(
    icon: ImageVector,
    title: String,
    description: String,
    badge: String
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                Surface(
                    color = ZamaElectricCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badge,
                        color = ZamaElectricCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = ZamaChromeMid,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SecurityCheckBullet(text: String) {
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
