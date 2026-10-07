package com.android.appupdater.data.model

import android.app.UiModeManager

enum class ThemeMode(val nightMode: Int) {
    System(UiModeManager.MODE_NIGHT_AUTO),
    Light(UiModeManager.MODE_NIGHT_NO),
    Dark(UiModeManager.MODE_NIGHT_YES)
}
