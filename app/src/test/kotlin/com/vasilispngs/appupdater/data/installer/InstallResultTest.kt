package com.vasilispngs.appupdater.data.installer

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.icu.util.ULocale
import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.model.FailureException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

        assertEquals(Failure.MissingLibrary("com.google.android.trichromelibrary_744404833"), failure(status))
    }

    @Test
    fun `names the cause of a failed install as the system installer does`() {
        mapOf(
            PackageInstaller.STATUS_FAILURE_BLOCKED to Failure.Blocked,
            PackageInstaller.STATUS_FAILURE_CONFLICT to Failure.Conflict,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE to Failure.Incompatible,
            PackageInstaller.STATUS_FAILURE_INVALID to Failure.Invalid,
            PackageInstaller.STATUS_FAILURE_STORAGE to Failure.Storage
        ).forEach { (code, failure) -> assertEquals(failure, failure(status(code))) }
    }

    @Test
    fun `says the app was not installed instead of the system's debugging text`() {
        val status = status(PackageInstaller.STATUS_FAILURE_ABORTED)
            .putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, "Session was abandoned")

        assertEquals(Failure.NotInstalled, failure(status))
        assertEquals(Failure.NotInstalled, failure(status(PackageInstaller.STATUS_FAILURE)))
        assertEquals(Failure.NotInstalled, failure(status(PackageInstaller.STATUS_FAILURE_TIMEOUT)))
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
        assertEquals(Preapproval.Skipped, installer.preapproval(status(PackageInstaller.STATUS_FAILURE_BLOCKED), SESSION))
    }

    @Test
    fun `updates without asking when AppUpdater installed the app last`() {
        shadowOf(context.packageManager).setInstallSourceInfo(PACKAGE, context.packageName, null, null, context.packageName, null, PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED)

        assertTrue(installer.ownsUpdates(PACKAGE))
    }

    @Test
    fun `asks before the download when Google Play owns the app's updates`() {
        shadowOf(context.packageManager).setInstallSourceInfo(PACKAGE, PLAY_STORE, null, null, PLAY_STORE, PLAY_STORE, PackageInstaller.PACKAGE_SOURCE_STORE)

        assertFalse(installer.ownsUpdates(PACKAGE))
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
        assertEquals(ULocale.forLocale(context.resources.configuration.locales[0]), details.locale)
        assertEquals(iconSize, details.icon?.width)
    }

    private fun status(code: Int): Intent = Intent().putExtra(PackageInstaller.EXTRA_STATUS, code)

    private fun failure(status: Intent): Failure? = (installer.installResult(status).exceptionOrNull() as? FailureException)?.failure

    private companion object {
        const val SESSION = 7
        const val PACKAGE = "com.example.app"
        const val LABEL = "Example"
        const val PLAY_STORE = "com.android.vending"
    }
}
