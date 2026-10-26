package com.android.appupdater.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.android.appupdater.R
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.Appearance
import com.android.appupdater.data.model.InstalledApp
import com.android.appupdater.data.model.SelfUpdate
import com.android.appupdater.data.model.ThemeMode
import com.android.appupdater.ui.theme.AppUpdaterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReducedMotionTest {
    @get:Rule
    val compose = createComposeRule(ComposeUiTestConfig(effectContext = AnimationsRemoved))

    private val context = RuntimeEnvironment.getApplication()
    private lateinit var backDispatcher: OnBackPressedDispatcher

    @Test
    fun `switches screens at once when animations are removed`() {
        show()

        settlesAtOnce { compose.onNodeWithContentDescription(context.getString(R.string.settings)).performClick() }
        compose.onAllNodesWithText(APP).assertCountEquals(0)
        settlesAtOnce { compose.runOnIdle { backDispatcher.onBackPressed() } }
        compose.onAllNodesWithText(context.getString(R.string.theme)).assertCountEquals(0)
    }

    private fun settlesAtOnce(action: () -> Unit) {
        compose.waitForIdle()
        val start = compose.mainClock.currentTime
        action()
        compose.waitForIdle()
        assertTrue(compose.mainClock.currentTime - start <= SETTLE_MILLIS)
    }

    private fun show() {
        val packageName = "com.example.app"
        compose.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            AppUpdaterTheme {
                AppUpdaterScreen(
                    uiState = AppUpdaterUiState(
                        scanning = false,
                        loaded = true,
                        installedApps = listOf(InstalledApp(packageName, "1.0", 1L, 26, emptySet(), emptySet(), true, false)),
                        updates = listOf(AppUpdateInfo(packageName, APP, "2.0", 2L, null, "https://www.apkmirror.com/", false, null))
                    ),
                    appearance = Appearance(ThemeMode.System, dynamicColor = false),
                    snackbarHostState = remember { SnackbarHostState() },
                    onRefresh = {},
                    onPickBundle = {},
                    onApkMirror = {},
                    onPlay = {},
                    onIncludeDisabledAppsChange = {},
                    onThemeChange = {},
                    onDynamicColorChange = {},
                    selfUpdate = SelfUpdate.Idle,
                    onUpdateApp = {}
                )
            }
        }
    }

    private companion object {
        const val APP = "Alpha"
        const val SETTLE_MILLIS = 80L
        val AnimationsRemoved = object : MotionDurationScale {
            override val scaleFactor = 0f
        }
    }
}
