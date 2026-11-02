package com.vasilispngs.appupdater.data.model

sealed interface Failure {
    data object Blocked : Failure
    data object Conflict : Failure
    data object Incompatible : Failure
    data object Invalid : Failure
    data object Storage : Failure
    data object NotInstalled : Failure
    data object BadFile : Failure
    data class MissingLibrary(val library: String) : Failure
    data object NotOffered : Failure
    data object PlayUnavailable : Failure
    data object PlayUnreachable : Failure
    data object Unverified : Failure
}

sealed interface ErrorCode {
    data class Http(val status: Int) : ErrorCode
    data class Play(val status: Int) : ErrorCode
    data class Named(val name: String) : ErrorCode
}

class FailureException(val failure: Failure, cause: Throwable? = null, val code: ErrorCode? = null) : Exception(cause)
