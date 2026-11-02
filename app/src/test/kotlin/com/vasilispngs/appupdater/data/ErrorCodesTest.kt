package com.vasilispngs.appupdater.data

import com.aurora.gplayapi.PlayException
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
        assertEquals(ErrorCode.Play(7), PlayException.AppRemoved(7).errorCode())
        assertEquals(ErrorCode.Play(3), PlayException.AppNotPurchased(3).errorCode())
        assertEquals(ErrorCode.Http(429), PlayException.RateLimited().errorCode())
        assertEquals(ErrorCode.Http(401), PlayException.Unauthorized(401).errorCode())
        assertEquals(ErrorCode.Http(503), PlayException.ServerError(503).errorCode())
    }

    @Test
    fun `gives no code to an answer Google Play did not number`() {
        assertNull(PlayException.NotFound().errorCode())
        assertNull(PlayException.NoFilesAvailable(-1).errorCode())
        assertNull(PlayException.RequiresAccount("downloads").errorCode())
    }

    @Test
    fun `names the platform exception behind a response Google Play could not read`() {
        assertEquals(
            ErrorCode.Named("IllegalArgumentException"),
            PlayException.Unknown(-1, "Malformed response", IllegalArgumentException()).errorCode()
        )
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
        assertEquals(ErrorCode.Http(503), FailureException(Failure.PlayUnavailable, code = ErrorCode.Http(503)).errorCode())
        assertEquals(
            ErrorCode.Named("UnknownHostException"),
            FailureException(Failure.PlayUnreachable, UnknownHostException()).errorCode()
        )
        assertNull(FailureException(Failure.Unverified).errorCode())
    }

    private class StreamReset : IOException()
}
