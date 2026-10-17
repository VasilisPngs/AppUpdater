package com.android.appupdater.data.installer

import android.content.Intent
import android.content.pm.PackageInstaller
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InstallResultTest {
    private val installer = PackageInstallerManager(RuntimeEnvironment.getApplication())

    @Test
    fun `names the package a session installed`() {
        val status = status(PackageInstaller.STATUS_SUCCESS).putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, "com.android.chrome")

        assertEquals("com.android.chrome", installer.installResult(status).getOrThrow())
    }

    @Test
    fun `asks to install a missing shared library first`() {
        val status = status(PackageInstaller.STATUS_FAILURE_INCOMPATIBLE).putExtra(
            PackageInstaller.EXTRA_STATUS_MESSAGE,
            "INSTALL_FAILED_MISSING_SHARED_LIBRARY: Package com.android.chrome requires unavailable shared library " +
                "com.google.android.trichromelibrary_744404833; failing!"
        )

        assertEquals(
            "Requires the shared library com.google.android.trichromelibrary_744404833, install it first.",
            installer.installResult(status).exceptionOrNull()?.message
        )
    }

    @Test
    fun `keeps the reason the installer gave`() {
        val status = status(PackageInstaller.STATUS_FAILURE_ABORTED)
            .putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, "Session was abandoned")

        assertEquals("Session was abandoned", installer.installResult(status).exceptionOrNull()?.message)
    }

    @Test
    fun `says installation failed without a reason`() {
        assertEquals(
            "Installation failed",
            installer.installResult(status(PackageInstaller.STATUS_FAILURE)).exceptionOrNull()?.message
        )
    }

    private fun status(code: Int): Intent = Intent().putExtra(PackageInstaller.EXTRA_STATUS, code)
}
