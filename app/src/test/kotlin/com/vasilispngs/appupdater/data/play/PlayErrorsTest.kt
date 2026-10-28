package com.vasilispngs.appupdater.data.play

import com.aurora.gplayapi.exceptions.GooglePlayException
import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.model.FailureException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class PlayErrorsTest {
    @Test
    fun `says Google Play does not offer a release it withholds`() {
        listOf(
            GooglePlayException.AppNotPurchased(403),
            GooglePlayException.AppNotSupported(2),
            GooglePlayException.AppRemoved(7),
            GooglePlayException.NotFound(404),
            GooglePlayException.Unknown(-1)
        ).forEach { assertEquals(Failure.NotOffered, it.playUpdateFailure()) }
    }

    @Test
    fun `tells a Google Play failure from a release it does not offer`() {
        listOf(
            GooglePlayException.Unknown(429, "HTTP 429"),
            GooglePlayException.Server(503, "Service Unavailable"),
            GooglePlayException.AuthException(401, "Token expired")
        ).forEach { assertEquals(Failure.PlayUnavailable, it.playUpdateFailure()) }
    }

    @Test
    fun `tells a lost connection from a Google Play failure`() {
        assertEquals(Failure.PlayUnreachable, UnknownHostException().playUpdateFailure())
        assertEquals(Failure.PlayUnreachable, IOException().playLookupFailure())
    }

    @Test
    fun `keeps the failure the app already named`() {
        val signIn = FailureException(Failure.SignInLimited)

        assertEquals(Failure.SignInLimited, signIn.playUpdateFailure())
        assertEquals(Failure.SignInLimited, signIn.playLookupFailure())
    }

    @Test
    fun `says an update failed when nothing names the cause`() {
        assertEquals(Failure.NotInstalled, IllegalStateException().playUpdateFailure())
        assertEquals(Failure.PlayUnavailable, IllegalStateException().playLookupFailure())
        assertEquals(Failure.PlayUnavailable, GooglePlayException.NotFound(404).playLookupFailure())
    }
}
