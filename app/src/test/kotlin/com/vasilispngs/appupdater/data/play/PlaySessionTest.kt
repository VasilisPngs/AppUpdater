package com.vasilispngs.appupdater.data.play

import com.aurora.gplayapi.PlayException
import com.aurora.gplayapi.device.DeviceRegistry
import com.aurora.gplayapi.net.HttpEngine
import com.aurora.gplayapi.net.HttpResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaySessionTest {
    @Test
    fun `checks in again after Google Play rejects the session`() = runTest {
        var checkins = 0
        val engine = HttpEngine { request ->
            when {
                request.url.endsWith("/checkin") -> HttpResponse(200, ByteArray(0)).also { checkins++ }
                request.url.endsWith("/uploadDeviceConfig") -> HttpResponse(200, ByteArray(0))
                else -> HttpResponse(401, ByteArray(0))
            }
        }
        val catalog = PlayCatalog(lazyOf(engine), lazyOf(DeviceRegistry.default))

        repeat(2) { assertTrue(runCatching { catalog.details(PACKAGE) }.exceptionOrNull() is PlayException.Unauthorized) }

        assertEquals(2, checkins)
    }

    private companion object {
        const val PACKAGE = "com.example.app"
    }
}
