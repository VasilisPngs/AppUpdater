package com.vasilispngs.appupdater.data

import com.aurora.gplayapi.exceptions.GooglePlayException
import com.vasilispngs.appupdater.data.model.ErrorCode
import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.model.FailureException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class ErrorCodesTest {
    @Test
    fun `names the HTTP status of a failed request`() {
        assertEquals(ErrorCode.Http(403), HttpStatusException(403).errorCode())
    }

    @Test
    fun `tells a Google Play status from an HTTP status`() {
        assertEquals(ErrorCode.Play(7), GooglePlayException.AppRemoved(7).errorCode())
        assertEquals(ErrorCode.Http(429), GooglePlayException.Unknown(429, "HTTP 429").errorCode())
        assertEquals(ErrorCode.Http(401), GooglePlayException.AuthException(401, "Token expired").errorCode())
    }

    @Test
    fun `names a platform exception`() {
        assertEquals(ErrorCode.Named("UnknownHostException"), UnknownHostException().errorCode())
        assertEquals(ErrorCode.Named("IllegalStateException"), IllegalStateException().errorCode())
    }

    @Test
    fun `names the platform exception that a renamed library exception extends`() {
        assertEquals(ErrorCode.Named("IOException"), StreamReset().errorCode())
    }

    @Test
    fun `keeps the code a failure carries or takes its cause's`() {
        assertEquals(ErrorCode.Http(503), FailureException(Failure.SignInUnavailable, code = ErrorCode.Http(503)).errorCode())
        assertEquals(
            ErrorCode.Named("UnknownHostException"),
            FailureException(Failure.SignInUnreachable("auroraoss.com"), UnknownHostException()).errorCode()
        )
        assertNull(FailureException(Failure.Unverified).errorCode())
    }

    private class StreamReset : IOException()
}
