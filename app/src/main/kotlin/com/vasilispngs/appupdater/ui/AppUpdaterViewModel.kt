package com.vasilispngs.appupdater.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vasilispngs.appupdater.AppUpdaterApplication
import com.vasilispngs.appupdater.data.installer.InstallRepository
import com.vasilispngs.appupdater.data.installer.SelfUpdater
import com.vasilispngs.appupdater.data.model.AppUpdateInfo
import com.vasilispngs.appupdater.data.model.InstallEvent
import com.vasilispngs.appupdater.data.model.InstallState
import com.vasilispngs.appupdater.data.model.InstalledApp
import com.vasilispngs.appupdater.data.model.PlayInstall
import com.vasilispngs.appupdater.data.model.SelfUpdate
import com.vasilispngs.appupdater.data.model.ThemeMode
import com.vasilispngs.appupdater.data.preferences.AppPreferences
import com.vasilispngs.appupdater.data.repository.AppUpdateRepository
import com.vasilispngs.appupdater.data.repository.ScanStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val STOP_TIMEOUT_MILLIS = 5_000L

data class UserMessage(val id: Long, val event: InstallEvent)

data class AppUpdaterUiState(
    val scanning: Boolean = true,
    val loaded: Boolean = false,
    val notice: String? = null,
    val installedApps: List<InstalledApp> = emptyList(),
    val updates: List<AppUpdateInfo> = emptyList(),
    val includeDisabledApps: Boolean = false,
    val installs: Map<String, InstallState> = emptyMap(),
    val playInstalls: Map<String, PlayInstall> = emptyMap(),
    val messages: List<UserMessage> = emptyList(),
    val confirmation: Intent? = null
)

class AppUpdaterViewModel(
    private val repository: AppUpdateRepository,
    private val installs: InstallRepository,
    private val preferences: AppPreferences,
    private val selfUpdater: SelfUpdater
) : ViewModel() {
    private val state = MutableStateFlow(AppUpdaterUiState())
    private val installedAppsLock = Mutex()
    private var started = false
    private var nextMessageId = 0L
    private var scanJob: Job? = null

    val uiState: StateFlow<AppUpdaterUiState> =
        combine(state, installs.bundles, installs.play, installs.confirmation) { state, bundles, play, confirmation ->
            state.copy(installs = bundles, playInstalls = play, confirmation = confirmation)
        }
            .onStart { start() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppUpdaterUiState())

    val selfUpdate: StateFlow<SelfUpdate> = selfUpdater.status

    val themeMode: StateFlow<ThemeMode?> = preferences.themeMode
        .onStart { preferences.restoreThemeMode() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private fun start() {
        if (started) return
        started = true
        selfUpdater.check()
        viewModelScope.launch {
            repository.packageChanges.collect { packageName ->
                AppIconCache.evict(packageName)
                installedAppsLock.withLock {
                    val app = repository.getInstalledApp(packageName)
                    state.update { current ->
                        val apps = current.installedApps.filterNot { it.packageName == packageName } + listOfNotNull(app)
                        current.copy(installedApps = apps, updates = current.updates.pending(apps))
                    }
                }
            }
        }
        viewModelScope.launch {
            installs.results.collect { event ->
                val message = UserMessage(nextMessageId++, event)
                state.update { it.copy(messages = it.messages + message) }
            }
        }
        viewModelScope.launch {
            preferences.includeDisabledApps.collect { include ->
                state.update { it.copy(includeDisabledApps = include) }
                scanForUpdates()
            }
        }
    }

    fun refresh() {
        if (!state.value.scanning) scanForUpdates()
    }

    private fun scanForUpdates() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            state.update { it.copy(scanning = true) }
            val apps = refreshInstalledApps()
            val includeDisabledApps = state.value.includeDisabledApps
            val appsToCheck = apps.filter { includeDisabledApps || it.isEnabled }

            val status = repository.scanForUpdates(appsToCheck)
            state.update { current ->
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

    fun installBundle(uri: Uri) = installs.installBundle(uri)

    fun openBundle(uri: Uri) = installs.installBundle(uri) { packageName, versionCode ->
        state.value.updates.offersFromApkMirror(packageName, versionCode)
    }

    fun updateFromPlay(update: AppUpdateInfo) = installs.updateFromPlay(update)

    fun setIncludeDisabledApps(include: Boolean) {
        if (state.value.includeDisabledApps == include) return
        viewModelScope.launch { preferences.setIncludeDisabledApps(include) }
    }

    fun updateApp() = selfUpdater.update()

    fun setThemeMode(mode: ThemeMode) {
        if (themeMode.value == mode) return
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun messageShown(id: Long) {
        state.update { current -> current.copy(messages = current.messages.filterNot { it.id == id }) }
    }

    fun confirmationShown(intent: Intent) = installs.confirmationShown(intent)

    private suspend fun refreshInstalledApps(): List<InstalledApp> = installedAppsLock.withLock {
        val apps = repository.getInstalledApps()
        state.update { current -> current.copy(installedApps = apps, updates = current.updates.pending(apps)) }
        apps
    }

    private fun List<AppUpdateInfo>.pending(apps: List<InstalledApp>): List<AppUpdateInfo> {
        val versions = apps.associate { it.packageName to it.versionCode }
        return filter { update -> versions[update.packageName]?.let { update.newVersionCode > it } == true }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as AppUpdaterApplication).container
                AppUpdaterViewModel(container.updates, container.installs, container.preferences, container.selfUpdater)
            }
        }
    }
}

internal fun List<AppUpdateInfo>.offersFromApkMirror(packageName: String, versionCode: Long): Boolean =
    any { it.packageName == packageName && it.apkMirrorUrl != null && it.newVersionCode == versionCode }
