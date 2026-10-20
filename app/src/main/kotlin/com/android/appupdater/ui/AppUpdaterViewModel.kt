package com.android.appupdater.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.appupdater.AppUpdaterApplication
import com.android.appupdater.data.installer.InstallRepository
import com.android.appupdater.data.installer.SelfUpdater
import com.android.appupdater.data.model.Appearance
import com.android.appupdater.data.model.AppUpdate
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.InstallEvent
import com.android.appupdater.data.model.InstallState
import com.android.appupdater.data.model.InstalledApp
import com.android.appupdater.data.model.PlayInstall
import com.android.appupdater.data.model.ThemeMode
import com.android.appupdater.data.preferences.AppPreferences
import com.android.appupdater.data.repository.AppUpdateRepository
import com.android.appupdater.data.repository.ScanStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    val appUpdate: StateFlow<AppUpdate> = combine(preferences.automaticUpdates, selfUpdater.status, ::AppUpdate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppUpdate())

    val appearance: StateFlow<Appearance?> = preferences.appearance
        .onStart { preferences.restoreThemeMode() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private fun start() {
        if (started) return
        started = true
        viewModelScope.launch { selfUpdater.check(preferences.automaticUpdates.first()) }
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

    fun updateFromPlay(update: AppUpdateInfo) = installs.updateFromPlay(update)

    fun setIncludeDisabledApps(include: Boolean) {
        if (state.value.includeDisabledApps == include) return
        viewModelScope.launch { preferences.setIncludeDisabledApps(include) }
    }

    fun setAutomaticUpdates(enabled: Boolean) {
        if (appUpdate.value.automatic == enabled) return
        viewModelScope.launch { preferences.setAutomaticUpdates(enabled) }
    }

    fun updateApp() = selfUpdater.update()

    fun setThemeMode(mode: ThemeMode) {
        if (appearance.value?.themeMode == mode) return
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        if (appearance.value?.dynamicColor == enabled) return
        viewModelScope.launch { preferences.setDynamicColor(enabled) }
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
