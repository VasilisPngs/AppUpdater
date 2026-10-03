package com.android.appupdater.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.appupdater.data.installer.BundleInstaller
import com.android.appupdater.data.installer.PlayInstaller
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.InstallState
import com.android.appupdater.data.model.InstalledApp
import com.android.appupdater.data.model.PlayInstall
import com.android.appupdater.data.play.PlayAuthProvider
import com.android.appupdater.data.play.PlayCatalog
import com.android.appupdater.data.play.PlayHttpClient
import com.android.appupdater.data.preferences.AppPreferences
import com.android.appupdater.data.repository.AppUpdateRepository
import com.android.appupdater.data.repository.ScanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

sealed interface InstallEvent {
    data class Finished(val appName: String) : InstallEvent
    data class Failed(val message: String) : InstallEvent
}

data class AppUpdaterUiState(
    val scanStatus: ScanStatus = ScanStatus.Scanning,
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
    private val _events = MutableSharedFlow<InstallEvent>(extraBufferCapacity = 16)
    private val installJobs = ConcurrentHashMap<String, Job>()
    private var scanJob: Job? = null

    val uiState: StateFlow<AppUpdaterUiState> = _uiState.asStateFlow()
    val events: SharedFlow<InstallEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val includeDisabledApps = withContext(Dispatchers.IO) { preferences.includeDisabledApps }
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
                            current.copy(scanStatus = status, updates = status.updates)
                        is ScanStatus.Error ->
                            current.copy(scanStatus = status, updates = status.partialUpdates)
                    }
                }
            }
        }
    }

    fun installBundle(uri: Uri) {
        val key = uri.toString()
        if (installJobs.containsKey(key)) return

        installJobs[key] = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                bundleInstaller.install(uri) { state ->
                    when (state) {
                        is InstallState.Success -> _events.tryEmit(InstallEvent.Finished(state.appName))
                        is InstallState.Error -> _events.tryEmit(InstallEvent.Failed(state.message))
                        else -> Unit
                    }
                    _uiState.update { it.copy(installs = it.installs + (key to state)) }
                }
            }

            installJobs.remove(key)
            _uiState.update { it.copy(installs = it.installs - key) }
            if (result.isSuccess) refreshInstalledApps()
        }
    }

    fun installFromPlay(update: AppUpdateInfo, versionCode: Long, manual: Boolean) {
        val packageName = update.packageName
        if (installJobs.containsKey(packageName)) return

        _uiState.update { it.copy(playInstalls = it.playInstalls + (packageName to PlayInstall(manual, null))) }
        installJobs[packageName] = viewModelScope.launch {
            val result = playInstaller.install(packageName, versionCode, manual) { progress ->
                _uiState.update { it.copy(playInstalls = it.playInstalls + (packageName to PlayInstall(manual, progress))) }
            }

            installJobs.remove(packageName)
            _uiState.update { it.copy(playInstalls = it.playInstalls - packageName) }
            result
                .onSuccess {
                    _events.tryEmit(InstallEvent.Finished(update.appName))
                    refreshInstalledApps()
                }
                .onFailure { _events.tryEmit(InstallEvent.Failed(it.message ?: "Installation failed")) }
        }
    }

    fun setIncludeDisabledApps(include: Boolean) {
        if (_uiState.value.includeDisabledApps == include) return
        _uiState.update { it.copy(includeDisabledApps = include) }
        viewModelScope.launch(Dispatchers.IO) { preferences.includeDisabledApps = include }
        scanForUpdates()
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
        scanJob?.cancel()
        installJobs.values.forEach(Job::cancel)
    }
}
