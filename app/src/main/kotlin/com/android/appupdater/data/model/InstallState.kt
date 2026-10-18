package com.android.appupdater.data.model

sealed interface InstallState {
    val appName: String

    data class Installing(override val appName: String) : InstallState
    data class Success(override val appName: String, val updated: Boolean) : InstallState
    data class Error(override val appName: String, val message: String) : InstallState
}

sealed interface InstallEvent {
    data class Installed(val appName: String) : InstallEvent
    data class InstallFailed(val message: String) : InstallEvent
    data class Finished(val appName: String) : InstallEvent
    data class FinishedFromPlay(val appName: String, val versionName: String) : InstallEvent
    data class Failed(val message: String) : InstallEvent
}

internal fun InstallState.installEvent(): InstallEvent? = when (this) {
    is InstallState.Installing -> null
    is InstallState.Success -> if (updated) InstallEvent.Finished(appName) else InstallEvent.Installed(appName)
    is InstallState.Error -> InstallEvent.InstallFailed(message)
}
