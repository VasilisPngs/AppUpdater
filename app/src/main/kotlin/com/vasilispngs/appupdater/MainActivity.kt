package com.vasilispngs.appupdater

import android.content.ContentResolver
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vasilispngs.appupdater.ui.AppUpdaterScreen
import com.vasilispngs.appupdater.ui.AppUpdaterViewModel
import com.vasilispngs.appupdater.ui.theme.AppUpdaterTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppUpdaterViewModel by viewModels { AppUpdaterViewModel.Factory }
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        if (savedInstanceState == null) installFromIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            themeMode?.let { current ->
                AppUpdaterTheme {
                    AppUpdaterScreen(viewModel = viewModel, themeMode = current)
                }
                SideEffect { ready = true }
            }
        }
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                return ready
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val light = resources.getBoolean(R.bool.light_system_bars)
        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
        window.setBackgroundDrawableResource(R.color.window_background)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        installFromIntent(intent)
    }

    private fun installFromIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        intent.data?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT }?.let(viewModel::openBundle)
    }
}
