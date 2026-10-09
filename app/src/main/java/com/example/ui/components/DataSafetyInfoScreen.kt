package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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

/**
 * DataSafetyInfoScreen composable detailing data types collected (chat history,
 * agent events, auth metadata) to ensure full compliance with Google Play
 * Data Safety requirements.
 */
@Composable
fun DataSafetyInfoScreen(
    onBack: () -> Unit,
    onNavigateToPrivacySettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalChatThemePalette.current

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
            .testTag("data_safety_info_screen")
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
                        .testTag("data_safety_back_button")
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
                        text = "GOOGLE PLAY DATA SAFETY",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Official Data Collection & Handling Disclosures",
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
            // Overview Compliance Banner
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
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = ZamaNeonGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Play Data Safety Declaration",
                                    color = ZamaChromeLight,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Transparent declarations for all handled data types",
                                    color = ZamaChromeMid,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Summary Badges Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SafetyPill(
                        label = "No 3rd-Party Sharing",
                        icon = Icons.Default.Shield,
                        modifier = Modifier.weight(1f)
                    )
                    SafetyPill(
                        label = "Encrypted In Transit",
                        icon = Icons.Default.Https,
                        modifier = Modifier.weight(1f)
                    )
                    SafetyPill(
                        label = "Deletion Supported",
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Data Types Collected Header
            item {
                Text(
                    text = "DECLARED DATA TYPES & PURPOSES",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // 1. Chat History
            item {
                DataSafetyTypeCard(
                    testTag = "data_safety_messages",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    dataType = "Messages (In-App & WhatsApp)",
                    dataCategory = "Category: Messages • In-app messages",
                    collected = "Collected: Yes (Stored in local private SQLite sandbox)",
                    shared = "Shared with 3rd parties: No",
                    ephemeral = "Ephemeral: No (Persisted locally until user wipes)",
                    purpose = "App functionality: Enables conversational concierge, salon booking workflows, appointment scheduling, and multi-turn context retention.",
                    details = "Messages, timestamp, sender identifier, and conversation threads are kept on the user device. No message scraping or ad monetization."
                )
            }

            // 2. Agent Events & Performance Diagnostics
            item {
                DataSafetyTypeCard(
                    testTag = "data_safety_agent_events",
                    icon = Icons.Default.Analytics,
                    dataType = "App Performance & Agent Events",
                    dataCategory = "Category: App info and performance • Diagnostics",
                    collected = "Collected: Yes (Stored in local Room database table)",
                    shared = "Shared with 3rd parties: No",
                    ephemeral = "Ephemeral: No (Persisted for audit trail)",
                    purpose = "Analytics & App functionality: Records AI model response latency, intent classification tags, autonomous dispatch events, and human handoff escalations.",
                    details = "Enables business owners to review autonomous salon assistant accuracy and diagnose dispatch anomalies without transmitting telemetry to third-party ad networks."
                )
            }

            // 3. Auth Metadata & Security Credentials
            item {
                DataSafetyTypeCard(
                    testTag = "data_safety_auth_metadata",
                    icon = Icons.Default.Lock,
                    dataType = "Authentication & Security Metadata",
                    dataCategory = "Category: Personal info • Account info / Security credentials",
                    collected = "Collected: Yes (PBKDF2 cryptographic hash & salt)",
                    shared = "Shared with 3rd parties: No",
                    ephemeral = "Ephemeral: No (Retained until user resets app)",
                    purpose = "Account management, security & fraud prevention: Enforces PIN vault protection, brute-force rate-limiting, and temporary lockout safeguards.",
                    details = "Plaintext PINs are NEVER stored or transmitted. Hashes use PBKDF2WithHmacSHA256 with 16-byte random salt. Biometrics use Android TEE Hardware Enclave."
                )
            }

            // 4. Voice Dictation Audio
            item {
                DataSafetyTypeCard(
                    testTag = "data_safety_audio",
                    icon = Icons.Default.Mic,
                    dataType = "Voice & Audio Dictation",
                    dataCategory = "Category: Audio files • Voice or sound recordings",
                    collected = "Collected: Ephemeral processing only",
                    shared = "Shared with 3rd parties: No",
                    ephemeral = "Ephemeral: Yes (Discarded immediately upon text transcription)",
                    purpose = "App functionality: Hands-free voice speech-to-text input for crafting messages and salon queries.",
                    details = "Microphone input is captured only while holding the record button. Transcribed on-device via Android SpeechRecognizer; audio stream is immediately released."
                )
            }

            // Security Practices Section
            item {
                Text(
                    text = "SECURITY PRACTICES (PLAY COMPLIANCE)",
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
                        SecurityRowItem(
                            title = "Data encrypted in transit",
                            description = "All network traffic to WhatsApp Business APIs and Gemini AI intelligence endpoints enforces TLS 1.3 / HTTPS encryption."
                        )
                        SecurityRowItem(
                            title = "Requests for deletion provided",
                            description = "Users can permanently delete all chat history, agent logs, and auth credentials with the 'Wipe All Local Data' feature."
                        )
                        SecurityRowItem(
                            title = "Commitment to Google Play Families Policy",
                            description = "The app does not collect advertising IDs (AAID), background location, or external storage files."
                        )
                    }
                }
            }

            // Link to Privacy Settings / Data Wipe
            if (onNavigateToPrivacySettings != null) {
                item {
                    Button(
                        onClick = onNavigateToPrivacySettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (palette.isNeonGlow) ZamaElectricCyan else Color(0xFF27272A),
                            contentColor = if (palette.isNeonGlow) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("manage_privacy_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MANAGE RETENTION & WIPE DATA",
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

@Composable
private fun DataSafetyTypeCard(
    testTag: String,
    icon: ImageVector,
    dataType: String,
    dataCategory: String,
    collected: String,
    shared: String,
    ephemeral: String,
    purpose: String,
    details: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ZamaCardSurface),
        border = BorderStroke(1.dp, ZamaBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = ZamaElectricCyan.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = dataType,
                        color = ZamaChromeLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = dataCategory,
                        color = ZamaNeonGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x22000000), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "• $collected",
                    color = ZamaChromeLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "• $shared",
                    color = ZamaChromeLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "• $ephemeral",
                    color = ZamaChromeLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = purpose,
                color = ZamaChromeLight,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = details,
                color = ZamaChromeMid,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SafetyPill(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ZamaCardSurface,
        border = BorderStroke(1.dp, ZamaBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ZamaNeonGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = ZamaChromeLight,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SecurityRowItem(
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ZamaNeonGreen,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                color = ZamaChromeLight,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = ZamaChromeMid,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
