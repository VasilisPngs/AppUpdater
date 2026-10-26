package com.vasilispngs.appupdater.ui

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.vasilispngs.appupdater.R
import com.vasilispngs.appupdater.data.model.AppUpdateInfo
import com.vasilispngs.appupdater.data.model.Appearance
import com.vasilispngs.appupdater.data.model.InstalledApp
import com.vasilispngs.appupdater.data.model.PlayInstall
import com.vasilispngs.appupdater.data.model.PlayVersion
import com.vasilispngs.appupdater.data.model.SelfUpdate
import com.vasilispngs.appupdater.data.model.ThemeMode
import com.vasilispngs.appupdater.ui.theme.AppUpdaterTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccessibilityTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var dark by mutableStateOf(false)

    @Before
    fun enableAccessibilityChecks() {
        compose.enableAccessibilityChecks()
    }

    @Test
    fun updates() = check(loaded(entry("Chrome"), entry("Firefox", play = true)))

    @Test
    fun googlePlayDownload() {
        val firefox = entry("Firefox", play = true)
        check(loaded(firefox, playInstalls = mapOf(firefox.first.packageName to PlayInstall(DOWNLOADED))))
    }

    @Test
    fun firstCheck() = check(AppUpdaterUiState(scanning = true, loaded = false))

    @Test
    fun upToDate() = check(loaded())

    @Test
    fun failedCheck() = check(loaded(notice = NOTICE))

    @Test
    fun settings() {
        show(loaded(), SelfUpdate.Available)

        compose.onNodeWithText(string(R.string.settings)).performClick()
        checkLightAndDark()
        compose.onNodeWithText(string(R.string.theme)).performClick()
        checkLightAndDark()
    }

    @Test
    fun appUpdateDownload() {
        show(loaded(), SelfUpdate.Downloading(DOWNLOADED))

        compose.onNodeWithText(string(R.string.settings)).performClick()
        checkLightAndDark()
    }

    private fun check(state: AppUpdaterUiState) {
        show(state, SelfUpdate.Idle)
        checkLightAndDark()
    }

    private fun checkLightAndDark() {
        dark = false
        compose.onRoot().tryPerformAccessibilityChecks()
        dark = true
        compose.onRoot().tryPerformAccessibilityChecks()
    }

    private fun show(state: AppUpdaterUiState, selfUpdate: SelfUpdate) {
        compose.setContent {
            AppUpdaterTheme(darkTheme = dark) {
                AppUpdaterScreen(
                    uiState = state,
                    appearance = Appearance(ThemeMode.System, dynamicColor = false),
                    snackbarHostState = remember { SnackbarHostState() },
                    onRefresh = {},
                    onPickBundle = {},
                    onApkMirror = {},
                    onPlay = {},
                    onIncludeDisabledAppsChange = {},
                    onThemeChange = {},
                    onDynamicColorChange = {},
                    selfUpdate = selfUpdate,
                    onUpdateApp = {}
                )
            }
        }
    }

    private fun string(@StringRes id: Int): String = compose.activity.getString(id)

    private fun loaded(
        vararg entries: Pair<InstalledApp, AppUpdateInfo>,
        notice: String? = null,
        playInstalls: Map<String, PlayInstall> = emptyMap()
    ) = AppUpdaterUiState(
        scanning = false,
        loaded = true,
        notice = notice,
        installedApps = entries.map { it.first },
        updates = entries.map { it.second },
        playInstalls = playInstalls
    )

    private fun entry(name: String, play: Boolean = false): Pair<InstalledApp, AppUpdateInfo> {
        val packageName = "com.example.${name.lowercase()}"
        return InstalledApp(packageName, "1.0", INSTALLED_CODE, 26, emptySet(), emptySet(), true, false) to AppUpdateInfo(
            packageName = packageName,
            appName = name,
            newVersionName = "2.0",
            newVersionCode = LATEST_CODE,
            publishedAt = null,
            apkMirrorUrl = "https://www.apkmirror.com/apk/$packageName/",
            playAvailable = play,
            playUpdate = if (play) PlayVersion("2.0", LATEST_CODE) else null
        )
    }

    private companion object {
        const val INSTALLED_CODE = 100L
        const val LATEST_CODE = 200L
        const val DOWNLOADED = 0.42f
        const val NOTICE = "Network unavailable"
    }
}
