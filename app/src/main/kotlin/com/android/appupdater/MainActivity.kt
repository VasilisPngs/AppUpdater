package com.android.appupdater

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.appupdater.data.installer.InstallConfirmation
import com.android.appupdater.ui.AppUpdaterScreen
import com.android.appupdater.ui.AppUpdaterViewModel
import com.android.appupdater.ui.theme.AppUpdaterTheme
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AppUpdaterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        if (savedInstanceState == null) installFromIntent(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                InstallConfirmation.intent.filterNotNull().collect { confirmation ->
                    if (InstallConfirmation.consume(confirmation)) runCatching { startActivity(confirmation) }
                }
            }
        }
        setContent {
            AppUpdaterTheme {
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
