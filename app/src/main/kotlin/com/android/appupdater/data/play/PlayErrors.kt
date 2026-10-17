package com.android.appupdater.data.play

import com.aurora.gplayapi.exceptions.GooglePlayException

private const val HTTP_STATUS_MIN = 100

internal val GooglePlayException.isUnavailable: Boolean
    get() = when (this) {
        is GooglePlayException.AppNotPurchased,
        is GooglePlayException.AppNotSupported,
        is GooglePlayException.AppRemoved,
        is GooglePlayException.NotFound,
        is GooglePlayException.EmptyDownloads -> true
        is GooglePlayException.Unknown -> code < HTTP_STATUS_MIN
        else -> false
    }

internal fun GooglePlayException.deliveryFailure(packageName: String, versionCode: Long): IllegalStateException =
    IllegalStateException(
        if (isUnavailable) "Google Play does not offer $packageName $versionCode." else "Google Play: $message",
        this
    )
