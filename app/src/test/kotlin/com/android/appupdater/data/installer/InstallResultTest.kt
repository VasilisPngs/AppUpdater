package com.android.appupdater.data.installer

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.icu.util.ULocale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class InstallResultTest {
    private val context = RuntimeEnvironment.getApplication()
    private val installer = PackageInstallerManager(context)

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

    @Test
    fun `keeps the session the user approved before the download`() {
        assertEquals(Preapproval.Approved(SESSION), installer.preapproval(status(PackageInstaller.STATUS_SUCCESS), SESSION))
    }

    @Test
    fun `stops when the user declines the update`() {
        assertEquals(Preapproval.Declined, installer.preapproval(status(PackageInstaller.STATUS_FAILURE_ABORTED), SESSION))
    }

    @Test
    fun `asks after the download when the system cannot ask before it`() {
        assertEquals(Preapproval.Unavailable, installer.preapproval(status(PackageInstaller.STATUS_FAILURE_BLOCKED), SESSION))
    }

    @Test
    fun `asks for the update with the name and icon the installed app has`() {
        val info = ApplicationInfo().apply {
            packageName = PACKAGE
            nonLocalizedLabel = LABEL
        }
        shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            packageName = PACKAGE
            applicationInfo = info
        })

        val details = installer.preapprovalDetails(PACKAGE)
        val installed = context.packageManager.getApplicationInfo(PACKAGE, PackageManager.ApplicationInfoFlags.of(0))
        val iconSize = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)

        assertEquals(PACKAGE, details.packageName)
        assertEquals(context.packageManager.getApplicationLabel(installed).toString(), details.label.toString())
        assertEquals(LABEL, details.label.toString())
        assertEquals(ULocale.forLocale(context.resources.configuration.locales[0]), details.locale)
        assertEquals(iconSize, details.icon?.width)
    }

    private fun status(code: Int): Intent = Intent().putExtra(PackageInstaller.EXTRA_STATUS, code)

    private companion object {
        const val SESSION = 7
        const val PACKAGE = "com.example.app"
        const val LABEL = "Example"
    }
}
