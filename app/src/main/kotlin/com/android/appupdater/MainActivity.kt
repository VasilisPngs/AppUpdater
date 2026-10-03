package com.android.appupdater

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.appupdater.ui.AppUpdaterScreen
import com.android.appupdater.ui.AppUpdaterViewModel
import com.android.appupdater.ui.theme.AppUpdaterTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppUpdaterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        if (savedInstanceState == null) installFromIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            AppUpdaterTheme(themeMode = themeMode) {
                AppUpdaterScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        installFromIntent(intent)
    }

    private fun installFromIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        intent.data?.let(viewModel::installBundle)
    }
}
