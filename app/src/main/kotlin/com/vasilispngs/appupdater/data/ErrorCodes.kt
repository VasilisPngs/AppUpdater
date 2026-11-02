package com.vasilispngs.appupdater.data

import com.aurora.gplayapi.PlayException
import com.vasilispngs.appupdater.data.model.ErrorCode
import com.vasilispngs.appupdater.data.model.FailureException
import java.io.IOException

private const val HTTP_STATUS_MIN = 100

class HttpStatusException(val status: Int) : IOException("HTTP $status")

internal fun Throwable.errorCode(): ErrorCode? = when (this) {
    is FailureException -> code ?: cause?.errorCode()
    is HttpStatusException -> ErrorCode.Http(status)
    is PlayException -> status?.let { if (it < HTTP_STATUS_MIN) ErrorCode.Play(it) else ErrorCode.Http(it) } ?: cause?.errorCode()
    else -> ErrorCode.Named(
        generateSequence<Class<*>>(javaClass) { it.superclass }
            .first { it.classLoader === Throwable::class.java.classLoader }
            .simpleName
    )
}

private val PlayException.status: Int?
    get() = when (this) {
        is PlayException.AppNotPurchased -> code
        is PlayException.AppNotSupported -> code
        is PlayException.AppRemoved -> code
        is PlayException.NoFilesAvailable -> code
        is PlayException.NotFound -> code
        is PlayException.RateLimited -> code
        is PlayException.ServerError -> code
        is PlayException.Unauthorized -> code
        is PlayException.Unknown -> code
        is PlayException.RequiresAccount -> null
    }?.takeIf { it > 0 }
