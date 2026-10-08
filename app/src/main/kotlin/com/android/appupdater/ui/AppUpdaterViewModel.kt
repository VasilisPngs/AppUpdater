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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

sealed interface InstallEvent {
    data class Finished(val appName: String) : InstallEvent
    data class FinishedFromPlay(val appName: String, val versionName: String) : InstallEvent
    data class Failed(val message: String) : InstallEvent
}

data class AppUpdaterUiState(
    val scanStatus: ScanStatus = ScanStatus.Scanning,
    val loaded: Boolean = false,
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
    private val _events = MutableSharedFlow<InstallEvent>(extraBufferCapacity = 16)
    private val installJobs = ConcurrentHashMap<String, Job>()
    private val packageChanges = Channel<Unit>(Channel.CONFLATED)
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.data?.schemeSpecificPart?.let(AppIconCache::evict)
            packageChanges.trySend(Unit)
        }
    }
    private var scanJob: Job? = null

    val uiState: StateFlow<AppUpdaterUiState> = _uiState.asStateFlow()
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    val events: SharedFlow<InstallEvent> = _events.asSharedFlow()

    init {
        application.registerReceiver(
            packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme(PACKAGE_SCHEME)
            },
            Context.RECEIVER_NOT_EXPORTED
        )
        viewModelScope.launch {
            packageChanges.receiveAsFlow().collect { refreshInstalledApps() }
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

    fun scanForUpdates() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(scanStatus = ScanStatus.Scanning) }
            val apps = refreshInstalledApps()
            val includeDisabledApps = _uiState.value.includeDisabledApps
            val appsToCheck = apps.filter { includeDisabledApps || it.isEnabled }

            repository.scanForUpdates(appsToCheck).collect { status ->
                _uiState.update { current ->
                    when (status) {
                        ScanStatus.Scanning -> current.copy(scanStatus = status)
                        is ScanStatus.Success ->
                            current.copy(scanStatus = status, loaded = true, updates = status.updates)
                        is ScanStatus.Error ->
                            current.copy(scanStatus = status, loaded = true, updates = status.partialUpdates)
                    }
                }
            }
        }
    }

    fun installBundle(uri: Uri) {
        val key = uri.toString()
        if (installJobs.containsKey(key)) return

        installJobs[key] = viewModelScope.launch {
            bundleInstaller.install(uri) { state ->
                when (state) {
                    is InstallState.Success -> _events.tryEmit(InstallEvent.Finished(state.appName))
                    is InstallState.Error -> _events.tryEmit(InstallEvent.Failed(state.message))
                    else -> Unit
                }
                _uiState.update { it.copy(installs = it.installs + (key to state)) }
            }

            installJobs.remove(key)
            _uiState.update { it.copy(installs = it.installs - key) }
        }
    }

    fun updateFromPlay(update: AppUpdateInfo) {
        val target = update.playUpdate ?: return
        installFromPlay(update, manual = false) { onProgress ->
            val result = playInstaller.install(update.packageName, target.code, onProgress)
            if (target.code == update.newVersionCode) {
                result.toEvent(update.appName)
            } else {
                result.fold(
                    onSuccess = { InstallEvent.FinishedFromPlay(update.appName, target.name) },
                    onFailure = { InstallEvent.Failed(it.message ?: INSTALLATION_FAILED) }
                )
            }
        }
    }

    fun installManually(update: AppUpdateInfo, versionCode: Long) {
        installFromPlay(update, manual = true) { onProgress ->
            playInstaller.install(update.packageName, versionCode, onProgress).toEvent(update.appName)
        }
    }

    private fun installFromPlay(
        update: AppUpdateInfo,
        manual: Boolean,
        install: suspend (onProgress: (Float?) -> Unit) -> InstallEvent
    ) {
        val packageName = update.packageName
        if (installJobs.containsKey(packageName)) return

        _uiState.update { it.copy(playInstalls = it.playInstalls + (packageName to PlayInstall(manual, null))) }
        installJobs[packageName] = viewModelScope.launch {
            val event = install { progress ->
                _uiState.update { it.copy(playInstalls = it.playInstalls + (packageName to PlayInstall(manual, progress))) }
            }

            installJobs.remove(packageName)
            _uiState.update { it.copy(playInstalls = it.playInstalls - packageName) }
            _events.tryEmit(event)
        }
    }

    private fun Result<Unit>.toEvent(appName: String): InstallEvent = fold(
        onSuccess = { InstallEvent.Finished(appName) },
        onFailure = { InstallEvent.Failed(it.message ?: INSTALLATION_FAILED) }
    )

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
        val versions = apps.associate { it.packageName to it.versionCode }
        _uiState.update { state ->
            state.copy(
                installedApps = apps,
                updates = state.updates.filter {
                    it.newVersionCode > (versions[it.packageName] ?: 0L)
                }
            )
        }
        return apps
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(packageReceiver)
        scanJob?.cancel()
        installJobs.values.forEach(Job::cancel)
    }
}

private const val PACKAGE_SCHEME = "package"
