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
    data class SignInUnreachable(val host: String) : Failure
    data object SignInRejected : Failure
    data object SignInRefused : Failure
    data object SignInLimited : Failure
    data object SignInUnavailable : Failure
    data object SignInFailed : Failure
}

class FailureException(val failure: Failure, cause: Throwable? = null) : Exception(cause)
