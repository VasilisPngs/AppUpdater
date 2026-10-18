package com.android.appupdater.data.installer

import android.content.Intent
import android.net.Uri
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.InstallEvent
import com.android.appupdater.data.model.InstallState
import com.android.appupdater.data.model.PlayInstall
import com.android.appupdater.data.model.installEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class InstallRepository(
    private val scope: CoroutineScope,
    private val bundleInstaller: BundleInstaller,
    private val playInstaller: PlayInstaller,
    private val packageInstaller: PackageInstallerManager
) {
    private val pendingBundles = ConcurrentHashMap.newKeySet<String>()
    private val bundleStates = MutableStateFlow<Map<String, InstallState>>(emptyMap())
    private val playStates = MutableStateFlow<Map<String, PlayInstall>>(emptyMap())
    private val finished = MutableSharedFlow<InstallEvent>(extraBufferCapacity = RESULT_BUFFER)

    val bundles: StateFlow<Map<String, InstallState>> = bundleStates.asStateFlow()
    val play: StateFlow<Map<String, PlayInstall>> = playStates.asStateFlow()
    val results: SharedFlow<InstallEvent> = finished.asSharedFlow()
    val confirmation: StateFlow<Intent?> = packageInstaller.confirmation

    fun installBundle(uri: Uri) {
        val key = uri.toString()
        if (!pendingBundles.add(key)) return

        scope.launch {
            bundleInstaller.install(uri) { state ->
                state.installEvent()?.let(finished::tryEmit)
                bundleStates.update { it + (key to state) }
            }

            pendingBundles.remove(key)
            bundleStates.update { it - key }
        }
    }

    fun updateFromPlay(update: AppUpdateInfo) {
        val target = update.playUpdate ?: return
        val packageName = update.packageName
        if (packageName in playStates.value) return

        playStates.update { it + (packageName to PlayInstall(null)) }
        scope.launch {
            val result = playInstaller.install(packageName, target.code) { progress ->
                playStates.update { it + (packageName to PlayInstall(progress)) }
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

            playStates.update { it - packageName }
            finished.tryEmit(event)
        }
    }

    fun confirmationShown(intent: Intent) = packageInstaller.confirmationShown(intent)

    private companion object {
        const val RESULT_BUFFER = 64
    }
}
