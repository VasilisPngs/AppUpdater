package com.vasilispngs.appupdater.data

import com.aurora.gplayapi.exceptions.GooglePlayException
import com.vasilispngs.appupdater.data.model.ErrorCode
import com.vasilispngs.appupdater.data.model.FailureException
import java.io.IOException

private const val HTTP_STATUS_MIN = 100

class HttpStatusException(val status: Int) : IOException("HTTP $status")

internal fun Throwable.errorCode(): ErrorCode? = when (this) {
    is FailureException -> code ?: cause?.errorCode()
    is HttpStatusException -> ErrorCode.Http(status)
    is GooglePlayException -> if (status < HTTP_STATUS_MIN) ErrorCode.Play(status) else ErrorCode.Http(status)
    else -> ErrorCode.Named(
        generateSequence<Class<*>>(javaClass) { it.superclass }
            .first { it.classLoader === Throwable::class.java.classLoader }
            .simpleName
    )
}

private val GooglePlayException.status: Int
    get() = when (this) {
        is GooglePlayException.AppNotPurchased -> code
        is GooglePlayException.AppNotSupported -> code
        is GooglePlayException.AppRemoved -> code
        is GooglePlayException.AuthException -> code
        is GooglePlayException.EmptyDownloads -> code
        is GooglePlayException.NotFound -> code
        is GooglePlayException.Server -> code
        is GooglePlayException.Unknown -> code
    }
