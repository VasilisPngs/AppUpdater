package com.vasilispngs.appupdater.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleasesTest {
    @Test
    fun `treats plain versions as stable`() {
        listOf("144.0", "v2.0.0", "26.39.33 (260400-995470952)", "build-985-bundle-play", "").forEach {
            assertTrue(it, isStableRelease(it))
        }
    }

    @Test
    fun `treats the RC in Google's build names as stable`() {
        listOf("20261013_00_RC00", "carrierservices.android_20250826_00_RC00.phone").forEach {
            assertTrue(it, isStableRelease(it))
        }
    }

    @Test
    fun `treats marked versions as pre-releases`() {
        listOf(
            "2.0-beta1",
            "12.36.0-beta.01",
            "121.0.6167.0 Canary",
            "5.0-alpha",
            "3.1-dev",
            "8.0 Preview",
            "243.0.1001198216-publicbeta",
            "phone-openbeta_dynamic"
        ).forEach {
            assertFalse(it, isStableRelease(it))
        }
    }

    @Test
    fun `ignores markers inside other words`() {
        listOf("12.1 Arcade", "3.0 Developer edition", "4.2-alphabet").forEach {
            assertTrue(it, isStableRelease(it))
        }
    }
}
