package com.android.appupdater.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.android.appupdater.data.model.ThemeMode

class AppPreferences(private val context: Context) {
    private val preferences by lazy {
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    var includeDisabledApps: Boolean
        get() = preferences.getBoolean(KEY_INCLUDE_DISABLED_APPS, false)
        set(value) = preferences.edit { putBoolean(KEY_INCLUDE_DISABLED_APPS, value) }

    var themeMode: ThemeMode
        get() = preferences.getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.System
        set(value) = preferences.edit { putString(KEY_THEME_MODE, value.name) }

    companion object {
        private const val KEY_INCLUDE_DISABLED_APPS = "include_disabled_apps"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
