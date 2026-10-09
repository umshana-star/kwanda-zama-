package com.example.ui.theme

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeManagerTest {

    private lateinit var application: Application
    private lateinit var themeViewModel: ThemeViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        themeViewModel = ThemeViewModel(application)
    }

    @Test
    fun testChatThemeMode_toggleBetweenFuturisticNeonAndMinimalistDark() {
        themeViewModel.setChatThemeMode(ChatThemeMode.FUTURISTIC_NEON)
        assertEquals(ChatThemeMode.FUTURISTIC_NEON, themeViewModel.chatThemeMode.value)
        assertTrue(themeViewModel.chatThemeMode.value.isNeonGlowEnabled)
        assertEquals("NEON", themeViewModel.chatThemeMode.value.badgeText)

        // Toggle to Minimalist Dark
        val next = themeViewModel.toggleChatTheme()
        assertEquals(ChatThemeMode.MINIMALIST_DARK, next)
        assertEquals(ChatThemeMode.MINIMALIST_DARK, themeViewModel.chatThemeMode.value)
        assertFalse(themeViewModel.chatThemeMode.value.isNeonGlowEnabled)
        assertEquals("MINIMAL", themeViewModel.chatThemeMode.value.badgeText)

        // Toggle back to Futuristic Neon
        val cycled = themeViewModel.toggleChatTheme()
        assertEquals(ChatThemeMode.FUTURISTIC_NEON, cycled)
        assertEquals(ChatThemeMode.FUTURISTIC_NEON, themeViewModel.chatThemeMode.value)
    }

    @Test
    fun testChatThemePalette_mapping() {
        val neonPalette = ChatThemeMode.FUTURISTIC_NEON.toPalette()
        assertEquals(ChatThemeMode.FUTURISTIC_NEON, neonPalette.mode)
        assertTrue(neonPalette.isNeonGlow)
        assertEquals(ZamaElectricCyan, neonPalette.primaryAccent)

        val darkPalette = ChatThemeMode.MINIMALIST_DARK.toPalette()
        assertEquals(ChatThemeMode.MINIMALIST_DARK, darkPalette.mode)
        assertFalse(darkPalette.isNeonGlow)
    }

    @Test
    fun testGlobalThemeMode_toggle() {
        themeViewModel.setThemeMode(ZamaThemeMode.FUTURISTIC_DARK)
        assertTrue(themeViewModel.themeMode.value.isDark)

        themeViewModel.toggleTheme()
        assertEquals(ZamaThemeMode.HIGH_CONTRAST_LIGHT, themeViewModel.themeMode.value)
        assertFalse(themeViewModel.themeMode.value.isDark)
    }
}
