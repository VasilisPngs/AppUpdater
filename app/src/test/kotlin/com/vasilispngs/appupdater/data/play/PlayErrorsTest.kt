package com.vasilispngs.appupdater.data.play

import com.aurora.gplayapi.PlayException
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
            PlayException.AppNotPurchased(403),
            PlayException.AppNotSupported(2),
            PlayException.AppRemoved(7),
            PlayException.NotFound(404),
            PlayException.NoFilesAvailable(-1),
            PlayException.Unknown(5)
        ).forEach { assertEquals(Failure.NotOffered, it.playUpdateFailure()) }
    }

    @Test
    fun `tells a Google Play failure from a release it does not offer`() {
        listOf(
            PlayException.RateLimited(),
            PlayException.ServerError(503),
            PlayException.Unauthorized(401),
            PlayException.Unknown(-1, "Malformed response", IllegalArgumentException())
        ).forEach { assertEquals(Failure.PlayUnavailable, it.playUpdateFailure()) }
    }

    @Test
    fun `tells a lost connection from a Google Play failure`() {
        assertEquals(Failure.PlayUnreachable, UnknownHostException().playUpdateFailure())
        assertEquals(Failure.PlayUnreachable, IOException().playLookupFailure())
    }

    @Test
    fun `keeps the failure the app already named`() {
        val unverified = FailureException(Failure.Unverified)

        assertEquals(Failure.Unverified, unverified.playUpdateFailure())
        assertEquals(Failure.Unverified, unverified.playLookupFailure())
    }

    @Test
    fun `says an update failed when nothing names the cause`() {
        assertEquals(Failure.NotInstalled, IllegalStateException().playUpdateFailure())
        assertEquals(Failure.PlayUnavailable, IllegalStateException().playLookupFailure())
        assertEquals(Failure.PlayUnavailable, PlayException.NotFound(404).playLookupFailure())
    }
}
