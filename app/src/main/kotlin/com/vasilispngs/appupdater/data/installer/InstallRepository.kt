package com.vasilispngs.appupdater.data.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vasilispngs.appupdater.data.model.AppUpdateInfo
import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.model.InstallEvent
import com.vasilispngs.appupdater.data.model.InstallState
import com.vasilispngs.appupdater.data.model.PlayInstall
import com.vasilispngs.appupdater.data.model.PlayUpdate
import com.vasilispngs.appupdater.data.model.installEvent
import com.vasilispngs.appupdater.data.play.playUpdateFailure
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
    private val context: Context,
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

    fun installBundle(uri: Uri, isSuggested: ((String, Long) -> Boolean)? = null) {
        val key = uri.toString()
        if (!pendingBundles.add(key)) return

        scope.launch {
            bundleInstaller.install(uri, isSuggested) { state ->
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
        val request = PlayUpdate(packageName, update.appName, target.name, target.code, target.code == update.newVersionCode)
        if (!PlayUpdateService.schedule(context, request)) {
            playStates.update { it - packageName }
            finished.tryEmit(InstallEvent.Failed(Failure.NotInstalled))
        }
    }

    suspend fun runPlayUpdate(update: PlayUpdate, onSize: (Long) -> Unit, onProgress: (Float?) -> Unit): InstallEvent? {
        val packageName = update.packageName
        playStates.update { it + (packageName to PlayInstall(null)) }
        val event = try {
            val approval = packageInstaller.preapprove(packageName)
            if (approval == Preapproval.Declined) {
                null
            } else {
                playInstaller.install(packageName, update.versionCode, (approval as? Preapproval.Approved)?.sessionId, onSize) { progress ->
                    playStates.update { it + (packageName to PlayInstall(progress)) }
                    onProgress(progress)
                }.fold(
                    onSuccess = {
                        if (update.latest) InstallEvent.Finished(update.appName) else InstallEvent.FinishedFromPlay(update.appName, update.versionName)
                    },
                    onFailure = { InstallEvent.Failed(it.playUpdateFailure()) }
                )
            }
        } finally {
            playStates.update { it - packageName }
        }
        event?.let(finished::tryEmit)
        return event
    }

    fun confirmationShown(intent: Intent) = packageInstaller.confirmationShown(intent)

    private companion object {
        const val RESULT_BUFFER = 64
    }
}
