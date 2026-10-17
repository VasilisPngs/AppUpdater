package com.android.appupdater.ui

import android.app.Application
import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.appupdater.data.installer.BundleInstaller
import com.android.appupdater.data.installer.INSTALLATION_FAILED
import com.android.appupdater.data.installer.PlayInstaller
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.InstallState
import com.android.appupdater.data.model.InstalledApp
import com.android.appupdater.data.model.PlayInstall
import com.android.appupdater.data.model.ThemeMode
import com.android.appupdater.data.play.PlayAuthProvider
import com.android.appupdater.data.play.PlayCatalog
import com.android.appupdater.data.play.PlayHttpClient
import com.android.appupdater.data.preferences.AppPreferences
import com.android.appupdater.data.repository.AppUpdateRepository
import com.android.appupdater.data.repository.ScanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface InstallEvent {
    data class Finished(val appName: String) : InstallEvent
    data class FinishedFromPlay(val appName: String, val versionName: String) : InstallEvent
    data class Failed(val message: String) : InstallEvent
}

data class AppUpdaterUiState(
    val scanning: Boolean = true,
    val loaded: Boolean = false,
    val notice: String? = null,
    val installedApps: List<InstalledApp> = emptyList(),
    val updates: List<AppUpdateInfo> = emptyList(),
    val includeDisabledApps: Boolean = false,
    val installs: Map<String, InstallState> = emptyMap(),
    val playInstalls: Map<String, PlayInstall> = emptyMap()
)

class AppUpdaterViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application.applicationContext)
    private val playHttpClient = PlayHttpClient(application.packageName)
    private val playCatalog = PlayCatalog(PlayAuthProvider(application.applicationContext, playHttpClient))
    private val repository = AppUpdateRepository(application.applicationContext, playCatalog)
    private val bundleInstaller = BundleInstaller(application.applicationContext)
    private val playInstaller = PlayInstaller(application.applicationContext, playCatalog, playHttpClient)
    private val _uiState = MutableStateFlow(AppUpdaterUiState())
    private val _themeMode = MutableStateFlow(ThemeMode.System)
    private val _events = Channel<InstallEvent>(Channel.BUFFERED)
    private val pendingBundles = mutableSetOf<String>()
    private val packageChanges = Channel<String>(Channel.UNLIMITED)
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_PACKAGE_REPLACED && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            val packageName = intent.data?.schemeSpecificPart ?: return
            AppIconCache.evict(packageName)
            packageChanges.trySend(packageName)
        }
    }
    private var scanJob: Job? = null

    val uiState: StateFlow<AppUpdaterUiState> = _uiState.asStateFlow()
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    val events: Flow<InstallEvent> = _events.receiveAsFlow()

    init {
        application.registerReceiver(
            packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme(PACKAGE_SCHEME)
            }
        )
        viewModelScope.launch {
            packageChanges.receiveAsFlow().collect { packageName ->
                val app = repository.getInstalledApp(packageName)
                _uiState.update { state ->
                    val apps = state.installedApps.filterNot { it.packageName == packageName } + listOfNotNull(app)
                    state.copy(installedApps = apps, updates = state.updates.pending(apps))
                }
            }
        }
        viewModelScope.launch {
            val (includeDisabledApps, themeMode) = withContext(Dispatchers.IO) {
                preferences.includeDisabledApps to preferences.themeMode
            }
            _themeMode.value = themeMode
            _uiState.update { it.copy(includeDisabledApps = includeDisabledApps) }
            scanForUpdates()
        }
    }

    fun refresh() {
        if (!_uiState.value.scanning) scanForUpdates()
    }

    private fun scanForUpdates() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(scanning = true) }
            val apps = refreshInstalledApps()
            val includeDisabledApps = _uiState.value.includeDisabledApps
            val appsToCheck = apps.filter { includeDisabledApps || it.isEnabled }

            val status = repository.scanForUpdates(appsToCheck)
            _uiState.update { current ->
                when (status) {
                    is ScanStatus.Success -> current.copy(
                        scanning = false,
                        loaded = true,
                        notice = null,
                        updates = status.updates.pending(current.installedApps)
                    )
                    is ScanStatus.Error -> current.copy(
                        scanning = false,
                        loaded = true,
                        notice = status.message,
                        updates = status.partialUpdates.pending(current.installedApps)
                    )
                }
            }
        }
    }

    fun installBundle(uri: Uri) {
        val key = uri.toString()
        if (!pendingBundles.add(key)) return

        viewModelScope.launch {
            bundleInstaller.install(uri) { state ->
                when (state) {
                    is InstallState.Success -> _events.trySend(InstallEvent.Finished(state.appName))
                    is InstallState.Error -> _events.trySend(InstallEvent.Failed(state.message))
                    else -> Unit
                }
                _uiState.update { it.copy(installs = it.installs + (key to state)) }
            }

            pendingBundles.remove(key)
            _uiState.update { it.copy(installs = it.installs - key) }
        }
    }

    fun updateFromPlay(update: AppUpdateInfo) {
        val target = update.playUpdate ?: return
        val packageName = update.packageName
        if (packageName in _uiState.value.playInstalls) return

        _uiState.update { it.copy(playInstalls = it.playInstalls + (packageName to PlayInstall(null))) }
        viewModelScope.launch {
            val result = playInstaller.install(packageName, target.code) { progress ->
                _uiState.update { it.copy(playInstalls = it.playInstalls + (packageName to PlayInstall(progress))) }
            }
            val event = result.fold(
                onSuccess = {
                    if (target.code == update.newVersionCode) {
                        InstallEvent.Finished(update.appName)
                    } else {
                        InstallEvent.FinishedFromPlay(update.appName, target.name)
                    }
                },
                onFailure = { InstallEvent.Failed(it.message ?: INSTALLATION_FAILED) }
            )

            _uiState.update { it.copy(playInstalls = it.playInstalls - packageName) }
            _events.trySend(event)
        }
    }

    fun setIncludeDisabledApps(include: Boolean) {
        if (_uiState.value.includeDisabledApps == include) return
        _uiState.update { it.copy(includeDisabledApps = include) }
        viewModelScope.launch(Dispatchers.IO) { preferences.includeDisabledApps = include }
        scanForUpdates()
    }

    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value == mode) return
        _themeMode.value = mode
        viewModelScope.launch(Dispatchers.IO) { preferences.themeMode = mode }
        getApplication<Application>().getSystemService(UiModeManager::class.java).setApplicationNightMode(mode.nightMode)
    }

    private suspend fun refreshInstalledApps(): List<InstalledApp> {
        val apps = repository.getInstalledApps()
        _uiState.update { state -> state.copy(installedApps = apps, updates = state.updates.pending(apps)) }
        return apps
    }

    private fun List<AppUpdateInfo>.pending(apps: List<InstalledApp>): List<AppUpdateInfo> {
        val versions = apps.associate { it.packageName to it.versionCode }
        return filter { update -> versions[update.packageName]?.let { update.newVersionCode > it } == true }
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(packageReceiver)
    }
}

private const val PACKAGE_SCHEME = "package"
