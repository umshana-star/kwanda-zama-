package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChatThemeMode
import com.example.ui.theme.LocalChatThemeMode
import com.example.ui.theme.LocalChatThemePalette
import com.example.ui.theme.LocalZamaThemeMode
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaPurple
import com.example.ui.theme.ZamaThemeMode

/**
 * Compact header pill allowing instant 1-tap switching between 'Futuristic Neon' and 'Minimalist Dark'
 * modes for the chat interface to enhance the digital art experience.
 */
@Composable
fun ChatThemeSwitcherPill(
    currentChatTheme: ChatThemeMode = LocalChatThemeMode.current,
    onToggleChatTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNeon = currentChatTheme == ChatThemeMode.FUTURISTIC_NEON

    val bgColor by animateColorAsState(
        targetValue = if (isNeon) Color(0x3300E5FF) else Color(0xFF1F1F24),
        animationSpec = tween(240),
        label = "chat_theme_pill_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isNeon) ZamaElectricCyan else Color(0xFF52525B),
        animationSpec = tween(240),
        label = "chat_theme_pill_border"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isNeon) ZamaElectricCyan else Color(0xFFE4E4E7),
        animationSpec = tween(240),
        label = "chat_theme_pill_content"
    )

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clickable { onToggleChatTheme() }
            .testTag("chat_theme_switcher_btn")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isNeon) Icons.Default.AutoAwesome else Icons.Default.Contrast,
                contentDescription = "Switch Chat Interface Theme between Futuristic Neon and Minimalist Dark",
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = currentChatTheme.badgeText,
                color = contentColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Interactive Digital Art Chat Theme Switcher Bar allowing users to toggle or select between
 * 'Futuristic Neon' and 'Minimalist Dark' modes directly inside the chat interface.
 */
@Composable
fun ChatThemeSwitcherBar(
    currentChatTheme: ChatThemeMode,
    onSelectChatTheme: (ChatThemeMode) -> Unit,
    onToggleChatTheme: () -> Unit = {
        val next = if (currentChatTheme == ChatThemeMode.FUTURISTIC_NEON) {
            ChatThemeMode.MINIMALIST_DARK
        } else {
            ChatThemeMode.FUTURISTIC_NEON
        }
        onSelectChatTheme(next)
    },
    modifier: Modifier = Modifier
) {
    val isNeon = currentChatTheme == ChatThemeMode.FUTURISTIC_NEON
    val barBg by animateColorAsState(
        targetValue = if (isNeon) Color(0xFF0A1320) else Color(0xFF101014),
        animationSpec = tween(240),
        label = "chat_theme_bar_bg"
    )
    val barBorder by animateColorAsState(
        targetValue = if (isNeon) Color(0x4D00E5FF) else Color(0xFF27272A),
        animationSpec = tween(240),
        label = "chat_theme_bar_border"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
            .border(BorderStroke(0.5.dp, barBorder))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("chat_theme_switcher_bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Digital Art Chat Theme Mode",
                tint = if (isNeon) ZamaElectricCyan else Color(0xFFA1A1AA),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "ART MODE:",
                color = if (isNeon) Color(0xFF9EC5E8) else Color(0xFFA1A1AA),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = if (isNeon) Color(0x26D500F9) else Color(0xFF1F1F24),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(
                    0.5.dp,
                    if (isNeon) ZamaPurple else Color(0xFF3F3F46)
                ),
                modifier = Modifier
                    .clickable { onToggleChatTheme() }
                    .testTag("chat_active_theme_badge")
            ) {
                Text(
                    text = currentChatTheme.label.uppercase(),
                    color = if (isNeon) ZamaElectricCyan else Color(0xFFF4F4F5),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Futuristic Neon Mode Option Pill
            Surface(
                color = if (isNeon) Color(0x3300E5FF) else Color(0x14FFFFFF),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(
                    width = if (isNeon) 1.dp else 0.5.dp,
                    color = if (isNeon) ZamaElectricCyan else Color(0xFF27272A)
                ),
                modifier = Modifier
                    .clickable { onSelectChatTheme(ChatThemeMode.FUTURISTIC_NEON) }
                    .testTag("chat_theme_futuristic_neon_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (isNeon) ZamaElectricCyan else Color(0xFF52525B),
                                CircleShape
                            )
                    )
                    Text(
                        text = "Futuristic Neon",
                        color = if (isNeon) ZamaElectricCyan else Color(0xFFA1A1AA),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isNeon) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // Minimalist Dark Mode Option Pill
            Surface(
                color = if (!isNeon) Color(0xFF27272A) else Color(0x14FFFFFF),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(
                    width = if (!isNeon) 1.dp else 0.5.dp,
                    color = if (!isNeon) Color(0xFFE4E4E7) else Color(0xFF27272A)
                ),
                modifier = Modifier
                    .clickable { onSelectChatTheme(ChatThemeMode.MINIMALIST_DARK) }
                    .testTag("chat_theme_minimalist_dark_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (!isNeon) Color(0xFFF4F4F5) else Color(0xFF52525B),
                                CircleShape
                            )
                    )
                    Text(
                        text = "Minimalist Dark",
                        color = if (!isNeon) Color(0xFFF4F4F5) else Color(0xFFA1A1AA),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (!isNeon) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Compact header pill allowing instant 1-tap switching between Futuristic Dark and High-Contrast Light mode.
 */
@Composable
fun ThemeTogglePill(
    currentMode: ZamaThemeMode,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = currentMode.isDark

    val backgroundColor by animateColorAsState(
        targetValue = if (isDark) Color(0x2E00E5FF) else Color(0x2E007A99),
        animationSpec = tween(250),
        label = "pill_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isDark) Color(0x6600E5FF) else Color(0x88007A99),
        animationSpec = tween(250),
        label = "pill_border"
    )

    val contentColor = if (isDark) ZamaElectricCyan else Color(0xFF0097A7)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clickable { onToggleTheme() }
            .testTag("btn_theme_toggle")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode,
                contentDescription = "Switch Theme",
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isDark) "LIGHT" else "DARK",
                color = contentColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
    }
}
