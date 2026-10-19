package com.android.appupdater.ui

import android.content.pm.PackageManager
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.android.appupdater.R
import com.android.appupdater.data.model.Appearance
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.InstallState
import com.android.appupdater.data.model.InstalledApp
import com.android.appupdater.data.model.PlayInstall
import com.android.appupdater.data.model.PlayVersion
import com.android.appupdater.data.model.ThemeMode
import com.android.appupdater.ui.theme.AppUpdaterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.NumberFormat

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppUpdaterScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = RuntimeEnvironment.getApplication()
    private var state by mutableStateOf(AppUpdaterUiState())
    private val played = mutableListOf<AppUpdateInfo>()
    private val opened = mutableListOf<String>()
    private val themes = mutableListOf<ThemeMode>()
    private val dynamicColors = mutableListOf<Boolean>()
    private val disabledApps = mutableListOf<Boolean>()
    private var refreshes = 0
    private var bundlePicks = 0
    private lateinit var backDispatcher: OnBackPressedDispatcher

    @Test
    fun `shows each update with its installed and latest versions`() {
        show(loaded(entry("Chrome", current = "141.0", latest = "142.0")))

        compose.onNodeWithText("Chrome").assertIsDisplayed()
        compose.onNodeWithText("Current: v141.0 ($INSTALLED_CODE)").assertIsDisplayed()
        compose.onNodeWithText("Latest: v142.0 ($LATEST_CODE)").assertIsDisplayed()
    }

    @Test
    fun `shows a version name that does not start with a number without a v prefix`() {
        show(loaded(entry("Poweramp", current = "build-984-bundle-play", latest = "build-985-bundle-play")))

        compose.onNodeWithText("Current: build-984-bundle-play ($INSTALLED_CODE)").assertIsDisplayed()
        compose.onNodeWithText("Latest: build-985-bundle-play ($LATEST_CODE)").assertIsDisplayed()
    }

    @Test
    fun `updates from Google Play with its button`() {
        val firefox = entry("Firefox", play = true, apkMirror = false)
        show(loaded(firefox))

        compose.onNodeWithContentDescription(string(R.string.source_play)).performClick()

        assertEquals(listOf(firefox.second), played)
    }

    @Test
    fun `opens the APKMirror release with its button`() {
        val chrome = entry("Chrome")
        show(loaded(chrome))

        compose.onNodeWithContentDescription(string(R.string.source_apkmirror)).performClick()

        assertEquals(listOf(chrome.second.apkMirrorUrl), opened)
    }

    @Test
    fun `shows a Google Play download and holds both buttons`() {
        val firefox = entry("Firefox", play = true)
        show(loaded(firefox, playInstalls = mapOf(firefox.first.packageName to PlayInstall(DOWNLOADED))))

        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(DOWNLOADED, 0f..1f))).assertExists()
        compose.onNodeWithText(NumberFormat.getPercentInstance().format(DOWNLOADED)).assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.source_apkmirror)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(string(R.string.source_play)).assertIsNotEnabled().performClick()

        assertTrue(played.isEmpty())
    }

    @Test
    fun `shows the loading indicator during the first check`() {
        show(AppUpdaterUiState(scanning = true, loaded = false))

        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun `tells when every app is up to date`() {
        show(loaded())

        compose.onNodeWithText(string(R.string.all_up_to_date)).assertIsDisplayed()
    }

    @Test
    fun `offers to try again when the check failed`() {
        show(loaded(notice = NOTICE))

        compose.onNodeWithText(string(R.string.check_failed)).assertIsDisplayed()
        compose.onNodeWithText(NOTICE).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.try_again)).performClick()

        assertEquals(1, refreshes)
    }

    @Test
    fun `refreshes and installs a bundle from the top app bar`() {
        show(loaded(entry("Chrome")))

        compose.onNodeWithContentDescription(string(R.string.refresh)).performClick()
        compose.onNodeWithContentDescription(string(R.string.install_bundle)).performClick()

        assertEquals(1, refreshes)
        assertEquals(1, bundlePicks)
    }

    @Test
    fun `counts the available updates on the navigation badge`() {
        show(loaded(entry("Chrome"), entry("Spotify")))

        compose.onNodeWithContentDescription(context.resources.getQuantityString(R.plurals.updates_available, 2, 2))
            .assertIsDisplayed()
    }

    @Test
    fun `returns from Settings to Updates with back`() {
        show(loaded(entry("Alpha")))

        compose.onNodeWithText(string(R.string.settings)).performClick()
        compose.onNodeWithText(string(R.string.theme)).assertIsDisplayed()
        compose.onAllNodesWithText("Alpha").assertCountEquals(0)

        compose.runOnIdle { backDispatcher.onBackPressed() }
        compose.onNodeWithText("Alpha").assertIsDisplayed()
        compose.onAllNodesWithText(string(R.string.theme)).assertCountEquals(0)
    }

    @Test
    fun `follows the back gesture from Settings with Google's full screen motion`() {
        show(loaded(entry("Alpha")))
        val updates = compose.onNodeWithText("Alpha").getBoundsInRoot()
        compose.onNodeWithText(string(R.string.settings)).performClick()
        val settings = compose.onNodeWithText(string(R.string.theme)).getBoundsInRoot()

        backGesture(0.1f)
        val exit = PredictiveBack.transform(0.1f)
        assertScaled(settings, 1f - 0.1f * exit, compose.onNodeWithText(string(R.string.theme)).getBoundsInRoot())
        assertScaled(updates, 1.1f - 0.1f * exit, compose.onNodeWithText("Alpha").getBoundsInRoot())

        compose.runOnIdle { backDispatcher.dispatchOnBackCancelled() }
        compose.onAllNodesWithText("Alpha").assertCountEquals(0)
        assertScaled(settings, 1f, compose.onNodeWithText(string(R.string.theme)).getBoundsInRoot())

        backGesture(0.6f)
        compose.runOnIdle { backDispatcher.onBackPressed() }
        assertScaled(updates, 1f, compose.onNodeWithText("Alpha").getBoundsInRoot())
        compose.onAllNodesWithText(string(R.string.theme)).assertCountEquals(0)
    }

    @Test
    fun `changes the theme from settings`() {
        show(loaded())

        compose.onNodeWithText(string(R.string.settings)).performClick()
        compose.onNodeWithText(string(R.string.theme_system)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.theme)).performClick()
        compose.onNodeWithText(string(R.string.theme_dark)).performClick()

        assertEquals(listOf(ThemeMode.Dark), themes)
    }

    @Test
    fun `turns dynamic color on from settings`() {
        show(loaded())

        compose.onNodeWithText(string(R.string.settings)).performClick()
        compose.onNodeWithText(string(R.string.dynamic_color)).assertIsOff().performClick()

        assertEquals(listOf(true), dynamicColors)
    }

    @Test
    fun `includes disabled apps from settings`() {
        show(loaded())

        compose.onNodeWithText(string(R.string.settings)).performClick()
        compose.onNodeWithText(string(R.string.disabled_apps)).performClick()

        assertEquals(listOf(true), disabledApps)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `stacks updates in one column on phones`() {
        show(loaded(entry("Alpha"), entry("Beta")))

        val alpha = bounds("Alpha")
        val beta = bounds("Beta")
        assertEquals(alpha.left, beta.left)
        assertEquals(alpha.right, beta.right)
        assertEquals(ListItemDefaults.SegmentedGap, beta.top - alpha.bottom)
        assertEquals(true to false, corners("Alpha"))
        assertEquals(false to true, corners("Beta"))
    }

    @Test
    @Config(qualifiers = "w1100dp-h800dp")
    fun `places updates side by side with their buttons on one line from the expanded breakpoint`() {
        show(loaded(entry("Alpha", current = LONG_VERSION), entry("Beta")))

        val alpha = bounds("Alpha")
        val beta = bounds("Beta")
        assertEquals(alpha.top, beta.top)
        assertEquals(alpha.bottom, beta.bottom)
        assertEquals(ListItemDefaults.SegmentedGap, beta.left - alpha.right)
        val latest = compose.onAllNodesWithText("Latest: v2.0 ($LATEST_CODE)", useUnmergedTree = true)
        assertTrue(latest[0].getBoundsInRoot().bottom > latest[1].getBoundsInRoot().bottom)
        val buttons = compose.onAllNodesWithContentDescription(string(R.string.source_apkmirror))
        assertEquals(buttons[0].getBoundsInRoot().bottom, buttons[1].getBoundsInRoot().bottom)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `keeps compact margins and the top app bar on the content edge on phones`() {
        assertContentEdge(16.dp)
    }

    @Test
    @Config(qualifiers = "w700dp-h1000dp")
    fun `widens the margins from the medium breakpoint with the top app bar on the content edge`() {
        assertContentEdge(24.dp)
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-television")
    fun `keeps the margins inside the television overscan`() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        assertContentEdge(24.dp + 48.dp)
    }

    @Test
    fun `shows a bundle install that starts while the updates are at the top`() {
        show(loaded(entry("Alpha"), entry("Beta")))

        state = state.copy(installs = mapOf(BUNDLE to InstallState.Installing(BUNDLE_NAME)))

        compose.onNodeWithText(BUNDLE_NAME).assertIsDisplayed()
    }

    @Test
    fun `moves focus to the next update when the focused one leaves`() {
        val alpha = entry("Alpha")
        val gamma = entry("Gamma")
        show(loaded(alpha, entry("Beta"), gamma), keyboard = true)
        compose.onAllNodesWithContentDescription(string(R.string.source_apkmirror))[1].requestFocus()

        state = loaded(alpha, gamma)

        compose.onAllNodesWithContentDescription(string(R.string.source_apkmirror))[1].assertIsFocused()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `measures the settings by Google's list and app bar tokens`() {
        show(loaded())
        compose.onNodeWithText(string(R.string.settings)).performClick()
        val heading = text(string(R.string.appearance))
        val themeItem = compose.onNodeWithText(string(R.string.theme)).getBoundsInRoot()
        val themeTitle = text(string(R.string.theme))
        val dynamicItem = compose.onNodeWithText(string(R.string.dynamic_color)).getBoundsInRoot()
        val title = text(string(R.string.settings))

        assertEquals(16.dp, title.left)
        assertEquals(themeTitle.left, heading.left)
        assertEquals(themeItem.left + 16.dp, themeTitle.left)
        assertEquals(72.dp, themeItem.bottom - themeItem.top)
        assertEquals(72.dp, dynamicItem.bottom - dynamicItem.top)
        assertEquals(themeItem.left, dynamicItem.left)
        assertEquals(themeItem.right, dynamicItem.right)
        assertEquals(ListItemDefaults.SegmentedGap, dynamicItem.top - themeItem.bottom)
        assertEquals(true to false, corners(string(R.string.theme)))
        assertEquals(false to true, corners(string(R.string.dynamic_color)))
        assertEquals(true to true, corners(string(R.string.disabled_apps)))

        compose.onNodeWithText(string(R.string.theme)).performClick()
        val light = compose.onNodeWithText(string(R.string.theme_light)).getBoundsInRoot()
        val dark = compose.onNodeWithText(string(R.string.theme_dark)).getBoundsInRoot()
        assertEquals(56.dp, light.bottom - light.top)
        assertEquals(light.bottom, dark.top)
    }

    private fun text(value: String): DpRect {
        val nodes = compose.onAllNodesWithText(value, useUnmergedTree = true)
        return List(nodes.fetchSemanticsNodes().size) { nodes[it].getBoundsInRoot() }.minBy { it.top }
    }

    private fun corners(text: String): Pair<Boolean, Boolean> {
        val image = compose.onNodeWithText(text).captureToImage().asAndroidBitmap()
        val container = image.getPixel(CORNER_INSET, image.height / 2)
        return (image.getPixel(CORNER_INSET, CORNER_INSET) != container) to
            (image.getPixel(CORNER_INSET, image.height - 1 - CORNER_INSET) != container)
    }

    private fun backGesture(progress: Float) {
        compose.runOnIdle {
            backDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT))
            backDispatcher.dispatchOnBackProgressed(BackEventCompat(0f, 0f, progress, BackEventCompat.EDGE_LEFT))
        }
        compose.waitForIdle()
    }

    private fun assertScaled(rest: DpRect, scale: Float, actual: DpRect) {
        assertEquals((rest.right - rest.left).value * scale, (actual.right - actual.left).value, 0.5f)
        assertEquals(((rest.left + rest.right) / 2).value, ((actual.left + actual.right) / 2).value, 0.5f)
    }

    private fun assertContentEdge(margin: Dp) {
        show(loaded(entry("Alpha"), entry("Beta")))

        val width = compose.onRoot().getBoundsInRoot().right
        val cards = listOf(bounds("Alpha"), bounds("Beta"))
        val refresh = compose.onNodeWithContentDescription(string(R.string.refresh), useUnmergedTree = true).getBoundsInRoot()
        val title = text(string(R.string.updates))
        assertEquals(width - margin, cards.maxOf { it.right })
        assertEquals(width - margin, refresh.right)
        assertEquals(cards.minOf { it.left }, title.left)
    }

    private fun show(initial: AppUpdaterUiState, keyboard: Boolean = false) {
        state = initial
        compose.setContent {
            if (keyboard) {
                val inputModeManager = LocalInputModeManager.current
                LaunchedEffect(Unit) { inputModeManager.requestInputMode(InputMode.Keyboard) }
            }
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            AppUpdaterTheme {
                AppUpdaterScreen(
                    uiState = state,
                    appearance = Appearance(ThemeMode.System, dynamicColor = false),
                    snackbarHostState = remember { SnackbarHostState() },
                    onRefresh = { refreshes++ },
                    onPickBundle = { bundlePicks++ },
                    onApkMirror = { opened += it },
                    onPlay = { played += it },
                    onIncludeDisabledAppsChange = { disabledApps += it },
                    onThemeChange = { themes += it },
                    onDynamicColorChange = { dynamicColors += it }
                )
            }
        }
    }

    private fun bounds(text: String): DpRect = compose.onNodeWithText(text).getBoundsInRoot()

    private fun string(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

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

    private fun entry(
        name: String,
        current: String = "1.0",
        latest: String = "2.0",
        play: Boolean = false,
        apkMirror: Boolean = true
    ): Pair<InstalledApp, AppUpdateInfo> {
        val packageName = "com.example.${name.lowercase()}"
        return InstalledApp(packageName, current, INSTALLED_CODE, 26, emptySet(), emptySet(), true, false) to AppUpdateInfo(
            packageName = packageName,
            appName = name,
            newVersionName = latest,
            newVersionCode = LATEST_CODE,
            publishedAt = null,
            apkMirrorUrl = if (apkMirror) "https://www.apkmirror.com/apk/$packageName/" else null,
            playAvailable = play,
            playUpdate = if (play) PlayVersion(latest, LATEST_CODE) else null
        )
    }

    private companion object {
        const val CORNER_INSET = 3
        const val INSTALLED_CODE = 100L
        const val LATEST_CODE = 200L
        const val DOWNLOADED = 0.42f
        const val NOTICE = "Network unavailable"
        const val BUNDLE = "content://downloads/bundle.apkm"
        const val BUNDLE_NAME = "Bundle.apkm"
        const val LONG_VERSION = "141.0.7390.122.4567.890.12345.67890.1234.5678.9012.3456.7890.1234.5678"
        val PredictiveBack = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
    }
}
