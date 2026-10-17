package com.android.appupdater.data

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReleasesTest {
    @Test
    fun `treats plain versions as stable`() {
        listOf("144.0", "v2.0.0", "26.39.33 (260400-995470952)", "build-985-bundle-play", "").forEach {
            assertTrue(isStableRelease(it), it)
        }
    }

    @Test
    fun `treats marked versions as pre-releases`() {
        listOf("2.0-beta1", "1.0.0-rc2", "121.0.6167.0 Canary", "5.0-alpha", "3.1-dev", "8.0 Preview").forEach {
            assertFalse(isStableRelease(it), it)
        }
    }

    @Test
    fun `ignores markers inside other words`() {
        listOf("12.1 Arcade", "3.0 Developer edition", "4.2-alphabet").forEach {
            assertTrue(isStableRelease(it), it)
        }
    }
}
