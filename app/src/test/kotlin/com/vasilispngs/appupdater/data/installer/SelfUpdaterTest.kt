package com.vasilispngs.appupdater.data.installer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SelfUpdaterTest {
    @Test
    fun `names a file by its SHA-256 digest as GitHub does`() {
        assertEquals(
            "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256("abc".byteInputStream())
        )
    }

    @Test
    fun `hands every block it reads on`() {
        val copied = StringBuilder()

        sha256("abc".byteInputStream()) { buffer, read -> copied.append(String(buffer, 0, read)) }

        assertEquals("abc", copied.toString())
    }

    @Test
    fun `confirms a self-update only when the installed APK matches the expected digest`() {
        val context = RuntimeEnvironment.getApplication()
        val source = File(context.cacheDir, "installed.apk").apply { writeText("installed-apk") }
        context.applicationInfo.sourceDir = source.path
        val marker = File(context.noBackupFilesDir, UPDATED_MARKER)

        marker.writeText("sha256:stale")
        assertFalse(context.consumeSelfUpdate())
        assertFalse(marker.exists())

        marker.writeText(source.inputStream().use(::sha256))
        assertTrue(context.consumeSelfUpdate())
        assertFalse(marker.exists())
    }
}
