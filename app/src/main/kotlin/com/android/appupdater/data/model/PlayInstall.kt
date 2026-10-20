package com.android.appupdater.data.model

data class PlayInstall(val progress: Float?)

data class PlayUpdate(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val latest: Boolean
)
