package com.android.appupdater.data.preferences

import android.app.UiModeManager
import com.android.appupdater.data.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppPreferencesTest {
    private val context = RuntimeEnvironment.getApplication()
    private val uiModeManager = context.getSystemService(UiModeManager::class.java)

    @Test
    fun `applies the chosen theme`() {
        AppPreferences(context).themeMode = ThemeMode.Dark

        assertEquals(UiModeManager.MODE_NIGHT_YES, shadowOf(uiModeManager).applicationNightMode)
    }

    @Test
    fun `applies the stored theme again when the system no longer has it`() {
        AppPreferences(context).themeMode = ThemeMode.Light
        uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)

        assertEquals(ThemeMode.Light, AppPreferences(context).restoreThemeMode())
        assertEquals(UiModeManager.MODE_NIGHT_NO, shadowOf(uiModeManager).applicationNightMode)
    }
}
