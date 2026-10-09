package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

val ZamaDarkColorScheme = darkColorScheme(
    primary = ZamaElectricCyan,
    onPrimary = ZamaVoid,
    secondary = ZamaNeonGreen,
    onSecondary = ZamaVoid,
    tertiary = ZamaPurple,
    background = ZamaVoid,
    onBackground = ZamaChromeLight,
    surface = ZamaDarkSurface,
    onSurface = ZamaChromeLight,
    surfaceVariant = ZamaCardSurface,
    onSurfaceVariant = ZamaChromeMid,
    outline = ZamaBorder
)

val ZamaLightColorScheme = lightColorScheme(
    primary = ZamaDaylightCyan,
    onPrimary = ZamaDaylightSurface,
    secondary = ZamaDaylightGreen,
    onSecondary = ZamaDaylightSurface,
    tertiary = ZamaDaylightBlue,
    background = ZamaDaylightBackground,
    onBackground = ZamaDaylightTextPrimary,
    surface = ZamaDaylightSurface,
    onSurface = ZamaDaylightTextPrimary,
    surfaceVariant = ZamaDaylightCard,
    onSurfaceVariant = ZamaDaylightTextSecondary,
    outline = ZamaDaylightBorder
)

@Composable
fun ZamaTheme(
    themeMode: ZamaThemeMode = ZamaThemeMode.FUTURISTIC_DARK,
    chatThemeMode: ChatThemeMode = ChatThemeMode.FUTURISTIC_NEON,
    content: @Composable () -> Unit
) {
    val colorScheme = if (themeMode.isDark) ZamaDarkColorScheme else ZamaLightColorScheme
    val chatPalette = chatThemeMode.toPalette()

    CompositionLocalProvider(
        LocalZamaThemeMode provides themeMode,
        LocalChatThemeMode provides chatThemeMode,
        LocalChatThemePalette provides chatPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
