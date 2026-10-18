package com.android.appupdater.data.preferences

import android.app.UiModeManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.appupdater.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appPreferences: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(
    private val context: Context,
    private val store: DataStore<Preferences> = context.appPreferences
) {
    val includeDisabledApps: Flow<Boolean> = store.data.map { it[INCLUDE_DISABLED_APPS] ?: false }.distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = store.data
        .map { preferences -> ThemeMode.entries.firstOrNull { it.name == preferences[THEME_MODE] } ?: ThemeMode.System }
        .distinctUntilChanged()

    suspend fun setIncludeDisabledApps(value: Boolean) {
        store.edit { it[INCLUDE_DISABLED_APPS] = value }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
        applyNightMode(mode)
    }

    suspend fun restoreThemeMode(): ThemeMode = themeMode.first().also(::applyNightMode)

    private fun applyNightMode(mode: ThemeMode) {
        context.getSystemService(UiModeManager::class.java).setApplicationNightMode(mode.nightMode)
    }

    private companion object {
        val INCLUDE_DISABLED_APPS = booleanPreferencesKey("include_disabled_apps")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
