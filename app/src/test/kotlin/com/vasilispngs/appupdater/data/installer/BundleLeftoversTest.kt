package com.vasilispngs.appupdater.data.installer

import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BundleLeftoversTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `removes a bundle that an install killed with its process left in the cache`() = runTest {
        val leftover = File(context.cacheDir, "bundle_${System.nanoTime()}").apply { writeBytes(ByteArray(SIZE)) }
        val installer = BundleInstaller(context, PackageInstallerManager(context), Dispatchers.Unconfined)

        installer.install("content://missing/bundle.apkm".toUri(), null) {}

        assertFalse(leftover.exists())
    }

    private companion object {
        const val SIZE = 1024
    }
}
