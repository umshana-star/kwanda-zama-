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
        description = "Sunlight-optimized pure white canvas with stark ink contrast",
        isDark = false
    )
}

/**
 * Digital Art Chat Interface Theme Modes allowing users to toggle between
 * 'Futuristic Neon' and 'Minimalist Dark' modes to enhance the digital art experience.
 */
enum class ChatThemeMode(
    val id: String,
    val label: String,
    val badgeText: String,
    val description: String,
    val isNeonGlowEnabled: Boolean
) {
    FUTURISTIC_NEON(
        id = "futuristic_neon",
        label = "Futuristic Neon",
        badgeText = "NEON",
        description = "Vibrant cyber-cyan, electric magenta & neon green glow borders with digital art shaders",
        isNeonGlowEnabled = true
    ),
    MINIMALIST_DARK(
        id = "minimalist_dark",
        label = "Minimalist Dark",
        badgeText = "MINIMAL",
        description = "Ultra-clean matte obsidian & monochrome slate geometry for distraction-free focus",
        isNeonGlowEnabled = false
    );

    companion object {
        fun fromId(id: String?): ChatThemeMode {
            return entries.find {
                it.id.equals(id, ignoreCase = true) || it.label.equals(id, ignoreCase = true)
            } ?: FUTURISTIC_NEON
        }
    }
}

data class ChatThemePalette(
    val mode: ChatThemeMode,
    val containerSurface: Color,
    val canvasGradientTop: Color,
    val canvasGradientMiddle: Color,
    val canvasGradientBottom: Color,
    val headerBackground: Color,
    val headerBorder: Color,
    val messageListBackground: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val tertiaryGlow: Color,
    val customerBubbleBackground: Color,
    val customerBubbleBorder: Color,
    val aiBubbleBackground: Color,
    val aiBubbleBorder: Color,
    val inputBarBackground: Color,
    val inputFieldFocusedContainer: Color,
    val inputFieldUnfocusedContainer: Color,
    val inputFieldFocusedBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isNeonGlow: Boolean
)

val FuturisticNeonChatPalette = ChatThemePalette(
    mode = ChatThemeMode.FUTURISTIC_NEON,
    containerSurface = Color(0xFF070B14),
    canvasGradientTop = Color(0xFF060913),
    canvasGradientMiddle = Color(0xFF0B1324),
    canvasGradientBottom = Color(0xFF080E1A),
    headerBackground = Color(0xFF0D1624),
    headerBorder = Color(0xFF00E5FF),
    messageListBackground = Color(0xFF070C15),
    primaryAccent = Color(0xFF00E5FF),
    secondaryAccent = Color(0xFF00E676),
    tertiaryGlow = Color(0xFFD500F9),
    customerBubbleBackground = Color(0xFF083832),
    customerBubbleBorder = Color(0xFF00E676),
    aiBubbleBackground = Color(0xFF111B2B),
    aiBubbleBorder = Color(0xFF00E5FF),
    inputBarBackground = Color(0xFF0D1522),
    inputFieldFocusedContainer = Color(0xFF142033),
    inputFieldUnfocusedContainer = Color(0xFF101928),
    inputFieldFocusedBorder = Color(0xFF00E5FF),
    textPrimary = Color(0xFFF5F9FF),
    textSecondary = Color(0xFF9EC5E8),
    isNeonGlow = true
)

val MinimalistDarkChatPalette = ChatThemePalette(
    mode = ChatThemeMode.MINIMALIST_DARK,
    containerSurface = Color(0xFF0A0A0C),
    canvasGradientTop = Color(0xFF09090B),
    canvasGradientMiddle = Color(0xFF0E0E11),
    canvasGradientBottom = Color(0xFF0B0B0E),
    headerBackground = Color(0xFF121216),
    headerBorder = Color(0xFF27272A),
    messageListBackground = Color(0xFF0B0B0E),
    primaryAccent = Color(0xFFE4E4E7),
    secondaryAccent = Color(0xFFA1A1AA),
    tertiaryGlow = Color(0xFF52525B),
    customerBubbleBackground = Color(0xFF22232A),
    customerBubbleBorder = Color(0xFF3F3F46),
    aiBubbleBackground = Color(0xFF16161B),
    aiBubbleBorder = Color(0xFF27272A),
    inputBarBackground = Color(0xFF111115),
    inputFieldFocusedContainer = Color(0xFF1C1C22),
    inputFieldUnfocusedContainer = Color(0xFF16161B),
    inputFieldFocusedBorder = Color(0xFFA1A1AA),
    textPrimary = Color(0xFFF4F4F5),
    textSecondary = Color(0xFFA1A1AA),
    isNeonGlow = false
)

fun ChatThemeMode.toPalette(): ChatThemePalette = when (this) {
    ChatThemeMode.FUTURISTIC_NEON -> FuturisticNeonChatPalette
    ChatThemeMode.MINIMALIST_DARK -> MinimalistDarkChatPalette
}

val LocalChatThemeMode = compositionLocalOf { ChatThemeMode.FUTURISTIC_NEON }
val LocalChatThemePalette = compositionLocalOf { FuturisticNeonChatPalette }
val LocalZamaThemeMode = compositionLocalOf { ZamaThemeMode.FUTURISTIC_DARK }

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("zama_theme_preferences", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadInitialTheme())
    val themeMode: StateFlow<ZamaThemeMode> = _themeMode.asStateFlow()

    private val _chatThemeMode = MutableStateFlow(loadInitialChatTheme())
    val chatThemeMode: StateFlow<ChatThemeMode> = _chatThemeMode.asStateFlow()

    private val _chatThemePalette = MutableStateFlow(_chatThemeMode.value.toPalette())
    val chatThemePalette: StateFlow<ChatThemePalette> = _chatThemePalette.asStateFlow()

    private fun loadInitialTheme(): ZamaThemeMode {
        val savedId = prefs.getString(KEY_THEME_MODE, ZamaThemeMode.FUTURISTIC_DARK.id)
        return ZamaThemeMode.entries.find { it.id == savedId } ?: ZamaThemeMode.FUTURISTIC_DARK
    }

    private fun loadInitialChatTheme(): ChatThemeMode {
        val savedId = prefs.getString(KEY_CHAT_THEME_MODE, ChatThemeMode.FUTURISTIC_NEON.id)
        return ChatThemeMode.fromId(savedId)
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

    fun setChatThemeMode(mode: ChatThemeMode) {
        prefs.edit().putString(KEY_CHAT_THEME_MODE, mode.id).apply()
        _chatThemeMode.value = mode
        _chatThemePalette.value = mode.toPalette()
    }

    fun toggleChatTheme(): ChatThemeMode {
        val next = when (_chatThemeMode.value) {
            ChatThemeMode.FUTURISTIC_NEON -> ChatThemeMode.MINIMALIST_DARK
            ChatThemeMode.MINIMALIST_DARK -> ChatThemeMode.FUTURISTIC_NEON
        }
        setChatThemeMode(next)
        return next
    }

    companion object {
        const val KEY_THEME_MODE = "key_active_theme_mode"
        const val KEY_CHAT_THEME_MODE = "key_chat_theme_mode"

        class Factory(private val app: Application) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ThemeViewModel(app) as T
            }
        }
    }
}
