package com.android.appupdater

import android.app.Application
import android.content.Context
import androidx.compose.material3.ComposeMaterial3Flags
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.appupdater.data.installer.BundleInstaller
import com.android.appupdater.data.installer.InstallRepository
import com.android.appupdater.data.installer.PackageInstallerManager
import com.android.appupdater.data.installer.PlayInstaller
import com.android.appupdater.data.installer.SelfUpdater
import com.android.appupdater.data.play.PlayAuthProvider
import com.android.appupdater.data.play.PlayCatalog
import com.android.appupdater.data.play.PlayHttpClient
import com.android.appupdater.data.preferences.AppPreferences
import com.android.appupdater.data.repository.AppUpdateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map

class AppUpdaterApplication : Application() {
    val container by lazy { AppContainer(this) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate() {
        super.onCreate()
        ComposeMaterial3Flags.isSnackbarStylingFixEnabled = true
    }
}

class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val playHttpClient = PlayHttpClient(context.packageName)
    private val playCatalog = PlayCatalog(PlayAuthProvider(context, playHttpClient))
    private val packageInstaller = PackageInstallerManager(context)

    val preferences = AppPreferences(context)
    val updates = AppUpdateRepository(context, playCatalog)
    val selfUpdater = SelfUpdater(
        context,
        scope,
        packageInstaller,
        ProcessLifecycleOwner.get().lifecycle.currentStateFlow.map { it.isAtLeast(Lifecycle.State.STARTED) }
    )
    val installs = InstallRepository(
        context,
        scope,
        BundleInstaller(context, packageInstaller),
        PlayInstaller(context, playCatalog, playHttpClient, packageInstaller),
        packageInstaller
    )
}
