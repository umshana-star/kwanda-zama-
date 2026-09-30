package com.example.ui.theme

import android.app.Application
import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Display theme options for Zama AI Business Hub.
 */
enum class ZamaThemeMode(
    val id: String,
    val label: String,
    val description: String,
    val isDark: Boolean
) {
    FUTURISTIC_DARK(
        id = "futuristic_dark",
        label = "Futuristic Dark",
        description = "Deep obsidian canvas with luminous cyber-cyan & neon green telemetry",
        isDark = true
    ),
    HIGH_CONTRAST_LIGHT(
        id = "high_contrast_light",
        label = "High-Contrast Daylight",
        description = "Sunlight-optimized pure white canvas with stark ink contrast for daylight legibility",
        isDark = false
    )
}

/**
 * Semantic theme palette holding dynamic color tokens for the active theme.
 */
data class ZamaCustomPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val cardSurface: Color,
    val elevatedSurface: Color,
    val border: Color,
    val borderGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val cyanAccent: Color,
    val greenAccent: Color,
    val blueAccent: Color,
    val glassBackground: Color,
    val glassBorder: Color
)

val DarkPalette = ZamaCustomPalette(
    isDark = true,
    background = ZamaVoid,
    surface = ZamaDarkSurface,
    cardSurface = ZamaCardSurface,
    elevatedSurface = ZamaElevated,
    border = ZamaBorder,
    borderGlow = ZamaBorderGlow,
    textPrimary = ZamaChromeLight,
    textSecondary = ZamaChromeMid,
    textMuted = ZamaGraphite,
    cyanAccent = ZamaElectricCyan,
    greenAccent = ZamaNeonGreen,
    blueAccent = ZamaElectricBlue,
    glassBackground = ZamaGlassBackground,
    glassBorder = ZamaGlassBorder
)

val LightPalette = ZamaCustomPalette(
    isDark = false,
    background = ZamaDaylightBackground,
    surface = ZamaDaylightSurface,
    cardSurface = ZamaDaylightCard,
    elevatedSurface = ZamaDaylightElevated,
    border = ZamaDaylightBorder,
    borderGlow = ZamaDaylightBorderGlow,
    textPrimary = ZamaDaylightTextPrimary,
    textSecondary = ZamaDaylightTextSecondary,
    textMuted = ZamaDaylightTextMuted,
    cyanAccent = ZamaDaylightCyan,
    greenAccent = ZamaDaylightGreen,
    blueAccent = ZamaDaylightBlue,
    glassBackground = ZamaDaylightGlassBg,
    glassBorder = ZamaDaylightGlassBorder
)

val LocalZamaThemeMode = compositionLocalOf { ZamaThemeMode.FUTURISTIC_DARK }
val LocalZamaPalette = compositionLocalOf { DarkPalette }

/**
 * ViewModel managing global theme selection and persistent SharedPreferences.
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("zama_theme_preferences", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadInitialTheme())
    val themeMode: StateFlow<ZamaThemeMode> = _themeMode.asStateFlow()

    private fun loadInitialTheme(): ZamaThemeMode {
        val savedId = prefs.getString(KEY_THEME_MODE, ZamaThemeMode.FUTURISTIC_DARK.id)
        return ZamaThemeMode.values().find { it.id == savedId } ?: ZamaThemeMode.FUTURISTIC_DARK
    }

    fun setThemeMode(mode: ZamaThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.id).apply()
        _themeMode.value = mode
    }

    fun toggleTheme() {
        val next = if (_themeMode.value.isDark) {
            ZamaThemeMode.HIGH_CONTRAST_LIGHT
        } else {
            ZamaThemeMode.FUTURISTIC_DARK
        }
        setThemeMode(next)
    }

    companion object {
        private const val KEY_THEME_MODE = "key_active_theme_mode"

        class Factory(private val app: Application) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ThemeViewModel(app) as T
            }
        }
    }
}
