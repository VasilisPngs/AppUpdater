package com.android.appupdater.data.installer

import android.Manifest
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.NetworkCapabilities
import com.android.appupdater.data.installer.PlayUpdateService.Companion.toExtras
import com.android.appupdater.data.installer.PlayUpdateService.Companion.toPlayUpdate
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.PlayInstall
import com.android.appupdater.data.model.PlayUpdate
import com.android.appupdater.data.model.PlayVersion
import com.android.appupdater.data.play.PlayAuthProvider
import com.android.appupdater.data.play.PlayCatalog
import com.android.appupdater.data.play.PlayHttpClient
import kotlinx.coroutines.MainScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PlayUpdateJobTest {
    private val context = RuntimeEnvironment.getApplication()
    private val scheduler = context.getSystemService(JobScheduler::class.java)

    @Test
    fun `runs a Google Play update as a user-initiated data transfer job`() {
        val installs = repository()
        installs.updateFromPlay(update(PlayVersion("2.1", 21)))

        val job = scheduler.allPendingJobs.single()
        assertTrue(job.isUserInitiated)
        assertTrue(job.requiredNetwork?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        assertEquals(ComponentName(context, PlayUpdateService::class.java), job.service)
        assertEquals(PlayUpdate(PACKAGE, NAME, "2.1", 21, latest = false), job.extras.toPlayUpdate())
        assertEquals(mapOf(PACKAGE to PlayInstall(null)), installs.play.value)
    }

    @Test
    fun `holds the network state permission that a job with a network constraint needs`() {
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            .requestedPermissions

        assertTrue(Manifest.permission.ACCESS_NETWORK_STATE in requested.orEmpty())
    }

    @Test
    fun `knows when Google Play delivers the newest version`() {
        repository().updateFromPlay(update(PlayVersion("3.0", NEWEST)))

        assertTrue(scheduler.allPendingJobs.single().extras.toPlayUpdate().latest)
    }

    @Test
    fun `schedules an update only once while it runs`() {
        val installs = repository()
        installs.updateFromPlay(update(PlayVersion("3.0", NEWEST)))
        installs.updateFromPlay(update(PlayVersion("3.0", NEWEST)))

        assertEquals(1, scheduler.allPendingJobs.size)
    }

    @Test
    fun `keeps every detail of the update in the job`() {
        val update = PlayUpdate(PACKAGE, NAME, "2.1", 21, latest = true)

        assertEquals(update, update.toExtras().toPlayUpdate())
    }

    private fun repository(): InstallRepository {
        val http = PlayHttpClient(context.packageName)
        val packageInstaller = PackageInstallerManager(context)
        return InstallRepository(
            context,
            MainScope(),
            BundleInstaller(context, packageInstaller),
            PlayInstaller(context, PlayCatalog(PlayAuthProvider(context, http)), http, packageInstaller),
            packageInstaller
        )
    }

    private fun update(play: PlayVersion) = AppUpdateInfo(
        packageName = PACKAGE,
        appName = NAME,
        newVersionName = "3.0",
        newVersionCode = NEWEST,
        publishedAt = null,
        apkMirrorUrl = null,
        playAvailable = true,
        playUpdate = play
    )

    private companion object {
        const val PACKAGE = "com.example.app"
        const val NAME = "Example"
        const val NEWEST = 30L
    }
}
