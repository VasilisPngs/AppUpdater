package com.android.appupdater.data.play

import com.aurora.gplayapi.exceptions.GooglePlayException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayErrorsTest {
    @Test
    fun `says Google Play does not offer a release it withholds`() {
        listOf(
            GooglePlayException.AppNotPurchased(403),
            GooglePlayException.AppNotSupported(2),
            GooglePlayException.AppRemoved(7),
            GooglePlayException.NotFound(404),
            GooglePlayException.Unknown(-1)
        ).forEach {
            assertEquals("Google Play does not offer $PACKAGE $VERSION.", it.deliveryFailure(PACKAGE, VERSION).message)
        }
    }

    @Test
    fun `keeps the reason of a failure that is not about the release`() {
        assertEquals(
            "Google Play: HTTP 429",
            GooglePlayException.Unknown(429, "HTTP 429").deliveryFailure(PACKAGE, VERSION).message
        )
        assertEquals(
            "Google Play: Service Unavailable",
            GooglePlayException.Server(503, "Service Unavailable").deliveryFailure(PACKAGE, VERSION).message
        )
        assertEquals(
            "Google Play: Token expired",
            GooglePlayException.AuthException(401, "Token expired").deliveryFailure(PACKAGE, VERSION).message
        )
    }

    private companion object {
        const val PACKAGE = "com.example.app"
        const val VERSION = 42L
    }
}
