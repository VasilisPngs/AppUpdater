package com.vasilispngs.appupdater.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.vasilispngs.appupdater.R
import com.vasilispngs.appupdater.data.model.AppUpdateInfo
import com.vasilispngs.appupdater.data.model.InstalledApp
import com.vasilispngs.appupdater.data.model.PlayInstall
import com.vasilispngs.appupdater.data.model.PlayVersion
import com.vasilispngs.appupdater.data.model.SelfUpdate
import com.vasilispngs.appupdater.data.model.ThemeMode
import com.vasilispngs.appupdater.data.repository.ScanProblem
import com.vasilispngs.appupdater.ui.theme.AppUpdaterTvTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-television")
class TvAppUpdaterScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = RuntimeEnvironment.getApplication()
    private var state by mutableStateOf(AppUpdaterUiState())
    private val plays = mutableListOf<String>()
    private val apkMirrors = mutableListOf<String>()
    private var refreshes = 0
    private val disabledApps = mutableListOf<Boolean>()
    private val themes = mutableListOf<ThemeMode>()
    private lateinit var backDispatcher: OnBackPressedDispatcher

    @Test
    fun `focuses the first update and updates from each source with the remote`() {
        show(loaded(BOTH, PLAY_ONLY))
        compose.onNode(isFocused()).assert(hasText(BOTH.appName) and hasContentDescription(string(R.string.source_play)))
        pressKey(Key.DirectionCenter)
        pressKey(Key.DirectionDown)
        pressKey(Key.DirectionCenter)

        assertEquals(listOf(BOTH.packageName), plays)
        assertEquals(listOf(BOTH.apkMirrorUrl), apkMirrors)
    }

    @Test
    fun `shows the Google Play download on its row and keeps it from starting twice`() {
        show(loaded(BOTH).copy(playInstalls = mapOf(BOTH.packageName to PlayInstall(0.42f))))
        compose.onNodeWithText("42%").assertIsDisplayed()

        pressKey(Key.DirectionCenter)

        assertEquals(emptyList<String>(), plays)
    }

    @Test
    fun `keeps the focus on an update when the focused one is gone`() {
        show(loaded(BOTH, PLAY_ONLY))
        pressKey(Key.DirectionDown)
        pressKey(Key.DirectionDown)
        compose.onNode(isFocused()).assert(hasText(PLAY_ONLY.appName))

        state = loaded(BOTH)

        compose.onNode(isFocused()).assert(hasText(BOTH.appName))
    }

    @Test
    fun `says when every app is up to date`() {
        show(loaded())

        compose.onNodeWithText(string(R.string.all_up_to_date)).assertIsDisplayed()
    }

    @Test
    fun `offers to try again when the check failed`() {
        show(loaded().copy(problems = listOf(ScanProblem.ApkMirror(null))))
        compose.onNodeWithText(string(R.string.try_again)).assertIsFocused()

        pressKey(Key.DirectionCenter)

        assertEquals(1, refreshes)
    }

    @Test
    fun `turns disabled apps on and changes the theme in settings`() {
        show(loaded(BOTH))
        select(compose.onNodeWithContentDescription(string(R.string.settings)))
        compose.onNodeWithText(string(R.string.theme)).assertIsFocused()

        pressKey(Key.DirectionRight)
        compose.onNode(isFocused()).assert(hasText(string(R.string.theme_system)))
        pressKey(Key.DirectionDown)
        pressKey(Key.DirectionCenter)
        compose.runOnIdle { backDispatcher.onBackPressed() }
        compose.onNodeWithText(string(R.string.theme)).assertIsFocused()

        pressKey(Key.DirectionDown)
        compose.onNodeWithText(string(R.string.disabled_apps)).assertIsOff()
        pressKey(Key.DirectionCenter)

        assertEquals(listOf(ThemeMode.Light), themes)
        assertEquals(listOf(true), disabledApps)
    }

    private fun loaded(vararg updates: AppUpdateInfo) = AppUpdaterUiState(
        scanning = false,
        loaded = true,
        installedApps = updates.map { installed(it.packageName) },
        updates = updates.toList()
    )

    private fun installed(packageName: String) =
        InstalledApp(packageName, "1.0", 1, 34, emptySet(), emptySet(), emptySet(), isEnabled = true, isTelevisionBuild = true)

    private fun select(node: SemanticsNodeInteraction) {
        node.requestFocus()
        pressKey(Key.DirectionCenter)
    }

    private fun pressKey(key: Key) {
        compose.onNode(isFocused()).performKeyInput { pressKey(key) }
    }

    private fun show(initial: AppUpdaterUiState) {
        state = initial
        compose.setContent {
            val inputModeManager = LocalInputModeManager.current
            LaunchedEffect(Unit) { inputModeManager.requestInputMode(InputMode.Keyboard) }
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            AppUpdaterTvTheme {
                TvAppUpdaterScreen(
                    uiState = state,
                    themeMode = ThemeMode.System,
                    onRefresh = { refreshes++ },
                    onPickBundle = {},
                    onApkMirror = { apkMirrors += it },
                    onPlay = { plays += it.packageName },
                    onIncludeDisabledAppsChange = { disabledApps += it },
                    onThemeChange = { themes += it },
                    selfUpdate = SelfUpdate.Idle,
                    onUpdateApp = {}
                )
            }
        }
    }

    private fun string(@StringRes id: Int): String = context.getString(id)

    private companion object {
        val BOTH = AppUpdateInfo(
            packageName = "com.netflix.ninja",
            appName = "Netflix",
            newVersionName = "9.2.0",
            newVersionCode = 92,
            publishedAt = null,
            apkMirrorUrl = "https://www.apkmirror.com/apk/netflix-inc/netflix-android-tv/",
            playAvailable = true,
            playUpdate = PlayVersion("9.2.0", 92)
        )
        val PLAY_ONLY = AppUpdateInfo(
            packageName = "com.google.android.youtube.tv",
            appName = "YouTube",
            newVersionName = "4.41",
            newVersionCode = 441,
            publishedAt = null,
            apkMirrorUrl = null,
            playAvailable = true,
            playUpdate = PlayVersion("4.41", 441)
        )
    }
}
