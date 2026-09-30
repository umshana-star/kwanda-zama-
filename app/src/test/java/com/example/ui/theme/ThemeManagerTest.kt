package com.example.ui.theme

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeManagerTest {

    private lateinit var app: Application
    private lateinit var viewModel: ThemeViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test
        app.getSharedPreferences("zama_theme_preferences", 0).edit().clear().commit()
        viewModel = ThemeViewModel(app)
    }

    @Test
    fun testDefaultThemeIsFuturisticDark() {
        val currentMode = viewModel.themeMode.value
        assertEquals(ZamaThemeMode.FUTURISTIC_DARK, currentMode)
        assertTrue(currentMode.isDark)
    }

    @Test
    fun testToggleThemeToHighContrastLight() {
        assertEquals(ZamaThemeMode.FUTURISTIC_DARK, viewModel.themeMode.value)

        viewModel.toggleTheme()
        val lightMode = viewModel.themeMode.value
        assertEquals(ZamaThemeMode.HIGH_CONTRAST_LIGHT, lightMode)
        assertFalse(lightMode.isDark)

        // Toggle back to dark
        viewModel.toggleTheme()
        val darkMode = viewModel.themeMode.value
        assertEquals(ZamaThemeMode.FUTURISTIC_DARK, darkMode)
        assertTrue(darkMode.isDark)
    }

    @Test
    fun testSetThemeModeExplicitly() {
        viewModel.setThemeMode(ZamaThemeMode.HIGH_CONTRAST_LIGHT)
        assertEquals(ZamaThemeMode.HIGH_CONTRAST_LIGHT, viewModel.themeMode.value)

        // Verify another instance reads the persisted preference
        val newVm = ThemeViewModel(app)
        assertEquals(ZamaThemeMode.HIGH_CONTRAST_LIGHT, newVm.themeMode.value)
    }

    @Test
    fun testPalettesIntegrity() {
        assertTrue(DarkPalette.isDark)
        assertFalse(LightPalette.isDark)

        assertNotNull(DarkPalette.background)
        assertNotNull(LightPalette.background)
        assertNotNull(DarkPalette.textPrimary)
        assertNotNull(LightPalette.textPrimary)

        // High contrast check: light mode uses bright background and dark primary text
        assertEquals(ZamaDaylightBackground, LightPalette.background)
        assertEquals(ZamaDaylightTextPrimary, LightPalette.textPrimary)
    }
}
