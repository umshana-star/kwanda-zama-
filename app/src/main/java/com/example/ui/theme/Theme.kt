package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val ZamaDarkColorScheme = darkColorScheme(
  primary = ZamaElectricCyan,
  onPrimary = ZamaVoid,
  primaryContainer = ZamaElevated,
  onPrimaryContainer = ZamaChromeLight,
  secondary = ZamaChromeMid,
  onSecondary = ZamaVoid,
  secondaryContainer = ZamaCardSurface,
  onSecondaryContainer = ZamaChromeLight,
  tertiary = ZamaElectricBlue,
  onTertiary = Color.White,
  background = ZamaVoid,
  onBackground = ZamaChromeLight,
  surface = ZamaDarkSurface,
  onSurface = ZamaChromeLight,
  surfaceVariant = ZamaCardSurface,
  onSurfaceVariant = ZamaSilverMuted,
  outline = ZamaBorder,
  outlineVariant = ZamaBorderGlow
)

private val ZamaLightColorScheme = lightColorScheme(
  primary = ZamaDaylightCyan,
  onPrimary = Color.White,
  primaryContainer = ZamaDaylightElevated,
  onPrimaryContainer = ZamaDaylightTextPrimary,
  secondary = ZamaDaylightTextSecondary,
  onSecondary = Color.White,
  secondaryContainer = ZamaDaylightCard,
  onSecondaryContainer = ZamaDaylightTextPrimary,
  tertiary = ZamaDaylightBlue,
  onTertiary = Color.White,
  background = ZamaDaylightBackground,
  onBackground = ZamaDaylightTextPrimary,
  surface = ZamaDaylightSurface,
  onSurface = ZamaDaylightTextPrimary,
  surfaceVariant = ZamaDaylightCard,
  onSurfaceVariant = ZamaDaylightTextSecondary,
  outline = ZamaDaylightBorder,
  outlineVariant = ZamaDaylightBorderGlow
)

@Composable
fun ZamaTheme(
  themeMode: ZamaThemeMode = ZamaThemeMode.FUTURISTIC_DARK,
  content: @Composable () -> Unit
) {
  val colorScheme = if (themeMode.isDark) ZamaDarkColorScheme else ZamaLightColorScheme
  val palette = if (themeMode.isDark) DarkPalette else LightPalette

  CompositionLocalProvider(
    LocalZamaThemeMode provides themeMode,
    LocalZamaPalette provides palette
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val mode = if (darkTheme) ZamaThemeMode.FUTURISTIC_DARK else ZamaThemeMode.HIGH_CONTRAST_LIGHT
  ZamaTheme(themeMode = mode, content = content)
}

/**
 * Reusable preview wrapper that explicitly provides [ZamaTheme] ([MaterialTheme])
 * and a [Surface] container so all Composable previews render with proper colors,
 * typography, and surface styling in the IDE.
 */
@Composable
fun PreviewWrapper(
  modifier: Modifier = Modifier,
  darkTheme: Boolean = true,
  themeMode: ZamaThemeMode = if (darkTheme) ZamaThemeMode.FUTURISTIC_DARK else ZamaThemeMode.HIGH_CONTRAST_LIGHT,
  content: @Composable () -> Unit
) {
  val colorScheme = if (themeMode.isDark) ZamaDarkColorScheme else ZamaLightColorScheme
  val palette = if (themeMode.isDark) DarkPalette else LightPalette

  CompositionLocalProvider(
    LocalZamaThemeMode provides themeMode,
    LocalZamaPalette provides palette
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography
    ) {
      Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        content = content
      )
    }
  }
}

