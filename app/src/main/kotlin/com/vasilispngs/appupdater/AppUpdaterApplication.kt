package com.vasilispngs.appupdater

import android.app.Application
import android.content.Context
import androidx.compose.material3.ComposeMaterial3Flags
import androidx.compose.material3.ExperimentalMaterial3Api
import com.aurora.gplayapi.PlayClient
import com.aurora.gplayapi.android.NativeDeviceProperties
import com.aurora.gplayapi.device.DeviceRegistry
import com.aurora.gplayapi.engine.OkHttpEngine
import com.aurora.gplayapi.session.DefaultPlaySessions
import com.vasilispngs.appupdater.data.api.SharedHttpClient
import com.vasilispngs.appupdater.data.installer.BundleInstaller
import com.vasilispngs.appupdater.data.installer.InstallRepository
import com.vasilispngs.appupdater.data.installer.PackageInstallerManager
import com.vasilispngs.appupdater.data.installer.PlayInstaller
import com.vasilispngs.appupdater.data.installer.SelfUpdater
import com.vasilispngs.appupdater.data.play.PlayCatalog
import com.vasilispngs.appupdater.data.preferences.AppPreferences
import com.vasilispngs.appupdater.data.repository.AppUpdateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.Locale

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
    private val playCatalog = PlayCatalog(
        lazy {
            val engine = OkHttpEngine(SharedHttpClient.instance)
            val device = DeviceRegistry.parse(PLAY_DEVICE, NativeDeviceProperties.read(context))
            PlayClient(engine, DefaultPlaySessions(engine, device = { device }, locale = { Locale.getDefault() }))
        }
    )
    private val packageInstaller = PackageInstallerManager(context)

    val preferences = AppPreferences(context)
    val updates = AppUpdateRepository(context, playCatalog)
    val selfUpdater = SelfUpdater(context, scope, packageInstaller)
    val installs = InstallRepository(
        context,
        scope,
        BundleInstaller(context, packageInstaller),
        PlayInstaller(context, playCatalog, packageInstaller),
        packageInstaller
    )

    private companion object {
        const val PLAY_DEVICE = "native"
    }
}
