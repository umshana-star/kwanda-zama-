package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalZamaPalette
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen

@Composable
fun ZamaFloatingNav(
    activeSection: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalZamaPalette.current
    val navScrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, palette.border, RoundedCornerShape(32.dp)),
        color = palette.surface.copy(alpha = 0.92f),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onNavigate("hero") }
                    .testTag("nav_logo")
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(palette.cyanAccent, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ZAMA®",
                    color = palette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }

            // Right Actions (Horizontally scrollable for responsive mobile/tablet/desktop support)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(navScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0x1F00E5FF),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF)),
                    modifier = Modifier
                        .clickable { onNavigate("gl") }
                        .testTag("nav_gl_mesh_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "3D Mesh",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "3D MESH",
                            color = ZamaElectricCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Surface(
                    color = Color(0x1F00E676),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E676)),
                    modifier = Modifier
                        .clickable { onNavigate("chat") }
                        .testTag("nav_chat_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(ZamaNeonGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "AI AGENT",
                            color = ZamaNeonGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Open Chat",
                            tint = ZamaNeonGreen,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Surface(
                    color = Color(0x1F00E5FF),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF)),
                    modifier = Modifier
                        .clickable { onNavigate("agents") }
                        .testTag("nav_agents_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "WhatsApp Agents",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AGENTS",
                            color = ZamaElectricCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Surface(
                    color = Color(0x1F2979FF),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x442979FF)),
                    modifier = Modifier
                        .clickable { onNavigate("analytics") }
                        .testTag("nav_analytics_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "Chat Analytics",
                            tint = Color(0xFF82B1FF),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CHARTS",
                            color = Color(0xFF82B1FF),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Surface(
                    color = Color(0x1F00E5FF),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF)),
                    modifier = Modifier
                        .clickable { onNavigate("perf") }
                        .testTag("nav_perf_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Agent Performance",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PERF",
                            color = ZamaElectricCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Surface(
                    color = Color(0x1F00E676),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E676)),
                    modifier = Modifier
                        .clickable { onNavigate("cal") }
                        .testTag("nav_cal_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar Sync",
                            tint = ZamaNeonGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CAL",
                            color = ZamaNeonGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Surface(
                    color = Color(0x2600E5FF),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF)),
                    modifier = Modifier
                        .clickable { onNavigate("flow") }
                        .testTag("nav_flow_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = "Sentiment Flow Overlay",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "FLOW",
                            color = ZamaElectricCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Surface(
                    color = Color(0x26FF1744),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FF1744)),
                    modifier = Modifier
                        .clickable { onNavigate("lock") }
                        .testTag("nav_lock_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Security Lock",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LOCK",
                            color = Color(0xFFFF5252),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Surface(
                    color = Color(0x2200E5FF),
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF)),
                    modifier = Modifier
                        .clickable { onNavigate("settings") }
                        .testTag("nav_settings_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Theme and Security Settings",
                            tint = ZamaElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SETTINGS",
                            color = ZamaElectricCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
