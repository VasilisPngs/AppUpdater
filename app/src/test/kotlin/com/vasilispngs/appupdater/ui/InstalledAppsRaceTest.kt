package com.vasilispngs.appupdater.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowApplicationPackageManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [InstalledAppsRaceTest.ParkedPackageQuery::class])
class InstalledAppsRaceTest {
    private val context = RuntimeEnvironment.getApplication()
    private val packageManager = context.packageManager

    @Test
    fun `keeps a package update that lands while the installed apps are queried`() {
        ParkedPackageQuery.parked = CountDownLatch(1)
        ParkedPackageQuery.release = CountDownLatch(1)
        shadowOf(packageManager).getInternalMutablePackageInfo(context.packageName).applicationInfo!!.enabled = false
        install(versionCode = 1)
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(
            store,
            AppUpdaterViewModel.Factory,
            MutableCreationExtras().apply { set(APPLICATION_KEY, context) }
        )[AppUpdaterViewModel::class.java]
        val collector = MainScope().launch { viewModel.uiState.collect {} }

        assertTrue(idleUntil { ParkedPackageQuery.parked.count == 0L })
        install(versionCode = 2)
        context.sendBroadcast(Intent(Intent.ACTION_PACKAGE_REPLACED, Uri.fromParts("package", PACKAGE, null)))
        idleUntil(timeoutMillis = 500) { viewModel.versionCode() == 2L }
        ParkedPackageQuery.release.countDown()
        idleUntil { !viewModel.uiState.value.scanning }
        idleUntil(timeoutMillis = 2_000) { viewModel.versionCode() == 2L }

        assertEquals(2L, viewModel.versionCode())
        collector.cancel()
        store.clear()
    }

    private fun AppUpdaterViewModel.versionCode(): Long? =
        uiState.value.installedApps.singleOrNull { it.packageName == PACKAGE }?.versionCode

    private fun install(versionCode: Long) {
        shadowOf(packageManager).installPackage(
            PackageInfo().apply {
                packageName = PACKAGE
                longVersionCode = versionCode
                versionName = "$versionCode.0"
                applicationInfo = ApplicationInfo().apply {
                    packageName = PACKAGE
                    enabled = false
                }
            }
        )
    }

    private fun idleUntil(timeoutMillis: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return true
            Thread.sleep(5)
        }
        shadowOf(Looper.getMainLooper()).idle()
        return condition()
    }

    @Implements(className = "android.app.ApplicationPackageManager", isInAndroidSdk = false)
    class ParkedPackageQuery : ShadowApplicationPackageManager() {
        @Implementation(minSdk = 33)
        protected fun getInstalledPackages(flags: PackageManager.PackageInfoFlags): List<PackageInfo> {
            val snapshot = super.getInstalledPackages(flags as Any)
            parked.countDown()
            check(release.await(10, TimeUnit.SECONDS))
            return snapshot
        }

        companion object {
            var parked = CountDownLatch(1)
            var release = CountDownLatch(1)
        }
    }

    private companion object {
        const val PACKAGE = "com.example.updated"
    }
}
