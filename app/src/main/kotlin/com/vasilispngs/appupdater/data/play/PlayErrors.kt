package com.vasilispngs.appupdater.data.play

import com.aurora.gplayapi.exceptions.GooglePlayException
import com.vasilispngs.appupdater.data.errorCode
import com.vasilispngs.appupdater.data.model.ErrorCode
import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.model.FailureException
import java.io.IOException

internal val GooglePlayException.isUnavailable: Boolean
    get() = when (this) {
        is GooglePlayException.AppNotPurchased,
        is GooglePlayException.AppNotSupported,
        is GooglePlayException.AppRemoved,
        is GooglePlayException.NotFound,
        is GooglePlayException.EmptyDownloads -> true
        is GooglePlayException.Unknown -> errorCode() is ErrorCode.Play
        else -> false
    }

internal fun Throwable.playUpdateFailure(): Failure = when (this) {
    is FailureException -> failure
    is GooglePlayException -> if (isUnavailable) Failure.NotOffered else Failure.PlayUnavailable
    is IOException -> Failure.PlayUnreachable
    else -> Failure.NotInstalled
}

internal fun Throwable.playLookupFailure(): Failure = when (this) {
    is FailureException -> failure
    is IOException -> Failure.PlayUnreachable
    else -> Failure.PlayUnavailable
}
