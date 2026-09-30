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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.LocalZamaPalette
import com.example.ui.theme.LocalZamaThemeMode
import com.example.ui.theme.ZamaDaylightCyan
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaThemeMode

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
    val palette = LocalZamaPalette.current

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

    val contentColor = if (isDark) ZamaElectricCyan else ZamaDaylightCyan

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

/**
 * Full visual Theme Selector Card for the Settings dialog.
 */
@Composable
fun ThemeSettingsSection(
    currentMode: ZamaThemeMode,
    onSelectMode: (ZamaThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalZamaPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("theme_settings_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LightMode,
                contentDescription = null,
                tint = palette.cyanAccent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "DAYLIGHT & DISPLAY THEME",
                color = palette.cyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        Text(
            text = "Switch between immersive cyberpunk dark styling and high-contrast daylight white mode for outdoor reading and bright salon environments.",
            color = palette.textSecondary,
            fontSize = 10.5.sp,
            lineHeight = 14.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Futuristic Dark Card
            ThemeModeOptionCard(
                mode = ZamaThemeMode.FUTURISTIC_DARK,
                isSelected = currentMode == ZamaThemeMode.FUTURISTIC_DARK,
                onClick = { onSelectMode(ZamaThemeMode.FUTURISTIC_DARK) },
                previewColor = Color(0xFF0A0C10),
                accentColor = ZamaElectricCyan,
                icon = Icons.Default.DarkMode,
                modifier = Modifier.weight(1f)
            )

            // High-Contrast Light Card
            ThemeModeOptionCard(
                mode = ZamaThemeMode.HIGH_CONTRAST_LIGHT,
                isSelected = currentMode == ZamaThemeMode.HIGH_CONTRAST_LIGHT,
                onClick = { onSelectMode(ZamaThemeMode.HIGH_CONTRAST_LIGHT) },
                previewColor = Color(0xFFFFFFFF),
                accentColor = ZamaDaylightCyan,
                icon = Icons.Default.WbSunny,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeModeOptionCard(
    mode: ZamaThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    previewColor: Color,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val palette = LocalZamaPalette.current

    val borderColor = if (isSelected) accentColor else palette.border
    val cardBackground = if (isSelected) {
        if (palette.isDark) Color(0x2200E5FF) else Color(0x15007A99)
    } else {
        palette.cardSurface
    }

    Surface(
        color = cardBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .clickable { onClick() }
            .testTag("theme_option_${mode.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini theme preview disc
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                        .border(1.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = mode.label,
                color = if (isSelected) palette.textPrimary else palette.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = if (mode.isDark) "Cyber obsidian + neon" else "Stark daylight contrast",
                color = palette.textMuted,
                fontSize = 9.5.sp,
                lineHeight = 12.sp
            )
        }
    }
}
