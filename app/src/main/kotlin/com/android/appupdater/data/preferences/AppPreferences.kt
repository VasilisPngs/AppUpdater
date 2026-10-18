package com.android.appupdater.data.preferences

import android.app.UiModeManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.appupdater.data.model.ThemeMode
import kotlinx.coroutines.flow.first

private const val NAME = "app_preferences"

private val Context.appPreferences: DataStore<Preferences> by preferencesDataStore(
    name = NAME,
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, NAME)) }
)

class AppPreferences(
    private val context: Context,
    private val store: DataStore<Preferences> = context.appPreferences
) {
    suspend fun includeDisabledApps(): Boolean = store.data.first()[INCLUDE_DISABLED_APPS] ?: false

    suspend fun setIncludeDisabledApps(value: Boolean) {
        store.edit { it[INCLUDE_DISABLED_APPS] = value }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
        applyNightMode(mode)
    }

    suspend fun restoreThemeMode(): ThemeMode = themeMode().also(::applyNightMode)

    private suspend fun themeMode(): ThemeMode {
        val stored = store.data.first()[THEME_MODE]
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.System
    }

    private fun applyNightMode(mode: ThemeMode) {
        context.getSystemService(UiModeManager::class.java).setApplicationNightMode(mode.nightMode)
    }

    private companion object {
        val INCLUDE_DISABLED_APPS = booleanPreferencesKey("include_disabled_apps")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
