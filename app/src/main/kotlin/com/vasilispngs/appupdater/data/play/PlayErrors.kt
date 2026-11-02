package com.vasilispngs.appupdater.data.play

import com.aurora.gplayapi.PlayException
import com.vasilispngs.appupdater.data.errorCode
import com.vasilispngs.appupdater.data.model.ErrorCode
import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.model.FailureException
import java.io.IOException

internal val PlayException.isUnavailable: Boolean
    get() = when (this) {
        is PlayException.AppNotPurchased,
        is PlayException.AppNotSupported,
        is PlayException.AppRemoved,
        is PlayException.NotFound,
        is PlayException.NoFilesAvailable -> true
        is PlayException.Unknown -> errorCode() is ErrorCode.Play
        else -> false
    }

internal fun Throwable.playUpdateFailure(): Failure = when (this) {
    is FailureException -> failure
    is PlayException -> if (isUnavailable) Failure.NotOffered else Failure.PlayUnavailable
    is IOException -> Failure.PlayUnreachable
    else -> Failure.NotInstalled
}

internal fun Throwable.playLookupFailure(): Failure = when (this) {
    is FailureException -> failure
    is IOException -> Failure.PlayUnreachable
    else -> Failure.PlayUnavailable
}
