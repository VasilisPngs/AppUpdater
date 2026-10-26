package com.vasilispngs.appupdater.data.installer

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.vasilispngs.appupdater.data.model.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class InstalledAppTest {
    private val packageManager = RuntimeEnvironment.getApplication().packageManager

    @Test
    fun `names a newly installed app by its label`() {
        install(firstInstallTime = 1_000, lastUpdateTime = 1_000)

        assertEquals(InstallState.Success("Chrome", updated = false), packageManager.installedApp(PACKAGE, FILE_NAME))
    }

    @Test
    fun `tells when the install updated an app`() {
        install(firstInstallTime = 1_000, lastUpdateTime = 2_000)

        assertEquals(InstallState.Success("Chrome", updated = true), packageManager.installedApp(PACKAGE, FILE_NAME))
    }

    @Test
    fun `falls back to the file name without an installed package`() {
        assertEquals(InstallState.Success(FILE_NAME, updated = false), packageManager.installedApp("", FILE_NAME))
        assertEquals(InstallState.Success(FILE_NAME, updated = false), packageManager.installedApp(PACKAGE, FILE_NAME))
    }

    private fun install(firstInstallTime: Long, lastUpdateTime: Long) {
        shadowOf(packageManager).installPackage(
            PackageInfo().apply {
                packageName = PACKAGE
                applicationInfo = ApplicationInfo().apply {
                    packageName = PACKAGE
                    nonLocalizedLabel = "Chrome"
                }
                this.firstInstallTime = firstInstallTime
                this.lastUpdateTime = lastUpdateTime
            }
        )
    }

    private companion object {
        const val PACKAGE = "com.android.chrome"
        const val FILE_NAME = "com.android.chrome_142.0.7444.48-744404833_apkmirror.com.apkm"
    }
}
